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

import airsquared.blobsaver.app.natives.Libirecovery;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

public class Controller {


    @FXML private MenuBar menuBar;
    @FXML private MenuItem checkForUpdatesMenuItem, clearAllDataMenuItem, checkForValidBlobsMenuItem, debugLogMenuItem;

    @FXML private ChoiceBox<String> deviceTypeChoiceBox, deviceModelChoiceBox;

    @FXML private TextField ecidField, boardConfigField, apnonceField, versionField, identifierField,
            pathField, ipswField;

    @FXML private CheckBox apnonceCheckBox, allSignedVersionsCheckBox, identifierCheckBox, betaCheckBox;

    @FXML private Label versionLabel;

    @FXML private Button readApnonceButton, startBackgroundButton, chooseTimeToRunButton, forceCheckForBlobs,
            backgroundSettingsButton, saveDeviceButton;

    @FXML private Button device1Button, device2Button, device3Button, device4Button, device5Button, device6Button,
            device7Button, device8Button, device9Button, device10Button;
    private List<Button> savedDeviceButtons;

    @FXML private VBox savedDevicesVBox;

    @FXML private Button goButton;

    private Alert runningAlert;
    private DialogPane aboutStage;

    private final SimpleBooleanProperty getBoardConfig = new SimpleBooleanProperty();
    private boolean editingSavedDevices = false;
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

        savedDeviceButtons = Arrays.asList(device1Button, device2Button, device3Button, device4Button, device5Button, device6Button, device7Button, device8Button, device9Button, device10Button);
        for (int i = 1; i <= 10; i++) { // sets the names for the device buttons
            Button btn = savedDeviceButtons.get(i - 1); // needed so it's 'effectively final'
            Prefs.savedDevice(i).ifPresent(savedDevice -> btn.setText(savedDevice.getName()));
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
        if (allSignedVersionsCheckBox.isSelected()) {
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
            if (!allSignedVersionsCheckBox.isSelected()) {
                allSignedVersionsCheckBox.fire(); // disable version field
            }
            allSignedVersionsCheckBox.setSelected(false); // turn latest version off without enabling version field
            allSignedVersionsCheckBox.setDisable(true);
        } else {
            ipswField.setEffect(null);
            ipswField.setText("");
            allSignedVersionsCheckBox.setDisable(false);
            allSignedVersionsCheckBox.setSelected(true);
        }
    }

    public void filePickerHandler() {
        File result = Utils.showFilePickerDialog(Main.primaryStage, new File(pathField.getText()));
        if (result != null) {
            pathField.setText(result.getAbsolutePath());
        }
    }

