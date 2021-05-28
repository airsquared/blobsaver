/*
 * Copyright (c) 2021  airsquared
 *
 * This file is part of blobsaver.
 *
 * blobsaver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * blobsaver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with blobsaver.  If not, see <https://www.gnu.org/licenses/>.
 */

package airsquared.blobsaver.app;

import airsquared.blobsaver.app.natives.Libirecovery;
import airsquared.blobsaver.app.natives.Libplist;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;

import java.nio.ByteOrder;
import java.util.Objects;

import static airsquared.blobsaver.app.natives.Libimobiledevice.*;


public class LibimobiledeviceUtil {

    public static long getECID() throws LibimobiledeviceException {
        return Long.parseLong(getKeyFromConnectedDevice("UniqueChipID", PlistType.INTEGER));
    }

    public static String getDeviceModelIdentifier() throws LibimobiledeviceException {
        return getKeyFromConnectedDevice("ProductType", PlistType.STRING);
    }

    public static String getBoardConfig() throws LibimobiledeviceException {
        return getKeyFromConnectedDevice("HardwareModel", PlistType.STRING);
    }

    public static String getApNonceNormalMode() throws LibimobiledeviceException {
        return LibimobiledeviceUtil.plistDataToString(LibimobiledeviceUtil.getMobileGestaltKey("ApNonce"), ByteOrder.BIG_ENDIAN);
    }

    public static String getGenerator() throws LibimobiledeviceException {
        return "0x" + LibimobiledeviceUtil.plistDataToString(LibimobiledeviceUtil.getMobileGestaltKey("BootNonce"), ByteOrder.LITTLE_ENDIAN);
    }

    public static void exitRecovery(Pointer irecvClient) throws LibimobiledeviceException {
        throwIfNeeded(Libirecovery.setEnv(irecvClient, "auto-boot", "true"), ErrorCodeType.irecv_error);
        throwIfNeeded(Libirecovery.saveEnv(irecvClient), ErrorCodeType.irecv_error);
        throwIfNeeded(Libirecovery.reboot(irecvClient), ErrorCodeType.irecv_error);
        throwIfNeeded(Libirecovery.close(irecvClient), ErrorCodeType.irecv_error);
    }

    public static final class GetApnonceTask extends Task<Void> {
        private String apnonceResult, generatorResult;
        /**
         * If the device is jailbroken or has a generator set, no need to try to read the apnonce in normal mode.
         * If device doesn't have generator set, need to read the apnonce in normal mode to freeze it.
         */
        private final boolean jailbroken;

        public String getApnonceResult() {
            return apnonceResult;
        }

        public String getGeneratorResult() {
            return generatorResult;
        }

        public GetApnonceTask(boolean jailbroken) {
            this.jailbroken = jailbroken;
        }

