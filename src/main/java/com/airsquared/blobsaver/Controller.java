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
import com.sun.javafx.scene.control.skin.LabeledText;
import com.sun.jna.ptr.PointerByReference;
import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.airsquared.blobsaver.Main.appPrefs;
import static com.airsquared.blobsaver.Main.appVersion;
import static com.airsquared.blobsaver.Main.primaryStage;
import static com.airsquared.blobsaver.Shared.*;

public class Controller {


    @FXML private MenuBar menuBar;

    @FXML private ChoiceBox<String> deviceTypeChoiceBox, deviceModelChoiceBox;

    @FXML TextField ecidField, boardConfigField, apnonceField, versionField, identifierField,
            pathField, ipswField, buildIDField;

    @FXML CheckBox apnonceCheckBox, versionCheckBox, identifierCheckBox, betaCheckBox;

    @FXML private Label versionLabel;

    @FXML private Button readFromConnectedDeviceButton, readApnonceButton, startBackgroundButton,
            chooseTimeToRunButton, forceCheckForBlobs, backgroundSettingsButton, savePresetButton;

    @FXML private Button preset1Button, preset2Button, preset3Button, preset4Button, preset5Button, preset6Button,
            preset7Button, preset8Button, preset9Button, preset10Button;
    private ArrayList<Button> presetButtons;

    @FXML private VBox presetVBox;

    @FXML private Button goButton;

    boolean getBoardConfig = false;
    private boolean editingPresets = false;
    private boolean choosingRunInBackground = false;

    static final DropShadow errorBorder = new DropShadow(9.5, 0f, 0f, Color.RED);
    static final DropShadow borderGlow = new DropShadow(9.5, 0f, 0f, Color.DARKCYAN);

    static Controller INSTANCE;

    static void afterStageShowing() {
        for (int i = 0; i < 10; i++) { // sets the names for the presets
            if (!Shared.isEmptyOrNull(appPrefs.get("Name Preset" + i, ""))) {
                Button btn = (Button) primaryStage.getScene().lookup("#preset" + i);
                btn.setText("Load " + appPrefs.get("Name Preset" + i, ""));
            }
        }
        checkForUpdates(false);
    }

