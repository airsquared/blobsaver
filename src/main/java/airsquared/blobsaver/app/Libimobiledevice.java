/*
 * Copyright (c) 2020  airsquared
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

import com.sun.javafx.PlatformUtil;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * This class provides access to functions in the native library libimobiledevice.
 * <p>
 * See the libimobiledevice docs for
 * <a href="https://www.libimobiledevice.org/docs/html/lockdown_8h.html">lockdown.h</a>
 * and
 * <a href="https://www.libimobiledevice.org/docs/html/libimobiledevice_8h.html">libimobiledevice.h</a>
 * for information on the native functions. For information on the libplist functions,
 * look at the source code(files plist.c and xplist.c).
 *
 * @author airsquared
 */
@SuppressWarnings("WeakerAccess")
public class Libimobiledevice {

    public static long getECID(boolean showErrorAlert) {
        return Long.parseLong(getKeyFromConnectedDevice("UniqueChipID", PlistType.INTEGER, showErrorAlert));
    }

    public static String getDeviceModelIdentifier(boolean showErrorAlert) {
        return getKeyFromConnectedDevice("ProductType", PlistType.STRING, showErrorAlert);
    }

    public static String getBoardConfig(boolean showErrorAlert) {
        return getKeyFromConnectedDevice("HardwareModel", PlistType.STRING, showErrorAlert);
    }

    public static String getKeyFromConnectedDevice(String key, PlistType plistType, boolean showErrorAlert) {
        if (plistType == null) {
            plistType = PlistType.STRING;
        }
        if (key == null) {
            key = "";
        }

        Pointer client = lockdowndClientFromConnectedDevice(showErrorAlert);
        PointerByReference plist_value = new PointerByReference();
        int lockdowndGetValueErrorCode = lockdownd_get_value(client, Pointer.NULL, key, plist_value);
        if (lockdowndGetValueErrorCode == -8) {
            // try again, and if it doesn't work, show an error to the user + throw an exception
            // it always works the second time
            lockdownd_client_free(client);
            client = lockdowndClientFromConnectedDevice(showErrorAlert);
            throwIfNeeded(lockdownd_get_value(client, Pointer.NULL, key, plist_value), showErrorAlert, ErrorCodeType.lockdownd_error_t);
        } else {
            throwIfNeeded(lockdowndGetValueErrorCode, showErrorAlert, ErrorCodeType.lockdownd_error_t);
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

    public static Pointer lockdowndClientFromConnectedDevice(boolean showErrorAlert) {
        PointerByReference device = new PointerByReference();
        throwIfNeeded(idevice_new(device, Pointer.NULL), showErrorAlert, ErrorCodeType.idevice_error_t);
        PointerByReference client = new PointerByReference();
        throwIfNeeded(lockdownd_client_new(device.getValue(), client, "blobsaver"), showErrorAlert, ErrorCodeType.lockdownd_error_t);
        if (lockdownd_pair(client.getValue(), Pointer.NULL) != 0) {
            // try again, and if it doesn't work, show an error to the user + throw an exception
            throwIfNeeded(lockdownd_pair(client.getValue(), Pointer.NULL), showErrorAlert, ErrorCodeType.lockdownd_error_t);
        }
        idevice_free(device.getValue());
        return client.getValue();
    }

    public static void enterRecovery(boolean showErrorAlert) {
        Pointer client = lockdowndClientFromConnectedDevice(true);
        throwIfNeeded(lockdownd_enter_recovery(client), showErrorAlert, ErrorCodeType.lockdownd_error_t);
        lockdownd_client_free(client);
    }

    public static void exitRecovery(Pointer irecvClient, boolean showErrorAlert) {
        throwIfNeeded(Libirecovery.irecv_setenv(irecvClient, "auto-boot", "true"), showErrorAlert, ErrorCodeType.irecv_error_t);
        throwIfNeeded(Libirecovery.irecv_saveenv(irecvClient), showErrorAlert, ErrorCodeType.irecv_error_t);
        throwIfNeeded(Libirecovery.irecv_reboot(irecvClient), showErrorAlert, ErrorCodeType.irecv_error_t);
        throwIfNeeded(Libirecovery.irecv_close(irecvClient), showErrorAlert, ErrorCodeType.irecv_error_t);
    }

    public static Task<String> createGetApnonceTask() {
        return new Task<String>() {
            @Override
            protected String call() {
                updateMessage("Entering recovery mode...\n\nThis can take up to 60 seconds");
                System.out.println("Entering recovery mode");
                Libimobiledevice.enterRecovery(true);
                PointerByReference irecvClient = new PointerByReference();
                long endTime = System.currentTimeMillis() + 60_000; // timeout is 60 seconds
                int errorCode = -3;
                while (errorCode == -3 && System.currentTimeMillis() < endTime) {
                    if (!sleep(1000)) {
                        return null;
                    }
                    errorCode = Libimobiledevice.Libirecovery.irecv_open_with_ecid(irecvClient, 0);
                }
                throwIfNeeded(errorCode, true, Libimobiledevice.ErrorCodeType.irecv_error_t);
                Libirecovery.irecv_device_info deviceInfo = Libimobiledevice.Libirecovery.irecv_get_device_info(irecvClient.getValue());
                final StringBuilder apnonce = new StringBuilder();
                for (byte b : deviceInfo.ap_nonce.getByteArray(0, deviceInfo.ap_nonce_size)) {
                    apnonce.append(String.format("%02x", b));
                }
                System.out.println("Got apnonce");
                updateMessage("Successfully got apnonce, exiting recovery mode...");
                System.out.println("Exiting recovery mode");
                Libimobiledevice.exitRecovery(irecvClient.getValue(), true);
                sleep(3000);
                return apnonce.toString();
            }

            /**
             * @return false if task was cancelled during the Thread.sleep
             */
            private boolean sleep(long millis) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    return false;
                }
                return true;
            }
        };
    }

