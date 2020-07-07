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

import eu.hansolo.enzo.notification.Notification;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import org.json.JSONArray;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

class Background {

    static boolean inBackground = false;

    private static ScheduledExecutorService executor;
    private static TrayIcon trayIcon;

    static ArrayList<String> getPresetsToSaveFor() {
        ArrayList<String> presetsToSaveFor = new ArrayList<>();
        JSONArray presetsToSaveForJson = new JSONArray(Main.appPrefs.get("Presets to save in background", "[]"));
        for (int i = 0; i < presetsToSaveForJson.length(); i++) {
            presetsToSaveFor.add(presetsToSaveForJson.getString(i));
        }
        return presetsToSaveFor;
    }

    static void startBackground(boolean runOnlyOnce) {
        ArrayList<Integer> presetsToSave = new ArrayList<>();
        JSONArray presetsToSaveJson = new JSONArray(Main.appPrefs.get("Presets to save in background", "[]"));
        for (int i = 0; i < presetsToSaveJson.length(); i++) {
            presetsToSave.add(Integer.valueOf(presetsToSaveJson.getString(i)));
        }
        ArrayList<String> presetsToSaveNames = new ArrayList<>();
        if (!presetsToSave.isEmpty()) {
            presetsToSave.forEach(preset -> presetsToSaveNames.add(Main.appPrefs.get("Name Preset" + preset, "")));
        } else {
            inBackground = false;
            return;
        }
        if (!runOnlyOnce && Platform.isFxApplicationThread()) {
            if (Main.primaryStage.isShowing()) {
                Main.hideStage();
            }
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
            trayIcon = new TrayIcon(image, "blobsaver " + Main.appVersion);
            trayIcon.setImageAutoSize(true);

            ActionListener showListener = event -> Platform.runLater(Main::showStage);
            MenuItem openItem = new MenuItem("Open window");
            openItem.addActionListener(showListener);
            openItem.setFont(Font.decode(null).deriveFont(Font.BOLD)); // bold it

            MenuItem exitItem = new MenuItem("Quit");
            exitItem.addActionListener(event -> Platform.runLater(Platform::exit));

            // setup the popup menu for the application.
            final PopupMenu popup = new PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            if (Main.SHOW_BREAKPOINT) {
                MenuItem breakpointItem = new MenuItem("Breakpoint");
                breakpointItem.addActionListener(e -> System.out.println("breakpoint"));
                popup.add(breakpointItem);
            }
            trayIcon.setPopupMenu(popup);
            trayIcon.addActionListener(showListener);

            // add the application tray icon to the system tray.
            try {
                tray.add(trayIcon);
                System.out.println("in tray");
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
        if (runOnlyOnce) {
            if (!presetsToSave.isEmpty()) {
                System.out.println("there are some presets to save");
                presetsToSave.forEach(Background::saveBackgroundBlobs);
            }
            inBackground = false;
        } else {
            TimeUnit timeUnit;
            int timeAmount = Main.appPrefs.getInt("Time to run", 1);
            switch (Main.appPrefs.get("Time unit for background", "Days")) {
                case "Minutes":
                    timeUnit = TimeUnit.MINUTES;
                    break;
                case "Hours":
                    timeUnit = TimeUnit.HOURS;
                    break;
                case "Weeks":
                    timeAmount *= 7;
                case "Days":
                default:
                    timeUnit = TimeUnit.DAYS;
                    break;
            }
            executor.scheduleAtFixedRate(() -> {
                if (!presetsToSave.isEmpty()) {
                    System.out.println("there are some presets to save");
                    presetsToSave.forEach(Background::saveBackgroundBlobs);
                }
                System.out.println("done with executor");
            }, 0, timeAmount, timeUnit);
            executor.scheduleAtFixedRate(() -> Utils.checkForUpdates(false), 1, 1, TimeUnit.DAYS);
        }
    }

    private static void saveBackgroundBlobs(int preset) {
        System.out.println("attempting to save for preset " + preset);
        String presetName;
        if ("".equals(Main.appPrefs.get("Name Preset" + preset, ""))) {
            presetName = "Preset " + preset;
        } else {
            presetName = Main.appPrefs.get("Name Preset" + preset, "");
        }
        Preferences presetPrefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
        String identifier;
        if ("none".equals(presetPrefs.get("Device Model", ""))) {
            identifier = presetPrefs.get("Device Identifier", "");
        } else {
            identifier = Devices.textToIdentifier(presetPrefs.get("Device Model", ""));
        }
        String ecid = presetPrefs.get("ECID", "");
        String path = presetPrefs.get("Path", "");
        String boardConfig = presetPrefs.get("Board Config", "");
        String apnonce = presetPrefs.get("Apnonce", "");

        TSS.Builder builder = new TSS.Builder()
                .setDevice(identifier).setEcid(ecid).setSavePath(path);
        if (!"none".equals(boardConfig) && !"".equals(boardConfig)) {
            builder.setBoardConfig(boardConfig);
        }
        if (!Utils.isEmptyOrNull(apnonce)) {
            builder.setApnonce(apnonce);
        }
        TSS tss = builder.build();
        tss.setOnSucceeded(event -> {
            Notification notification = new Notification("Successfully saved blobs for", presetName, Notification.SUCCESS_ICON);
            Notification.Notifier.INSTANCE.setPopupLifetime(Duration.seconds(30));
            Notification.Notifier.INSTANCE.notify(notification);
        });
        tss.setOnFailed(event -> {
            Notification notification = new Notification("Saving blobs failed", "Click here to view more.", Notification.ERROR_ICON);
            Notification.Notifier.INSTANCE.setPopupLifetime(Duration.minutes(1));
            Notification.Notifier.INSTANCE.setOnNotificationPressed(event1 -> {
                Notification.Notifier.INSTANCE.stop();
                Main.showStage();
                Throwable t = tss.getException();
                t.printStackTrace();
                if (t instanceof TSS.TSSException) {
                    TSS.TSSException e = (TSS.TSSException) t;
                    if (e.isReportable && e.tssLog != null) {
                        Utils.showReportableError(e.getMessage(), e.tssLog);
                    } else if (e.isReportable) {
                        Utils.showReportableError(e.getMessage(), Utils.exceptionToString(e));
                    } else {
                        Utils.showUnreportableError(e.getMessage());
                    }
                } else {
                    Utils.showReportableError("An unknown error occurred.", Utils.exceptionToString(t));
                }
                Notification.Notifier.INSTANCE.setOnNotificationPressed(null);
            });
            Notification.Notifier.INSTANCE.notify(notification);
        });
        Utils.executeInThreadPool(tss);
    }

    static void stopBackground(boolean showAlert) {
        inBackground = false;
        executor.shutdownNow();
        if (SwingUtilities.isEventDispatchThread()) {
            SystemTray.getSystemTray().remove(trayIcon);
        } else {
            SwingUtilities.invokeLater(() -> SystemTray.getSystemTray().remove(trayIcon));
        }
        if (showAlert) {
            Utils.runSafe(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "The background process has been cancelled",
                        ButtonType.OK);
                alert.showAndWait();
            });
        }
        System.out.println("Stopped background");
    }
}