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

import com.sun.javafx.PlatformUtil;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

public class Main {

    static final String appVersion = "v3.0b0";
    static Stage primaryStage;
    static final File jarDirectory;
    static final boolean runningFromJar;

    static { // set jarDirectory and runningFromJar variables
        final String url = Main.class.getResource("Main.class").toString();
        String path = url.substring(0, url.length() - "airsquared/blobsaver/app/Main.class".length());
        if (path.startsWith("jar:")) {
            runningFromJar = true;
            path = path.substring("jar:".length(), path.length() - 2);
        } else {
            runningFromJar = false;
        }
        jarDirectory = Paths.get(URI.create(path)).getParent().toFile();
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
        try {
            Class.forName("javafx.application.Application");
            if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
                try {
                    JUnique.acquireLock("airsquared.blobsaver.app");
                } catch (AlreadyLockedException e) {
                    javax.swing.JOptionPane.showMessageDialog(null, "blobsaver already running, exiting");
                    System.exit(-1);
                }
            }
            JavaFxApplication.launch(JavaFxApplication.class, args);
        } catch (ClassNotFoundException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "JavaFX is not installed. " +
                    "Either install Oracle Java or\nif you are using OpenJRE/OpenJDK, install openjfx." +
                    "\nOn Linux, use sudo apt-get install openjfx", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    private static void setJNALibraryPath() {
        if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
            return;
        }
        File path;
        if (!runningFromJar) {
            path = new File(Utils.getPlatformDistDir(), PlatformUtil.isMac() ? "Frameworks" : "lib");
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
                root = FXMLLoader.load(getClass().getResource("blobsaver.fxml"));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            primaryStage.setTitle("blobsaver");
            primaryStage.setScene(new Scene(root));
            if (PlatformUtil.isWindows()) {
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
