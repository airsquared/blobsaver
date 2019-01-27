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
import de.codecentric.centerdevice.util.StageUtils;
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
import java.util.Objects;
import java.util.prefs.Preferences;

public class Main {

    //enables debug menu icon in tray icon and to create a breakpoint.
    //You can also create a breakpoint on the helpLabelHandler() function and click the question marks to debug
    static final boolean DEBUG_MODE = false;

    static final String appVersion = "v2.2.3";
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
                /*if (appPrefs.getBoolean("Start background immediately", false) && PlatformUtil.isMac()) {
                    System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
//                    PlatformImpl.setTaskbarApplication(false);
                }*/
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

    static Stage getStage(String stageName) { //get stage with given name 'stageName'. Returns null if not found
        for (Stage stage : StageUtils.getStages()) {
            if (Objects.equals(stage.getTitle(), stageName)) { //if both are null or strings are same
                return stage;
            }
        }
        return null;
    }

    static void showStage() {
        //if not actually hidden, (just not in focus) no need to show dock icon again or center on screen.
        if (!Main.primaryStage.isShowing()) {
            if (firstTimeShown) {
                if (PlatformUtil.isMac()) { // remove the dummy stage generated by libdockvisibility

                    Stage invisibleStage;   // TODO: implement this in libdockvisibility instead
                    if ((invisibleStage = getStage(null)) != null) { //if exists a stage named null
                        invisibleStage.hide();
                    }
                }
                firstTimeShown = false;
            } else { //centers on screen if it's not the first time it's opened
                System.out.println("attempting to center primaryStage");
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                primaryStage.setX((screenBounds.getWidth() - primaryStage.getWidth()) / 2);
                primaryStage.setY((screenBounds.getHeight() - primaryStage.getHeight()) / 2);
            }

            if (PlatformUtil.isMac()) { //show the dock icon
                System.out.println("showing dock icon");
                DockVisibility.INSTANCE.show();
            }
        }

        //centers on screen if actually hidden and not the first launch

        primaryStage.show();
        primaryStage.toFront();
        primaryStage.requestFocus();
    }

    static void hideStage() {
        primaryStage.hide();
        if (PlatformUtil.isMac()) {
            System.out.println("hiding dock icon");
            DockVisibility.INSTANCE.hide();
        }
    }

    static void quit() {
        if (Background.inBackground) {
            Background.executor.shutdownNow();
            Platform.runLater(Platform::exit);
//            SystemTray.getSystemTray().remove(Background.trayIcon);
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

            appPrefs.put("App version", appVersion);
        }

        /*private void addShutdownListener() { //doesn't seem to work
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("got here!!");
                Background.executor.shutdownNow();
                Platform.exit();
                System.out.println("did i get here??");
                System.exit(0);
            }));
        }*/
    }
}