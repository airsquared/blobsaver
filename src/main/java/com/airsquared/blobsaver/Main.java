/*
 * Copyright (c) 2018  airsquared
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
import com.sun.javafx.application.PlatformImpl;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.prefs.Preferences;

public class Main {

    static final String appVersion = "v2.2.1";
    static final Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
    private static final String appID = "com.airsquared.blobsaver";
    static Stage primaryStage;

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
                if (appPrefs.getBoolean("Start background immediately", false) && PlatformUtil.isMac()) {
                    PlatformImpl.setTaskbarApplication(false);
                }
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
            javax.swing.JOptionPane.showMessageDialog(null, "JavaFX is not installed. Either install Oracle Java or\nif you are using OpenJRE/OpenJDK, install openjfx.\nOn Linux, use sudo apt-get install openjfx", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
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
            if (PlatformUtil.isMac()) {
                com.apple.eawt.Application.getApplication().setDockIconImage(javax.imageio.ImageIO.read(getClass().getResourceAsStream("blob_emoji.png")));
            } else {
                primaryStage.getIcons().clear();
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("blob_emoji.png")));
            }
            primaryStage.show();
            primaryStage.setResizable(false);
            Controller.afterStageShowing();
            Platform.setImplicitExit(false);
            if (appPrefs.getBoolean("Start background immediately", false)) {
                Background.startBackground(false);
            }
            primaryStage.setOnCloseRequest(event -> {
                event.consume();
                if (Background.inBackground) {
                    primaryStage.hide();
                } else {
                    Platform.exit();
                    System.exit(0);
                }
            });
            appPrefs.put("App version", appVersion);
        }
    }
}