    @FXML
    public void initialize() {
        INSTANCE = this;

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
        deviceTypeChoiceBox.setValue("iPhone");
        deviceModelChoiceBox.getSelectionModel().selectedItemProperty().addListener((x, y, newValue) -> {
            deviceModelChoiceBox.setEffect(null);
            if (Shared.isEmptyOrNull(newValue)) {
                return;
            }
            String identifier = Devices.getDeviceModelIdentifiersMap().get(newValue);
            requireBoardConfig(identifier);
            requireApnonce(identifier);
        });
        identifierField.textProperty().addListener((x, y, identifier) -> {
            identifierField.setEffect(null);
            if (Shared.isEmptyOrNull(identifier)) {
                return;
            }
            requireBoardConfig(identifier);
            requireApnonce(identifier);
        });

        addListenerToSetNullEffect(ecidField, versionField, boardConfigField, apnonceField, pathField, buildIDField, ipswField);

        presetButtons = new ArrayList<>(Arrays.asList(preset1Button, preset2Button, preset3Button, preset4Button, preset5Button, preset6Button, preset7Button, preset8Button, preset9Button, preset10Button));


        pathField.setText(new File(System.getProperty("user.home"), "Blobs").getAbsolutePath());


        if (PlatformUtil.isMac()) {
            // resize stage to account for removed menu bar
            ((VBox) menuBar.getParent()).setMinHeight(560.0);
            ((VBox) menuBar.getParent()).setPrefHeight(560.0);
            presetVBox.setMinHeight(560.0);
            presetVBox.setPrefHeight(560.0);

            ((VBox) menuBar.getParent()).getChildren().remove(menuBar);

            primaryStage.setOnShowing(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    useMacOSMenuBar();
                    primaryStage.removeEventHandler(event.getEventType(), this);
                }
            });
        }
    }

    private static void addListenerToSetNullEffect(TextField... textFields) {
        for (TextField textField : textFields) {
            textField.textProperty().addListener((x, y, z) -> textField.setEffect(null));
        }
    }

    public void newGithubIssue() { Shared.newGithubIssue(); }

    public void sendRedditPM() { Shared.sendRedditPM(); }

    private void requireBoardConfig(String identifier) {
        if (!"".equals(identifier) && (Devices.getRequiresBoardConfigMap().containsKey(identifier) ||
                !Devices.getDeviceModelIdentifiersMap().containsKey(identifier))) {
            boardConfigField.setEffect(borderGlow);
            getBoardConfig = true;
            boardConfigField.setDisable(false);
            boardConfigField.setText(Devices.getRequiresBoardConfigMap().get(identifier));
        } else {
            boardConfigField.setEffect(null);
            getBoardConfig = false;
            boardConfigField.setText("");
            boardConfigField.setDisable(true);
        }
    }

    private void requireApnonce(String identifier) {
        if (!"".equals(identifier) && (identifier.startsWith("iPhone11,") || identifier.startsWith("iPhone12,") ||
                identifier.startsWith("iPad8,") || identifier.startsWith("iPad11,"))) {
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

    public void checkForUpdatesHandler() { checkForUpdates(true); }

    public void apnonceCheckBoxHandler() {
        if (apnonceCheckBox.isSelected()) {
            apnonceField.setDisable(false);
            readApnonceButton.setDisable(false);
            apnonceField.setEffect(borderGlow);
        } else {
            apnonceField.setEffect(null);
            apnonceField.setText("");
            apnonceField.setDisable(true);
            readApnonceButton.setDisable(true);
        }
    }

    public void versionCheckBoxHandler() {
        if (versionCheckBox.isSelected()) {
            versionField.setDisable(true);
            versionField.setEffect(null);
            versionField.setText("");
        } else {
            versionField.setEffect(borderGlow);
            versionField.setDisable(false);
        }
    }

    public void identifierCheckBoxHandler() {
        if (identifierCheckBox.isSelected()) {
            identifierField.setDisable(false);
            identifierField.setEffect(borderGlow);
            deviceTypeChoiceBox.getSelectionModel().clearSelection();
            deviceModelChoiceBox.getSelectionModel().clearSelection();
            deviceTypeChoiceBox.setValue(null);
            deviceTypeChoiceBox.setDisable(true);
            deviceModelChoiceBox.setDisable(true);
            deviceTypeChoiceBox.setEffect(null);
            deviceModelChoiceBox.setEffect(null);
        } else {
            identifierField.setEffect(null);
            identifierField.setText("");
            identifierField.setDisable(true);
            deviceTypeChoiceBox.setDisable(false);
            deviceModelChoiceBox.setDisable(false);
        }
    }

    public void betaCheckBoxHandler() {
        if (betaCheckBox.isSelected()) {
            ipswField.setDisable(false);
            ipswField.setEffect(borderGlow);
            buildIDField.setDisable(false);
            buildIDField.setEffect(borderGlow);
            if (versionCheckBox.isSelected()) { //cannot use latest versions + beta blobs in conjunction
                versionCheckBox.fire(); //turns latest version off
            }
            versionCheckBox.setDisable(true);
        } else {
            ipswField.setEffect(null);
            ipswField.setText("");
            ipswField.setDisable(true);
            buildIDField.setEffect(null);
            buildIDField.setText("");
            buildIDField.setDisable(true);
            versionCheckBox.setDisable(false);
        }
    }

    public void filePickerHandler() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose a folder to save Blobs in");
        File startIn = new File(pathField.getText());
        if (startIn.exists()) {
            dirChooser.setInitialDirectory(startIn);
        } else if (startIn.getParentFile().exists()) {
            dirChooser.setInitialDirectory(startIn.getParentFile());
        } else {
            dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
        File result = dirChooser.showDialog(primaryStage);
        if (result != null) {
            pathField.setText(result.toString());
        }
    }

    private void loadPreset(int preset) {
        Preferences prefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
        if (!prefs.getBoolean("Exists", false)) {
            return;
        }
        ecidField.setText(prefs.get("ECID", ""));
        if (!Shared.isEmptyOrNull(prefs.get("Path", ""))) {
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
        if (!"none".equals(prefs.get("Board Config", "")) && getBoardConfig) {
            boardConfigField.setText(prefs.get("Board Config", ""));
        }
        if (!Shared.isEmptyOrNull(prefs.get("Apnonce", ""))) {
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

    public void presetButtonHandler(ActionEvent evt) {
        Button btn = (Button) evt.getTarget();
        int preset = Integer.parseInt(btn.getId().substring("preset".length()));
        if (editingPresets) {
            savePreset(preset);
            savePresetButton.fire();
        } else if (choosingRunInBackground) {
            Preferences presetPrefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
            if (!presetPrefs.getBoolean("Exists", false)) {
                newUnreportableError("Preset doesn't have anything");
                return;
            }
            ArrayList<String> presetsToSaveFor = Background.getPresetsToSaveFor();
            if (btn.getText().startsWith("Cancel ")) {
                presetsToSaveFor.remove(Integer.toString(preset));
                if (presetsToSaveFor.isEmpty()) {
                    appPrefs.putBoolean("Background setup", false);
                }
                System.out.println("removed " + preset + " from list");
                backgroundSettingsButton.fire();
            } else {
                presetsToSaveFor.add(Integer.toString(preset));
                appPrefs.putBoolean("Background setup", true);
                System.out.println("added preset" + preset + " to list");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "If it doesn't work, please remove it, fix the error, and add it back");
                alert.setTitle("Testing preset " + preset);
                alert.setHeaderText("Testing preset");
                backgroundSettingsButton.fire();
                btn.fire();
                goButton.fire();
            }
            appPrefs.put("Presets to save in background", new JSONArray(presetsToSaveFor).toString());
        } else {
            loadPreset(preset);
        }
    }

    @SuppressWarnings("Duplicates")
    private void savePreset(int preset) {
        boolean doReturn = false;
        if (!identifierCheckBox.isSelected() && Shared.isEmptyOrNull(deviceModelChoiceBox.getValue())) {
            deviceModelChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        doReturn = doReturn || isTextFieldInvalid(true, ecidField);
        doReturn = doReturn || isTextFieldInvalid(identifierCheckBox, identifierField);
        doReturn = doReturn || isTextFieldInvalid(getBoardConfig, boardConfigField);
        doReturn = doReturn || isTextFieldInvalid(apnonceCheckBox, apnonceField);
        if (doReturn) {
            return;
        }
        TextInputDialog textInputDialog = new TextInputDialog(appPrefs.get("Name Preset" + preset, "Preset " + preset));
        textInputDialog.setTitle("Name Preset " + preset);
        textInputDialog.setHeaderText("Name Preset");
        textInputDialog.setContentText("Please enter a name for the preset:");
        textInputDialog.showAndWait();

        String result = textInputDialog.getResult();
        if (Shared.isEmptyOrNull(result)) {
            appPrefs.put("Name Preset" + preset, textInputDialog.getResult());
            ((Button) primaryStage.getScene().lookup("#preset" + preset)).setText("Save in " + textInputDialog.getResult());
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
        if (getBoardConfig) {
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
            presetVBox.setEffect(borderGlow);
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

    public void checkBlobs() { openURL("https://tsssaver.1conan.com/check.php"); }

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
        String helpItem = labelID.substring(0, labelID.indexOf("Help"));
        Alert alert;
        ButtonType openLink = new ButtonType("Open link");
        switch (helpItem) {
            case "ecid":
                alert = new Alert(Alert.AlertType.INFORMATION, "Connect your device to your computer and go to iTunes and open the device \"page.\" Then click on the serial number twice and copy and paste it here.", ButtonType.OK);
                alert.setTitle("Help: ECID");
                alert.setHeaderText("Help");
                alert.showAndWait();
                break;
            case "buildID":
                alert = new Alert(Alert.AlertType.INFORMATION, "Get the build ID for the iOS version from theiphonewiki.com/wiki/Beta_Firmware and paste it here.", openLink, ButtonType.OK);
                alert.setTitle("Help: Build ID");
                alert.setHeaderText("Help");
                alert.showAndWait();
                if (openLink.equals(alert.getResult())) {
                    openURL("https://www.theiphonewiki.com/wiki/Beta_Firmware");
                }
                break;
            case "ipswURL":
                alert = new Alert(Alert.AlertType.INFORMATION, "Get the IPSW download URL for the iOS version from theiphonewiki.com/wiki/Beta_Firmware and paste it here.", openLink, ButtonType.OK);
                alert.setTitle("Help: IPSW URL");
                alert.setHeaderText("Help");
                alert.showAndWait();
                if (openLink.equals(alert.getResult())) {
                    openURL("https://www.theiphonewiki.com/wiki/Beta_Firmware");
                }
                break;
            case "boardConfig":
                openLink = new ButtonType("BMSSM app");
                alert = new Alert(Alert.AlertType.INFORMATION, "Get the board configuration from the BMSSM app from the appstore. Go to the system tab and it'll be called the model. It can be something like \"n69ap\"", openLink, ButtonType.OK);
                alert.setTitle("Help: Board Configuration");
                alert.setHeaderText("Help");
                alert.showAndWait();
                if (openLink.equals(alert.getResult())) {
                    openURL("https://itunes.apple.com/us/app/battery-memory-system-status-monitor/id497937231");
                }
                break;
            case "location":
                openLink = new ButtonType("Open link");
                alert = new Alert(Alert.AlertType.INFORMATION, "Click \"Open link\" to see how to automatically upload blobs you save to the cloud.", openLink, ButtonType.OK);
                alert.setTitle("Help: Saving Blobs to the Cloud");
                alert.setHeaderText("Help");
                alert.showAndWait();
                if (openLink.equals(alert.getResult())) {
                    openURL("https://github.com/airsquared/blobsaver/wiki/Automatically-saving-blobs-to-the-cloud(Dropbox,-Google-Drive,-iCloud)");
                }
                break;
        }
    }

    private Stage aboutStage = null;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void aboutMenuHandler() {
        if (aboutStage != null) { // prevent opening multiple "About" windows
            aboutStage.toFront();
            aboutStage.requestFocus();
            return;
        }
        ButtonType githubRepo = new ButtonType("Github Repo");
        ButtonType viewLicense = new ButtonType("View License");
        ButtonType librariesUsed = new ButtonType("Libraries Used");
        ButtonType donate = new ButtonType("Donate!");
        ButtonType customOK = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "About text here",
                librariesUsed, viewLicense, donate, githubRepo, customOK);

        alert.setTitle("About");

        //Deactivate default behavior for librariesUsed Button:
        Button libButton = (Button) alert.getDialogPane().lookupButton(librariesUsed);
        libButton.setDefaultButton(false);

        //Activate default behavior for OK-Button:
        Button okButton = (Button) alert.getDialogPane().lookupButton(customOK);
        okButton.setDefaultButton(true);

        alert.setHeaderText("blobsaver " + appVersion);
        alert.setContentText("blobsaver Copyright (c) 2019  airsquared\n\n" +
                "This program is licensed under GNU GPL v3.0-only");

        resizeAlertButtons(alert);

        aboutStage = (Stage) alert.getDialogPane().getScene().getWindow();

        alert.showAndWait();
        switch (alert.getResult().getText()) {
            case "Github Repo":
                openURL("https://github.com/airsquared/blobsaver");
                break;
            case "View License":
                File licenseFile;
                if (!Main.runningFromJar) {
                    licenseFile = new File(Main.jarDirectory.getParentFile().getParentFile(),
                            PlatformUtil.isWindows() ? "dist/windows/LICENSE_windows.txt" : "LICENSE");
                } else if (PlatformUtil.isMac()) {
                    licenseFile = new File(Main.jarDirectory.getParentFile(), "Resources/LICENSE");
                } else { // if Linux or Windows
                    licenseFile = new File(Main.jarDirectory, "LICENSE");
                }
                licenseFile.setReadOnly();
                openURL(licenseFile.toURI().toString());
                break;
            case "Libraries Used":
                File librariesUsedFile;
                if (!Main.runningFromJar) {
                    librariesUsedFile = new File(Main.jarDirectory.getParentFile().getParentFile(),
                            PlatformUtil.isWindows() ? "dist/windows/libraries_used_windows.txt" : "libraries_used.txt");
                } else if (PlatformUtil.isMac()) {
                    librariesUsedFile = new File(Main.jarDirectory.getParentFile(), "Resources/libraries_used.txt");
                } else { // if Linux or Windows
                    librariesUsedFile = new File(Main.jarDirectory, "libraries_used.txt");
                }
                librariesUsedFile.setReadOnly();
                System.out.println(librariesUsedFile.toURI().toString());
                openURL(librariesUsedFile.toURI().toString());
                break;
            case "Donate!":
                donate();
                break;
        }
        aboutStage = null;
    }

    private void useMacOSMenuBar() {
        MenuBar macOSMenuBar = new MenuBar();
        MenuToolkit tk = MenuToolkit.toolkit();


        Menu applicationMenu = tk.createDefaultApplicationMenu("blobsaver", null);

        MenuItem aboutMenuItem = new MenuItem("About blobsaver");
        aboutMenuItem.setOnAction(event2 -> aboutMenuHandler());
        applicationMenu.getItems().set(0, aboutMenuItem);

        MenuItem checkForUpdatesMenuItem = new MenuItem("Check for Updates...");
        checkForUpdatesMenuItem.setOnAction(event1 -> checkForUpdatesHandler());
        applicationMenu.getItems().add(1, new SeparatorMenuItem());
        applicationMenu.getItems().add(2, checkForUpdatesMenuItem);

        MenuItem clearAllDataMenuItem = new MenuItem("Uninstall...");
        clearAllDataMenuItem.setOnAction(event1 -> resetAppHandler());
        applicationMenu.getItems().add(3, new SeparatorMenuItem());
        applicationMenu.getItems().add(4, clearAllDataMenuItem);

        macOSMenuBar.getMenus().add(0, applicationMenu);


        Menu windowMenu = new Menu("Window");

        windowMenu.getItems().add(new SeparatorMenuItem());
        windowMenu.getItems().add(tk.createMinimizeMenuItem());
        windowMenu.getItems().add(tk.createCycleWindowsItem());
        windowMenu.getItems().add(new SeparatorMenuItem());

        MenuItem debugLogMenuItem = new MenuItem("Open/Close Debug Log");
        debugLogMenuItem.setOnAction(event -> {
            debugLogHandler();
            tk.setMenuBar(DebugWindow.getDebugStage(), macOSMenuBar);
        });
        windowMenu.getItems().add(debugLogMenuItem);

        windowMenu.getItems().add(new SeparatorMenuItem());
        windowMenu.getItems().add(tk.createBringAllToFrontItem());
        windowMenu.getItems().add(new SeparatorMenuItem());
        tk.autoAddWindowMenuItems(windowMenu);

        macOSMenuBar.getMenus().add(windowMenu);


        Menu helpMenu = menuBar.getMenus().get(1);

        helpMenu.getItems().add(1, new SeparatorMenuItem());
        helpMenu.getItems().add(4, new SeparatorMenuItem());

        MenuItem checkForValidBlobsMenuItem = new MenuItem("Check for Valid Blobs...");
        checkForValidBlobsMenuItem.setOnAction(event -> checkBlobs());
        helpMenu.getItems().set(5, checkForValidBlobsMenuItem);
        helpMenu.getItems().add(6, new SeparatorMenuItem());

        macOSMenuBar.getMenus().add(helpMenu);

        // needs to be run with Platform.runLater(), otherwise the application menu doesn't show up
        Platform.runLater(() -> tk.setGlobalMenuBar(macOSMenuBar));
    }

    public void backgroundSettingsHandler() {
        choosingRunInBackground = !choosingRunInBackground;
        if (choosingRunInBackground) {
            backgroundSettingsButton.setText("Back");
            presetVBox.setEffect(borderGlow);
            presetButtons.forEach(btn -> {
                ArrayList<String> presetsToSaveFor = Background.getPresetsToSaveFor();
                if (presetsToSaveFor.contains(btn.getId().substring("preset".length()))) {
                    btn.setText("Cancel " + btn.getText().substring("Load ".length()));
                } else {
                    btn.setText("Use " + btn.getText().substring("Load ".length()));
                }
            });
            if (Background.inBackground) {
                startBackgroundButton.setText("Stop Background");
            }
            goButton.setDefaultButton(false);
            goButton.setDisable(true);
            savePresetButton.setDisable(true);
            savePresetButton.setVisible(false);
            savePresetButton.setManaged(false);
            backgroundSettingsButton.setDefaultButton(true);
            chooseTimeToRunButton.setVisible(true);
            forceCheckForBlobs.setVisible(true);
            startBackgroundButton.setVisible(true);
            if (!appPrefs.getBoolean("Background setup", false)) {
                startBackgroundButton.setDisable(true);
                forceCheckForBlobs.setDisable(true);
                chooseTimeToRunButton.setDisable(true);
            }
        } else {
            backgroundSettingsButton.setDefaultButton(false);
            goButton.setDefaultButton(true);
            goButton.setDisable(false);
            presetVBox.setEffect(null);
            savePresetButton.setDisable(false);
            savePresetButton.setVisible(true);
            savePresetButton.setManaged(true);
            backgroundSettingsButton.setText("Background settings");
            presetButtons.forEach(btn -> {
                if (btn.getText().startsWith("Cancel ")) {
                    btn.setText("Load " + btn.getText().substring("Cancel ".length()));
                } else {
                    btn.setText("Load " + btn.getText().substring("Use ".length()));
                }
            });
            chooseTimeToRunButton.setDisable(false);
            chooseTimeToRunButton.setVisible(false);
            forceCheckForBlobs.setDisable(false);
            forceCheckForBlobs.setVisible(false);
            startBackgroundButton.setDisable(false);
            startBackgroundButton.setVisible(false);
        }
    }

    public void chooseTimeToRunHandler() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Frequency to check for new blobs");
        alert.setHeaderText("Frequency to check");
        TextField textField = new TextField(Integer.toString(appPrefs.getInt("Time to run", 1)));
        // make it so user can only enter integers
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList("Minutes", "Hours", "Days", "Weeks"));
        choiceBox.setValue(appPrefs.get("Time unit for background", "Days"));
        HBox hBox = new HBox();
        hBox.getChildren().addAll(textField, choiceBox);
        alert.getDialogPane().setContent(hBox);
        alert.showAndWait();
        if ((alert.getResult() != null) && !ButtonType.CANCEL.equals(alert.getResult()) && !Shared.isEmptyOrNull(textField.getText()) && Shared.isEmptyOrNull(choiceBox.getValue())) {
            appPrefs.putInt("Time to run", Integer.parseInt(textField.getText()));
            appPrefs.put("Time unit for background", choiceBox.getValue());
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
            newUnreportableError("System Tray is not supported on your OS/platform. Saving blobs in the background will not work without System Tray support.");
        }
        if (Background.inBackground) { //stops background if already in background
            Background.stopBackground(true);
            appPrefs.putBoolean("Show background startup message", true);
            appPrefs.putBoolean("Start background immediately", false);
            startBackgroundButton.setText("Start background");
        } else if (appPrefs.getBoolean("Show background startup message", true)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "The application will now enter the background. By default, when you launch this application, it will start up in the background. "
                            + "If you would like to show the window, find the icon in your system tray/status bar and click on \"Open Window\"", ButtonType.OK);
            alert.showAndWait();
            appPrefs.putBoolean("Show background startup message", false);
            appPrefs.putBoolean("Start background immediately", true);
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
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you would like to uninstall this application?", ButtonType.NO, ButtonType.YES);
            confirmationAlert.showAndWait();
            if ((confirmationAlert.getResult() == null)
                    || ButtonType.CANCEL.equals(confirmationAlert.getResult())
                    || ButtonType.NO.equals(confirmationAlert.getResult())) {
                return;
            }
            Preferences prefs = Preferences.userRoot().node("airsquared/blobsaver");
            prefs.flush();
            prefs.clear();
            prefs.removeNode();
            prefs.flush();
            Alert applicationCloseAlert = new Alert(Alert.AlertType.INFORMATION, "The application data and files have been removed. If you are running Windows, you still will need to run the uninstall .exe. Otherwise, you can just delete the .app or .jar file.\nThe application will now exit.", ButtonType.OK);
            applicationCloseAlert.showAndWait();
            Platform.exit();
        } catch (BackingStoreException e) {
            newReportableError("There was an error resetting the application.", e.getMessage());
        }
    }

    public void debugLogHandler() {
        if (DebugWindow.isShowing()) {
            DebugWindow.hide();
        } else {
            DebugWindow.show();
        }
    }

    public void showWiki() {
        openURL("https://github.com/airsquared/blobsaver/wiki");
    }

    public void readInfo() {
        if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText("IMPORTANT: make sure to install libimobiledevice and have it in your path before running this.");
            alert.showAndWait();
            if (alert.getResult().equals(ButtonType.CANCEL)) {
                return;
            }
        }
        try {
            // read ECID
            ecidField.setText(Long.toHexString(Libimobiledevice.getEcid(true)).toUpperCase());
            // read device model
            String deviceModel = Libimobiledevice.getDeviceModelIdentifier(true);
            if (deviceModel.startsWith("iPhone")) {
                deviceTypeChoiceBox.setValue("iPhone");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(deviceModel));
            } else if (deviceModel.startsWith("iPod")) {
                deviceTypeChoiceBox.setValue("iPod");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(deviceModel));
            } else if (deviceModel.startsWith("iPad")) {
                deviceTypeChoiceBox.setValue("iPad");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(deviceModel));
            } else if (deviceModel.startsWith("AppleTV")) {
                deviceTypeChoiceBox.setValue("Apple TV");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(deviceModel));
            } else {
                newReportableError("Unknown model: " + deviceModel);
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
            newReportableError("Error: unable to register native methods", exceptionToString(e));
        } finally {
            readFromConnectedDeviceButton.setDisable(false);
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
        Shared.forEachButton(alert2, button -> button.setDisable(true));
        alert2.show();
        new Thread(() -> { // run later to make sure alert shows
            try {
                System.out.println("Entering recovery mode");
                Libimobiledevice.enterRecovery(true);
                PointerByReference irecvClient = new PointerByReference();
                long endTime = System.currentTimeMillis() + 60_000; // timeout is 60 seconds
                int errorCode = -3;
                while (errorCode == -3 && System.currentTimeMillis() < endTime) {
                    Thread.sleep(1000);
                    errorCode = Libimobiledevice.Libirecovery.irecv_open_with_ecid(irecvClient, 0);
                }
                Libimobiledevice.throwIfNeeded(errorCode, true, Libimobiledevice.ErrorCodeType.irecv_error_t);
                Libimobiledevice.Libirecovery.irecv_device_info deviceInfo = Libimobiledevice.Libirecovery.irecv_get_device_info(irecvClient.getValue());
                final StringBuilder apnonce = new StringBuilder();
                for (byte b : deviceInfo.ap_nonce.getByteArray(0, deviceInfo.ap_nonce_size)) {
                    apnonce.append(String.format("%02x", b));
                }
                System.out.println("Got apnonce");
                Platform.runLater(() -> {
                    apnonceField.setText(apnonce.toString());
                    alert2.setContentText("Successfully got apnonce, exiting recovery mode...");
                });
                System.out.println("Exiting recovery mode");
                Libimobiledevice.exitRecovery(irecvClient.getValue(), true);
                Thread.sleep(3000);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(alert2::close);
            } finally {
                Shared.forEachButton(alert2, button -> button.setDisable(false));
            }
        }).start();
    }

    public void donate() { openURL("https://www.paypal.me/airsqrd"); }

    @SuppressWarnings("Duplicates")
    public void goButtonHandler() {
        boolean doReturn = false;
        if (!identifierCheckBox.isSelected() && Shared.isEmptyOrNull(deviceTypeChoiceBox.getValue())) {
            deviceTypeChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && Shared.isEmptyOrNull(deviceModelChoiceBox.getValue())) {
            deviceModelChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        doReturn = doReturn || isTextFieldInvalid(true, ecidField);
        doReturn = doReturn || isTextFieldInvalid(identifierCheckBox, identifierField);
        doReturn = doReturn || isTextFieldInvalid(getBoardConfig, boardConfigField);
        doReturn = doReturn || isTextFieldInvalid(apnonceCheckBox, apnonceField);
        doReturn = doReturn || isTextFieldInvalid(true, pathField);
        doReturn = doReturn || isTextFieldInvalid(!versionCheckBox.isSelected(), versionField);
        doReturn = doReturn || isTextFieldInvalid(betaCheckBox, buildIDField);
        doReturn = doReturn || isTextFieldInvalid(betaCheckBox, ipswField);
        if (doReturn) return;

        String deviceModel = deviceModelChoiceBox.getValue();
        if (Shared.isEmptyOrNull(deviceModel)) {
            String identifierText = identifierField.getText();
            try {
                if (identifierText.startsWith("iPad") || identifierText.startsWith("iPod") || identifierText.startsWith("iPhone") || identifierText.startsWith("AppleTV")) {
                    TSSChecker.run(identifierText);
                    return;
                }
            } catch (StringIndexOutOfBoundsException ignored) {
            }
            identifierField.setEffect(errorBorder);
            newUnreportableError("\"" + identifierText + "\" is not a valid identifier");
        } else {
            TSSChecker.run(textToIdentifier(deviceModel));
        }
    }

    private Alert runningAlert;

    void showRunningAlert() {
        runningAlert = new Alert(Alert.AlertType.INFORMATION);
        runningAlert.setTitle("");
        runningAlert.setHeaderText("Saving blobs...            ");
        runningAlert.getDialogPane().setContent(new ProgressBar());
        Shared.forEachButton(runningAlert, button -> button.setDisable(true));
        runningAlert.getDialogPane().getScene().getWindow().setOnCloseRequest(Event::consume);
        runningAlert.show();
    }

    void hideRunningAlert() {
        if (runningAlert != null) {
            Shared.forEachButton(runningAlert, button -> button.setDisable(false));
            runningAlert.getDialogPane().getScene().getWindow().setOnCloseRequest(null);
            runningAlert.close();
            runningAlert = null;
        }
    }

    private static boolean isTextFieldInvalid(CheckBox checkBox, TextField textField) {
        return isTextFieldInvalid(checkBox.isSelected(), textField);
    }

    private static boolean isTextFieldInvalid(boolean isTextFieldRequired, TextField textField) {
        if (isTextFieldRequired && Shared.isEmptyOrNull(textField.getText())) {
            textField.setEffect(errorBorder);
            return true;
        }
        return false;
    }
}
