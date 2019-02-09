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

import eu.hansolo.enzo.notification.Notification;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static com.airsquared.blobsaver.Main.appPrefs;
import static com.airsquared.blobsaver.Main.appVersion;
import static com.airsquared.blobsaver.Main.primaryStage;
import static com.airsquared.blobsaver.Shared.*;

class Background {

    static boolean inBackground = false;

    private static ScheduledExecutorService executor;
    private static TrayIcon trayIcon;

    static ArrayList<String> getPresetsToSaveFor() {
        ArrayList<String> presetsToSaveFor = new ArrayList<>();
        JSONArray presetsToSaveForJson = new JSONArray(appPrefs.get("Presets to save in background", "[]"));
        for (int i = 0; i < presetsToSaveForJson.length(); i++) {
            presetsToSaveFor.add(presetsToSaveForJson.getString(i));
        }
        return presetsToSaveFor;
    }

    static void startBackground(boolean runOnlyOnce) {
        ArrayList<Integer> presetsToSave = new ArrayList<>();
        JSONArray presetsToSaveJson = new JSONArray(appPrefs.get("Presets to save in background", "[]"));
        for (int i = 0; i < presetsToSaveJson.length(); i++) {
            presetsToSave.add(Integer.valueOf(presetsToSaveJson.getString(i)));
        }
        ArrayList<String> presetsToSaveNames = new ArrayList<>();
        if (!presetsToSave.isEmpty()) {
            presetsToSave.forEach((preset) -> presetsToSaveNames.add(appPrefs.get("Name Preset" + preset, "")));
        } else {
            inBackground = false;
            return;
        }
        if (!runOnlyOnce && Platform.isFxApplicationThread()) {
            primaryStage.hide();
            Notification.Notifier.INSTANCE.setPopupLifetime(Duration.seconds(30));
            Notification.Notifier.INSTANCE.notifyInfo("Background process has started", "Check your system tray/status bar for\nthe icon."
                    + presetsToSaveNames.toString().substring(1, presetsToSaveNames.toString().length() - 1));
        }
        if (!runOnlyOnce) {
            inBackground = true;
            executor = Executors.newScheduledThreadPool(1);
            SystemTray tray = SystemTray.getSystemTray();

            Image image = null;
            try {
                image = ImageIO.read(Background.class.getResourceAsStream("blob_emoji.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            //noinspection ConstantConditions
            trayIcon = new TrayIcon(image, "blobsaver " + appVersion);
            trayIcon.setImageAutoSize(true);

            MenuItem openItem = new MenuItem("Open window");
            openItem.addActionListener((evt) -> Platform.runLater(Background::showStage));
            openItem.setFont(Font.decode(null).deriveFont(Font.BOLD)); // bold it

            MenuItem exitItem = new MenuItem("Quit");
            exitItem.addActionListener(event -> {
                executor.shutdownNow();
                Platform.runLater(Platform::exit);
                tray.remove(trayIcon);
                System.exit(0);
            });

            // setup the popup menu for the application.
            final PopupMenu popup = new PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // add the application tray icon to the system tray.
            try {
                tray.add(trayIcon);
                log("in tray");
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
        if (runOnlyOnce) {
            if (!presetsToSave.isEmpty()) {
                log("there are some presets to save");
                presetsToSave.forEach(Background::saveBackgroundBlobs);
            }
            inBackground = false;
        } else {
            TimeUnit timeUnit;
            int timeAmount = appPrefs.getInt("Time to run", 1);
            switch (appPrefs.get("Time unit for background", "Days")) {
                case "Minutes":
                    timeUnit = TimeUnit.MINUTES;
                    break;
                case "Hours":
                    timeUnit = TimeUnit.HOURS;
                    break;
                case "Days":
                    timeUnit = TimeUnit.DAYS;
                    break;
                case "Weeks":
                    timeUnit = TimeUnit.DAYS;
                    timeAmount = timeAmount * 7;
                    break;
                default:
                    timeUnit = TimeUnit.DAYS;
                    break;
            }
            executor.scheduleAtFixedRate(() -> {
                if (!presetsToSave.isEmpty()) {
                    log("there are some presets to save");
                    presetsToSave.forEach(Background::saveBackgroundBlobs);
                }
                log("done w execution of executor");
            }, 0, timeAmount, timeUnit);
            executor.scheduleAtFixedRate(() -> checkForUpdates(false), 4, 4, TimeUnit.DAYS);
        }
    }

    private static void saveBackgroundBlobs(int preset) {
        log("attempting to save for " + preset);
        Preferences presetPrefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
//        presetPrefs.put("Saved Versions", "[]");                                                        // for testing
        String identifier;
        if ("none".equals(presetPrefs.get("Device Model", ""))) {
            identifier = presetPrefs.get("Device Identifier", "");
        } else {
            identifier = textToIdentifier(presetPrefs.get("Device Model", ""));
        }
        log("identifier:" + identifier);
        String response;
        try {
            response = makeRequest(new URL("https://api.ipsw.me/v4/device/" + identifier));
        } catch (IOException e) {
            Notification notification = new Notification("Saving blobs failed", "Check your internet connection.\nIf it is working, click here to report this error.", Notification.ERROR_ICON);
            Notification.Notifier.INSTANCE.setPopupLifetime(Duration.minutes(1));
            Notification.Notifier.INSTANCE.setOnNotificationPressed((event) -> {
                Notification.Notifier.INSTANCE.stop();
                showStage();
                Alert alert = new Alert(Alert.AlertType.ERROR,
                        "Saving blobs failed. Check your internet connection.\n\nIf your internet is working and you can connect to the website ipsw.me in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                        githubIssue, redditPM, ButtonType.OK);
                resizeAlertButtons(alert);
                alert.showAndWait();
                alert.getDialogPane().toFront();
                reportError(alert, e.getMessage());
            });
            Notification.Notifier.INSTANCE.notify(notification);
            return;
        }
        log("made request");
        JSONArray firmwareListJson = new JSONObject(response).getJSONArray("firmwares");
        @SuppressWarnings("unchecked") List<Map<String, Object>> firmwareList = (List) firmwareListJson.toList();
        List<String> signedVersions = firmwareList.stream().filter(map -> Boolean.TRUE.equals(map.get("signed"))).map(map -> map.get("version").toString()).collect(Collectors.toList());
        log("signed versions:" + signedVersions);
        ArrayList<String> savedVersions = new ArrayList<>();
        JSONArray savedVersionsJson = new JSONArray(presetPrefs.get("Saved Versions", "[]"));
        for (int i = 0; i < savedVersionsJson.length(); i++) {
            savedVersions.add(savedVersionsJson.getString(i));
        }
        log("saved versions:" + savedVersions);
        ArrayList<String> versionsToSave = new ArrayList<>();
        signedVersions.forEach((version) -> {
            if (!savedVersions.contains(version)) {
                versionsToSave.add(version);
            }
        });
        log("versions to save:" + versionsToSave);
        if (versionsToSave.isEmpty()) {
            return;
        }
        String ecid = presetPrefs.get("ECID", "");
        String path = presetPrefs.get("Path", "");
        String boardConfig = presetPrefs.get("Board Config", "");
        for (String version : versionsToSave) {
            File tsschecker;
            try {
                tsschecker = getTsschecker();
            } catch (IOException e) {
                Notification notification = new Notification("Saving blobs failed", "There was an error creating tsschecker. Click here to report this error.", Notification.ERROR_ICON);
                Notification.Notifier.INSTANCE.setPopupLifetime(Duration.minutes(1));
                Notification.Notifier.INSTANCE.setOnNotificationPressed((event) -> {
                    Notification.Notifier.INSTANCE.stop();
                    showStage();
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "There was an error creating tsschecker.\n\nIf your internet is working and you can connect to apple.com in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                            githubIssue, redditPM, ButtonType.OK);
                    resizeAlertButtons(alert);
                    alert.showAndWait();
                    alert.getDialogPane().toFront();
                    reportError(alert, e.getMessage());
                });
                Notification.Notifier.INSTANCE.notify(notification);
                continue;
            }

            //noinspection ResultOfMethodCallIgnored
            new File(path).mkdirs();
            String tsscheckerLog;
            try {
                if (!"none".equals(boardConfig) && !"".equals(boardConfig)) { // needs board config
                    tsscheckerLog = executeProgram(tsschecker.getPath(), "--generator", "0x1111111111111111", "--nocache", "-d", identifier, "-s", "-e", ecid,
                            "--save-path", path, "-i", version, "--boardconfig", boardConfig);
                } else {
                    tsscheckerLog = executeProgram(tsschecker.getPath(), "--generator", "0x1111111111111111", "--nocache", "-d", identifier, "-s", "-e", ecid,
                            "--save-path", path, "-i", version);
                }
            } catch (IOException e) {
                Notification notification = new Notification("Saving blobs failed", "There was an error starting tsschecker. Click here to report this error.", Notification.ERROR_ICON);
                Notification.Notifier.INSTANCE.setPopupLifetime(Duration.minutes(1));
                Notification.Notifier.INSTANCE.setOnNotificationPressed((event) -> {
                    Notification.Notifier.INSTANCE.stop();
                    showStage();
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "There was an error getting the tsschecker result.\n\nPlease create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                            githubIssue, redditPM, ButtonType.OK);
                    resizeAlertButtons(alert);
                    alert.showAndWait();
                    alert.getDialogPane().toFront();
                    reportError(alert, e.getMessage());
                });
                Notification.Notifier.INSTANCE.notify(notification);
                continue;
            }
            String presetName;
            if ("".equals(appPrefs.get("Name Preset" + preset, ""))) {
                presetName = "Preset " + preset;
            } else {
                presetName = appPrefs.get("Name Preset" + preset, "");
            }
            if (tsscheckerLog.contains("Saved shsh blobs")) {
                Notification notification = new Notification("Successfully saved blobs for", "iOS " + version + " (" + presetName + ") in\n" + path, Notification.SUCCESS_ICON);
                Notification.Notifier.INSTANCE.setPopupLifetime(Duration.seconds(30));
                Notification.Notifier.INSTANCE.setOnNotificationPressed((event) -> {
                    Notification.Notifier.INSTANCE.stop();
                    showStage();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Successfully saved blobs in\n" + path, ButtonType.OK);
                    alert.setTitle("Success");
                    alert.setHeaderText("Success!");
                    resizeAlertButtons(alert);
                    alert.showAndWait();
                    alert.getDialogPane().toFront();
                });
                Notification.Notifier.INSTANCE.notify(notification);

                log("displayed message");

            } else if (tsscheckerLog.contains("[Error] ERROR: TSS request failed: Could not resolve host:")) {
                Notification notification = new Notification("Saving blobs failed", "Check your internet connection. If it is working, click here to report this error.", Notification.ERROR_ICON);
                Notification.Notifier.INSTANCE.setPopupLifetime(Duration.minutes(1));
                Notification.Notifier.INSTANCE.setOnNotificationPressed((event) -> {
                    Notification.Notifier.INSTANCE.stop();
                    showStage();
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Saving blobs failed. Check your internet connection.\n\nIf your internet is working and you can connect to apple.com in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                            githubIssue, redditPM, ButtonType.OK);
                    resizeAlertButtons(alert);
                    alert.showAndWait();
                    alert.getDialogPane().toFront();
                    reportError(alert, tsscheckerLog);
                });
                Notification.Notifier.INSTANCE.notify(notification);
            } else if (tsscheckerLog.contains("iOS " + version + " for device " + identifier + " IS NOT being signed")) {
                continue;
            } else {
                Notification notification = new Notification("Saving blobs failed", "An unknown error occurred. Click here to report this error.", Notification.ERROR_ICON);
                Notification.Notifier.INSTANCE.setPopupLifetime(Duration.minutes(1));
                Notification.Notifier.INSTANCE.setOnNotificationPressed((event) -> {
                    Notification.Notifier.INSTANCE.stop();
                    showStage();
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Saving blobs failed." + "\n\nPlease create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                            githubIssue, redditPM, ButtonType.CANCEL);
                    resizeAlertButtons(alert);
                    alert.showAndWait();
                    alert.getDialogPane().toFront();
                    reportError(alert, tsscheckerLog);
                });
                Notification.Notifier.INSTANCE.notify(notification);
            }
            savedVersions.add(version);
            presetPrefs.put("Saved Versions", new JSONArray(savedVersions).toString());
            log("it worked");
        }
    }

    private static void showStage() {
        primaryStage.show();
        primaryStage.toFront();
        primaryStage.requestFocus();
    }

    static void stopBackground(boolean showAlert) {
        inBackground = false;
        executor.shutdownNow();
        SwingUtilities.invokeLater(() -> SystemTray.getSystemTray().remove(trayIcon));
        if (showAlert && Platform.isFxApplicationThread()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "The background process has been cancelled",
                    ButtonType.OK);
            alert.showAndWait();
        } else if (showAlert) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "The background process has been cancelled",
                        ButtonType.OK);
                alert.showAndWait();
            });
        }
        log("stopped background");
    }

    private static void log(String msg) {
        System.out.println(msg);
    }
}