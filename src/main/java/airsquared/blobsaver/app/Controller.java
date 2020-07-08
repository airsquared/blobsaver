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
import com.sun.javafx.scene.control.skin.LabeledText;
import com.sun.jna.ptr.PointerByReference;
import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Controller {


    @FXML private MenuBar menuBar;
    @FXML private MenuItem checkForUpdatesMenuItem, clearAllDataMenuItem, checkForValidBlobsMenuItem, debugLogMenuItem;

    @FXML private ChoiceBox<String> deviceTypeChoiceBox, deviceModelChoiceBox;

    @FXML private TextField ecidField, boardConfigField, apnonceField, versionField, identifierField,
            pathField, ipswField;

    @FXML private CheckBox apnonceCheckBox, versionCheckBox, identifierCheckBox, betaCheckBox;

    @FXML private Label versionLabel;

    @FXML private Button readApnonceButton, startBackgroundButton, chooseTimeToRunButton, forceCheckForBlobs,
            backgroundSettingsButton, savePresetButton;

    @FXML private Button preset1Button, preset2Button, preset3Button, preset4Button, preset5Button, preset6Button,
            preset7Button, preset8Button, preset9Button, preset10Button;
    private List<Button> presetButtons;

    @FXML private VBox presetVBox;

    @FXML private Button goButton;

    private Alert runningAlert;
    private DialogPane aboutStage;

    private final SimpleBooleanProperty getBoardConfig = new SimpleBooleanProperty();
    private boolean editingPresets = false;
    private boolean choosingRunInBackground = false;


    @FXML
    public void initialize() {
        deviceTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((x, y, newValue) -> {
            deviceTypeChoiceBox.setEffect(null);
            switch (newValue == null ? "" : newValue) {
                case "iPhone":
                    deviceModelChoiceBox.setItems(Devices.getiPhones());
                    versionLabel.setText("iOS Version");
                    break;
                case "iPod":
                    deviceModelChoiceBox.setItems(Devices.getiPods());
                    versionLabel.setText("iOS Version");
                    break;
                case "iPad":
                    deviceModelChoiceBox.setItems(Devices.getiPads());
                    versionLabel.setText("iOS/iPadOS Version");
                    break;
                case "AppleTV":
                    deviceModelChoiceBox.setItems(Devices.getAppleTVs());
                    versionLabel.setText("tvOS Version");
                    break;
                default:
                    versionLabel.setText("Version");
                    break;
            }
        });
        deviceModelChoiceBox.getSelectionModel().selectedItemProperty().addListener((x, y, deviceModel) -> {
            deviceModelChoiceBox.setEffect(null);
            String identifier = Devices.getDeviceModelIdentifiersMap().get(deviceModel);
            requireBoardConfig(identifier);
            requireApnonce(identifier);
        });
        identifierField.textProperty().addListener((x, y, identifier) -> {
            identifierField.setEffect(null);
            requireBoardConfig(identifier);
            requireApnonce(identifier);
        });

        Utils.addListenerToSetNullEffect(ecidField, versionField, boardConfigField, apnonceField, pathField, ipswField);
        setBindings();

        presetButtons = Arrays.asList(preset1Button, preset2Button, preset3Button, preset4Button, preset5Button, preset6Button, preset7Button, preset8Button, preset9Button, preset10Button);
        for (int i = 1; i <= 10; i++) { // sets the names for the presets
            if (!Utils.isEmptyOrNull(Main.appPrefs.get("Name Preset" + i, ""))) {
                Button btn = presetButtons.get(i - 1);
                btn.setText("Load " + Main.appPrefs.get("Name Preset" + i, ""));
            }
        }

        pathField.setText(System.getProperty("user.home") + File.separator + "Blobs");

        if (PlatformUtil.isMac()) {
            useMacOSMenuBar();
        }
    }

    private void setBindings() {
        BooleanExpression disableApnonce = apnonceCheckBox.selectedProperty().not();
        apnonceField.disableProperty().bind(disableApnonce);
        readApnonceButton.disableProperty().bind(disableApnonce);

        BooleanExpression disableChoiceBoxes = identifierCheckBox.selectedProperty();
        deviceTypeChoiceBox.disableProperty().bind(disableChoiceBoxes);
        deviceModelChoiceBox.disableProperty().bind(disableChoiceBoxes);
        identifierField.disableProperty().bind(disableChoiceBoxes.not());

        BooleanExpression disableIPSW = betaCheckBox.selectedProperty().not();
        ipswField.disableProperty().bind(disableIPSW);

        boardConfigField.disableProperty().bind(getBoardConfig.not());
    }

    public void newGithubIssue() { Utils.newGithubIssue(); }

    public void sendRedditPM() { Utils.sendRedditPM(); }

    private void requireBoardConfig(String identifier) {
        if (!Utils.isEmptyOrNull(identifier) && Devices.doesRequireBoardConfig(identifier)) {
            getBoardConfig.set(true);
            boardConfigField.setEffect(Utils.borderGlow);
            boardConfigField.setText(Devices.getRequiresBoardConfigMap().get(identifier));
        } else if (getBoardConfig.get()) {
            boardConfigField.setEffect(null);
            getBoardConfig.set(false);
            boardConfigField.setText("");
        }
    }

    private void requireApnonce(String identifier) {
        if (!Utils.isEmptyOrNull(identifier) && Devices.doesRequireApnonce(identifier)) {
            if (!apnonceCheckBox.isSelected()) {
                apnonceCheckBox.fire();
            }
            apnonceCheckBox.setDisable(true);
        } else {
            apnonceCheckBox.setDisable(false);
            if (apnonceCheckBox.isSelected()) {
                apnonceCheckBox.fire();
            }
        }
    }

    public void checkForUpdatesHandler() { Utils.checkForUpdates(true); }

    public void apnonceCheckBoxHandler() {
        if (apnonceCheckBox.isSelected()) {
            apnonceField.setEffect(Utils.borderGlow);
        } else {
            apnonceField.setEffect(null);
            apnonceField.setText("");
        }
    }

    public void versionCheckBoxHandler() {
        if (versionCheckBox.isSelected()) {
            versionField.setDisable(true);
            versionField.setEffect(null);
            versionField.setText("");
        } else {
            versionField.setEffect(Utils.borderGlow);
            versionField.setDisable(false);
        }
    }

    public void identifierCheckBoxHandler() {
        if (identifierCheckBox.isSelected()) {
            identifierField.setEffect(Utils.borderGlow);
            deviceTypeChoiceBox.getSelectionModel().clearSelection();
            deviceModelChoiceBox.getSelectionModel().clearSelection();
            deviceTypeChoiceBox.setEffect(null);
            deviceModelChoiceBox.setEffect(null);
        } else {
            identifierField.setEffect(null);
            identifierField.setText("");
        }
    }

    public void betaCheckBoxHandler() {
        if (betaCheckBox.isSelected()) {
            ipswField.setEffect(Utils.borderGlow);
            if (!versionCheckBox.isSelected()) {
                versionCheckBox.fire(); // disable version field
            }
            versionCheckBox.setSelected(false); // turn latest version off without enabling version field
            versionCheckBox.setDisable(true);
        } else {
            ipswField.setEffect(null);
            ipswField.setText("");
            versionCheckBox.setDisable(false);
            versionCheckBox.setSelected(true);
        }
    }

    public void filePickerHandler() {
        File result = Utils.showFilePickerDialog(Main.primaryStage, new File(pathField.getText()));
        if (result != null) {
            pathField.setText(result.getAbsolutePath());
        }
    }

    private void loadPreset(int preset) {
        Preferences prefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
        if (!prefs.getBoolean("Exists", false)) {
            return;
        }
        ecidField.setText(prefs.get("ECID", ""));
        if (!Utils.isEmptyOrNull(prefs.get("Path", ""))) {
            pathField.setText(prefs.get("Path", ""));
        }
        if ("none".equals(prefs.get("Device Model", ""))) {
            identifierCheckBox.setSelected(true);
            identifierCheckBoxHandler();
            identifierField.setText(prefs.get("Device Identifier", ""));
        } else {
            identifierCheckBox.setSelected(false);
            identifierCheckBoxHandler();
            deviceTypeChoiceBox.setValue(prefs.get("Device Type", ""));
            deviceModelChoiceBox.setValue(prefs.get("Device Model", ""));
        }
        if (!"none".equals(prefs.get("Board Config", "")) && getBoardConfig.get()) {
            boardConfigField.setText(prefs.get("Board Config", ""));
        }
        if (!Utils.isEmptyOrNull(prefs.get("Apnonce", ""))) {
            if (!apnonceCheckBox.isSelected()) {
                apnonceCheckBox.fire();
            }
            apnonceField.setText(prefs.get("Apnonce", ""));
        } else {
            if (apnonceCheckBox.isSelected()) {
                apnonceCheckBox.fire();
            }
        }
    }

    public void presetButtonHandler(Event evt) {
        Button btn = (Button) evt.getTarget();
        int preset = presetButtons.indexOf(btn) + 1;
        if (editingPresets) {
            savePreset(preset);
            savePresetButton.fire();
        } else if (choosingRunInBackground) {
            Preferences presetPrefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
            if (!presetPrefs.getBoolean("Exists", false)) {
                Utils.showUnreportableError("Preset doesn't have anything");
                return;
            }
            if (btn.getText().startsWith("Cancel ")) {
                ArrayList<String> presetsToSaveFor = Background.getPresetsToSaveFor();
                presetsToSaveFor.remove(Integer.toString(preset));
                if (presetsToSaveFor.isEmpty()) {
                    Main.appPrefs.putBoolean("Background setup", false);
                }
                System.out.println("removed preset" + preset + " from list");
                btn.setText("Use " + btn.getText().substring("Cancel ".length()));
                Main.appPrefs.put("Presets to save in background", Utils.jsonStringFromList(presetsToSaveFor));
            } else {
                loadPreset(preset);
                if (betaCheckBox.isSelected()) betaCheckBox.fire();
                if (!versionCheckBox.isSelected()) versionCheckBox.fire();
                TSS tss = createTSS();
                EventHandler<WorkerStateEvent> oldEventHandler = tss.getOnSucceeded();
                tss.setOnSucceeded(event -> {
                    ArrayList<String> presetsToSaveFor = Background.getPresetsToSaveFor();
                    presetsToSaveFor.add(Integer.toString(preset));
                    Main.appPrefs.put("Presets to save in background", Utils.jsonStringFromList(presetsToSaveFor));
                    Main.appPrefs.putBoolean("Background setup", true);
                    btn.setText("Cancel " + btn.getText().substring("Use ".length()));
                    startBackgroundButton.setDisable(false);
                    forceCheckForBlobs.setDisable(false);
                    chooseTimeToRunButton.setDisable(false);
                    System.out.println("added preset" + preset + " to list");

                    oldEventHandler.handle(event);
                });
                Utils.executeInThreadPool(tss);
                showRunningAlert("Testing preset...");
            }
        } else {
            loadPreset(preset);
        }
    }

    @SuppressWarnings("Duplicates")
    private void savePreset(int preset) {
        boolean doReturn = Utils.isFieldEmpty(!identifierCheckBox.isSelected(), deviceModelChoiceBox.getValue(), deviceModelChoiceBox);
        doReturn |= Utils.isFieldEmpty(true, ecidField);
        doReturn |= Utils.isFieldEmpty(identifierCheckBox, identifierField);
        doReturn |= Utils.isFieldEmpty(getBoardConfig.get(), boardConfigField);
        doReturn |= Utils.isFieldEmpty(apnonceCheckBox, apnonceField);
        if (doReturn) return;

        TextInputDialog textInputDialog = new TextInputDialog(Main.appPrefs.get("Name Preset" + preset, "Preset " + preset));
        textInputDialog.setTitle("Name Preset " + preset);
        textInputDialog.setHeaderText("Name Preset");
        textInputDialog.setContentText("Please enter a name for the preset:");
        textInputDialog.showAndWait();

        String result = textInputDialog.getResult();
        if (!Utils.isEmptyOrNull(result)) {
            Main.appPrefs.put("Name Preset" + preset, textInputDialog.getResult());
            presetButtons.get(preset - 1).setText("Save in " + textInputDialog.getResult());
        } else {
            return;
        }

        Preferences presetPrefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
        presetPrefs.putBoolean("Exists", true);
        presetPrefs.put("ECID", ecidField.getText());
        presetPrefs.put("Path", pathField.getText());
        if (identifierCheckBox.isSelected()) {
            presetPrefs.put("Device Type", "none");
            presetPrefs.put("Device Model", "none");
            presetPrefs.put("Device Identifier", identifierField.getText());
        } else {
            presetPrefs.put("Device Type", deviceTypeChoiceBox.getValue());
            presetPrefs.put("Device Model", deviceModelChoiceBox.getValue());
        }
        if (getBoardConfig.get()) {
            presetPrefs.put("Board Config", boardConfigField.getText());
        } else {
            presetPrefs.put("Board Config", "none");
        }
        if (apnonceCheckBox.isSelected()) {
            presetPrefs.put("Apnonce", apnonceField.getText());
        }
    }

    public void savePresetHandler() {
        editingPresets = !editingPresets;
        if (editingPresets) {
            savePresetButton.setText("Back");
            presetVBox.setEffect(Utils.borderGlow);
            presetButtons.forEach(btn -> btn.setText("Save in " + btn.getText().substring("Load ".length())));
            goButton.setDefaultButton(false);
            goButton.setDisable(true);
            backgroundSettingsButton.setVisible(false);
            backgroundSettingsButton.setManaged(false);
            savePresetButton.setDefaultButton(true);
        } else {
            savePresetButton.setDefaultButton(false);
            goButton.setDefaultButton(true);
            goButton.setDisable(false);
            backgroundSettingsButton.setVisible(true);
            backgroundSettingsButton.setManaged(true);
            presetVBox.setEffect(null);
            savePresetButton.setText("Save");
            presetButtons.forEach(btn -> btn.setText("Load " + btn.getText().substring("Save in ".length())));
        }
    }

    public void checkBlobs() { Utils.openURL("https://verify.shsh.host"); }

    public void helpLabelHandler(MouseEvent evt) {
        if (Main.SHOW_BREAKPOINT) {
            return; // remember to put a breakpoint here
        }

        String labelID;
        // if user clicks on question mark instead of padding, evt.getTarget() returns LabeledText instead of Label
        if (evt.getTarget() instanceof LabeledText) {
            labelID = ((LabeledText) evt.getTarget()).getParent().getId();
        } else {
            labelID = ((Label) evt.getTarget()).getId();
        }
        ButtonType openURL = new ButtonType("Open URL");
        ButtonType customOK = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", openURL, customOK);
        ((Button) alert.getDialogPane().lookupButton(customOK)).setDefaultButton(true);
        String url;
        switch (labelID) {
            case "ipswURLHelp":
                alert.setContentText("Get the IPSW download URL for the iOS version from theiphonewiki.com/wiki/Beta_Firmware and paste it here.");
                alert.setTitle("Help: IPSW URL");
                alert.setHeaderText("Help");
                url = "https://www.theiphonewiki.com/wiki/Beta_Firmware";
                break;
            case "locationHelp":
                alert.setContentText("Click \"Open URL\" to see how to automatically upload blobs you save to the cloud.");
                alert.setTitle("Tip: Saving Blobs to the Cloud");
                alert.setHeaderText("Tip");
                url = "https://github.com/airsquared/blobsaver/wiki/Automatically-saving-blobs-to-the-cloud";
                break;
            default:
                throw new IllegalStateException("Unexpected value for labelID: " + labelID);
        }
        alert.showAndWait();
        if (openURL.equals(alert.getResult())) {
            Utils.openURL(url);
        }
    }

    public void aboutMenuHandler(Event ignored) {
        if (aboutStage != null) { // prevent opening multiple "About" windows
            aboutStage.toFront();
            aboutStage.requestFocus();
            return;
        }
        ButtonType customOK = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", new ButtonType("Libraries Used"),
                new ButtonType("License"), new ButtonType("Github Repo"), customOK);

        //Activate default behavior for OK-Button:
        ((Button) alert.getDialogPane().lookupButton(customOK)).setDefaultButton(true);

        alert.setTitle("About");
        alert.setHeaderText("blobsaver " + Main.appVersion);
        alert.setContentText("blobsaver Copyright (c) 2019  airsquared\n\n" +
                "This program is licensed under GNU GPL v3.0-only");
        Utils.resizeAlertButtons(alert);

        aboutStage = alert.getDialogPane();
        alert.showAndWait();
        aboutStage = null;
        switch (alert.getResult().getText()) {
            case "Github Repo":
                Utils.openURL("https://github.com/airsquared/blobsaver");
                break;
            case "View License":
                Utils.openURL(Utils.getLicenseFile().toURI().toString());
                break;
            case "Libraries Used":
                Utils.openURL(Utils.getLibrariesUsedFile().toURI().toString());
                break;
        }
    }

    private void useMacOSMenuBar() {
        // resize stage to account for removed menu bar
        ((VBox) menuBar.getParent()).setMinHeight(560.0);
        ((VBox) menuBar.getParent()).setPrefHeight(560.0);
        ((VBox) menuBar.getParent()).getChildren().remove(menuBar);
        presetVBox.setMinHeight(560.0);
        presetVBox.setPrefHeight(560.0);
        menuBar.getMenus().get(0).getItems().clear(); // clear old options menu

        MenuToolkit tk = MenuToolkit.toolkit();


        Menu applicationMenu = tk.createDefaultApplicationMenu("blobsaver", null);
        menuBar.getMenus().set(0, applicationMenu);
        applicationMenu.getItems().get(0).setOnAction(this::aboutMenuHandler);
        applicationMenu.getItems().add(1, new SeparatorMenuItem());
        applicationMenu.getItems().add(2, checkForUpdatesMenuItem);
        applicationMenu.getItems().add(3, new SeparatorMenuItem());
        applicationMenu.getItems().add(4, clearAllDataMenuItem);


        Menu windowMenu = new Menu("Window");
        menuBar.getMenus().add(1, windowMenu);
        windowMenu.getItems().addAll(new SeparatorMenuItem(), tk.createMinimizeMenuItem(), tk.createCycleWindowsItem(),
                new SeparatorMenuItem(), debugLogMenuItem, new SeparatorMenuItem(), tk.createBringAllToFrontItem());
        tk.autoAddWindowMenuItems(windowMenu);


        menuBar.getMenus().get(2).getItems().add(0, checkForValidBlobsMenuItem); // add to help menu
        menuBar.getMenus().get(2).getItems().remove(8, 10); // remove about

        tk.setApplicationMenu(applicationMenu);
        tk.setGlobalMenuBar(menuBar);
    }

    public void backgroundSettingsHandler() {
        choosingRunInBackground = !choosingRunInBackground;
        if (choosingRunInBackground) {
            backgroundSettingsButton.setText("Back");
            presetVBox.setEffect(Utils.borderGlow);
            presetButtons.forEach(btn -> {
                ArrayList<String> presetsToSaveFor = Background.getPresetsToSaveFor();
                if (presetsToSaveFor.contains(Integer.toString(presetButtons.indexOf(btn) + 1))) {
                    btn.setText("Cancel " + btn.getText().substring("Load ".length()));
                } else {
                    btn.setText("Use " + btn.getText().substring("Load ".length()));
                }
            });
            if (Background.inBackground) {
                startBackgroundButton.setText("Stop Background");
            }
            setShowBackgroundSettings(true);
        } else {
            setShowBackgroundSettings(false);
            presetVBox.setEffect(null);
            backgroundSettingsButton.setText("Background settings");
            presetButtons.forEach(btn -> {
                if (btn.getText().startsWith("Cancel ")) {
                    btn.setText("Load " + btn.getText().substring("Cancel ".length()));
                } else {
                    btn.setText("Load " + btn.getText().substring("Use ".length()));
                }
            });
        }
    }

    private void setShowBackgroundSettings(boolean showBackgroundSettings) {
        goButton.setDefaultButton(!showBackgroundSettings);
        goButton.setDisable(showBackgroundSettings);
        savePresetButton.setDisable(showBackgroundSettings);
        savePresetButton.setVisible(!showBackgroundSettings);
        savePresetButton.setManaged(!showBackgroundSettings);
        backgroundSettingsButton.setDefaultButton(showBackgroundSettings);
        chooseTimeToRunButton.setVisible(showBackgroundSettings);
        forceCheckForBlobs.setVisible(showBackgroundSettings);
        startBackgroundButton.setVisible(showBackgroundSettings);
        if (!showBackgroundSettings || !Main.appPrefs.getBoolean("Background setup", false)) {
            startBackgroundButton.setDisable(showBackgroundSettings);
            forceCheckForBlobs.setDisable(showBackgroundSettings);
            chooseTimeToRunButton.setDisable(showBackgroundSettings);
        }
    }

    public void chooseTimeToRunHandler() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Frequency to check for new blobs");
        alert.setHeaderText("Frequency to check");
        TextField textField = new TextField(Integer.toString(Main.appPrefs.getInt("Time to run", 1)));
        // make it so user can only enter integers
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList("Minutes", "Hours", "Days", "Weeks"));
        choiceBox.setValue(Main.appPrefs.get("Time unit for background", "Days"));
        HBox hBox = new HBox();
        hBox.getChildren().addAll(textField, choiceBox);
        alert.getDialogPane().setContent(hBox);
        alert.showAndWait();
        if ((alert.getResult() != null) && !ButtonType.CANCEL.equals(alert.getResult()) && !Utils.isEmptyOrNull(textField.getText()) && !Utils.isEmptyOrNull(choiceBox.getValue())) {
            Main.appPrefs.putInt("Time to run", Integer.parseInt(textField.getText()));
            Main.appPrefs.put("Time unit for background", choiceBox.getValue());
        } else {
            backgroundSettingsButton.fire(); //goes back to main menu
            return;
        }
        if (Background.inBackground) {
            ButtonType stopBackgroundButtonType = new ButtonType("Stop Background");
            Alert restartBackgroundAlert = new Alert(Alert.AlertType.INFORMATION,
                    "You will need to restart the background for changes to take effect.", stopBackgroundButtonType);
            restartBackgroundAlert.showAndWait();
            startBackgroundButton.fire();
        }
    }

    public void startBackgroundHandler() {
        if (!java.awt.SystemTray.isSupported()) {
            Utils.showUnreportableError("System Tray is not supported on your OS/platform. Saving blobs in the background will not work without System Tray support.");
        }
        if (Background.inBackground) { //stops background if already in background
            Background.stopBackground(true);
            Main.appPrefs.putBoolean("Show background startup message", true);
            Main.appPrefs.putBoolean("Start background immediately", false);
            startBackgroundButton.setText("Start background");
        } else if (Main.appPrefs.getBoolean("Show background startup message", true)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "The application will now enter the background. By default, when you launch this application, it will start up in the background. "
                            + "If you would like to show the window, find the icon in your system tray/status bar and click on \"Open Window\"", ButtonType.OK);
            alert.showAndWait();
            Main.appPrefs.putBoolean("Show background startup message", false);
            Main.appPrefs.putBoolean("Start background immediately", true);
            startBackgroundButton.setText("Stop background");
            Background.startBackground(false);
        } else {
            Background.startBackground(false);
            startBackgroundButton.setText("Stop Background");
        }
    }

    public void forceCheckForBlobsHandler() {
        if (Background.inBackground) {
            Background.stopBackground(false);
            Background.startBackground(false);
        } else {
            Background.startBackground(true);
        }
    }

    public void resetAppHandler() {
        try {
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you would like to reset/remove all blobsaver data?");
            confirmationAlert.showAndWait();
            if (confirmationAlert.getResult() == null || ButtonType.CANCEL.equals(confirmationAlert.getResult())) {
                return;
            }
            Utils.resetAppPrefs();
            Alert applicationCloseAlert = new Alert(Alert.AlertType.INFORMATION, "The application data and files have been removed. If you are running Windows, you still will need to run the uninstall from your programs/applications manager. Otherwise, you may just delete the app.\nThe application will now exit.", ButtonType.OK);
            applicationCloseAlert.showAndWait();
            Platform.exit();
        } catch (BackingStoreException e) {
            Utils.showReportableError("There was an error resetting the application.", e.getMessage());
        }
    }

    public void debugLogHandler() {
        DebugWindow.toggleShowing();
    }

    public void showWiki() {
        Utils.openURL("https://github.com/airsquared/blobsaver/wiki");
    }

    public void readInfo() {
        try {
            // read ECID
            ecidField.setText(Long.toHexString(Libimobiledevice.getECID(true)).toUpperCase());
            // read device model
            String deviceIdentifier = Libimobiledevice.getDeviceModelIdentifier(true);
            try {
                deviceTypeChoiceBox.setValue(Devices.getDeviceType(deviceIdentifier));
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(deviceIdentifier));
            } catch (IllegalArgumentException e) {
                Utils.showReportableError("Unknown model: " + deviceIdentifier);
                return;
            }
            // read board config
            if (!boardConfigField.isDisabled()) {
                boardConfigField.setText(Libimobiledevice.getBoardConfig(true));
            }
        } catch (Libimobiledevice.LibimobiledeviceException e) {
            e.printStackTrace(); // error alert should have already been shown to user
        } catch (Throwable e) {
            e.printStackTrace();
            Utils.showReportableError("Error: unable to register native methods", Utils.exceptionToString(e));
        }
    }

    public void readApnonce() {
        Alert alert1 = new Alert(Alert.AlertType.CONFIRMATION);
        alert1.setHeaderText("Read apnonce from connected device");
        alert1.setContentText("blobsaver can read the apnonce from a connected device.\n\n" +
                "It is recommended, but not required to set a generator on your device prior to reading the apnonce. " +
                "If you set a generator, make sure to take note of that generator so you can use it in the future.\n\n" +
                "Please connect your device and hit \"OK\" to begin. Your device will enter recovery mode while retrieving the apnonce and will automatically reboot to normal mode when complete.\n\n" +
                "NOTE: an apnonce is only required for devices with an A12 processor or newer.");
        Optional<ButtonType> result = alert1.showAndWait();
        if (!result.isPresent() || !result.get().equals(ButtonType.OK)) return;
        final Alert alert2 = new Alert(Alert.AlertType.INFORMATION, "Entering recovery mode...\n\n" +
                "This can take up to 60 seconds", ButtonType.FINISH);
        alert2.setHeaderText("Reading apnonce from connected device...");
        Utils.forEachButton(alert2, button -> button.setDisable(true));

        Task<String> getApnonceTask = Libimobiledevice.createGetApnonceTask();
        getApnonceTask.setOnSucceeded(event -> Utils.forEachButton(alert2, button -> button.setDisable(false)));
        getApnonceTask.setOnFailed(event -> {
            getApnonceTask.getException().printStackTrace();
            Utils.forEachButton(alert2, button -> button.setDisable(false));
            alert2.close();
        });

        alert2.contentTextProperty().bind(getApnonceTask.messageProperty());
        new Thread(getApnonceTask).start();
        alert2.show();
    }

    public void exitRecoveryHandler() {
        PointerByReference irecvClient = new PointerByReference();
        int errorCode = Libimobiledevice.Libirecovery.irecv_open_with_ecid(irecvClient, 0);
        if (errorCode != 0) {
            Utils.showReportableError("irecovery error: code=" + errorCode + "\n\nUnable to find a device, try using another tool to exit recovery mode.");
            return;
        }
        Libimobiledevice.exitRecovery(irecvClient.getValue(), true);
    }

    public void donate() { Utils.openURL("https://www.paypal.me/airsqrd"); }

    public void goButtonHandler() {
        if (checkInputs()) return;

        Utils.executeInThreadPool(createTSS());
        showRunningAlert("Saving blobs...");
    }

    /**
     * @return true if inputs are wrong, otherwise true
     */
    private boolean checkInputs() {
        boolean incorrect = Utils.isFieldEmpty(!identifierCheckBox.isSelected(), deviceTypeChoiceBox.getValue(), deviceTypeChoiceBox);
        incorrect |= Utils.isFieldEmpty(!identifierCheckBox.isSelected(), deviceModelChoiceBox.getValue(), deviceModelChoiceBox);
        incorrect |= Utils.isFieldEmpty(true, ecidField);
        incorrect |= Utils.isFieldEmpty(identifierCheckBox, identifierField);
        incorrect |= Utils.isFieldEmpty(getBoardConfig.get(), boardConfigField);
        incorrect |= Utils.isFieldEmpty(apnonceCheckBox, apnonceField);
        incorrect |= Utils.isFieldEmpty(true, pathField);
        incorrect |= Utils.isFieldEmpty(!versionCheckBox.isSelected(), versionField);
        incorrect |= Utils.isFieldEmpty(betaCheckBox, ipswField);
        return incorrect;
    }

    private TSS createTSS() {
        TSS.Builder builder = new TSS.Builder()
                .setDevice(identifierCheckBox.isSelected() ?
                        identifierField.getText() : Devices.textToIdentifier(deviceModelChoiceBox.getValue()))
                .setEcid(ecidField.getText())
                .setSavePath(pathField.getText());
        if (getBoardConfig.get()) {
            builder.setBoardConfig(boardConfigField.getText());
        }
        if (!versionCheckBox.isSelected() && !betaCheckBox.isSelected()) {
            builder.setManualVersion(versionField.getText());
        } else if (betaCheckBox.isSelected()) {
            builder.setManualIpswURL(ipswField.getText());
        }
        if (apnonceCheckBox.isSelected()) {
            builder.setApnonce(apnonceField.getText());
        }

        TSS tss = builder.build();

        tss.setOnSucceeded(event -> {
            runningAlert.close();
            int versionsSavedAmt = tss.getValue().size();
            String versionsSavedString = tss.getValue().toString();
            versionsSavedString = versionsSavedString.substring(1, versionsSavedString.length() - 1);
            if (versionsSavedAmt > 1) {
                versionsSavedString = "\n\nFor versions " + versionsSavedString;
            } else if (versionsSavedAmt == 1) {
                versionsSavedString = "\n\nFor version " + versionsSavedString;
            } else {
                versionsSavedString = "";
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Successfully saved blobs in\n" + pathField.getText() + versionsSavedString);
            alert.setHeaderText("Success!");
            alert.showAndWait();
        });
        tss.setOnFailed(event -> {
            runningAlert.close();
            tss.getException().printStackTrace();
            parseException(tss.getException());
        });
        return tss;
    }

    private void showRunningAlert(String title) {
        runningAlert = new Alert(Alert.AlertType.INFORMATION);
        runningAlert.setTitle(title);
        runningAlert.setHeaderText("Saving blobs...            ");
        runningAlert.getDialogPane().setContent(new ProgressBar());
        Utils.forEachButton(runningAlert, button -> button.setDisable(true));
        runningAlert.getDialogPane().getScene().getWindow().setOnCloseRequest(Event::consume);
        runningAlert.show();
    }

    private void parseException(Throwable t) {
        if (t instanceof TSS.TSSException) {
            TSS.TSSException e = (TSS.TSSException) t;
            String message = t.getMessage();
            if (message.contains("not a valid identifier")) {
                identifierField.setEffect(Utils.errorBorder);
            } else if (message.contains("IPSW URL is not valid")
                    || message.contains("IPSW URL might not be valid")) {
                ipswField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid ECID")) {
                ecidField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid apnonce")) {
                apnonceField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid path")) {
                pathField.setEffect(Utils.errorBorder);
            } else if (message.contains("not being signed")) {
                if (betaCheckBox.isSelected()) {
                    ipswField.setEffect(Utils.errorBorder);
                } else if (!versionCheckBox.isSelected()) {
                    versionField.setEffect(Utils.errorBorder);
                }
            }
            if (e.isReportable && e.tssLog != null) {
                Utils.showReportableError(e.getMessage(), e.tssLog);
            } else if (e.isReportable) {
                Utils.showReportableError(e.getMessage(), Utils.exceptionToString(e));
            } else {
                Utils.showUnreportableError(e.getMessage());
            }
        } else {
            Utils.showReportableError("An unknown error occurred.", Utils.exceptionToString(t));
        }
    }

}
