package blobsaver;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class Controller {


    @FXML private ChoiceBox deviceTypeChoiceBox;
    @FXML private ChoiceBox deviceModelChoiceBox;
    @FXML private TextField ecidField;
    @FXML private TextField boardConfigField;
    @FXML private TextField apnonceField;
    @FXML private TextField versionField;
    @FXML private CheckBox apnonceCheckBox;
    @FXML private CheckBox versionCheckBox;
    @FXML private Label versionLabel;
    @FXML private Button preset1Button;
    @FXML private Button preset2Button;
    @FXML private Button preset3Button;
    private boolean boardConfig = false;
    private boolean editingPresets = false;

    @SuppressWarnings("unchecked")
    @FXML
    public void initialize() {
        ObservableList iPhones = FXCollections.observableArrayList("iPhone 5s (GSM)", "iPhone 5s (Global)",
                "iPhone 6+ ", "iPhone 6", "iPhone 6s", "iPhone 6s+", "iPhone SE", "iPhone 7 (Global)(iPhone9,1)",
                "iPhone 7+ (Global)(iPhone9,2)", "iPhone 7 (GSM)(iPhone9,3)", "iPhone 7+ (GSM)(iPhone9,4)",
                "iPhone 8 (iPhone10,1)", "iPhone 8+ (iPhone10,2)", "iPhone X (iPhone10,3)", "iPhone 8 (iPhone10,4)", "iPhone 8+ (iPhone10,5)", "iPhone X (iPhone10,6)");
        ObservableList iPods = FXCollections.observableArrayList();
        ObservableList iPads = FXCollections.observableArrayList();
        ObservableList AppleTVs = FXCollections.observableArrayList();
        deviceTypeChoiceBox.setItems(FXCollections.observableArrayList("iPhone", "iPod(not supported yet)", "iPad(not supported yet)", "AppleTV(not supported yet)"));

        deviceTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            String v = (String) newValue;
            switch (v) {
                case "iPhone":
                    deviceModelChoiceBox.setItems(iPhones);
                    versionLabel.setText("iOS Version");
                    break;
                case "iPod(not supported yet)":
                    deviceModelChoiceBox.setItems(iPods);
                    versionLabel.setText("iOS Version");
                    break;
                case "iPad(not supported yet)":
                    deviceModelChoiceBox.setItems(iPads);
                    versionLabel.setText("iOS Version");
                    break;
                case "AppleTV(not supported yet)":
                    deviceModelChoiceBox.setItems(AppleTVs);
                    versionLabel.setText("tvOS Version");
                    break;
            }
        });
        deviceModelChoiceBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            String v = (String) newValue;
            if (v.equals("iPhone 6s") || v.equals("iPhone 6s+") || v.equals("iPhone SE")) {
                boardConfig = true;
                boardConfigField.setDisable(false);
            } else {
                boardConfig = false;
                boardConfigField.setText("");
                boardConfigField.setDisable(true);
            }
        });
    }

    private void run(String device) {
        ArrayList<String> args = new ArrayList<String>(Arrays.asList(getClass().getResource("tsschecker").getPath(), "-d", device, "-s", "-e", ecidField.getText()));
        if (boardConfig) {
            args.add("--boardconfig");
            args.add(boardConfigField.getText());
        }
        if (apnonceCheckBox.isSelected()) {
            args.add("--apnonce");
            args.add(apnonceField.getText());
        }
        if (versionCheckBox.isSelected()) {
            args.add("-l");
        } else {
            args.add("-i");
            args.add(versionField.getText());
        }
        Process proc = null;
        try {
            proc = new ProcessBuilder(args).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                System.out.print(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void apnonceCheckBoxHandler() {
        if (apnonceCheckBox.isSelected()) {
            apnonceField.setDisable(false);
        } else {
            apnonceField.setDisable(true);
        }
    }

    public void versionCheckBoxHandler() {
        if (versionCheckBox.isSelected()) {
            versionField.setDisable(true);
        } else {
            versionField.setDisable(false);
        }
    }

    private void loadPreset(int preset) {
        File file;
        try {
            file = new File(getClass().getResource("preset" + Integer.toString(preset) + ".properties").toURI());
            if (file.exists()) {
                Properties prop = new Properties();
                try (InputStream input = new FileInputStream(file)) {
                    prop.load(input);
                    ecidField.setText(prop.getProperty("ecid"));
                    deviceTypeChoiceBox.setValue(prop.getProperty("deviceType"));
                    deviceModelChoiceBox.setValue(prop.getProperty("deviceModel"));
                    if (!prop.getProperty("boardConfig").equals("none")) {
                        boardConfigField.setText(prop.getProperty("boardConfig"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("No options file");
        }
    }

    public void presetButtonHandler(ActionEvent evt) {
        Button btn = (Button) evt.getTarget();
        String text = btn.getText();
        int preset = Integer.valueOf(text.substring(text.length() - 1));
        if (editingPresets) {
            saveOptions(preset);
            saveOptionsHandler();
        } else {
            loadPreset(preset);
        }
    }

    private void saveOptions(int preset) {
        Properties prop = new Properties();
        File file = new File(getClass().getResource("").toString().substring(5), "preset" + Integer.toString(preset) + ".properties");
        try (OutputStream output = new FileOutputStream(file)) {
            prop.setProperty("ecid", ecidField.getText());
            prop.setProperty("deviceType", (String) deviceTypeChoiceBox.getValue());
            prop.setProperty("deviceModel", (String) deviceModelChoiceBox.getValue());
            if (boardConfig) {
                prop.setProperty("boardConfig", boardConfigField.getText());
            } else {
                prop.setProperty("boardConfig", "none");
            }
            prop.store(output, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveOptionsHandler() {
        editingPresets = !editingPresets;
        if (editingPresets) {
            preset1Button.setText("Save in Preset 1");
            preset2Button.setText("Save in Preset 2");
            preset3Button.setText("Save in Preset 3");
        } else {
            preset1Button.setText("Load Preset 1");
            preset2Button.setText("Load Preset 2");
            preset3Button.setText("Load Preset 3");
        }
        System.out.println(editingPresets);
    }


    public void go() {
        if (ecidField.getText().equals("") || deviceModelChoiceBox.getValue().equals("") || (boardConfig && boardConfigField.getText().equals("")) || (apnonceCheckBox.isSelected() && apnonceField.getText().equals(""))) {
            return; // TODO: Print an error message
        }
        String deviceModel = (String) deviceModelChoiceBox.getValue();
        switch (deviceModel) {
            case "iPhone 5s (GSM)":
                run("iPhone6,1");
                break;
            case "iPhone 5s (Global)":
                run("iPhone6,2");
                break;
            case "iPhone 6+":
                run("iPhone7,1");
                break;
            case "iPhone 6":
                run("iPhone7,2");
                break;
            case "iPhone 6s":
                run("iPhone8,1");
                break;
            case "iPhone 6s+":
                run("iPhone8,2");
                break;
            case "iPhone SE":
                run("iPhone8,4");
                break;
            case "iPhone 7 (Global)(iPhone9,1)":
                run("iPhone9,1");
                break;
            case "iPhone 7+ (Global)(iPhone9,2)":
                run("iPhone9,2");
                break;
            case "iPhone 7 (GSM)(iPhone9,3)":
                run("iPhone9,3");
                break;
            case "iPhone 7+ (GSM)(iPhone9,4)":
                run("iPhone9,4");
                break;
            case "iPhone 8 (iPhone10,1)":
                run("iPhone10,1");
                break;
            case "iPhone 8+ (iPhone10,2)":
                run("iPhone10,2");
                break;
            case "iPhone X (iPhone10,3)":
                run("iPhone10,3");
                break;
            case "iPhone 8 (iPhone10,4)":
                run("iPhone10,4");
                break;
            case "iPhone 8+ (iPhone10,5)":
                run("iPhone10,5");
                break;
            case "iPhone X (iPhone10,6)":
                run("iPhone10,6");
            default:
                System.out.print("");
                break;
        }
    }
}
