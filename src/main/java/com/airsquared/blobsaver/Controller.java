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
import de.codecentric.centerdevice.dialogs.about.AboutStageBuilder;
import de.codecentric.centerdevice.icns.IcnsParser;
import de.codecentric.centerdevice.icns.IcnsType;
import de.codecentric.centerdevice.labels.LabelMaker;
import de.codecentric.centerdevice.labels.LabelName;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.airsquared.blobsaver.Main.appPrefs;
import static com.airsquared.blobsaver.Main.appVersion;
import static com.airsquared.blobsaver.Main.primaryStage;
import static com.airsquared.blobsaver.Shared.checkForUpdates;
import static com.airsquared.blobsaver.Shared.copyStreamToFile;
import static com.airsquared.blobsaver.Shared.executeProgram;
import static com.airsquared.blobsaver.Shared.getJarLocation;
import static com.airsquared.blobsaver.Shared.getTsschecker;
import static com.airsquared.blobsaver.Shared.getideviceinfo;
import static com.airsquared.blobsaver.Shared.getidevicepair;
import static com.airsquared.blobsaver.Shared.githubIssue;
import static com.airsquared.blobsaver.Shared.newReportableError;
import static com.airsquared.blobsaver.Shared.newUnreportableError;
import static com.airsquared.blobsaver.Shared.openURL;
import static com.airsquared.blobsaver.Shared.redditPM;
import static com.airsquared.blobsaver.Shared.reportError;
import static com.airsquared.blobsaver.Shared.resizeAlertButtons;
import static com.airsquared.blobsaver.Shared.textToIdentifier;

public class Controller {


    @FXML public MenuBar menuBar;

    @FXML private ChoiceBox deviceTypeChoiceBox;
    @FXML private ChoiceBox deviceModelChoiceBox;

    @FXML private TextField ecidField;
    @FXML private TextField boardConfigField;
    @FXML private TextField apnonceField;
    @FXML private TextField versionField;
    @FXML private TextField identifierField;
    @FXML private TextField pathField;
    @FXML private TextField ipswField;
    @FXML private TextField buildIDField;

    @FXML private CheckBox apnonceCheckBox;
    @FXML private CheckBox versionCheckBox;
    @FXML private CheckBox identifierCheckBox;
    @FXML private CheckBox betaCheckBox;

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

    @FXML public VBox presetVBox;

    @FXML private Button goButton;

    //in order to make default about stage and quit menu item
    private final LabelMaker labelMaker = new LabelMaker(Locale.ENGLISH);

