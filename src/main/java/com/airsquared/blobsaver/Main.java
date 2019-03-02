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
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import me.matetoes.libdockvisibility.DockVisibility;

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

public class Main {

    //enables debug menu icon in tray icon and to create a breakpoint.
    //You can also create a breakpoint on the helpLabelHandler() function and click the question marks to debug
    static final boolean DEBUG_MODE = true;

    static final Version appVersion = new Version("2.2.4");
    static final Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
    private static final String appID = "com.airsquared.blobsaver";
    private static boolean firstTimeShown = true; //whether it is the first time that primaryStage is shown
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
        if (!Main.primaryStage.isShowing()) {
            // centers on screen if it's not the first time it's opened and it's actually hidden
            System.out.println("attempting to center primaryStage");
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX((screenBounds.getWidth() - primaryStage.getWidth()) / 2);
            primaryStage.setY((screenBounds.getHeight() - primaryStage.getHeight()) / 2);

            if (PlatformUtil.isMac()) { // show the dock icon
                System.out.println("showing dock icon");
                DockVisibility.show();
            }

            primaryStage.show();
            firstTimeShown = false;
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

    static void quit() {
        if (Background.inBackground) {
            Background.executor.shutdownNow();
            Platform.runLater(Platform::exit);
        } else {
            Platform.exit();
        }
        System.exit(0);
    }

    public static class JavaFxApplication extends Application {

        static void launchIt(String[] args) {
            launch(args);
        }

        private static void setupStage(URL fxml, String css) throws IOException {
            Parent root = FXMLLoader.load(fxml);
            primaryStage.setTitle("blobsaver " + Main.appVersion);
            primaryStage.setScene(new Scene(root));
            primaryStage.getScene().getStylesheets().add(css);
            primaryStage.setResizable(false);
        }

        @Override
        public void start(Stage primaryStage) throws IOException {
            Main.primaryStage = primaryStage;

            //setup stage
            setupStage(getClass().getResource("blobsaver.fxml"),
                    getClass().getResource("app.css").toExternalForm());

            //sets up the dock icon
            if (PlatformUtil.isMac()) {
                com.apple.eawt.Application.getApplication().setDockIconImage(javax.imageio.ImageIO.read(getClass().getResourceAsStream("blob_emoji.png")));
            } else {
                primaryStage.getIcons().clear();
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("blob_emoji.png")));
            }

            //sets up preset and checks for updates
            Controller.afterStageShowing();


            //starts background
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
                    quit();
                }
            });

            appPrefs.put("App version", appVersion.toString()); //No uses for now; maybe needed later
        }
    }
}
