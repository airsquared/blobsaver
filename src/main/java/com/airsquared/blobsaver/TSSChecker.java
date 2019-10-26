/*
 * Copyright (c) 2019  airsquared
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

package com.airsquared.blobsaver;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.effect.Effect;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.airsquared.blobsaver.Shared.*;

/**
 * For executing the tsschecker program(currently only used in {@link Controller}).
 * TODO: fix this class to separate GUI logic
 */
class TSSChecker {

    static void run(String device) {
        Controller controller = Controller.INSTANCE;
        if (controller.versionCheckBox.isSelected()) {
            List<Map<String, Object>> allSignedFirmwares; // really is List<Map<String, String>>
            ArrayList<String> signedVersionsStringList = new ArrayList<>();
            try {
                allSignedFirmwares = getAllSignedFirmwares(device);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Saving blobs failed. Check your internet connection.\n\nIf your internet is working and you can connect to the website ipsw.me in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                        githubIssue, redditPM, ButtonType.OK);
                resizeAlertButtons(alert);
                alert.showAndWait();
                reportError(alert, e.getMessage());
                return;
            }
            try {
                // can't use the `forEach()` method because exception won't be caught properly
                for (Map<String, Object> firmware : allSignedFirmwares) { // really is List<Map<String, String>>
                    signedVersionsStringList.add(firmware.get("version").toString());
                    run(new URL(firmware.get("url").toString()), device, firmware.get("version").toString());
                }
            } catch (TSSCheckerException | MalformedURLException e) { // exception is only needed so lambda doesn't keep on going after an error
                return; // the error alert should already be shown
            }
            String signedVersionsString = signedVersionsStringList.toString().substring(1, signedVersionsStringList.toString().length() - 1);
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Successfully saved blobs in\n" + controller.pathField.getText() + "\n\nFor "
                            + (signedVersionsStringList.size() == 1 ? "version " : "versions ") + signedVersionsString, ButtonType.OK);
            alert.setHeaderText("Success!");
            alert.showAndWait();
        } else {
            try {
                URL ipswURL;
                if (controller.betaCheckBox.isSelected()) {
                    try {
                        ipswURL = new URL(controller.ipswField.getText());
                    } catch (MalformedURLException e) {
                        newUnreportableError("\"" + controller.ipswField.getText() + "\" is not a valid URL.\n\nMake sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"");
                        e.printStackTrace();
                        return;
                    }
                } else {
                    try {
                        ipswURL = new URL(getFirmwareList(device).stream().filter(stringObjectMap ->
                                controller.versionField.getText().equals(stringObjectMap.get("version")))
                                .collect(Collectors.toList()).get(0).get("url").toString());
                    } catch (IOException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "Saving blobs failed. Check your internet connection.\n\nIf your internet is working and you can connect to the website ipsw.me in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                                githubIssue, redditPM, ButtonType.OK);
                        resizeAlertButtons(alert);
                        alert.showAndWait();
                        reportError(alert, e.getMessage());
                        return;
                    }
                }
                run(ipswURL, device, controller.versionField.getText());
            } catch (TSSCheckerException e) {
                // the error alert should already be shown
            }

        }
    }

    private static void run(URL ipswURL, String device, String version) throws TSSCheckerException {
        if ("".equals(device)) {
            return;
        }

        Controller controller = Controller.INSTANCE;
        String ecid = controller.ecidField.getText();
        String savePath = controller.pathField.getText();
        String boardConfig = controller.boardConfigField.getText();
        String apnonce = controller.apnonceField.getText();

        Effect errorBorder = Controller.errorBorder;

        File tsschecker = getTsschecker();

        File locationToSaveBlobs = new File(controller.pathField.getText());
        //noinspection ResultOfMethodCallIgnored
        locationToSaveBlobs.mkdirs();
        ArrayList<String> args = new ArrayList<>(Arrays.asList(tsschecker.getPath(), "--generator", "0x1111111111111111", "--nocache", "-d", device, "-s", "-e", ecid, "--save-path", savePath));
        if (controller.getBoardConfig) {
            Collections.addAll(args, "--boardconfig", boardConfig);
        }
        if (controller.apnonceCheckBox.isSelected()) {
            Collections.addAll(args, "--apnonce", apnonce);
        }
        if (controller.betaCheckBox.isSelected()) {
            if (!controller.ipswField.getText().matches("https?://.*apple.*\\.ipsw")) {
                newUnreportableError("\"" + ipswURL + "\" is not a valid URL.\n\nMake sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"");
                return;
            }
            Collections.addAll(args, "--beta", "--buildid", controller.buildIDField.getText());
        }
        try {
            Collections.addAll(args, "-m", extractBuildManifest(ipswURL).toString());
        } catch (IOException e) {
            newReportableError("Unable to extract BuildManifest from .ipsw.", e.getMessage());
            e.printStackTrace();
            return;
        }
        String tsscheckerLog;
        try {
            System.out.println("Running: " + args.toString());
            tsscheckerLog = executeProgram(args.toArray(new String[0]));
            System.out.println(tsscheckerLog);
        } catch (IOException e) {
            newReportableError("There was an error starting tsschecker.", e.toString());
            e.printStackTrace();
            throw new TSSCheckerException(e);
        }

        if (containsIgnoreCase(tsscheckerLog, "Saved signing tickets")) {
            // if multiple versions are being saved at the same time, do not show success message multiple times
            // the success message will be shown after saving everything is completed
            if (!controller.versionCheckBox.isSelected()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Successfully saved blobs for version " + version + " in\n" + savePath, ButtonType.OK);
                alert.setHeaderText("Success!");
                alert.showAndWait();
            }
            return;
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] manually specified ecid=" + ecid + ", but parsing failed")) {
            newUnreportableError("\"" + ecid + "\"" + " is not a valid ECID. Try getting it from iTunes.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.");
            controller.ecidField.setEffect(errorBorder);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] device " + device + " could not be found in devicelist")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "tsschecker could not find device: \"" + device +
                    "\"\n\nPlease create a new Github issue or PM me on Reddit if you used the dropdown menu.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.", githubIssue, redditPM, ButtonType.CANCEL);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] ERROR: could not get url for device " + device + " on iOS " + version)) {
            newUnreportableError("Could not find device \"" + device + "\" on iOS/tvOS " + version +
                    "\n\nThe version doesn't exist or isn't compatible with the device");
            controller.versionField.setEffect(errorBorder);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] manually specified apnonce=" + apnonce + ", but parsing failed")) {
            newUnreportableError("\"" + apnonce + "\" is not a valid apnonce");
            controller.apnonceField.setEffect(errorBorder);
        } else if (containsIgnoreCase(tsscheckerLog, "[WARNING] [TSSC] could not get id0 for installType=Erase. Using fallback installType=Update since user did not specify installType manually")
                && containsIgnoreCase(tsscheckerLog, "[Error] [TSSR] Error: could not get id0 for installType=Update")
                && containsIgnoreCase(tsscheckerLog, "[Error] [TSSR] faild to build TSS request")
                && containsIgnoreCase(tsscheckerLog, "Error] [TSSC] checking tss status failed!")) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Saving blobs failed. Check the board configuration or try again later.\n\nIf this doesn't work, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.",
                    githubIssue, redditPM, ButtonType.OK);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] ERROR: TSS request failed: Could not resolve host:")) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Saving blobs failed. Check your internet connection.\n\nIf your internet is working and you can connect to apple.com in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.",
                    githubIssue, redditPM, ButtonType.OK);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [Error] can't save signing tickets at " + savePath)) {
            newUnreportableError("\'" + savePath + "\' is not a valid path\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.");
            controller.pathField.setEffect(errorBorder);
        } else if (containsIgnoreCase(tsscheckerLog, "iOS " + version + " for device " + device + " IS NOT being signed!") || containsIgnoreCase(tsscheckerLog, "Build " + controller.buildIDField.getText() + " for device" + device + "IS NOT being signed!")) {
            newUnreportableError("iOS/tvOS " + version + " is not being signed for device " + device);
            if (version.equals(controller.versionField.getText())) {
                controller.versionField.setEffect(errorBorder);
            }
            if (controller.betaCheckBox.isSelected()) {
                controller.buildIDField.setEffect(errorBorder);
                controller.ipswField.setEffect(errorBorder);
            }
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] failed to load manifest")) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Failed to load manifest.\n\n \"" + ipswURL + "\" might not be a valid URL.\n\nMake sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"\n\nIf the URL is fine, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard",
                    githubIssue, redditPM, ButtonType.OK);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] selected device can't be used with that buildmanifest")) {
            newUnreportableError("Device and build manifest don't match.");
        } else if (containsIgnoreCase(tsscheckerLog, "[Error]")) {
            newReportableError("Saving blobs failed.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.", tsscheckerLog);
        } else {
            newReportableError("Unknown result.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.", tsscheckerLog);
        }
        throw new TSSCheckerException();

    }

    /**
     * RuntimeException for all tsschecker related errors.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    static class TSSCheckerException extends Exception {

        /**
         * Constructs a new tsschecker exception with {@code null} as its
         * detail message.  The cause is not initialized, and may subsequently be
         * initialized by a call to {@link #initCause}.
         */
        public TSSCheckerException() {
            super();
        }

        /**
         * Constructs a new tsschecker exception with the specified detail message.
         * The cause is not initialized, and may subsequently be initialized by a
         * call to {@link #initCause}.
         *
         * @param message the detail message. The detail message is saved for
         *                later retrieval by the {@link #getMessage()} method.
         */
        public TSSCheckerException(String message) {
            super(message);
        }

        /**
         * Constructs a new tsschecker exception with the specified detail message and
         * cause.  <p>Note that the detail message associated with
         * {@code cause} is <i>not</i> automatically incorporated in
         * this runtime exception's detail message.
         *
         * @param message the detail message (which is saved for later retrieval
         *                by the {@link #getMessage()} method).
         * @param cause   the cause (which is saved for later retrieval by the
         *                {@link #getCause()} method).  (A <tt>null</tt> value is
         *                permitted, and indicates that the cause is nonexistent or
         *                unknown.)
         */
        public TSSCheckerException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new tsschecker exception with the specified cause and a
         * detail message of <tt>(cause==null ? null : cause.toString())</tt>
         * (which typically contains the class and detail message of
         * <tt>cause</tt>).  This constructor is useful for runtime exceptions
         * that are little more than wrappers for other throwables.
         *
         * @param cause the cause (which is saved for later retrieval by the
         *              {@link #getCause()} method).  (A <tt>null</tt> value is
         *              permitted, and indicates that the cause is nonexistent or
         *              unknown.)
         */
        public TSSCheckerException(Throwable cause) {
            super(cause);
        }

        /**
         * Constructs a new tsschecker exception with the specified detail
         * message, cause, suppression enabled or disabled, and writable
         * stack trace enabled or disabled.
         *
         * @param message            the detail message.
         * @param cause              the cause.  (A {@code null} value is permitted,
         *                           and indicates that the cause is nonexistent or unknown.)
         * @param enableSuppression  whether or not suppression is enabled
         *                           or disabled
         * @param writableStackTrace whether or not the stack trace should
         *                           be writable
         */
        protected TSSCheckerException(String message, Throwable cause,
                                      boolean enableSuppression,
                                      boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

    }
}