    private boolean getBoardConfig = false;
    private boolean editingPresets = false;
    private boolean choosingRunInBackground = false;
    private MenuBar macOSMenuBar;
    private static DropShadow errorBorder = new DropShadow();
    private static DropShadow borderGlow = new DropShadow();

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
        // create effect
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
        } else { // because libimobiledevice support not fully supported
            readFromConnectedDeviceButton.setText("Read from connected device(beta)");
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

    public void checkForUpdatesHandler() {
        checkForUpdates(true);
    }

    private void run(String device) {
        if ("".equals(device)) {
            return;
        }
        File tsschecker;
        File buildManifestPlist = null;
        try {
            tsschecker = getTsschecker();
        } catch (IOException e) {
            newReportableError("There was an error creating tsschecker.", e.getMessage());
            return;
        }
        tsschecker.deleteOnExit();

        File locationToSaveBlobs = new File(pathField.getText());
        //noinspection ResultOfMethodCallIgnored
        locationToSaveBlobs.mkdirs();
        ArrayList<String> args = new ArrayList<>(Arrays.asList(tsschecker.getPath(), "-d", device, "-s", "-e", ecidField.getText(), "--save-path", pathField.getText()));
        if (getBoardConfig) {
            Collections.addAll(args, "--boardconfig", boardConfigField.getText());
        }
        if (apnonceCheckBox.isSelected()) {
            Collections.addAll(args, "--apnonce", apnonceField.getText());
        }
        if (versionCheckBox.isSelected()) {
            args.add("-l");
        } else if (betaCheckBox.isSelected()) {
            try {
                if (!ipswField.getText().matches("https?://.*apple.*\\.ipsw")) {
                    newUnreportableError("\"" + ipswField.getText() + "\" is not a valid URL.\n\nMake sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"");
                    deleteTempFiles(tsschecker, null);
                    return;
                }
                buildManifestPlist = File.createTempFile("BuildManifest", ".plist");
                ZipInputStream zin;
                try {
                    URL url = new URL(ipswField.getText());
                    zin = new ZipInputStream(url.openStream());
                } catch (IOException e) {
                    newUnreportableError("\"" + ipswField.getText() + "\" is not a valid URL.\n\nMake sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"");
                    deleteTempFiles(tsschecker, buildManifestPlist);
                    return;
                }
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    if ("BuildManifest.plist".equals(ze.getName())) {
                        copyStreamToFile(zin, buildManifestPlist);
                        break;
                    }
                }
                buildManifestPlist.deleteOnExit();
            } catch (IOException e) {
                newReportableError("Unable to get BuildManifest from .ipsw.", e.getMessage());
                e.printStackTrace();
                deleteTempFiles(tsschecker, buildManifestPlist);
                return;
            }
            Collections.addAll(args, "-i", versionField.getText(), "--beta", "--buildid", buildIDField.getText(), "-m", buildManifestPlist.toString());
        } else {
            Collections.addAll(args, "-i", versionField.getText());
        }
        Process proc;
        try {
            log("Running: " + args.toString());
            proc = new ProcessBuilder(args).start();
        } catch (IOException e) {
            newReportableError("There was an error starting tsschecker.", e.toString());
            e.printStackTrace();
            deleteTempFiles(tsschecker, buildManifestPlist);
            return;
        }
        String tsscheckerLog;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            StringBuilder logBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                log(line + "\n");
                logBuilder.append(line).append("\n");
            }
            tsscheckerLog = logBuilder.toString();
        } catch (IOException e) {
            newReportableError("There was an error getting the tsschecker result", e.toString());
            e.printStackTrace();
            deleteTempFiles(tsschecker, buildManifestPlist);
            return;
        }
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            newReportableError("The tsschecker process was interrupted.", e.toString());
        }
        if (tsscheckerLog.contains("Saved shsh blobs")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Successfully saved blobs in\n" + pathField.getText(), ButtonType.OK);
            alert.setHeaderText("Success!");
            alert.showAndWait();
        } else if (tsscheckerLog.contains("[Error] [TSSC] manually specified ecid=" + ecidField.getText() + ", but parsing failed")) {
            newUnreportableError("\"" + ecidField.getText() + "\"" + " is not a valid ECID. Try getting it from iTunes.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.");
            ecidField.setEffect(errorBorder);
        } else if (tsscheckerLog.contains("[Error] [TSSC] device " + device + " could not be found in devicelist")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "tsschecker could not find device: \"" + device +
                    "\"\n\nPlease create a new Github issue or PM me on Reddit if you used the dropdown menu.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.", githubIssue, redditPM, ButtonType.CANCEL);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert);
        } else if (tsscheckerLog.contains("[Error] [TSSC] ERROR: could not get url for device " + device + " on iOS " + versionField.getText())) {
            newUnreportableError("Could not find device \"" + device + "\" on iOS/tvOS " + versionField.getText() +
                    "\n\nThe version doesn't exist or isn't compatible with the device");
            versionField.setEffect(errorBorder);
        } else if (tsscheckerLog.contains("[Error] [TSSC] manually specified apnonce=" + apnonceField.getText() + ", but parsing failed")) {
            newUnreportableError("\"" + apnonceField.getText() + "\" is not a valid apnonce");
            apnonceField.setEffect(errorBorder);
        } else if (tsscheckerLog.contains("[WARNING] [TSSC] could not get id0 for installType=Erase. Using fallback installType=Update since user did not specify installType manually")
                && tsscheckerLog.contains("[Error] [TSSR] Error: could not get id0 for installType=Update")
                && tsscheckerLog.contains("[Error] [TSSR] faild to build tssrequest")
                && tsscheckerLog.contains("Error] [TSSC] checking tss status failed!")) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Saving blobs failed. Check the board configuration or try again later.\n\nIf this doesn't work, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.",
                    githubIssue, redditPM, ButtonType.OK);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (tsscheckerLog.contains("[Error] ERROR: TSS request failed: Could not resolve host:")) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Saving blobs failed. Check your internet connection.\n\nIf your internet is working and you can connect to apple.com in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.",
                    githubIssue, redditPM, ButtonType.OK);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (tsscheckerLog.contains("[Error] [Error] can't save shsh at " + pathField.getText())) {
            newUnreportableError("\'" + pathField.getText() + "\' is not a valid path\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.");
            pathField.setEffect(errorBorder);
        } else if (tsscheckerLog.contains("iOS " + versionField.getText() + " for device " + device + " IS NOT being signed!") || tsscheckerLog.contains("Build " + buildIDField.getText() + " for device iPhone9,2 IS NOT being signed!")) {
            newUnreportableError("iOS/tvOS " + versionField.getText() + " is not being signed for device " + device);
            versionField.setEffect(errorBorder);
            if (betaCheckBox.isSelected()) {
                buildIDField.setEffect(errorBorder);
                ipswField.setEffect(errorBorder);
            }
        } else if (tsscheckerLog.contains("[Error] [TSSC] failed to load manifest")) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Failed to load manifest.\n\n \"" + ipswField.getText() + "\" might not be a valid URL.\n\nMake sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"\n\nIf the URL is fine, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard",
                    githubIssue, redditPM, ButtonType.OK);
            resizeAlertButtons(alert);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (tsscheckerLog.contains("[Error] [TSSC] selected device can't be used with that buildmanifest")) {
            newUnreportableError("Device and build manifest don't match.");
        } else if (tsscheckerLog.contains("[Error]")) {
            newReportableError("Saving blobs failed.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.", tsscheckerLog);
        } else {
            newReportableError("Unknown result.\n\nIf this was done to test whether the preset works in the background, please cancel that preset, fix the error, and try again.", tsscheckerLog);
        }

        deleteTempFiles(tsschecker, buildManifestPlist);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void deleteTempFiles(File tsschecker, File buildManifestPlist) {
        try {
            if (tsschecker.exists()) {
                tsschecker.delete();
            }
            if (buildManifestPlist != null && buildManifestPlist.exists()) {
                buildManifestPlist.delete();
            }
        } catch (NullPointerException ignored) {
        }
    }

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
        if ("".equals(ecidField.getText())) {
            ecidField.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && "".equals(deviceModelChoiceBox.getValue())) {
            deviceModelChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        if (identifierCheckBox.isSelected() && "".equals(identifierField.getText())) {
            identifierField.setEffect(errorBorder);
            doReturn = true;
        }
        if (getBoardConfig && "".equals(boardConfigField.getText())) {
            boardConfigField.setEffect(errorBorder);
            doReturn = true;
        }
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

    public void checkBlobs() {
        openURL("https://tsssaver.1conan.com/check.php");
    }

    public void helpLabelHandler(MouseEvent evt) {
        if (Main.DEBUG_MODE) return; //click on the question mark and add this method as a breakpoint
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
    public void aboutMenuHandler() { //handles the "About Blobsaver" menu
        Stage aboutStage;
        if ((aboutStage = Main.getStage("About")) != null) { //if about menu already opened
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

        Platform.runLater(() -> { //run this later so that alert.showAndWait() makes the stage first
            Stage aboutStage2; //aboutStage is already used in this scope
            if ((aboutStage2 = Main.getStage("About")) != null) {
                aboutStage2.setAlwaysOnTop(true);
            } else {
                System.out.println("bad news: About stage hasn't been initialized yet");
            }
        });

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

    private Stage createDefaultAboutStage() {
        AboutStageBuilder stageBuilder = AboutStageBuilder.start(
                labelMaker.getLabel(LabelName.ABOUT, "blobsaver"))
                .withAppName("blobsaver").withCloseOnFocusLoss().withCopyright("Copyright \u00A9 " + Calendar
                        .getInstance().get(Calendar.YEAR));

        try {
            IcnsParser parser = IcnsParser.forFile(AboutStageBuilder.DEFAULT_APP_ICON);
            stageBuilder = stageBuilder.withImage(new Image(parser.getIconStream(IcnsType.ic08)));
        } catch (IOException e) {
            newUnreportableError("Failed to load about stage dummy image");
            e.printStackTrace();
        }

        return stageBuilder.build();
    }

    // so that there's no crash on cmd + q
    private Menu createCustomApplicationMenu(MenuToolkit tk) {
        return new Menu("Apple", null, tk.createAboutMenuItem("blobsaver", createDefaultAboutStage()),
                new SeparatorMenuItem(),
                tk.createHideMenuItem("blobsaver"),
                tk.createHideOthersMenuItem(),
                tk.createUnhideAllMenuItem(),
                new SeparatorMenuItem(),
                createCustomQuitMenuItem());
    }

    private MenuItem createCustomQuitMenuItem() {
        MenuItem quit = new MenuItem(labelMaker.getLabel(LabelName.QUIT, "blobsaver"));
        quit.setOnAction(event -> Main.quit());
        quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.META_DOWN));
        return quit;
    }

    //sets up mac OS menu bar and returns it
    private MenuBar getMacOSMenuBar() {
        macOSMenuBar = new MenuBar();
        MenuToolkit tk = MenuToolkit.toolkit();

        Menu applicationMenu = createCustomApplicationMenu(tk);

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
        windowMenu.getItems().add(tk.createCloseWindowMenuItem()); //TODO: add windows/linux equivalent [ctrl + w]
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
            /*if (PlatformUtil.isMac()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "You will need to restart the application for changes to take effect.", ButtonType.OK);
                alert.showAndWait();
                appPrefs.putBoolean("Show background startup message", true);
                appPrefs.putBoolean("Start background immediately", false);
                Platform.exit();
                Background.stopBackground(false);
                System.exit(0);
            } else {*/
            Background.stopBackground(true);
            appPrefs.putBoolean("Show background startup message", true);
            appPrefs.putBoolean("Start background immediately", false);
            startBackgroundButton.setText("Start background");
        } else if (appPrefs.getBoolean("Show background startup message", true)) {
//            Alert alert = new Alert(Alert.AlertType.INFORMATION,
//                    "You will need to restart the application for changes to take effect. By default, when you launch this application, it will start up in the background. "
//                            + "If you would like to show the window, find the icon in your system tray/status bar and click on \"Open Window\"", ButtonType.OK);
//            alert.showAndWait();
            appPrefs.putBoolean("Show background startup message", false);
            appPrefs.putBoolean("Start background immediately", true);
//            Platform.exit();
//            System.exit(0);
            startBackgroundButton.setText("Stop background");
            Background.startBackground(false);
        } /*else if (appPrefs.getBoolean("Show background startup message", true)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "The application will now enter the background. By default, when you launch this application, it will start up in the background. "
                            + "If you would like to show the window, find the icon in your system tray/status bar and click on \"Open Window\"", ButtonType.OK);
            alert.showAndWait();
            appPrefs.putBoolean("Show background startup message", false);
            appPrefs.putBoolean("Start background immediately", true);
            Background.startBackground(false);
        }*/ else {
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

    @SuppressWarnings({"unchecked", "UnnecessaryReturnStatement"})
    public void readInfo() {
        if (!PlatformUtil.isMac()) {
            String alertText;
            if (PlatformUtil.isWindows()) {
                alertText = "This feature will most likely not work, but you can give it a try. If this feature doesn't work, please don't report this bug to me, I already know and am trying to fix it.";
            } else {
                alertText = "IMPORTANT: make sure to install libimobiledevice before running it and `ideviceinfo` and `idevicepair` are in your $PATH\n\nThis feature is in beta, and may or may not work. Please report any bugs with this feature to me using the help menu.";
            }
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION, alertText);
            confirmationAlert.showAndWait();
            if (confirmationAlert.getResult().equals(ButtonType.CANCEL)) {
                return;
            }
        }
        String idevicepairPath;
        try {
            idevicepairPath = getidevicepair().getPath();
        } catch (FileNotFoundException e) {
            if ("idevicepair is not in $PATH".equals(e.getMessage())) {
                newUnreportableError("Either idevicepair is not in the $PATH, or libimobiledevice is not installed. Please install it before continuing");
            } else {
                e.printStackTrace();
            }
            return;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        // validate pairing status
        try {
            String idevicepairResult = executeProgram(idevicepairPath, "pair").trim();
            log("idevicepair: " + idevicepairResult);
            //noinspection StatementWithEmptyBody
            if (idevicepairResult.contains("Paired with device")) {
                // continue
            } else if (idevicepairResult.contains("dyld: Library not loaded:")) {
                deleteFolder(new File(System.getProperty("user.home", ".blobsaver_bin")));
                newUnreportableError("This error will happen if you have used version v2.2 before. This error will automatically be fixed after restarting the application.");
                Platform.exit();
                System.exit(-1);
                return;
            } else if (idevicepairResult.contains("Please accept the trust dialog")) {
                newUnreportableError("Please accept the trust dialog on the device");
                return;
            } else if (idevicepairResult.contains("No device found")) {
                newUnreportableError("No device found, is it plugged in?");
                return;
            } else if (idevicepairResult.contains("ERROR") && idevicepairResult.contains("a passcode is set")) {
                newUnreportableError("Please unlock your device.");
                return;
            } else if (idevicepairResult.contains("Device") && idevicepairResult.contains("returned unhandled error code -13")) {
                newUnreportableError("Please disconnect your device, unlock it, plug it back in, and try again. If this doesn't work, please create a new Github issue or PM me on Reddit via the help menu.");
                return;
            } else if (idevicepairResult.contains("ERROR")) {
                newReportableError("idevicepair error.", idevicepairResult);
                return;
            } else {
                newReportableError("Unknown idevicepair result: " + idevicepairResult, idevicepairResult);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            newReportableError("Unable to validate pairing.", e.getMessage());
        }
        String ideviceinfoPath;
        try {
            ideviceinfoPath = getideviceinfo().getPath();
        } catch (FileNotFoundException e) {
            if ("ideviceinfo is not in $PATH".equals(e.getMessage())) {
                newUnreportableError("Either ideviceinfo is not in the $PATH, or libimobiledevice is not installed. Please install it or add it to the path before continuing");
            } else {
                e.printStackTrace();
            }
            return;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        // read ECID
        try {
            String ideviceinfoResult = executeProgram(ideviceinfoPath, "-k", "UniqueChipID").trim();
            log("ideviceinfo -k UniqueChipID:" + ideviceinfoResult);
            if (ideviceinfoResult.contains("ERROR: Could not connect to lockdownd")) {
                newReportableError("ideviceinfo:" + ideviceinfoResult, ideviceinfoResult);
                return;
            } else if (ideviceinfoResult.contains("dyld: Library not loaded:")) {
                version2dot2libimobiledeviceError();
                return;
            } else if (ideviceinfoResult.contains("No device found")) {
                newUnreportableError("No device found, is it plugged in?");
                return;
            } else if (ideviceinfoResult.contains("ERROR")) {
                newReportableError("ideviceinfo error.", ideviceinfoResult);
                return;
            } else {
                try {
                    ecidField.setText(Long.toHexString(Long.valueOf(ideviceinfoResult)).toUpperCase());
                } catch (Exception e) {
                    e.printStackTrace();
                    newReportableError("Unknown ideviceinfo(ecid) result: " + ideviceinfoResult, ideviceinfoResult);
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            newReportableError("Unable to get ECID.", e.getMessage());
        }
        // read device model
        try {
            String ideviceinfoResult = executeProgram(ideviceinfoPath, "-k", "ProductType").trim();
            log("ideviceinfo -k ProductType: " + ideviceinfoResult);
            if (ideviceinfoResult.contains("ERROR: Could not connect to lockdownd")) {
                newReportableError("ideviceinfo:" + ideviceinfoResult, ideviceinfoResult);
                return;
            } else if (ideviceinfoResult.contains("dyld: Library not loaded:")) {
                version2dot2libimobiledeviceError();
                return;
            } else if (ideviceinfoResult.contains("No device found")) {
                newUnreportableError("No device found, is it plugged in?");
                return;
            } else if (ideviceinfoResult.contains("ERROR")) {
                newReportableError("ideviceinfo error.", ideviceinfoResult);
            } else if (ideviceinfoResult.startsWith("iPhone")) {
                deviceTypeChoiceBox.setValue("iPhone");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().getOrDefault(ideviceinfoResult, null));
            } else if (ideviceinfoResult.startsWith("iPod")) {
                deviceTypeChoiceBox.setValue("iPod");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(ideviceinfoResult));
            } else if (ideviceinfoResult.startsWith("iPad")) {
                deviceTypeChoiceBox.setValue("iPad");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(ideviceinfoResult));
            } else if (ideviceinfoResult.startsWith("AppleTV")) {
                deviceTypeChoiceBox.setValue("Apple TV");
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(ideviceinfoResult));
            } else {
                newReportableError("Unknown ideviceinfo(device model) result: " + ideviceinfoResult, ideviceinfoResult);
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            newReportableError("Unable to get device model.", e.getMessage());
        }
        // read board config
        if (!boardConfigField.isDisabled()) {
            try {
                String ideviceinfoResult = executeProgram(ideviceinfoPath, "-k", "HardwareModel").trim();
                log("ideviceinfo -k HardwareModel: " + ideviceinfoResult);
                if (ideviceinfoResult.contains("ERROR: Could not connect to lockdownd")) {
                    newReportableError("ideviceinfo:" + ideviceinfoResult, ideviceinfoResult);
                    return;
                } else if (ideviceinfoResult.contains("dyld: Library not loaded:")) {
                    version2dot2libimobiledeviceError();
                    return;
                } else if (ideviceinfoResult.contains("No device found")) {
                    newUnreportableError("No device found, is it plugged in?");
                    return;
                } else if (ideviceinfoResult.contains("ERROR")) {
                    newReportableError("ideviceinfo error.", ideviceinfoResult);
                    return;
                } else {
                    boardConfigField.setText(ideviceinfoResult);
                }
            } catch (IOException e) {
                e.printStackTrace();
                newReportableError("Unable to get board config.", e.getMessage());
            }
        }
    }

    private void version2dot2libimobiledeviceError() {
        deleteFolder(new File(System.getProperty("user.home", ".blobsaver_bin")));
        newUnreportableError("This error will happen if you have used version v2.2 before. This error will automatically be fixed after restarting the application.");
        Platform.exit();
        System.exit(-1);
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
        doReturn = doReturn || isTextFieldValid(true, ecidField);
        doReturn = doReturn || isTextFieldValid(identifierCheckBox, identifierField);
        doReturn = doReturn || isTextFieldValid(getBoardConfig, boardConfigField);
        doReturn = doReturn || isTextFieldValid(apnonceCheckBox, apnonceField);
        doReturn = doReturn || isTextFieldValid(true, pathField);
        doReturn = doReturn || isTextFieldValid(!versionCheckBox.isSelected(), versionField);
        doReturn = doReturn || isTextFieldValid(betaCheckBox, buildIDField);
        doReturn = doReturn || isTextFieldValid(betaCheckBox, ipswField);
        if (doReturn) {
            return;
        }
        String deviceModel = (String) deviceModelChoiceBox.getValue();
        if ("".equals(deviceModel)) {
            String identifierText = identifierField.getText();
            try {
                if (identifierText.startsWith("iPad") || identifierText.startsWith("iPod") || identifierText.startsWith("iPhone") || identifierText.startsWith("AppleTV")) {
                    run(identifierField.getText());
                } else {
                    identifierField.setEffect(errorBorder);
                    newUnreportableError("\"" + identifierText + "\" is not a valid identifier");
                }
            } catch (StringIndexOutOfBoundsException e) {
                newUnreportableError("\"" + identifierText + "\" is not a valid identifier");
            }
        } else {
            run(textToIdentifier(deviceModel));
        }
    }

    private static boolean isTextFieldValid(CheckBox checkBox, TextField textField) {
        return isTextFieldValid(checkBox.isSelected(), textField);
    }
    private static boolean isTextFieldValid(boolean isTextFieldRequired, TextField textField) {
        if (isTextFieldRequired && "".equals(textField.getText())) {
            textField.setEffect(errorBorder);
            return true;
        }
        return false;
    }
}
