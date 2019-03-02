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
import com.sun.javafx.scene.control.skin.LabeledText;
import de.codecentric.centerdevice.MenuToolkit;
import de.codecentric.centerdevice.labels.LabelMaker;
import de.codecentric.centerdevice.labels.LabelName;
import de.codecentric.centerdevice.util.StageUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONArray;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.airsquared.blobsaver.Main.appPrefs;
import static com.airsquared.blobsaver.Main.appVersion;
import static com.airsquared.blobsaver.Main.primaryStage;
import static com.airsquared.blobsaver.Shared.*;

public class Controller {


    @FXML private MenuBar menuBar;

    @FXML private ChoiceBox deviceTypeChoiceBox;
    @FXML private ChoiceBox deviceModelChoiceBox;

    @FXML TextField ecidField;
    @FXML TextField boardConfigField;
    @FXML TextField apnonceField;
    @FXML TextField versionField;
    @FXML TextField identifierField;
    @FXML TextField pathField;
    @FXML TextField ipswField;
    @FXML TextField buildIDField;

    @FXML CheckBox apnonceCheckBox;
    @FXML CheckBox versionCheckBox;
    @FXML CheckBox identifierCheckBox;
    @FXML CheckBox betaCheckBox;

    @FXML private Label versionLabel;

    @FXML private Button readFromConnectedDeviceButton;
    @FXML private Button startBackgroundButton;
    @FXML private Button chooseTimeToRunButton;
    @FXML private Button forceCheckForBlobs;
    @FXML private Button backgroundSettingsButton;
    @FXML private Button savePresetButton;
    @FXML private Button preset1Button;
    @FXML private Button preset2Button;
    @FXML private Button preset3Button;
    @FXML private Button preset4Button;
    @FXML private Button preset5Button;
    @FXML private Button preset6Button;
    @FXML private Button preset7Button;
    @FXML private Button preset8Button;
    @FXML private Button preset9Button;
    @FXML private Button preset10Button;
    private ArrayList<Button> presetButtons;

    @FXML private VBox presetVBox;

    @FXML private Button goButton;

    //in order to make default about stage and quit menu item
    private final LabelMaker labelMaker = new LabelMaker(Locale.ENGLISH);

    private MenuBar macOSMenuBar;

    private Stage aboutStage = null;

    boolean getBoardConfig = false;
    private boolean editingPresets = false;
    private boolean choosingRunInBackground = false;

    static DropShadow errorBorder = new DropShadow();
    private static DropShadow borderGlow = new DropShadow();

    static Controller INSTANCE;

    static void afterStageShowing() {
        for (int i = 1; i < 11; i++) { // sets the names for the presets
            if (!"".equals(appPrefs.get("Name Preset" + i, ""))) {
                Button btn = (Button) primaryStage.getScene().lookup("#preset" + i);
                btn.setText("Load " + appPrefs.get("Name Preset" + i, ""));
            }
        }
        checkForUpdates(false);
    }

