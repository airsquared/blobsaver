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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import static com.sun.jna.Platform.isMac;
import static com.sun.jna.Platform.isWindows;

public class Main {

    static final String appVersion = "v3.0b0";
    static Stage primaryStage;
    // make sure to set system property before running (automatically set if running from gradle)
    static final File jarDirectory;

    static {
        try {
            jarDirectory = new File(System.getProperty("jar.directory")).getCanonicalFile();
            System.out.println(jarDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Enables a menu item in the system tray to activate a breakpoint when in background and
     * replaces the question mark help labels with activating a breakpoint instead.
     * Remember to add a breakpoint in the correct methods to use this.
     */
    static final boolean SHOW_BREAKPOINT = false;

    public static void main(String[] args) {
        setJNALibraryPath();
        if (args.length == 1 && args[0].equals("--background-autosave")) {
            Background.saveAllBackgroundBlobs(); // don't unnecessarily initialize any FX
            return;
        }
        JavaFxApplication.launch(JavaFxApplication.class, args);
    }

    public static void exit() {
        Platform.exit();
    }

    static void setJNALibraryPath() {
        if (!isMac() && !isWindows()) {
            return;
        }
        String path;
        if (isMac()) {
            path = new File(jarDirectory, "Frameworks/").getAbsolutePath();
        } else { // if Windows
            path = new File(jarDirectory, "lib/").getAbsolutePath();
        }
        System.setProperty("jna.boot.library.path", path); // path for jnidispatch lib
        System.setProperty("jna.library.path", path);
        System.out.println("path = " + path);
        // disable getting library w/ auto unpacking / classpath since it will never be in jar/classpath
        System.setProperty("jna.noclasspath", "true");
        System.setProperty("jna.nounpack", "true");
    }

    public static final class JavaFxApplication extends Application {

        private static Application INSTANCE;

        public static Application getInstance() {
            return INSTANCE;
        }

        public JavaFxApplication() {
            super();
            INSTANCE = this;
        }

        @Override
        public void start(Stage primaryStage) {
            Main.primaryStage = primaryStage;
            Parent root = null;
            try {
                root = FXMLLoader.load(Main.class.getResource("blobsaver.fxml"));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            primaryStage.setTitle("blobsaver");
            primaryStage.setScene(new Scene(root));
            if (isWindows()) {
                primaryStage.getIcons().clear();
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("blob_emoji.png")));
            }
            primaryStage.setResizable(false);
            Utils.checkForUpdates(false);
            primaryStage.show();
            Prefs.setLastAppVersion(appVersion);
        }

    }
}
