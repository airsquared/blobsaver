package blobsaver;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URI;
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
    @FXML private TextField identifierField;

    @FXML private CheckBox apnonceCheckBox;
    @FXML private CheckBox versionCheckBox;
    @FXML private CheckBox identifierCheckBox;

    @FXML private Label versionLabel;

    @FXML private Button preset1Button;
    @FXML private Button preset2Button;
    @FXML private Button preset3Button;
    @FXML private Button goButton;

    private boolean boardConfig = false;
    private boolean editingPresets = false;

    private DropShadow errorBorder = new DropShadow();

    private ButtonType redditPM = new ButtonType("PM on Reddit");
    private ButtonType githubIssue = new ButtonType("Create Issue on Github");

    @SuppressWarnings("unchecked")
    @FXML
    public void initialize() {
        final ObservableList iPhones = FXCollections.observableArrayList("iPhone 3G[S]", "iPhone 4 (GSM)",
                "iPhone 4 (GSM 2012)", "iPhone 4 (CDMA)", "iPhone 4[S]", "iPhone 5 (GSM)", "iPhone 5 (Global)",
                "iPhone 5c (GSM)", "iPhone 5c (Global)", "iPhone 5s (GSM)", "iPhone 5s (Global)",
                "iPhone 6+ ", "iPhone 6", "iPhone 6s", "iPhone 6s+", "iPhone SE", "iPhone 7 (Global)(iPhone9,1)",
                "iPhone 7+ (Global)(iPhone9,2)", "iPhone 7 (GSM)(iPhone9,3)", "iPhone 7+ (GSM)(iPhone9,4)",
                "iPhone 8 (iPhone10,1)", "iPhone 8+ (iPhone10,2)", "iPhone X (iPhone10,3)", "iPhone 8 (iPhone10,4)",
                "iPhone 8+ (iPhone10,5)", "iPhone X (iPhone10,6)", "");
        final ObservableList iPods = FXCollections.observableArrayList("iPod Touch 3", "iPod Touch 4", "iPod Touch 5", "iPod Touch 6", "");
        final ObservableList iPads = FXCollections.observableArrayList("iPad 1", "iPad 2 (WiFi)", "iPad 2 (GSM)",
                "iPad 2 (CDMA)", "iPad 2 (Mid 2012)", "iPad Mini (Wifi)", "iPad Mini (GSM)", "iPad Mini (Global)",
                "iPad 3 (WiFi)", "iPad 3 (CDMA)", "iPad 3 (GSM)", "iPad 4 (WiFi)", "iPad 4 (GSM)", "iPad 4 (Global)",
                "iPad Air (Wifi)", "iPad Air (Cellular)", "iPad Air (China)", "iPad Mini 2 (WiFi)", "iPad Mini 2 (Cellular)",
                "iPad Mini 2 (China)", "iPad Mini 3 (WiFi)", "iPad Mini 3 (Cellular)", "iPad Mini 3 (China)",
                "iPad Mini 4 (Wifi)", "iPad Mini 4 (Cellular)", "iPad Air 2 (WiFi)", "iPad Air 2 (Cellular)",
                "iPad Pro 9.7 (Wifi)", "iPad Pro 9.7 (Cellular)", "iPad Pro 12.9 (WiFi)", "iPad Pro 12.9 (Cellular)",
                "iPad 5 (Wifi)", "iPad 5 (Cellular)", "iPad Pro 2 12.9 (WiFi)(iPad7,1)", "iPad Pro 2 12.9 (Cellular)(iPad7,2)",
                "iPad Pro 10.5 (WiFi)(iPad7,3)", "iPad 10.5 (Cellular)(iPad7,4)", "iPad 6 (WiFi)(iPad 7,5)", "iPad 6 (Cellular)(iPad7,6)", "");
        final ObservableList AppleTVs = FXCollections.observableArrayList("Apple TV 2G", "Apple TV 3", "Apple TV 3 (2013)", "Apple TV 4 (2015)", "Apple TV 4K", "");
        deviceTypeChoiceBox.setItems(FXCollections.observableArrayList("iPhone", "iPod", "iPad", "AppleTV", ""));

        deviceTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            String v = (String) newValue;
            switch (v) {
                case "iPhone":
                    deviceModelChoiceBox.setItems(iPhones);
                    versionLabel.setText("iOS Version");
                    break;
                case "iPod":
                    deviceModelChoiceBox.setItems(iPods);
                    versionLabel.setText("iOS Version");
                    break;
                case "iPad":
                    deviceModelChoiceBox.setItems(iPads);
                    versionLabel.setText("iOS Version");
                    break;
                case "AppleTV":
                    deviceModelChoiceBox.setItems(AppleTVs);
                    versionLabel.setText("tvOS Version");
                    break;
            }
        });
        deviceModelChoiceBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            String v = (String) newValue;
            if (v.equals("iPhone 6s") || v.equals("iPhone 6s+") || v.equals("iPhone SE")) {
                int depth = 20;
                DropShadow borderGlow = new DropShadow();
                borderGlow.setOffsetY(0f);
                borderGlow.setOffsetX(0f);
                borderGlow.setColor(Color.DARKCYAN);
                borderGlow.setWidth(depth);
                borderGlow.setHeight(depth);
                boardConfigField.setEffect(borderGlow);
                boardConfig = true;
                boardConfigField.setDisable(false);
            } else {
                boardConfigField.setEffect(null);
                boardConfig = false;
                boardConfigField.setText("");
                boardConfigField.setDisable(true);
            }
        });
        identifierField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.equals("iPhone8,1") || newValue.equals("iPhone8,2") || newValue.equals("iPhone8,4")) {
                int depth = 20;
                DropShadow borderGlow = new DropShadow();
                borderGlow.setOffsetY(0f);
                borderGlow.setOffsetX(0f);
                borderGlow.setColor(Color.DARKCYAN);
                borderGlow.setWidth(depth);
                borderGlow.setHeight(depth);
                boardConfigField.setEffect(borderGlow);
                boardConfig = true;
                boardConfigField.setDisable(false);
            } else {
                boardConfigField.setEffect(null);
                boardConfig = false;
                boardConfigField.setText("");
                boardConfigField.setDisable(true);
            }
        });
        goButton.setDefaultButton(true);


        errorBorder.setOffsetY(0f);
        errorBorder.setOffsetX(0f);
        errorBorder.setColor(Color.RED);
        errorBorder.setWidth(20);
        errorBorder.setHeight(20);
    }

    private void run(String device) {
        ArrayList<String> args = new ArrayList<>(Arrays.asList(getClass().getResource("tsschecker").getPath(), "-d", device, "-s", "-e", ecidField.getText()));
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
            Alert alert = new Alert(Alert.AlertType.ERROR, "There was an error starting tsschecker.\n\nPlease create a new issue on Github or PM me on Reddit. The crash log has been copied to your clipboard", githubIssue, redditPM, ButtonType.CANCEL);
            StringSelection stringSelection = new StringSelection(e.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            alert.showAndWait();
            try {
                if (alert.getResult().equals(githubIssue) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new"));

                } else if (alert.getResult().equals(redditPM) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
                }
            } catch (IOException | URISyntaxException ee) {
                ee.printStackTrace();
            }
            e.printStackTrace();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                System.out.print(line + "\n");
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "There was an error getting the tsschecker log.\n\nPlease create a new issue on Github or PM me on Reddit. The crash log has been copied to your clipboard", githubIssue, redditPM, ButtonType.CANCEL);
            StringSelection stringSelection = new StringSelection(e.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            alert.showAndWait();
            try {
                if (alert.getResult().equals(githubIssue) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new"));

                } else if (alert.getResult().equals(redditPM) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
                }
            } catch (IOException | URISyntaxException ee) {
                ee.printStackTrace();
            }
            e.printStackTrace();
        }

        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The tsschecker process was interrupted.\n\nPlease create a new issue on Github or PM me on Reddit. The crash log has been copied to your clipboard", githubIssue, redditPM, ButtonType.CANCEL);
            StringSelection stringSelection = new StringSelection(e.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            alert.showAndWait();
            try {
                if (alert.getResult().equals(githubIssue) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new"));

                } else if (alert.getResult().equals(redditPM) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
                }
            } catch (IOException | URISyntaxException ee) {
                ee.printStackTrace();
            }
        }
    }

    public void apnonceCheckBoxHandler() {
        if (apnonceCheckBox.isSelected()) {
            apnonceField.setDisable(false);
            int depth = 20;
            DropShadow borderGlow = new DropShadow();
            borderGlow.setOffsetY(0f);
            borderGlow.setOffsetX(0f);
            borderGlow.setColor(Color.DARKCYAN);
            borderGlow.setWidth(depth);
            borderGlow.setHeight(depth);
            apnonceField.setEffect(borderGlow);
        } else {
            apnonceField.setEffect(null);
            apnonceField.setText("");
            apnonceField.setDisable(true);
        }
    }

    public void versionCheckBoxHandler() {
        if (versionCheckBox.isSelected()) {
            versionField.setDisable(true);
            versionField.setEffect(null);
            versionField.setText("");
        } else {
            int depth = 20;
            DropShadow borderGlow = new DropShadow();
            borderGlow.setOffsetY(0f);
            borderGlow.setOffsetX(0f);
            borderGlow.setColor(Color.DARKCYAN);
            borderGlow.setWidth(depth);
            borderGlow.setHeight(depth);
            versionField.setEffect(borderGlow);

            versionField.setDisable(false);
        }
    }

    @SuppressWarnings("unchecked")
    public void identifierCheckBoxHandler() {
        if (identifierCheckBox.isSelected()) {
            identifierField.setDisable(false);
            int depth = 20;
            DropShadow borderGlow = new DropShadow();
            borderGlow.setOffsetY(0f);
            borderGlow.setOffsetX(0f);
            borderGlow.setColor(Color.DARKCYAN);
            borderGlow.setWidth(depth);
            borderGlow.setHeight(depth);
            identifierField.setEffect(borderGlow);
            deviceTypeChoiceBox.setValue("");
            deviceModelChoiceBox.setValue("");
            deviceTypeChoiceBox.setDisable(true);
            deviceModelChoiceBox.setDisable(true);
        } else {
            identifierField.setEffect(null);
            identifierField.setText("");
            identifierField.setDisable(true);
            deviceTypeChoiceBox.setDisable(false);
            deviceModelChoiceBox.setDisable(false);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPreset(int preset) {
        File file;
        try {
            file = new File(getClass().getResource("preset" + Integer.toString(preset) + ".properties").toURI());
            if (file.exists()) {
                Properties prop = new Properties();
                try (InputStream input = new FileInputStream(file)) {
                    prop.load(input);
                    ecidField.setText(prop.getProperty("ecid"));
                    if (prop.getProperty("deviceModel").equals("none")) {
                        identifierCheckBox.fire();
                        identifierField.setText(prop.getProperty("deviceIdentifier"));
                    } else {
                        deviceTypeChoiceBox.setValue(prop.getProperty("deviceType"));
                        deviceModelChoiceBox.setValue(prop.getProperty("deviceModel"));
                    }
                    if (!prop.getProperty("boardConfig").equals("none")) {
                        boardConfigField.setText(prop.getProperty("boardConfig"));
                    }
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "There was an error loading the profile.\n\nPlease create a new issue on Github or PM me on Reddit. The crash log has been copied to your clipboard", githubIssue, redditPM, ButtonType.CANCEL);
                    StringSelection stringSelection = new StringSelection(e.toString());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                    alert.showAndWait();
                    try {
                        if (alert.getResult().equals(githubIssue) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new"));

                        } else if (alert.getResult().equals(redditPM) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
                        }
                    } catch (IOException | URISyntaxException ee) {
                        ee.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Preset " + preset + " does not have anything", ButtonType.OK);
            alert.showAndWait();
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
        boolean doReturn = false;
        if (ecidField.getText().equals("")) {
            ecidField.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && ((deviceModelChoiceBox.getValue() == null) || (deviceModelChoiceBox.getValue() == ""))) {
            deviceModelChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        if (identifierCheckBox.isSelected() && identifierField.getText().equals("")) {
            identifierField.setEffect(errorBorder);
            doReturn = true;
        }
        if (boardConfig && boardConfigField.getText().equals("")) {
            boardConfigField.setEffect(errorBorder);
            doReturn = true;
        }
        if (doReturn) {
            return;
        }
        Properties prop = new Properties();
        File file = new File(getClass().getResource("").toString().substring(5), "preset" + Integer.toString(preset) + ".properties");
        try (OutputStream output = new FileOutputStream(file)) {
            prop.setProperty("ecid", ecidField.getText());
            if (identifierCheckBox.isSelected()) {
                prop.setProperty("deviceType", "none");
                prop.setProperty("deviceModel", "none");
                prop.setProperty("deviceIdentifier", identifierField.getText());
            } else {
                prop.setProperty("deviceType", (String) deviceTypeChoiceBox.getValue());
                prop.setProperty("deviceModel", (String) deviceModelChoiceBox.getValue());
            }
            if (boardConfig) {
                prop.setProperty("boardConfig", boardConfigField.getText());
            } else {
                prop.setProperty("boardConfig", "none");
            }
            prop.store(output, null);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "There was an error while saving the data.\n\nPlease create a new issue on Github or PM me on Reddit. The crash log has been copied to your clipboard", githubIssue, redditPM, ButtonType.CANCEL);
            StringSelection stringSelection = new StringSelection(e.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            alert.showAndWait();
            try {
                if (alert.getResult().equals(githubIssue) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new"));

                } else if (alert.getResult().equals(redditPM) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
                }
            } catch (IOException | URISyntaxException ee) {
                ee.printStackTrace();
            }

            e.printStackTrace();
        }
    }

    public void saveOptionsHandler() {
        editingPresets = !editingPresets;
        if (editingPresets) {
            int depth = 40;
            DropShadow borderGlow = new DropShadow();
            borderGlow.setOffsetY(0f);
            borderGlow.setOffsetX(0f);
            borderGlow.setColor(Color.CYAN);
            borderGlow.setWidth(depth);
            borderGlow.setHeight(depth);

            preset1Button.setEffect(borderGlow);
            preset2Button.setEffect(borderGlow);
            preset3Button.setEffect(borderGlow);
            preset1Button.setText("Preset 1");
            preset2Button.setText("Preset 2");
            preset3Button.setText("Preset 3");
        } else {
            preset1Button.setEffect(null);
            preset2Button.setEffect(null);
            preset3Button.setEffect(null);
            preset1Button.setText("Load Preset 1");
            preset2Button.setText("Load Preset 2");
            preset3Button.setText("Load Preset 3");
        }
    }


    public void go() {
        boolean doReturn = false;
        if (ecidField.getText().equals("")) {
            ecidField.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && ((deviceModelChoiceBox.getValue() == null) || (deviceModelChoiceBox.getValue() == ""))) {
            deviceModelChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        if (identifierCheckBox.isSelected() && identifierField.getText().equals("")) {
            identifierField.setEffect(errorBorder);
            doReturn = true;
        }
        if (boardConfig && boardConfigField.getText().equals("")) {
            boardConfigField.setEffect(errorBorder);
            doReturn = true;
        }
        if (apnonceCheckBox.isSelected() && apnonceField.getText().equals("")) {
            apnonceField.setEffect(errorBorder);
            doReturn = true;
        }
        if (doReturn) {
            return;
        }
        String deviceModel = (String) deviceModelChoiceBox.getValue();
        switch (deviceModel) {
            case "iPhone 3G[S]":
                run("iPhone2,1");
                break;
            case "iPhone 4 (GSM)":
                run("iPhone3,1");
                break;
            case "iPhone 4 (GSM 2012)":
                run("iPhone3,2");
                break;
            case "iPhone 4 (CDMA)":
                run("iPhone3,3");
                break;
            case "iPhone 4[S]":
                run("iPhone4,1");
                break;
            case "iPhone 5 (GSM)":
                run("iPhone5,1");
                break;
            case "iPhone 5 (Global)":
                run("iPhone5,2");
                break;
            case "iPhone 5c (GSM)":
                run("iPhone5,3");
                break;
            case "iPhone 5c (Global)":
                run("iPhone5,4");
                break;
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
                break;
            case "iPod Touch 3":
                run("iPod3,1");
                break;
            case "iPod Touch 4":
                run("iPod4,1");
                break;
            case "iPod Touch 5":
                run("iPod5,1");
                break;
            case "iPod Touch 6":
                run("iPod7,1");
                break;
            case "Apple TV 2G":
                run("AppleTV2,1");
                break;
            case "Apple TV 3":
                run("AppleTV3,1");
                break;
            case "Apple TV 3 (2013)":
                run("AppleTV3,2");
                break;
            case "Apple TV 4 (2015)":
                run("AppleTV5,3");
                break;
            case "Apple TV 4K":
                run("AppleTV6,2");
                break;
            case "iPad 1":
                run("iPad1,1");
                break;
            case "iPad 2 (WiFi)":
                run("iPad2,1");
                break;
            case "iPad 2 (GSM)":
                run("iPad2,2");
                break;
            case "iPad 2 (CDMA)":
                run("iPad2,3");
                break;
            case "iPad 2 (Mid 2012)":
                run("iPad2,4");
                break;
            case "iPad Mini (Wifi)":
                run("iPad2,5");
                break;
            case "iPad Mini (GSM)":
                run("iPad2,6");
                break;
            case "iPad Mini (Global)":
                run("iPad2,7");
                break;
            case "iPad 3 (WiFi)":
                run("iPad3,1");
                break;
            case "iPad 3 (CDMA)":
                run("iPad3,2");
                break;
            case "iPad 3 (GSM)":
                run("iPad3,3");
                break;
            case "iPad 4 (WiFi)":
                run("iPad3,4");
                break;
            case "iPad 4 (GSM)":
                run("iPad3,5");
                break;
            case "iPad 4 (Global)":
                run("iPad3,6");
                break;
            case "iPad Air (Wifi)":
                run("iPad4,1");
                break;
            case "iPad Air (Cellular)":
                run("iPad4,2");
                break;
            case "iPad Air (China)":
                run("iPad4,3");
                break;
            case "iPad Mini 2 (WiFi)":
                run("iPad4,4");
                break;
            case "iPad Mini 2 (Cellular)":
                run("iPad4,5");
                break;
            case "iPad Mini 2 (China)":
                run("iPad4,6");
                break;
            case "iPad Mini 3 (WiFi)":
                run("iPad4,7");
                break;
            case "iPad Mini 3 (Cellular)":
                run("iPad4,8");
                break;
            case "iPad Mini 3 (China)":
                run("iPad4,9");
                break;
            case "iPad Mini 4 (Wifi)":
                run("iPad5,1");
                break;
            case "iPad Mini 4 (Cellular)":
                run("iPad5,2");
                break;
            case "iPad Air 2 (WiFi)":
                run("iPad5,3");
                break;
            case "iPad Air 2 (Cellular)":
                run("iPad5,4");
                break;
            case "iPad Pro 9.7 (Wifi)":
                run("iPad6,3");
                break;
            case "iPad Pro 9.7 (Cellular)":
                run("iPad6,4");
                break;
            case "iPad Pro 12.9 (WiFi)":
                run("iPad6,7");
                break;
            case "iPad Pro 12.9 (Cellular)":
                run("iPad6,8");
                break;
            case "iPad 5 (Wifi)":
                run("iPad6,11");
                break;
            case "iPad 5 (Cellular)":
                run("iPad6,12");
                break;
            case "iPad Pro 2 12.9 (WiFi)(iPad7,1)":
                run("iPad7,1");
                break;
            case "iPad Pro 2 12.9 (Cellular)(iPad7,2)":
                run("iPad7,2");
                break;
            case "iPad Pro 10.5 (WiFi)(iPad7,3)":
                run("iPad7,3");
                break;
            case "iPad 10.5 (Cellular)(iPad7,4)":
                run("iPad7,4");
                break;
            case "iPad 6 (WiFi)(iPad 7,5)":
                run("iPad7,6");
                break;
            case "iPad 6 (Cellular)(iPad7,6)":
                run("iPad7,6");
                break;
            case "":
                String identifierText = identifierField.getText();
                try {
                    // Throws StringIndexOutOfBoundsException even if identifier is correct if I don't do it like this:
                    if (identifierText.substring(0, 4).equals("iPad")) {
                        run(identifierField.getText());
                    } else if (identifierText.substring(0, 4).equals("iPod")) {
                        run(identifierField.getText());
                    } else if (identifierText.substring(0, 6).equals("iPhone")) {
                        run(identifierField.getText());
                    } else if (identifierText.substring(0, 7).equals("AppleTV")) {
                        run(identifierField.getText());
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "\"" + identifierText +
                                "\" is not a valid identifier", ButtonType.OK, ButtonType.CANCEL);
                        alert.showAndWait();
                        return;
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "\"" + identifierText +
                            "\" is not a valid identifier", ButtonType.OK, ButtonType.CANCEL);
                    alert.showAndWait();
                    return;
                }

                break;
            default:
                Alert alert = new Alert(Alert.AlertType.ERROR, "Could not find: \"" + deviceModel +
                        "\"\n\nPlease create a new Github issue or PM me on Reddit", githubIssue, redditPM, ButtonType.CANCEL);
                alert.showAndWait();
                try {
                    if (alert.getResult().equals(githubIssue) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/issues/new"));

                    } else if (alert.getResult().equals(redditPM) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report"));
                    }
                } catch (IOException | URISyntaxException ee) {
                    ee.printStackTrace();
                }
                break;
        }
    }
}
