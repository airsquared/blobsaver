package blobsaver;

import com.sun.javafx.PlatformUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main {

    static final String appVersion = "v1.2";
    static Stage primaryStage;

    public static void main(String[] args) {
        try {
            Class.forName("javafx.application.Application");
            if (PlatformUtil.isMac() || PlatformUtil.isWindows() || PlatformUtil.isLinux()) {
                JavaFxApplication.launchit(args);
            } else {
                int result = javax.swing.JOptionPane.showOptionDialog(null, "Cannot detect the OS. Assuming it is Linux. Continue?",
                        "Warning", javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE, null, null, null);
                if (result == javax.swing.JOptionPane.CANCEL_OPTION) {
                    System.exit(0);
                }
                JavaFxApplication.launchit(args);
            }
        } catch (ClassNotFoundException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "JavaFX is not installed. Either install Oracle Java or\nif you are using OpenJRE/OpenJDK, install openjfx.\nOn Linux, use sudo apt-get install openjfx", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    public static class JavaFxApplication extends Application {

        static void launchit(String[] args) {
            launch(args);
        }

        @Override
        public void start(Stage primaryStage) throws IOException {
            Main.primaryStage = primaryStage;
            Parent root = FXMLLoader.load(getClass().getResource("blobsaver.fxml"));
            primaryStage.setTitle("SHSH Blob Saver " + Main.appVersion);
            primaryStage.setScene(new Scene(root));
            primaryStage.getScene().getStylesheets().add(getClass().getResource("app.css").toExternalForm());
            primaryStage.show();
            primaryStage.setResizable(false);
            Controller.setPresetButtonNames();
        }
    }
}