    enum PlistType {
        STRING, INTEGER
    }

    enum ErrorCodeType {
        idevice_error_t, lockdownd_error_t, irecv_error_t
    }

    public static native int lockdownd_get_value(Pointer client, Pointer domain, String key, PointerByReference value);

    public static native int lockdownd_enter_recovery(Pointer client);

    public static native int idevice_new(PointerByReference device, Pointer udid);

    public static native int lockdownd_client_new(Pointer device, PointerByReference client, String label);

    public static native int lockdownd_pair(Pointer lockdownd_client, Pointer lockdownd_pair_record);

    public static native void lockdownd_client_free(Pointer client);

    public static native void idevice_free(Pointer idevice);

    public static class Libplist {

        public static void getStringVal(Pointer plist, PointerByReference value) {
            plist_get_string_val(plist, value);
        }

        public static void free(Pointer plist) {
            plist_free(plist);
        }

        public static void toXml(Pointer plist, PointerByReference plist_xml, PointerByReference length) {
            plist_to_xml(plist, plist_xml, length);
        }

        private static native void plist_get_string_val(Pointer plist, PointerByReference value);

        private static native void plist_free(Pointer plist);

        private static native void plist_to_xml(Pointer plist, PointerByReference plist_xml, PointerByReference length);

        static {
            Native.register(Libplist.class, "plist");
        }
    }

    public static class Libirecovery {

        public static native int irecv_open_with_ecid(PointerByReference irecv_client, long ecid);

        public static native int irecv_close(Pointer irecv_client);

        public static native int irecv_setenv(Pointer irecv_client, String variable, String value);

        public static native int irecv_saveenv(Pointer irecv_client);

        public static native int irecv_reboot(Pointer irecv_client);

        public static native irecv_device_info irecv_get_device_info(Pointer irecv_client);

        @SuppressWarnings({"unused", "SpellCheckingInspection"})
        @Structure.FieldOrder({"cpid", "cprv", "cpfm", "scep", "bdid", "ecid", "ibfl", "srnm", "imei", "srtg", "serial_string", "ap_nonce", "ap_nonce_size", "sep_nonce", "sep_nonce_size"})
        public static class irecv_device_info extends Structure {
            public int cpid, cprv, cpfm, scep, bdid;
            public long ecid;
            public int ibfl;
            public String srnm, imei, srtg, serial_string;
            public Pointer ap_nonce;
            public int ap_nonce_size;
            public Pointer sep_nonce;
            public int sep_nonce_size;
        }

        static {
            Native.register(Libirecovery.class, "irecovery");
        }
    }

