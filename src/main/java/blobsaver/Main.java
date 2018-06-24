package blobsaver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    static final String appVersion = "v1.1";
    static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Main.primaryStage = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("blobsaver.fxml"));
        primaryStage.setTitle("SHSH Blob Saver " + appVersion);
        primaryStage.setScene(new Scene(root, 685, 580));
        primaryStage.getScene().getStylesheets().add(getClass().getResource("app.css").toExternalForm());
        primaryStage.show();
        primaryStage.setResizable(false);
        Controller.setPresetButtonNames();
    }
}