        @Override
        protected Void call() throws LibimobiledeviceException {
            if (!jailbroken) {
                updateMessage("Reading APNonce in normal mode...");
                System.out.println("Read in normal mode: " + LibimobiledeviceUtil.getApNonceNormalMode());
            }
            updateMessage("Entering recovery mode...\n\nThis can take up to 60 seconds");
            LibimobiledeviceUtil.enterRecovery();
            PointerByReference irecvClient = waitForRecovery();
            if (irecvClient == null) return null;

            updateMessage("Reading APNonce...");
            apnonceResult = getApnonce(irecvClient.getValue());
            Libirecovery.sendCommand(irecvClient.getValue(), "reset");

            throwIfNeeded(Libirecovery.close(irecvClient.getValue()), ErrorCodeType.irecv_error);
            irecvClient = waitForRecovery();
            if (irecvClient == null) return null;

            if (apnonceResult.equals(getApnonce(irecvClient.getValue()))) {
                Utils.runSafe(() -> updateMessage("Successfully got APNonce, exiting recovery mode..."));
            } else {
                Utils.runSafe(() -> updateMessage("Warning: Got APNonce, but two successive reads didn't match. This could mean the generator wasn't set correctly.\n\nExiting recovery mode..."));
            }

            LibimobiledeviceUtil.exitRecovery(irecvClient.getValue());

            sleep(3000);

            PointerByReference device = new PointerByReference();
            long endTime = System.currentTimeMillis() + 80_000; // timeout is 80 seconds
            int errorCode = -3;
            while (errorCode == -3 && System.currentTimeMillis() < endTime) {
                if (!sleep(1000)) {
                    return null;
                }
                errorCode = idevice_new(device, Pointer.NULL);
            }
            throwIfNeeded(errorCode, ErrorCodeType.idevice_error);

            updateMessage("Please unlock your device");

            PointerByReference lockdown = new PointerByReference();
            endTime = System.currentTimeMillis() + 120_000; // timeout is 120 seconds
            errorCode = -17;
            while (errorCode == -17 && System.currentTimeMillis() < endTime) {
                if (!sleep(1000)) {
                    return null;
                }
                errorCode = lockdownd_client_new_with_handshake(device.getValue(), lockdown, "blobsaver");
            }
            throwIfNeeded(errorCode, ErrorCodeType.lockdownd_error);

            PointerByReference service = new PointerByReference();
            // don't reset timeout
            errorCode = -17;
            while (errorCode == -17 && System.currentTimeMillis() < endTime) {
                if (!sleep(1000)) {
                    return null;
                }
                errorCode = lockdownd_start_service(lockdown.getValue(), "com.apple.mobile.diagnostics_relay", service);
            }
            if (errorCode == 0) {
                lockdownd_service_descriptor_free(service.getValue());
            }
            lockdownd_client_free(lockdown.getValue());
            idevice_free(device.getValue());

            generatorResult = getGenerator();

            updateMessage("Success");

            Analytics.readAPNonce(jailbroken);
            return null;
        }

    }

    /**
     * @return false if task was cancelled during the Thread.sleep
     */
    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    private static PointerByReference waitForRecovery() throws LibimobiledeviceException {
        PointerByReference irecvClient = new PointerByReference();
        long endTime = System.currentTimeMillis() + 60_000; // timeout is 60 seconds
        int errorCode = -3;
        while (errorCode == -3 && System.currentTimeMillis() < endTime) {
            if (!sleep(1000)) {
                return null;
            }
            try {
                errorCode = Libirecovery.open(irecvClient);
            } catch (Error e) {
                if (Platform.isLinux()) {
                    throw new LibimobiledeviceException("Unable to load native libraries. Ensure you have libirecovery installed and as libirecovery.so. If it is installed under a different name, try creating a symlink.", null, 0, false);
                } else {
                    throw e;
                }
            }
        }
        throwIfNeeded(errorCode, ErrorCodeType.irecv_error);
        return irecvClient;
    }

    private static String getApnonce(Pointer irecv_client) {
        Libirecovery.irecv_device_info deviceInfo = Libirecovery.getDeviceInfo(irecv_client);
        byte[] apnonceBytes = deviceInfo.ap_nonce.getByteArray(0, deviceInfo.ap_nonce_size);

        return Utils.bytesToHex(apnonceBytes, ByteOrder.BIG_ENDIAN);
    }

    private static String getKeyFromConnectedDevice(String key, PlistType plistType) throws LibimobiledeviceException {
        if (plistType == null) {
            plistType = PlistType.STRING;
        }
        if (key == null) {
            key = "";
        }

        Pointer client = lockdowndClientFromConnectedDevice();
        PointerByReference plist_value = new PointerByReference();
        int lockdowndGetValueErrorCode = lockdownd_get_value(client, Pointer.NULL, key, plist_value);
        if (lockdowndGetValueErrorCode == -8) {
            // try again, and if it doesn't work, show an error to the user + throw an exception
            // it always works the second time
            lockdownd_client_free(client);
            client = lockdowndClientFromConnectedDevice();
            throwIfNeeded(lockdownd_get_value(client, Pointer.NULL, key, plist_value), ErrorCodeType.lockdownd_error);
        } else {
            throwIfNeeded(lockdowndGetValueErrorCode, ErrorCodeType.lockdownd_error);
        }
        lockdownd_client_free(client);
        if (plistType.equals(PlistType.INTEGER)) {
            PointerByReference xml_doc = new PointerByReference();
            Libplist.toXml(plist_value.getValue(), xml_doc, new PointerByReference());
            Libplist.free(plist_value.getValue());
            String toReturn = xml_doc.getValue().getString(0, "UTF-8");
            return toReturn.substring(toReturn.indexOf("<integer>") + "<integer>".length(), toReturn.indexOf("</integer>"));
        } else {
            PointerByReference toReturn = new PointerByReference();
            Libplist.getStringVal(plist_value.getValue(), toReturn);
            Libplist.free(plist_value.getValue());
            return toReturn.getValue().getString(0, "UTF-8");
        }
    }

