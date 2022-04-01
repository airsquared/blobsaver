/*
 * Copyright (c) 2021  airsquared
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
import com.sun.jna.Platform;
import com.sun.jna.ptr.PointerByReference;
import de.jangassen.MenuToolkit;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("TextBlockMigration")
public class Controller {


    @FXML private MenuBar menuBar;
    @FXML private MenuItem checkForUpdatesMenu, clearAllDataMenu, deleteDeviceMenu, backgroundSettingsMenu;
    @FXML private RadioMenuItem darkDisabled, darkSync, darkEnabled;

    @FXML private ChoiceBox<String> deviceTypeChoiceBox, deviceModelChoiceBox;

    @FXML private TextField ecidField, boardConfigField, apnonceField, generatorField, versionField, identifierField,
            pathField, ipswField;

    @FXML private CheckBox apnonceCheckBox, allSignedVersionsCheckBox, identifierCheckBox, betaCheckBox, saveToTSSSaverCheckBox, saveToSHSHHostCheckBox;

    @FXML private Label versionLabel, savedDevicesLabel;

    @FXML private Button startBackgroundButton, chooseTimeToRunButton, forceCheckForBlobs;
    @FXML private ToggleButton backgroundSettingsButton;

    @FXML private ListView<Prefs.SavedDevice> deviceList;
    @FXML private VBox savedDevicesVBox;

    public static final String initialPath = System.getProperty("user.home") + File.separator + "Blobs";

    public void initialize() {
        if (Platform.isMac()) {
            useMacOSMenuBar();
        }
        deviceList.getSelectionModel().selectedItemProperty().addListener((a, b, device) -> loadSavedDevice(device));
        deleteDeviceMenu.disableProperty().bind(Bindings.isNull(deviceList.getSelectionModel().selectedItemProperty()));
        backgroundSettingsMenu.textProperty().bind(Bindings.when(backgroundSettingsButton.selectedProperty())
                .then("Hide Background Settings").otherwise("Show Background Settings"));
        backgroundSettingsMenu.setOnAction(e -> backgroundSettingsButton.fire());
        allSignedVersionsCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                saveToTSSSaverCheckBox.setSelected(false);
                saveToSHSHHostCheckBox.setSelected(false);
            }
        });
        switch (Prefs.getDarkMode()) {
            case DISABLED -> darkDisabled.setSelected(true);
            case SYNC_WITH_OS -> darkSync.setSelected(true);
            case ENABLED -> darkEnabled.setSelected(true);
        }
    }

    public void newGithubIssue() { Utils.newGithubIssue(); }

    public void sendRedditPM() { Utils.sendRedditPM(); }

    public void clearEffectHandler(Event evt) {
        ((Node) evt.getSource()).setEffect(null);
    }

    @SuppressWarnings("unused")
    public void checkRequirements(ObservableValue<?> ignored, String ignored2, String identifier) {
        identifier = Utils.defaultIfNull(Devices.modelToIdentifier(identifier), identifier);
        if (!Utils.isEmptyOrNull(identifier) && Devices.doesRequireBoardConfig(identifier)) {
            boardConfigField.setDisable(false);
            boardConfigField.setEffect(Utils.borderGlow);
        } else if (!boardConfigField.isDisable()) {
            boardConfigField.setEffect(null);
            boardConfigField.setDisable(true);
            boardConfigField.setText("");
        }
        if (!Utils.isEmptyOrNull(identifier) && Devices.doesRequireApnonce(identifier)) {
            Utils.setSelectedFire(apnonceCheckBox, true);
            apnonceCheckBox.setDisable(true);
        } else {
            apnonceCheckBox.setDisable(false);
            Utils.setSelectedFire(apnonceCheckBox, false);
        }
    }

    @SuppressWarnings("unused")
    public void deviceTypeChoiceBoxHandler() {
        deviceModelChoiceBox.setItems(Devices.getModelsForType(deviceTypeChoiceBox.getValue()));
        versionLabel.setText(Utils.defaultIfNull(Devices.getOSNameForType(deviceTypeChoiceBox.getValue()), "Version"));
    }

    public void checkForUpdatesHandler() { Utils.checkForUpdates(true); }

    public void apnonceCheckBoxHandler() {
        if (apnonceCheckBox.isSelected()) {
            apnonceField.setEffect(Utils.borderGlow);
        } else {
            apnonceField.setEffect(null);
            apnonceField.setText("");
            generatorField.setText("");
        }
    }

    public void versionCheckBoxHandler() {
        if (allSignedVersionsCheckBox.isSelected()) {
            versionField.setEffect(null);
            versionField.setText("");
        } else {
            versionField.setEffect(Utils.borderGlow);
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
            versionField.setEffect(null);
            versionField.setText("");
            allSignedVersionsCheckBox.setSelected(false);
        } else {
            ipswField.setEffect(null);
            ipswField.setText("");
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
        if (savedDevice == null) {
            deleteDeviceMenu.setText("Remove Saved Device");
            return;
        }
        deleteDeviceMenu.setText("Remove Saved Device \"" + savedDevice + "\"");

        ecidField.setText(savedDevice.getEcid());
        pathField.setText(savedDevice.getSavePath());
        String identifier = savedDevice.getIdentifier();
        if (Devices.containsIdentifier(identifier)) {
            Utils.setSelectedFire(identifierCheckBox, false);
            deviceTypeChoiceBox.setValue(Devices.getDeviceType(identifier));
            deviceModelChoiceBox.setValue(Devices.identifierToModel(identifier));
        } else {
            Utils.setSelectedFire(identifierCheckBox, true);
            identifierField.setText(identifier);
        }
        savedDevice.getBoardConfig().ifPresent(boardConfigField::setText);
        savedDevice.getApnonce().ifPresent(a -> {
            Utils.setSelectedFire(apnonceCheckBox, true);
            apnonceField.setText(a);
            savedDevice.getGenerator().ifPresent(generatorField::setText);
        });
    }

    public void saveDeviceHandler() {
        boolean doReturn = Utils.isFieldEmpty(!identifierCheckBox.isSelected(), deviceModelChoiceBox.getValue(), deviceModelChoiceBox);
        doReturn |= Utils.isFieldEmpty(true, ecidField);
        doReturn |= Utils.isFieldEmpty(identifierCheckBox, identifierField);
        doReturn |= Utils.isFieldEmpty(!boardConfigField.isDisable(), boardConfigField);
        doReturn |= Utils.isFieldEmpty(apnonceCheckBox, apnonceField);
        if (doReturn) return;

        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Name Device");
        textInputDialog.setHeaderText("Name Device");
        textInputDialog.setContentText("Please enter a name for the device:");
        textInputDialog.showAndWait();
        String result = textInputDialog.getResult();

        if (!Utils.isEmptyOrNull(result)) {
            Prefs.SavedDeviceBuilder builder = new Prefs.SavedDeviceBuilder(result);

            builder.setEcid(ecidField.getText()).setSavePath(pathField.getText())
                    .setIdentifier(identifierField.isDisabled() ?
                            Devices.modelToIdentifier(deviceModelChoiceBox.getValue()) : identifierField.getText());
            if (!boardConfigField.isDisable()) {
                builder.setBoardConfig(boardConfigField.getText());
            }
            if (apnonceCheckBox.isSelected()) {
                builder.setApnonce(apnonceField.getText());
                if (!Utils.isEmptyOrNull(generatorField.getText())) {
                    builder.setGenerator(generatorField.getText());
                }
            }
            builder.save();
        }
    }

    public void deleteDeviceHandler() {
        Prefs.SavedDevice device = deviceList.getSelectionModel().getSelectedItem();
        if (device == null) return;

        ButtonType result = Utils.showConfirmAlert("Are you sure you would like to remove the device \"" + device + "\"?");
        if (ButtonType.OK.equals(result)) {
            device.delete();
        }
    }

    public void devicesListKeyEvent(KeyEvent event) {
        if (event.getCode().equals(KeyCode.DELETE) || event.getCode().equals(KeyCode.BACK_SPACE)) {
            deleteDeviceHandler();
        }
    }

    @SuppressWarnings("unused")
    public void devicesListFocusLoss(ObservableValue<?> a, boolean b, boolean focused) {
        if (!focused) deviceList.getSelectionModel().clearSelection();
    }

    public void showOlderDevicesHandler(Event evt) {
        Prefs.setShowOldDevices(((CheckMenuItem) evt.getSource()).isSelected());
        Devices.updateLists();
    }

    @SuppressWarnings("unused")
    public void darkModeHandler(ObservableValue<?> a, RadioMenuItem old, RadioMenuItem darkMode) {
        switch ((Prefs.DarkMode) darkMode.getUserData()) {
            case ENABLED -> updateDarkMode(true);
            case DISABLED -> updateDarkMode(false);
            case SYNC_WITH_OS -> System.err.println("Sync with OS not implemented yet"); // register listener for os here
        }
        Prefs.setDarkMode((Prefs.DarkMode) darkMode.getUserData());
    }

    @SuppressWarnings("ConstantConditions")
    private void updateDarkMode(boolean enable) {
        if (enable) {
            // eventually replace with https://github.com/openjdk/jfx/pull/511 once released
            com.sun.javafx.css.StyleManager.getInstance().addUserAgentStylesheet(Controller.class.getResource("modena_dark.css").toExternalForm());
            savedDevicesVBox.setStyle("-fx-background-color: -fx-background");
        } else {
            com.sun.javafx.css.StyleManager.getInstance().removeUserAgentStylesheet(Controller.class.getResource("modena_dark.css").toExternalForm());
            savedDevicesVBox.setStyle("-fx-background-color: white");
        }
    }

    public void disableAnalyticsHandler(Event evt) {
        Prefs.setDisableAnalytics(((CheckMenuItem) evt.getSource()).isSelected());
    }

    public void checkBlobs() {
        Utils.openURL("https://verify.shsh.host");
        Analytics.checkBlobs();
    }

    public void helpLabelHandler(Event evt) {
        if (Main.SHOW_BREAKPOINT) {
            return; // remember to put a breakpoint here
        }

        String labelID = ((Label) evt.getSource()).getId();

        ButtonType openURL = new ButtonType("Open URL");
        ButtonType customOK = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", openURL, customOK);
        ((Button) alert.getDialogPane().lookupButton(customOK)).setDefaultButton(true);
        String url = switch (labelID) {
            case "ipswURLHelp":
                alert.setContentText("Get the IPSW download URL for the iOS version from theiphonewiki.com/wiki/Beta_Firmware and paste it here.\n\nIf you already have the IPSW downloaded, you can also supply a 'file:' URL.");
                alert.setTitle("Help: IPSW URL");
                alert.setHeaderText("Help");
                yield "https://www.theiphonewiki.com/wiki/Beta_Firmware";
            case "locationHelp":
                alert.setContentText("Click \"Open URL\" to see how to automatically upload blobs you save to the cloud.");
                alert.setTitle("Tip: Saving Blobs to the Cloud");
                alert.setHeaderText("Tip");
                yield "https://github.com/airsquared/blobsaver/wiki/Automatically-saving-blobs-to-the-cloud";
            default:
                throw new IllegalStateException("Unexpected value for labelID: " + labelID);
        };
        alert.showAndWait();
        if (openURL.equals(alert.getResult())) {
            Utils.openURL(url);
        }
    }

    public void aboutMenuHandler(Event ignored) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", new ButtonType("Libraries Used"),
                new ButtonType("License"), new ButtonType("Github Repo"), ButtonType.CLOSE);
        alert.initOwner(Main.primaryStage);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.getDialogPane().getScene().getWindow().focusedProperty().addListener((a, b, focused) -> {
            if (!focused) alert.close();
        });
        //Activate default behavior for close button
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CLOSE)).setDefaultButton(true);

        alert.setTitle("About");
        alert.setHeaderText("blobsaver " + Main.appVersion);
        alert.setContentText("blobsaver Copyright (c) 2021 airsquared\n\n" +
                "This program is licensed under GNU GPL v3.0-only");
        Utils.resizeAlertButtons(alert);

        alert.showAndWait();
        switch (alert.getResult().getText()) {
            case "Github Repo" -> Utils.openURL("https://github.com/airsquared/blobsaver");
            case "View License" -> Utils.openURL(Utils.getLicenseFile().toURI().toString());
            case "Libraries Used" -> Utils.openURL(Utils.getLibrariesUsedFile().toURI().toString());
        }
    }

    private void useMacOSMenuBar() {
        ((VBox) menuBar.getParent()).getChildren().remove(menuBar);
        menuBar.getMenus().get(0).getItems().remove(5, 8); // clear old options menu

        MenuToolkit tk = MenuToolkit.toolkit();

        Menu applicationMenu = tk.createDefaultApplicationMenu("blobsaver", null);
        menuBar.getMenus().add(0, applicationMenu);
        applicationMenu.getItems().get(0).setOnAction(this::aboutMenuHandler);
        applicationMenu.getItems().add(1, new SeparatorMenuItem());
        applicationMenu.getItems().add(2, checkForUpdatesMenu);
        applicationMenu.getItems().add(3, new SeparatorMenuItem());
        applicationMenu.getItems().add(4, clearAllDataMenu);

        menuBar.getMenus().get(3).getItems().addAll(new SeparatorMenuItem(), tk.createMinimizeMenuItem(), tk.createCycleWindowsItem(),
                new SeparatorMenuItem(), tk.createBringAllToFrontItem());
        tk.autoAddWindowMenuItems(menuBar.getMenus().get(3));

        menuBar.getMenus().get(4).getItems().remove(10); // remove about

        tk.setApplicationMenu(applicationMenu);
        tk.setGlobalMenuBar(menuBar);
    }

    private void updateBackgroundSettings() {
        boolean disableBackgroundSettings = !Prefs.anyBackgroundDevices();
        startBackgroundButton.setDisable(disableBackgroundSettings);
        forceCheckForBlobs.setDisable(disableBackgroundSettings || !Background.isBackgroundEnabled());
        chooseTimeToRunButton.setDisable(disableBackgroundSettings);

        if (Background.isBackgroundEnabled()) {
            startBackgroundButton.setText("Stop background");
        } else {
            startBackgroundButton.setText("Start background");
        }
    }

    public void backgroundSettingsHandler() {
        updateBackgroundSettings();
        if (backgroundSettingsButton.isSelected()) {
            savedDevicesLabel.setText("Select Devices");
            backgroundSettingsButton.setText("Back");
            savedDevicesVBox.setEffect(Utils.borderGlow);

            deviceList.setCellFactory(CheckBoxListCell.forListView(device -> {
                final SimpleBooleanProperty property = new SimpleBooleanProperty(device.isBackground());
                property.addListener((obs, old, newValue) -> addBackgroundHandler(device, property));
                return property;
            }));
        } else {
            savedDevicesVBox.setEffect(null);
            savedDevicesLabel.setText("Saved Devices");
            backgroundSettingsButton.setText("Auto-Save Settings");
            deviceList.setCellFactory(null);
        }
    }

    public void addBackgroundHandler(Prefs.SavedDevice device, SimpleBooleanProperty enable) {
        if (device.isBackground()) {
            device.setBackground(enable.get());
            updateBackgroundSettings();
        } else if (enable.get()) {
            loadSavedDevice(device);
            Utils.setSelectedFire(betaCheckBox, false);
            Utils.setSelectedFire(allSignedVersionsCheckBox, true);
            TSS tss = createTSS("Testing device...");
            EventHandler<WorkerStateEvent> oldSucceeded = tss.getOnSucceeded();
            tss.setOnSucceeded(event -> {
                device.setBackground(true);
                updateBackgroundSettings();
                System.out.println("added device " + device + " to list");

                oldSucceeded.handle(event);
            });
            EventHandler<WorkerStateEvent> oldFailed = tss.getOnFailed();
            tss.setOnFailed(event -> {
                enable.set(false);
                oldFailed.handle(event);
            });
            Utils.executeInThreadPool(tss);
        }
    }

    public void chooseTimeToRunHandler() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Frequency to check for new blobs");
        alert.setHeaderText("Frequency to check");
        TextField textField = new TextField(Long.toString(Prefs.getBackgroundInterval()));
        textField.setTextFormatter(Utils.intOnlyFormatter());

        ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList("Minutes", "Hours", "Days", "Weeks"));
        choiceBox.setValue(Prefs.getBackgroundTimeUnit().toString());

        alert.getDialogPane().setContent(new HBox(textField, choiceBox));
        alert.showAndWait();
        if ((alert.getResult() != null) && !ButtonType.CANCEL.equals(alert.getResult()) && !Utils.isEmptyOrNull(textField.getText()) && !Utils.isEmptyOrNull(choiceBox.getValue())) {
            Prefs.setBackgroundInterval(Long.parseLong(textField.getText()), TimeUnit.valueOf(choiceBox.getValue().toUpperCase(Locale.ENGLISH)));
        } else {
            backgroundSettingsButton.fire(); //goes back to main menu
            return;
        }
        if (Background.isBackgroundEnabled()) {
            Utils.showInfoAlert("You will need to restart the background for changes to take effect.", new ButtonType("Stop Background"));
            startBackgroundButton.fire();
        }
    }

    public void startBackgroundHandler() {
        if (Background.isBackgroundEnabled()) { //stops background if already in background
            Background.stopBackground();
            updateBackgroundSettings();
        } else {
            Background.startBackground();
            if (Background.isBackgroundEnabled()) {
                updateBackgroundSettings();
                Utils.showInfoAlert("A background saving task has been scheduled. You can now quit this application, and blobs will continue to be saved at the interval you set.");
            } else {
                Utils.showUnreportableError("Background saving is not enabled. Try again with the debug log open to check for any errors.");
            }
        }
    }

    public void forceCheckForBlobsHandler() {
        Background.runOnce();
    }

    public void resetAppHandler() {
        ButtonType result = Utils.showConfirmAlert("Are you sure you would like to clear all blobsaver data?");
        if (ButtonType.OK.equals(result)) {
            Prefs.resetPrefs();
            Utils.showInfoAlert("The application data has been removed. \nThe application will now exit.");
            Main.exit();
        }
    }

    public void exportSavedDevices() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName("blobsaver.xml");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML File", "*.xml"));
        File file = chooser.showSaveDialog(Main.primaryStage);
        if (file != null) {
            Prefs.export(file);
        }
    }

    public void importFromXML() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML File", "*.xml"));
        File file = chooser.showOpenDialog(Main.primaryStage);
        if (file != null) {
            try {
                Prefs.importXML(file);
            } catch (Exception e) {
                e.printStackTrace();
                Utils.showUnreportableError("There was an error importing the preferences. " +
                        "Ensure that the right file is selected and the file isn't damaged.");
            }
        }
    }

    public void importFromOldVersion() {
        ButtonType result = Utils.showConfirmAlert("Continuing will import all presets into saved devices from blobsaver version 2.5.5 and older.");
        if (ButtonType.OK.equals(result)) {
            Prefs.importOldVersion();
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
            String ecid = Long.toHexString(LibimobiledeviceUtil.getECID()).toUpperCase();
            ecidField.setText(Utils.isNumeric(ecid) ? "0x" + ecid : ecid);
            // read device model
            String deviceIdentifier = LibimobiledeviceUtil.getDeviceModelIdentifier();
            try {
                deviceTypeChoiceBox.setValue(Devices.getDeviceType(deviceIdentifier));
                deviceModelChoiceBox.setValue(Devices.identifierToModel(deviceIdentifier));
            } catch (IllegalArgumentException e) {
                Utils.setSelectedFire(identifierCheckBox, true);
                identifierField.setText(deviceIdentifier);
            }
            // read board config
            if (!boardConfigField.isDisabled()) {
                boardConfigField.setText(LibimobiledeviceUtil.getBoardConfig());
            }
        } catch (LibimobiledeviceUtil.LibimobiledeviceException e) {
            System.err.println(e.getMessage()); // don't need full stack trace
            e.showErrorAlert();
        } catch (Throwable e) {
            e.printStackTrace();
            if (e instanceof Error && Platform.isLinux()) {
                Utils.showUnreportableError("Unable to load native libraries. Ensure you have libimobiledevice installed and as libimobiledevice.so. If it is installed under a different name, try creating a symlink.");
                System.exit(-1);
            } else {
                Utils.showReportableError("An unknown error occurred.", Utils.exceptionToString(e));
            }
        }
        Analytics.readInfo();
    }

    public void readApnonce() {
        Alert alert1 = new Alert(Alert.AlertType.CONFIRMATION);
        alert1.setHeaderText("Read APNonce from connected device");
        alert1.setContentText("blobsaver can read both the APNonce and generator from a connected device.\n\n" +
                "Your device will enter recovery mode while retrieving the APNonce and will automatically reboot to normal mode when complete.");
        if (!ButtonType.OK.equals(alert1.showAndWait().orElse(null))) {
            return;
        }
        final Alert alert2 = new Alert(Alert.AlertType.INFORMATION, "[This should not be visible]", ButtonType.FINISH);
        alert2.setHeaderText("Read APNonce from connected device");
        Utils.forEachButton(alert2, button -> button.setDisable(true));

        var task = new LibimobiledeviceUtil.GetApnonceTask(false);
        task.setOnSucceeded(event -> {
            apnonceField.setText(task.getApnonceResult());
            generatorField.setText(task.getGeneratorResult());
            Utils.forEachButton(alert2, button -> button.setDisable(false));
        });
        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            if (task.getException() instanceof LibimobiledeviceUtil.LibimobiledeviceException e) {
                e.showErrorAlert();
            } else {
                Utils.showReportableError("An unknown error occurred.", Utils.exceptionToString(task.getException()));
            }
            Utils.forEachButton(alert2, button -> button.setDisable(false));
            alert2.close();
        });

        alert2.contentTextProperty().bind(task.messageProperty());
        Utils.executeInThreadPool(task);
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
            LibimobiledeviceUtil.exitRecovery(irecvClient.getValue());
        } catch (LibimobiledeviceUtil.LibimobiledeviceException e) {
            e.printStackTrace();
            e.showErrorAlert();
        }
        Analytics.exitRecovery();
    }

    public void donate() { Utils.openURL("https://www.paypal.me/airsqrd"); }

    public void goButtonHandler() {
        if (checkInputs()) return;

        Utils.executeInThreadPool(createTSS("Saving blobs..."));
    }

    /**
     * @return true if inputs are wrong, otherwise true
     */
    private boolean checkInputs() {
        boolean incorrect = Utils.isFieldEmpty(!identifierCheckBox.isSelected(), deviceTypeChoiceBox.getValue(), deviceTypeChoiceBox);
        incorrect |= Utils.isFieldEmpty(!identifierCheckBox.isSelected(), deviceModelChoiceBox.getValue(), deviceModelChoiceBox);
        incorrect |= Utils.isFieldEmpty(true, ecidField);
        incorrect |= Utils.isFieldEmpty(identifierCheckBox, identifierField);
        incorrect |= Utils.isFieldEmpty(!boardConfigField.isDisable(), boardConfigField);
        incorrect |= Utils.isFieldEmpty(apnonceCheckBox, apnonceField);
        incorrect |= Utils.isFieldEmpty(true, pathField);
        incorrect |= Utils.isFieldEmpty(!allSignedVersionsCheckBox.isSelected() && !betaCheckBox.isSelected(), versionField);
        incorrect |= Utils.isFieldEmpty(betaCheckBox, ipswField);
        return incorrect;
    }

    private TSS createTSS(String runningAlertTitle) {
        TSS.Builder builder = new TSS.Builder()
                .setDevice(identifierCheckBox.isSelected() ?
                        identifierField.getText() : Devices.modelToIdentifier(deviceModelChoiceBox.getValue()))
                .setEcid(ecidField.getText()).setSavePath(pathField.getText())
                .saveToTSSSaver(saveToTSSSaverCheckBox.isSelected())
                .saveToSHSHHost(saveToSHSHHostCheckBox.isSelected());
        if (!boardConfigField.isDisabled()) {
            builder.setBoardConfig(boardConfigField.getText());
        }
        if (!versionField.isDisabled()) {
            builder.setManualVersion(versionField.getText());
        } else if (!ipswField.isDisabled()) {
            builder.setManualIpswURL(ipswField.getText());
        }
        if (!apnonceField.isDisabled()) {
            builder.setApnonce(apnonceField.getText());
            if (!Utils.isEmptyOrNull(generatorField.getText())) {
                builder.setGenerator(generatorField.getText());
            }
        }

        TSS tss = builder.build();

        final Alert runningAlert = new Alert(Alert.AlertType.INFORMATION);
        runningAlert.setTitle(runningAlertTitle);
        runningAlert.setHeaderText("Saving blobs...              ");
        runningAlert.getDialogPane().setContent(new ProgressBar());
        Utils.forEachButton(runningAlert, button -> button.setDisable(true));
        runningAlert.getDialogPane().getScene().getWindow().setOnCloseRequest(Event::consume);

        tss.setOnScheduled(event -> runningAlert.show());
        tss.setOnSucceeded(event -> {
            runningAlert.close();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, tss.getValue());
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

    private void parseException(Throwable t) {
        if (t instanceof TSS.TSSException e) {
            String message = e.getMessage();
            if (message.contains("not a valid identifier")) {
                identifierField.setEffect(Utils.errorBorder);
            } else if (message.contains("IPSW URL is not valid")) {
                ipswField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid ECID")) {
                ecidField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid APNonce")) {
                apnonceField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid path")) {
                pathField.setEffect(Utils.errorBorder);
            } else if (message.contains("not being signed") && betaCheckBox.isSelected()) {
                ipswField.setEffect(Utils.errorBorder);
            } else if (message.contains("not being signed") && !allSignedVersionsCheckBox.isSelected()) {
                versionField.setEffect(Utils.errorBorder);
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
