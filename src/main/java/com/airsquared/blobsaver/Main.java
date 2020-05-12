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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.prefs.Preferences;

public class Main {

    static final String appVersion = "v2.5.0";
    static final Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
    static Stage primaryStage;
    static final File jarDirectory = new File(URI.create(Main.class.getProtectionDomain().getCodeSource().getLocation().toString())).getParentFile();
    static final boolean runningFromIDE = true;


    /**
     * Enables a menu item in the system tray to activate a breakpoint when in background and
     * replaces the question mark help labels with activating a breakpoint instead.
     * Remember to add a breakpoint in the correct methods to use this.
     */
    static final boolean SHOW_BREAKPOINT = false;

    public static void main(String[] args) {
        if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
            try {
                JUnique.acquireLock("com.airsquared.blobsaver");
            } catch (AlreadyLockedException e) {
                System.err.println("blobsaver already running, exiting");
                System.exit(-1);
            }
        }
        // TODO: set the runningFromIDE variable
        System.out.println("jarDirectory = " + jarDirectory);
        setJNALibraryPath();
        JavaFxApplication.launch(JavaFxApplication.class, args);
        System.exit(0);
    }

    static void showStage() {
        if (PlatformUtil.isMac()) {
            DockVisibility.show();
        }
        try {
            primaryStage.show();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        primaryStage.centerOnScreen();
        primaryStage.requestFocus();
    }

    static void hideStage() {
        primaryStage.hide();
        if (PlatformUtil.isMac()) {
            DockVisibility.hide();
        }
    }

    private static void setJNALibraryPath() {
        if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
            return;
        }
        File path;
        if (runningFromIDE) {
            path = new File(jarDirectory.getParentFile().getParentFile().getParentFile(),
                    PlatformUtil.isMac() ? "dist/macos/Frameworks" : "dist/windows/lib");
        } else if (PlatformUtil.isMac()) {
            path = new File(jarDirectory.getParentFile(), "Frameworks/");
        } else { // if Windows
            path = new File(jarDirectory, "lib/");
        }
        System.setProperty("jna.boot.library.path", path.getAbsolutePath()); // path for jnidispatch lib
        System.setProperty("jna.library.path", path.getAbsolutePath());
        System.out.println("path = " + path.getAbsolutePath());
        // disable getting library w/ auto unpacking / classpath since it will never be in jar/classpath
        System.setProperty("jna.noclasspath", "true");
        System.setProperty("jna.nounpack", "true");
    }

    public static class JavaFxApplication extends Application {

        private static Application INSTANCE;

        public static Application getInstance() {
            return INSTANCE;
        }

        @Override
        public void init() {
            INSTANCE = this;
        }

        @Override
        public void start(Stage primaryStage) throws IOException {
            try {
                Main.primaryStage = primaryStage;
                Parent root = FXMLLoader.load(getClass().getResource("blobsaver.fxml"));
                primaryStage.setTitle("blobsaver " + Main.appVersion);
                primaryStage.setScene(new Scene(root));
                primaryStage.getScene().getStylesheets().add(getClass().getResource("app.css").toExternalForm());
                if (!PlatformUtil.isMac()) {
                    primaryStage.getIcons().clear();
                    primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("blob_emoji.png")));
                }
                primaryStage.setResizable(false);
                Controller.afterStageShowing();
                Platform.setImplicitExit(false);
                showStage();
                if (appPrefs.getBoolean("Start background immediately", false)) {
                    /* I have to show the stage then hide it again in Platform.runLater() otherwise
                     * the needed initialization code won't run at the right time when starting the background
                     * (for example, the macOS menu bar won't work properly if I don't do this)
                     */
                    Platform.runLater(() -> {
                        hideStage();
                        Background.startBackground(false);
                    });
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
                appPrefs.put("App version", appVersion);
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(-1);
            }
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
