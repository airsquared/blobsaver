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

import com.sun.javafx.PlatformUtil;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import me.matetoes.libdockvisibility.DockVisibility;

import java.io.IOException;
import java.util.prefs.Preferences;

public class Main {

    static final Version appVersion = new Version("2.2.4");
    static final Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
    private static final String appID = "com.airsquared.blobsaver";
    static Stage primaryStage;

    /**
     * Enables a menu item in the system tray to activate a breakpoint when in background and
     * replaces the question mark help labels with activating a breakpoint instead.
     * Remember to add a breakpoint in the correct methods to use this.
     */
    static final boolean SHOW_BREAKPOINT = false;

    public static void main(String[] args) {
        try {
            JUnique.acquireLock(appID);
        } catch (AlreadyLockedException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "blobsaver already running, exiting");
            System.exit(-1);
        }
        try {
            Class.forName("javafx.application.Application");
            if (PlatformUtil.isMac() || PlatformUtil.isWindows() || PlatformUtil.isLinux()) {
                JavaFxApplication.launchIt(args);
            } else {
                int result = javax.swing.JOptionPane.showOptionDialog(null, "Cannot detect the OS. Assuming it is Linux. Continue?",
                        "Warning", javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE, null, null, null);
                if (result == javax.swing.JOptionPane.CANCEL_OPTION) {
                    System.exit(0);
                }
                JavaFxApplication.launchIt(args);
            }
        } catch (ClassNotFoundException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "JavaFX is not installed. " +
                    "Either install Oracle Java or\nif you are using OpenJRE/OpenJDK, install openjfx." +
                    "\nOn Linux, use sudo apt-get install openjfx", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    static void showStage() {
        //show dock icon again or center on screen if actually hidden (as opposed to just being out of focus)
        if (!primaryStage.isShowing()) {
            // centers on screen if it's not the first time it's opened and it's actually hidden
            primaryStage.centerOnScreen();

            if (PlatformUtil.isMac()) { // show the dock icon
                System.out.println("showing dock icon");
                DockVisibility.show();
            }

            primaryStage.show();
        }

        primaryStage.toFront();
        primaryStage.requestFocus();
    }

    static void hideStage() {
        primaryStage.hide();
        if (PlatformUtil.isMac()) {
            System.out.println("hiding dock icon");
            DockVisibility.hide();
        }
    }

    public static class JavaFxApplication extends Application {

        static void launchIt(String[] args) {
            launch(args);
        }

        @Override
        public void start(Stage primaryStage) throws IOException {
            Main.primaryStage = primaryStage;
            Parent root = FXMLLoader.load(getClass().getResource("blobsaver.fxml"));
            primaryStage.setTitle("blobsaver " + Main.appVersion);
            primaryStage.setScene(new Scene(root));
            primaryStage.getScene().getStylesheets().add(getClass().getResource("app.css").toExternalForm());
            if (PlatformUtil.isMac()) { // setup the dock icon
                com.apple.eawt.Application.getApplication().setDockIconImage(javax.imageio.ImageIO.read(getClass().getResourceAsStream("blob_emoji.png")));
            } else {
                primaryStage.getIcons().clear();
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("blob_emoji.png")));
            }
            primaryStage.setResizable(false);
            Controller.afterStageShowing();
            Platform.setImplicitExit(false);
            if (appPrefs.getBoolean("Start background immediately", false)) {
                Background.startBackground(false);
            } else {
                showStage();
            }

            //if in background, hide; else quit
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                if (Background.inBackground) {
                    hideStage();
                } else {
                    Platform.exit();
                }
            });
            appPrefs.put("App version", appVersion.toString());
        }

        @Override
        public void stop() {
            if (Background.inBackground) {
                Background.stopBackground(false);
            }
            System.exit(0);
        }
    }
}