    public static void throwIfNeeded(int errorCode, boolean showAlert, ErrorCodeType errorType) {
        if (errorCode == 0) {
            return;
        }
        if (errorType == null) {
            throw new IllegalArgumentException("errorType cannot be null");
        }

        String exceptionMessage = "";
        String alertMessage = "";
        boolean reportableError = false;
        if (errorType.equals(ErrorCodeType.idevice_error_t)) {
            if (errorCode == -3) { // IDEVICE_E_NO_DEVICE
                exceptionMessage = "idevice error: no device found/connected (IDEVICE_E_NO_DEVICE)";
                alertMessage = "Error: No devices found/connected. Make sure your device is connected via USB and unlocked.";
                if (PlatformUtil.isWindows()) {
                    alertMessage += "\n\nEnsure iTunes or Apple's iOS Drivers are installed.";
                }
                if (showAlert) {
                    String finalAlertMessage1 = alertMessage;
                    Utils.runSafe(() -> {
                        ButtonType downloadItunes = new ButtonType("Download iTunes");
                        Alert alert = new Alert(Alert.AlertType.ERROR, finalAlertMessage1, ButtonType.OK);
                        if (PlatformUtil.isWindows()) {
                            alert.getButtonTypes().add(0, downloadItunes);
                        }
                        if (downloadItunes.equals(alert.showAndWait().orElse(null))) {
                            Utils.openURL("https://www.apple.com/itunes/download/win64");
                        }
                    });
                    showAlert = false;
                }
            } else {
                exceptionMessage = "idevice error: code=" + errorCode;
                alertMessage = exceptionMessage;
                reportableError = true;
            }
        } else if (errorType.equals(ErrorCodeType.lockdownd_error_t)) {
            switch (errorCode) {
                case -17:  // LOCKDOWN_E_PASSWORD_PROTECTED
                    exceptionMessage = "lockdownd error: LOCKDOWN_E_PASSWORD_PROTECTED (-17)";
                    alertMessage = "Error: The device is locked.\n\nPlease unlock your device and go to the homescreen then try again.";
                    break;
                case -18:  // LOCKDOWN_E_USER_DENIED_PAIRING
                    exceptionMessage = "lockdownd error: LOCKDOWN_E_USER_DENIED_PAIRING (-18)";
                    alertMessage = "Error: The user denied the trust/pair request on the device. Please unplug your device and accept the dialog next time.";
                    break;
                case -19:  // LOCKDOWN_E_PAIRING_DIALOG_RESPONSE_PENDING
                    exceptionMessage = "lockdownd error: LOCKDOWN_E_PAIRING_DIALOG_RESPONSE_PENDING (-19)";
                    alertMessage = "Error: Please accept the trust/pair dialog on the device and try again.";
                    break;
                case -8:  // LOCKDOWN_E_MUX_ERROR
                    exceptionMessage = "lockdownd error: LOCKDOWN_E_MUX_ERROR (-8)";
                    alertMessage = exceptionMessage;
                    reportableError = true;
                    break;
                default:
                    exceptionMessage = "lockdownd error: code=" + errorCode;
                    alertMessage = exceptionMessage;
                    reportableError = true;
                    break;
            }
        } else if (errorType.equals(ErrorCodeType.irecv_error_t)) {
            exceptionMessage = "irecovery error: code=" + errorCode;
            alertMessage = exceptionMessage + "\n\nIf your device is still in recovery mode, use the \"Exit Recovery Mode\" option from the help menu.";
            reportableError = true;
        }
        if (showAlert) {
            // temporary final variables are required because of the lambda
            final boolean finalReportableError = reportableError;
            final String finalAlertMessage = alertMessage;
            Utils.runSafe(() -> {
                if (finalReportableError) {
                    Utils.showReportableError(finalAlertMessage);
                } else {
                    Utils.showUnreportableError(finalAlertMessage);
                }
            });
        }
        throw new LibimobiledeviceException(exceptionMessage);
    }

    static {
        try {
            Native.register(Libimobiledevice.class, "imobiledevice");
        } catch (Throwable e) { // need to catch UnsatisfiedLinkError
            Utils.showReportableError("Error: unable to register native methods", Utils.exceptionToString(e));
            throw new LibimobiledeviceException("Unable to register native methods", e);
        }
    }

    @SuppressWarnings("unused")
    public static class LibimobiledeviceException extends RuntimeException {
        public LibimobiledeviceException() {
            super();
        }

        public LibimobiledeviceException(String message) { super(message); }

        public LibimobiledeviceException(String message, Throwable cause) {
            super(message, cause);
        }

        public LibimobiledeviceException(Throwable cause) {
            super(cause);
        }
    }

}