    private void loadSavedDevice(Prefs.SavedDevice savedDevice) {
        ecidField.setText(savedDevice.getEcid());
        pathField.setText(savedDevice.getEcid());
        String identifier = savedDevice.getIdentifier();
        if (Devices.getDeviceModelIdentifiersMap().containsKey(identifier)) {
            Utils.setSelectedFire(identifierCheckBox, false);
            deviceTypeChoiceBox.setValue(Devices.getDeviceType(identifier));
            deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(identifier));
        } else {
            Utils.setSelectedFire(identifierCheckBox, true);
            identifierField.setText(identifier);
        }
        savedDevice.getBoardConfig().ifPresent(b -> boardConfigField.setText(b));
        savedDevice.getApnonce().ifPresent(a -> {
            Utils.setSelectedFire(apnonceCheckBox, true);
            apnonceField.setText(a);
        });
    }

    public void savedDeviceButtonHandler(Event evt) {
        Button btn = (Button) evt.getTarget();
        int presNum = savedDeviceButtons.indexOf(btn) + 1;
        if (editingSavedDevices) {
            saveDevice(presNum);
            saveDeviceButton.fire();
        } else if (choosingRunInBackground) {
            Optional<Prefs.SavedDevice> savedDevice = Prefs.savedDevice(presNum);
            if (!savedDevice.isPresent()) {
                Utils.showUnreportableError("Device " + presNum + " doesn't exist");
                return;
            }
            if (btn.getText().startsWith("Cancel ")) {
                savedDevice.get().setBackground(false);
                System.out.println("removed device" + presNum + " from list");
                btn.setText("Use " + btn.getText().substring("Cancel ".length()));
            } else {
                loadSavedDevice(savedDevice.get());
                Utils.setSelectedFire(betaCheckBox, false);
                Utils.setSelectedFire(allSignedVersionsCheckBox, true);
                TSS tss = createTSS();
                EventHandler<WorkerStateEvent> oldEventHandler = tss.getOnSucceeded();
                tss.setOnSucceeded(event -> {
                    savedDevice.get().setBackground(true);
                    btn.setText("Cancel " + btn.getText().substring("Use ".length()));
                    startBackgroundButton.setDisable(false);
                    forceCheckForBlobs.setDisable(false);
                    chooseTimeToRunButton.setDisable(false);
                    System.out.println("added device" + presNum + " to list");

                    oldEventHandler.handle(event);
                });
                Utils.executeInThreadPool(tss);
                showRunningAlert("Testing device...");
            }
        } else {
            Prefs.savedDevice(presNum).ifPresent(this::loadSavedDevice);
        }
    }

    @SuppressWarnings("Duplicates")
    private void saveDevice(int num) {
        boolean doReturn = Utils.isFieldEmpty(!identifierCheckBox.isSelected(), deviceModelChoiceBox.getValue(), deviceModelChoiceBox);
        doReturn |= Utils.isFieldEmpty(true, ecidField);
        doReturn |= Utils.isFieldEmpty(identifierCheckBox, identifierField);
        doReturn |= Utils.isFieldEmpty(getBoardConfig.get(), boardConfigField);
        doReturn |= Utils.isFieldEmpty(apnonceCheckBox, apnonceField);
        if (doReturn) return;

        Prefs.SavedDevice pres = Prefs.createSavedDevice(num);
        TextInputDialog textInputDialog = new TextInputDialog(pres.getName());
        textInputDialog.setTitle("Name Device " + num);
        textInputDialog.setHeaderText("Name Device");
        textInputDialog.setContentText("Please enter a name for the device:");
        textInputDialog.showAndWait();

        String result = textInputDialog.getResult();
        if (!Utils.isEmptyOrNull(result)) {
            pres.setName(textInputDialog.getResult());
            savedDeviceButtons.get(num - 1).setText("Save in " + textInputDialog.getResult());
        } else {
            return;
        }

        pres.setEcid(ecidField.getText());
        pres.setSavePath(pathField.getText());
        pres.setIdentifier(identifierCheckBox.isSelected() ?
                identifierField.getText() : Devices.textToIdentifier(deviceModelChoiceBox.getValue()));

        if (getBoardConfig.get()) {
            pres.setBoardConfig(boardConfigField.getText());
        }
        if (apnonceCheckBox.isSelected()) {
            pres.setApnonce(apnonceField.getText());
        }
    }

    public void saveDeviceHandler() {
        editingSavedDevices = !editingSavedDevices;
        if (editingSavedDevices) {
            saveDeviceButton.setText("Back");
            savedDevicesVBox.setEffect(Utils.borderGlow);
            savedDeviceButtons.forEach(btn -> btn.setText("Save in " + btn.getText().substring("Load ".length())));
            goButton.setDefaultButton(false);
            goButton.setDisable(true);
            backgroundSettingsButton.setVisible(false);
            backgroundSettingsButton.setManaged(false);
            saveDeviceButton.setDefaultButton(true);
        } else {
            saveDeviceButton.setDefaultButton(false);
            goButton.setDefaultButton(true);
            goButton.setDisable(false);
            backgroundSettingsButton.setVisible(true);
            backgroundSettingsButton.setManaged(true);
            savedDevicesVBox.setEffect(null);
            saveDeviceButton.setText("Save");
            savedDeviceButtons.forEach(btn -> btn.setText("Load " + btn.getText().substring("Save in ".length())));
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
        savedDevicesVBox.setMinHeight(560.0);
        savedDevicesVBox.setPrefHeight(560.0);
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
            savedDevicesVBox.setEffect(Utils.borderGlow);
            savedDeviceButtons.forEach(btn -> {
                if (Prefs.isDeviceInBackground(savedDeviceButtons.indexOf(btn) + 1)) {
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
            savedDevicesVBox.setEffect(null);
            backgroundSettingsButton.setText("Background settings");
            savedDeviceButtons.forEach(btn -> {
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
        saveDeviceButton.setDisable(showBackgroundSettings);
        saveDeviceButton.setVisible(!showBackgroundSettings);
        saveDeviceButton.setManaged(!showBackgroundSettings);
        backgroundSettingsButton.setDefaultButton(showBackgroundSettings);
        chooseTimeToRunButton.setVisible(showBackgroundSettings);
        forceCheckForBlobs.setVisible(showBackgroundSettings);
        startBackgroundButton.setVisible(showBackgroundSettings);
        if (!showBackgroundSettings || !Prefs.anyBackgroundDevices()) {
            startBackgroundButton.setDisable(showBackgroundSettings);
            forceCheckForBlobs.setDisable(showBackgroundSettings);
            chooseTimeToRunButton.setDisable(showBackgroundSettings);
        }
    }

    public void chooseTimeToRunHandler() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Frequency to check for new blobs");
        alert.setHeaderText("Frequency to check");
        TextField textField = new TextField(Integer.toString(Prefs.appPrefs.getInt("Time to run", 1)));
        // make it so user can only enter integers
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList("Minutes", "Hours", "Days", "Weeks"));
        choiceBox.setValue(Prefs.appPrefs.get("Time unit for background", "Days"));
        HBox hBox = new HBox(textField, choiceBox);
        alert.getDialogPane().setContent(hBox);
        alert.showAndWait();
        if ((alert.getResult() != null) && !ButtonType.CANCEL.equals(alert.getResult()) && !Utils.isEmptyOrNull(textField.getText()) && !Utils.isEmptyOrNull(choiceBox.getValue())) {
            Prefs.appPrefs.putInt("Time to run", Integer.parseInt(textField.getText()));
            Prefs.appPrefs.put("Time unit for background", choiceBox.getValue());
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
            Prefs.appPrefs.putBoolean("Start background immediately", false);
            startBackgroundButton.setText("Start background");
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "The application will now enter the background. By default, when you launch this application, it will start up in the background. "
                            + "If you would like to show the window, find the icon in your system tray/status bar and click on \"Open Window\"", ButtonType.OK);
            alert.showAndWait();
            Prefs.appPrefs.putBoolean("Start background immediately", true);
            startBackgroundButton.setText("Stop background");
            Background.startBackground(false);
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
            Prefs.resetPrefs();
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
            ecidField.setText(Long.toHexString(LibimobiledeviceUtil.getECID(true)).toUpperCase());
            // read device model
            String deviceIdentifier = LibimobiledeviceUtil.getDeviceModelIdentifier(true);
            try {
                deviceTypeChoiceBox.setValue(Devices.getDeviceType(deviceIdentifier));
                deviceModelChoiceBox.setValue(Devices.getDeviceModelIdentifiersMap().get(deviceIdentifier));
            } catch (IllegalArgumentException e) {
                Utils.showReportableError("Unknown model: " + deviceIdentifier);
                return;
            }
            // read board config
            if (!boardConfigField.isDisabled()) {
                boardConfigField.setText(LibimobiledeviceUtil.getBoardConfig(true));
            }
        } catch (LibimobiledeviceUtil.LibimobiledeviceException e) {
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

        Task<String> getApnonceTask = LibimobiledeviceUtil.createGetApnonceTask();
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
        int errorCode = Libirecovery.open(irecvClient);
        if (errorCode != 0) {
            Utils.showReportableError("irecovery error: code=" + errorCode + "\n\nUnable to find a device, try using another tool to exit recovery mode.");
            return;
        }
        try {
            LibimobiledeviceUtil.exitRecovery(irecvClient.getValue(), true);
        } catch (LibimobiledeviceUtil.LibimobiledeviceException e) {
            e.printStackTrace();
        }
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
        incorrect |= Utils.isFieldEmpty(!allSignedVersionsCheckBox.isSelected() && !betaCheckBox.isSelected(), versionField);
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
        if (!allSignedVersionsCheckBox.isSelected() && !betaCheckBox.isSelected()) {
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
            runningAlert = null;
            Alert alert = new Alert(Alert.AlertType.INFORMATION, tss.getValue());
            alert.setHeaderText("Success!");
            alert.showAndWait();
        });
        tss.setOnFailed(event -> {
            runningAlert.close();
            runningAlert = null;
            tss.getException().printStackTrace();
            parseException(tss.getException());
        });
        return tss;
    }

    private void showRunningAlert(String title) {
        runningAlert = new Alert(Alert.AlertType.INFORMATION);
        runningAlert.setTitle(title);
        runningAlert.setHeaderText("Saving blobs...              ");
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
                } else if (!allSignedVersionsCheckBox.isSelected()) {
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