    /**
     * Returns a plist with the root element being the requested item.
     * <p>
     * It will look something like this:
     * <pre>
     * {@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
     * <plist version="1.0">
     * <data>
     * 8eNq5Eeej3vhqkA8SG9r4tWfbLPaRRtrhTefJ44Y7LE=
     * </data>
     * </plist>
     * }
     * </pre>
     */
    private static Pointer getMobileGestaltKey(String key) throws LibimobiledeviceException {
        PointerByReference device = new PointerByReference();
        throwIfNeeded(idevice_new(device, Pointer.NULL), ErrorCodeType.idevice_error);
        PointerByReference client = new PointerByReference();
        throwIfNeeded(lockdownd_client_new_with_handshake(device.getValue(), client, "blobsaver"), ErrorCodeType.lockdownd_error);
        PointerByReference service = new PointerByReference();
        throwIfNeeded(lockdownd_start_service(client.getValue(), "com.apple.mobile.diagnostics_relay", service), ErrorCodeType.lockdownd_error);
        lockdownd_client_free(client.getValue());

        PointerByReference diagnosticsRelayClient = new PointerByReference();
        throwIfNeeded(diagnostics_relay_client_new(device.getValue(), service.getValue(), diagnosticsRelayClient), ErrorCodeType.diagnostics_relay_error);

        Pointer keys = Libplist.newArray();
        Libplist.arrayAppendItem(keys, Libplist.newString(key));
        PointerByReference plistResult = new PointerByReference();
        throwIfNeeded(diagnostics_relay_query_mobilegestalt(diagnosticsRelayClient.getValue(), keys, plistResult), ErrorCodeType.diagnostics_relay_error);
        Pointer plistApnonce = Libplist.dictGetItem(Libplist.dictGetItem(plistResult.getValue(), "MobileGestalt"), key);

        diagnostics_relay_goodbye(diagnosticsRelayClient.getValue());
        diagnostics_relay_client_free(diagnosticsRelayClient.getValue());
        lockdownd_service_descriptor_free(service.getValue());
        idevice_free(device.getValue());
        Libplist.free(keys);

        return plistApnonce;
    }

    private static String plistDataToString(Pointer plist, ByteOrder byteOrder) {
        IntByReference apnonceLength = new IntByReference();
        byte[] apnonceBytes = Libplist.getDataPtr(plist, apnonceLength).getByteArray(0, apnonceLength.getValue());
        String toReturn = Utils.bytesToHex(apnonceBytes, byteOrder);
        Libplist.free(plist);

        return toReturn;
    }

    private static Pointer lockdowndClientFromConnectedDevice() throws LibimobiledeviceException {
        PointerByReference device = new PointerByReference();
        throwIfNeeded(idevice_new(device, Pointer.NULL), ErrorCodeType.idevice_error);
        PointerByReference client = new PointerByReference();
        if (lockdownd_client_new_with_handshake(device.getValue(), client, "blobsaver") != 0) {
            // try again, and if it doesn't work, show an error to the user + throw an exception
            throwIfNeeded(lockdownd_client_new_with_handshake(device.getValue(), client, "blobsaver"), ErrorCodeType.lockdownd_error);
        }
        idevice_free(device.getValue());
        return client.getValue();
    }

