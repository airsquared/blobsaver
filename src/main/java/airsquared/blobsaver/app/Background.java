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

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class Background {

    static boolean inBackground = false;

    private static ScheduledExecutorService executor;
    private static TrayIcon trayIcon;

    static void startBackground(boolean runOnlyOnce) {
        if (runOnlyOnce) {
            saveAllBackgroundBlobs();
        } else {
            if (Main.primaryStage.isShowing()) {
                Main.hideStage();
            }
            Notification.Notifier.INSTANCE.setPopupLifetime(Duration.seconds(30));
            Notification.Notifier.INSTANCE.notifyInfo("Background process has started", "Check your system tray/status bar for\nthe icon.");

            inBackground = true;
            executor = Executors.newScheduledThreadPool(1);

            try {
                trayIcon = createTrayIcon();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // add the application tray icon to the system tray.
            try {
                SystemTray.getSystemTray().add(trayIcon);
                System.out.println("in tray");
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }

            startExecutor();
        }
    }

    private static TrayIcon createTrayIcon() throws IOException {
        TrayIcon trayIcon = new TrayIcon(ImageIO.read(Background.class.getResourceAsStream("blob_emoji.png")),
                "blobsaver " + Main.appVersion);
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
        return trayIcon;
    }

    private static void startExecutor() {
        TimeUnit timeUnit;
        int timeAmount = Prefs.appPrefs.getInt("Time to run", 1);
        switch (Prefs.appPrefs.get("Time unit for background", "Days")) {
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
        executor.scheduleAtFixedRate(Background::saveAllBackgroundBlobs, 0, timeAmount, timeUnit);
        executor.scheduleAtFixedRate(() -> Utils.checkForUpdates(false), 1, 1, TimeUnit.DAYS);
    }

    private static void saveAllBackgroundBlobs() {
        Prefs.getBackgroundDevices().forEach(Background::saveBackgroundBlobs);
        System.out.println("Done saving all background blobs");
    }

    private static void saveBackgroundBlobs(Prefs.SavedDevice savedDevice) {
        System.out.println("attempting to save for device " + savedDevice.number);

        TSS.Builder builder = new TSS.Builder().setDevice(savedDevice.getIdentifier())
                .setEcid(savedDevice.getEcid()).setSavePath(savedDevice.getSavePath());
        savedDevice.getBoardConfig().ifPresent(builder::setBoardConfig);
        savedDevice.getApnonce().ifPresent(builder::setApnonce);

        TSS tss = builder.build();
        tss.setOnSucceeded(event -> {
            Notification notification = new Notification("Successfully saved blobs for", savedDevice.getName(), Notification.SUCCESS_ICON);
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