    @SuppressWarnings("unchecked")
    @FXML
    public void initialize() {
        INSTANCE = this;
        // create effects
        borderGlow.setOffsetY(0f);
        borderGlow.setOffsetX(0f);
        borderGlow.setColor(Color.DARKCYAN);
        borderGlow.setWidth(20);
        borderGlow.setHeight(20);
        errorBorder.setOffsetY(0f);
        errorBorder.setOffsetX(0f);
        errorBorder.setColor(Color.RED);
        errorBorder.setWidth(20);
        errorBorder.setHeight(20);

        deviceTypeChoiceBox.setItems(FXCollections.observableArrayList(Devices.getDeviceTypes()));

        deviceTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((x, y, newValue) -> {
            deviceTypeChoiceBox.setEffect(null);
            switch ((String) (newValue == null ? "" : newValue)) {
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
                    versionLabel.setText("iOS Version");
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
            requireBoardConfig((String) newValue);
        });
        identifierField.textProperty().addListener((x, y, newValue) -> {
            identifierField.setEffect(null);
            requireBoardConfig(Devices.getDeviceModelIdentifiersMap().get(newValue));
        });

        addListenerToSetNullEffect(ecidField, versionField, boardConfigField, apnonceField, pathField, buildIDField, ipswField);

        presetButtons = new ArrayList<>(Arrays.asList(preset1Button, preset2Button, preset3Button, preset4Button, preset5Button, preset6Button, preset7Button, preset8Button, preset9Button, preset10Button));
        presetButtons.forEach(btn -> btn.setOnAction(this::presetButtonHandler));

        // the following is to set the path to save blobs to the correct location
        String path = new File(getJarLocation()).getParentFile().toString().replaceAll("%20", " ");
        if (path.endsWith("blobsaver.app/Contents/Java")) {
            path = path.replaceAll("blobsaver\\.app/Contents/Java", "");
        }
        if (path.contains("\\Program Files") || path.contains("/Applications")) {
            path = System.getProperty("user.home");
        }
        if (path.endsWith(System.getProperty("file.separator"))) {
            path = path + "Blobs";
        } else {
            path = path + System.getProperty("file.separator") + "Blobs";
        }
        pathField.setText(path);

        // use macos menu bar or not
        if (PlatformUtil.isMac()) {
            // use system menubar instead
            menuBar.setUseSystemMenuBar(true);

            // makes the app taller to compensate for the missing menu bar
            ((VBox) menuBar.getParent()).setMinHeight(560.0);
            ((VBox) menuBar.getParent()).setPrefHeight(560.0);
            presetVBox.setMinHeight(560.0);
            presetVBox.setPrefHeight(560.0);
            primaryStage.setOnShowing(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    Platform.runLater(() -> useMacOSMenuBar());
                    log("using macOS menu bar");
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

    private void requireBoardConfig(String newValue) {
        if (!"".equals(newValue) && Devices.getRequiresBoardConfigMap().containsKey(newValue)) {
            boardConfigField.setEffect(borderGlow);
            getBoardConfig = true;
            boardConfigField.setDisable(false);
            boardConfigField.setText(Devices.getRequiresBoardConfigMap().get(newValue));
        } else {
            boardConfigField.setEffect(null);
            getBoardConfig = false;
            boardConfigField.setText("");
            boardConfigField.setDisable(true);
        }
    }

    public void checkForUpdatesHandler() { checkForUpdates(true); }

    public void apnonceCheckBoxHandler() {
        if (apnonceCheckBox.isSelected()) {
            apnonceField.setDisable(false);
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
            versionField.setEffect(borderGlow);

            versionField.setDisable(false);
        }
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    private void loadPreset(int preset) {
        Preferences prefs = Preferences.userRoot().node("airsquared/blobsaver/preset" + preset);
        if (!prefs.getBoolean("Exists", false)) {
            return;
        }
        ecidField.setText(prefs.get("ECID", ""));
        if (!"".equals(prefs.get("Path", ""))) {
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
        if (!"none".equals(prefs.get("Board Config", ""))) {
            boardConfigField.setText(prefs.get("Board Config", ""));
        }
        if (!"".equals(prefs.get("Apnonce", ""))) {
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

    private void presetButtonHandler(ActionEvent evt) {
        Button btn = (Button) evt.getTarget();
        int preset = Integer.valueOf(btn.getId().substring("preset".length()));
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
                log("removed " + preset + " from list");
                backgroundSettingsButton.fire();
            } else {
                presetsToSaveFor.add(Integer.toString(preset));
                appPrefs.putBoolean("Background setup", true);
                log("added preset" + preset + " to list");
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
        if (!identifierCheckBox.isSelected() && "".equals(deviceModelChoiceBox.getValue())) {
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
        if (result != null && !"".equals(result)) {
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
            presetPrefs.put("Device Type", (String) deviceTypeChoiceBox.getValue());
            presetPrefs.put("Device Model", (String) deviceModelChoiceBox.getValue());
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
        if (Main.DEBUG_MODE) {
            return; //click on the question mark and add this method as a breakpoint
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void aboutMenuHandler() {
        if (aboutStage != null) { //if about menu already opened
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
        Button OkButton = (Button) alert.getDialogPane().lookupButton(customOK);
        OkButton.setDefaultButton(true);

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
                try {
                    InputStream input;
                    if (PlatformUtil.isWindows()) {
                        input = Main.class.getResourceAsStream("gpl-3.0_windows.txt");
                    } else {
                        input = Main.class.getResourceAsStream("gpl-3.0.txt");
                    }
                    File licenseFile = File.createTempFile("gpl-3.0_", ".txt");
                    copyStreamToFile(input, licenseFile);
                    licenseFile.deleteOnExit();
                    licenseFile.setReadOnly();
                    Desktop.getDesktop().edit(licenseFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "Libraries Used":
                try {
                    InputStream input;
                    if (PlatformUtil.isWindows()) {
                        input = Main.class.getResourceAsStream("libraries_used_windows.txt");
                    } else {
                        input = Main.class.getResourceAsStream("libraries_used.txt");
                    }
                    File libsUsedFile = File.createTempFile("blobsaver-libraries_used_", ".txt");
                    copyStreamToFile(input, libsUsedFile);
                    libsUsedFile.deleteOnExit();
                    libsUsedFile.setReadOnly();
                    Desktop.getDesktop().edit(libsUsedFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "Donate!":
                donate();
                break;
        }
    }

    private MenuItem customQuitMenuItem() {
        MenuItem quit = new MenuItem(labelMaker.getLabel(LabelName.QUIT, "blobsaver"));
        quit.setOnAction(event -> Main.quit());
        quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.META_DOWN));
        return quit;
    }

    //so that when close main app, all the other windows are closed too
    private MenuItem createCloseWindowMenuItem() {
        MenuItem menuItem = new MenuItem(labelMaker.getLabel(LabelName.CLOSE_WINDOW));
        menuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.META_DOWN));
        menuItem.setOnAction(event -> {
            if (StageUtils.getFocusedStage().isPresent() &&
                    StageUtils.getFocusedStage().get().equals(primaryStage)) {
                System.out.println("looks like main stage was focused, hiding app...");
                Main.hideStage();
            } else {
                System.out.println("looks like debug window or something else is focused, closing...");
                StageUtils.closeCurrentStage();
            }
        });
        return menuItem;
    }

    //sets up mac OS menu bar and returns it
    private MenuBar getMacOSMenuBar() {
        macOSMenuBar = new MenuBar();
        MenuToolkit tk = MenuToolkit.toolkit();

        Menu applicationMenu = tk.createDefaultApplicationMenu("blobsaver");

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
        applicationMenu.getItems().set(10, customQuitMenuItem());

        macOSMenuBar.getMenus().add(0, applicationMenu);


        Menu windowMenu = new Menu("Window");

        windowMenu.getItems().add(new SeparatorMenuItem());
        windowMenu.getItems().add(tk.createMinimizeMenuItem());
        windowMenu.getItems().add(createCloseWindowMenuItem()); //TODO: add windows/linux equivalent [ctrl + w]
        windowMenu.getItems().add(tk.createCycleWindowsItem());

        windowMenu.getItems().add(new SeparatorMenuItem());

        MenuItem debugLogMenuItem = new MenuItem("Open Debug Log");
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

        return macOSMenuBar;
    }

    private void useMacOSMenuBar() {
        if (macOSMenuBar == null) {
            macOSMenuBar = getMacOSMenuBar();
        }
        MenuToolkit.toolkit().setGlobalMenuBar(macOSMenuBar);
        System.out.println("setting macos menu bar as global menu bar");
        if (macOSMenuBar.getMenus().get(0).getItems().size() != 11) {
            System.out.println("bad news: not right size: " + macOSMenuBar.getMenus().get(0).getItems().toString());
        }
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
        if ((alert.getResult() != null) && !ButtonType.CANCEL.equals(alert.getResult()) && !"".equals(textField.getText()) && (choiceBox.getValue() != null)) {
            log("info given");
            appPrefs.putInt("Time to run", Integer.valueOf(textField.getText()));
            appPrefs.put("Time unit for background", choiceBox.getValue());
        } else {
            log("alert menu canceled");
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
        if (Background.inBackground) { //stops background if already in background
            Background.stopBackground(true);
            appPrefs.putBoolean("Show background startup message", true);
            appPrefs.putBoolean("Start background immediately", false);
            startBackgroundButton.setText("Start background");
        } else if (appPrefs.getBoolean("Show background startup message", true)) {
            appPrefs.putBoolean("Show background startup message", false);
            appPrefs.putBoolean("Start background immediately", true);
            startBackgroundButton.setText("Stop background");
            Background.startBackground(false);
        } else {
            Background.startBackground(false);
            startBackgroundButton.setText("Cancel Background");
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
            File blobsaver_bin = new File(System.getProperty("user.home"), ".blobsaver_bin");
            deleteFolder(blobsaver_bin);
            Alert applicationCloseAlert = new Alert(Alert.AlertType.INFORMATION, "The application data and files have been removed. If you are running Windows, you still will need to run the uninstall .exe. Otherwise, you can just delete the .app or .jar file.\nThe application will now exit.", ButtonType.OK);
            applicationCloseAlert.showAndWait();
            Main.quit();
        } catch (BackingStoreException e) {
            newReportableError("There was an error resetting the application.", e.getMessage());
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                Arrays.asList(files).forEach(file -> {
                    if (file.isDirectory()) {
                        deleteFolder(file);
                    } else {
                        file.delete();
                    }
                });
            }
        }
    }

    public void debugLogHandler() {
        if (DebugWindow.isShowing()) {
            DebugWindow.hide();
        } else {
            DebugWindow.show();
        }
    }

    public void showWiki(ActionEvent event) {
        String url;
        switch (((MenuItem) event.getTarget()).getText()) {
            case "What are blobs?":
                url = "https://github.com/airsquared/blobsaver/wiki/What-are-blobs-and-why-do-you-need-them%3F";
                break;
            case "Getting the required information":
                url = "https://github.com/airsquared/blobsaver/wiki/Getting-the-required-information";
                break;
            case "Setting it up to run in the background":
                url = "https://github.com/airsquared/blobsaver/wiki/Setting-up-the-background";
                break;
            case "Running on system startup":
                url = "https://github.com/airsquared/blobsaver/wiki/Running-on-system-startup";
                break;
            case "Automatically upload blobs to the cloud":
                url = "https://github.com/airsquared/blobsaver/wiki/Automatically-saving-blobs-to-the-cloud(Dropbox,-Google-Drive,-iCloud)";
                break;
            case "How do I get a crash log?":
                url = "https://github.com/airsquared/blobsaver/wiki/How-do-I-get-a-crash-log%3F";
                break;
            default:
                url = "https://github.com/airsquared/blobsaver/wiki";
                break;
        }
        openURL(url);
    }

    @SuppressWarnings("unchecked")
    public void readInfo() {
        readFromConnectedDeviceButton.setText("Reading...");
        readFromConnectedDeviceButton.setDisable(true);
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
        } finally {
            readFromConnectedDeviceButton.setDisable(false);
            readFromConnectedDeviceButton.setText("Read from connected device");
        }
    }

    public void donate() { openURL("https://www.paypal.me/airsqrd"); }

    private static void log(String msg) { System.out.println(msg); }

    @SuppressWarnings("Duplicates")
    public void goButtonHandler() {
        boolean doReturn = false;
        if (!identifierCheckBox.isSelected() && "".equals(deviceTypeChoiceBox.getValue())) {
            deviceTypeChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && "".equals(deviceModelChoiceBox.getValue())) {
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
        if (doReturn) {
            return;
        }
        String deviceModel = (String) deviceModelChoiceBox.getValue();
        if ("".equals(deviceModel)) {
            String identifierText = identifierField.getText();
            try {
                if (identifierText.startsWith("iPad") || identifierText.startsWith("iPod") || identifierText.startsWith("iPhone") || identifierText.startsWith("AppleTV")) {
                    TSSChecker.run(identifierField.getText());
                } else {
                    identifierField.setEffect(errorBorder);
                    newUnreportableError("\"" + identifierText + "\" is not a valid identifier");
                }
            } catch (StringIndexOutOfBoundsException e) {
                newUnreportableError("\"" + identifierText + "\" is not a valid identifier");
            }
        } else {
            TSSChecker.run(textToIdentifier(deviceModel));
        }
    }

    private static boolean isTextFieldInvalid(CheckBox checkBox, TextField textField) {
        return isTextFieldInvalid(checkBox.isSelected(), textField);
    }

    private static boolean isTextFieldInvalid(boolean isTextFieldRequired, TextField textField) {
        if (isTextFieldRequired && "".equals(textField.getText())) {
            textField.setEffect(errorBorder);
            return true;
        }
        return false;
    }
}