    static void enterRecovery() throws LibimobiledeviceException {
        Pointer client = lockdowndClientFromConnectedDevice();
        throwIfNeeded(lockdownd_enter_recovery(client), ErrorCodeType.lockdownd_error);
        lockdownd_client_free(client);
    }

    private enum PlistType {
        STRING, INTEGER
    }

    private enum ErrorCodeType {
        idevice_error, lockdownd_error, irecv_error, diagnostics_relay_error
    }

    private static void throwIfNeeded(int errorCode, ErrorCodeType errorType) throws LibimobiledeviceException {
        if (errorCode == 0) {
            return;
        }
        Objects.requireNonNull(errorType);

        String message = "";
        boolean reportableError = false;
        if (errorType.equals(ErrorCodeType.idevice_error)) {
            if (errorCode == -3) { // IDEVICE_E_NO_DEVICE
                message = "No devices found/connected. Make sure your device is connected via USB and unlocked.";
            } else {
                message = "idevice error: code=" + errorCode;
                reportableError = true;
            }
        } else if (errorType.equals(ErrorCodeType.lockdownd_error)) {
            message = switch (errorCode) {
                case -17:  // LOCKDOWN_E_PASSWORD_PROTECTED
                    yield "The device is locked.\n\nPlease unlock your device and go to the homescreen then try again.\n\nLOCKDOWN_E_PASSWORD_PROTECTED (-17)";
                case -18:  // LOCKDOWN_E_USER_DENIED_PAIRING
                    yield "The user denied the pair request on the device. Please unplug your device and accept the dialog next time.\n\nLOCKDOWN_E_USER_DENIED_PAIRING (-18)";
                case -19:  // LOCKDOWN_E_PAIRING_DIALOG_RESPONSE_PENDING
                    yield "Please accept the trust/pair dialog on the device and try again.\n\nLOCKDOWN_E_PAIRING_DIALOG_RESPONSE_PENDING (-19)";
                case -21: // LOCKDOWN_E_INVALID_HOST_ID
                    yield "Try restarting both your iOS device and computer. If that doesn't work, try again with your device open and trusted in Finder/iTunes.\n\nLOCKDOWN_E_INVALID_HOST_ID (-21)";
                case -8:  // LOCKDOWN_E_MUX_ERROR
                    reportableError = true;
                    yield "lockdownd error: LOCKDOWN_E_MUX_ERROR (-8)";
                default:
                    reportableError = true;
                    yield "lockdownd error: code=" + errorCode;
            };
        } else if (errorType.equals(ErrorCodeType.irecv_error)) {
            message = "irecovery error: code=" + errorCode +
                    "\n\nIf your device is still in recovery mode, use the \"Exit Recovery Mode\" option from the help menu.";
            reportableError = true;
        } else if (errorType.equals(ErrorCodeType.diagnostics_relay_error)) {
            message = "Diagnostics Relay error: code=" + errorCode;
            reportableError = true;
        }
        throw new LibimobiledeviceException(message, errorType, errorCode, reportableError);
    }

    public static class LibimobiledeviceException extends Exception {

        public final ErrorCodeType errorType;
        public final int errorCode;
        public final boolean reportable;

        private LibimobiledeviceException(String message, ErrorCodeType errorType, int errorCode, boolean reportable) {
            super(message);
            this.errorType = errorType;
            this.errorCode = errorCode;
            this.reportable = reportable;
        }

        public void showErrorAlert() {
            String message = getMessage();
            if (reportable) {
                Utils.showReportableError(message);
            } else if (ErrorCodeType.idevice_error.equals(errorType) && errorCode == -3 && Platform.isWindows()) {
                message += "\n\nEnsure iTunes or Apple's iOS Drivers are installed.";
                ButtonType downloadItunes = new ButtonType("Download iTunes");
                if (downloadItunes.equals(Utils.showUnreportableError(message, downloadItunes))) {
                    Utils.openURL("https://www.apple.com/itunes/download/win64");
                }
            } else {
                Utils.showUnreportableError(message);
            }

        }
    }

}
