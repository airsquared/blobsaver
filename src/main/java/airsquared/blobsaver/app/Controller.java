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
import com.sun.jna.Platform;
import com.sun.jna.ptr.PointerByReference;
import de.jangassen.MenuToolkit;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;

public class Controller {


    @FXML private MenuBar menuBar;
    @FXML private MenuItem checkForUpdatesMenuItem, clearAllDataMenuItem, checkForValidBlobsMenuItem, debugLogMenuItem;

    @FXML private ChoiceBox<String> deviceTypeChoiceBox, deviceModelChoiceBox;

    @FXML private TextField ecidField, boardConfigField, apnonceField, versionField, identifierField,
            pathField, ipswField;

    @FXML private CheckBox apnonceCheckBox, allSignedVersionsCheckBox, identifierCheckBox, betaCheckBox;

    @FXML private Label versionLabel;

    @FXML private Button startBackgroundButton, chooseTimeToRunButton, forceCheckForBlobs;
    @FXML private ToggleButton backgroundSettingsButton, saveDeviceButton;

    @FXML private VBox savedDevicesVBox;

    private List<Button> savedDeviceButtons;

    public static final String initialPath = System.getProperty("user.home") + File.separator + "Blobs";

    public void initialize() {
        savedDeviceButtons = Utils.subList(savedDevicesVBox.getChildren(), 10);
        for (int i = 1; i <= 10; i++) { // sets the names for the device buttons
            Button btn = savedDeviceButtons.get(i - 1); // needed so it's 'effectively final'
            Prefs.savedDevice(i).ifPresent(savedDevice -> btn.setText("Load " + savedDevice.getName()));
        }
        if (Platform.isMac()) {
            useMacOSMenuBar();
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
            boardConfigField.setText(Devices.getBoardConfig(identifier));
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
        savedDevice.getBoardConfig().ifPresent(b -> boardConfigField.setText(b));
        savedDevice.getApnonce().ifPresent(a -> {
            Utils.setSelectedFire(apnonceCheckBox, true);
            apnonceField.setText(a);
        });
    }

    public void savedDeviceButtonHandler(Event evt) {
        Button btn = (Button) evt.getSource();
        int presNum = savedDeviceButtons.indexOf(btn) + 1;
        if (saveDeviceButton.isSelected()) {
            saveDevice(presNum);
            saveDeviceButton.fire();
        } else if (backgroundSettingsButton.isSelected()) {
            Optional<Prefs.SavedDevice> savedDevice = Prefs.savedDevice(presNum);
            if (savedDevice.isEmpty()) {
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
                TSS tss = createTSS("Testing device...");
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
        doReturn |= Utils.isFieldEmpty(!boardConfigField.isDisable(), boardConfigField);
        doReturn |= Utils.isFieldEmpty(apnonceCheckBox, apnonceField);
        if (doReturn) return;

        TextInputDialog textInputDialog = new TextInputDialog(Prefs.getSavedDeviceName(num));
        textInputDialog.setTitle("Name Device " + num);
        textInputDialog.setHeaderText("Name Device");
        textInputDialog.setContentText("Please enter a name for the device:");
        textInputDialog.showAndWait();
        String result = textInputDialog.getResult();

        if (!Utils.isEmptyOrNull(result)) {
            Prefs.SavedDeviceBuilder builder = new Prefs.SavedDeviceBuilder(num);

            savedDeviceButtons.get(num - 1).setText("Save in " + textInputDialog.getResult());

            builder.setName(textInputDialog.getResult()).setEcid(ecidField.getText()).setSavePath(pathField.getText());
            builder.setIdentifier(identifierField.isDisabled() ?
                    Devices.modelToIdentifier(deviceModelChoiceBox.getValue()) : identifierField.getText());
            if (!boardConfigField.isDisable()) {
                builder.setBoardConfig(boardConfigField.getText());
            }
            if (apnonceCheckBox.isSelected()) {
                builder.setApnonce(apnonceField.getText());
            }
            builder.save();
        }

    }

    public void saveDeviceHandler() {
        if (saveDeviceButton.isSelected()) {
            saveDeviceButton.setText("Back");
            savedDevicesVBox.setEffect(Utils.borderGlow);
            savedDeviceButtons.forEach(btn -> btn.setText("Save in " + btn.getText().substring("Load ".length())));
        } else {
            savedDevicesVBox.setEffect(null);
            saveDeviceButton.setText("Save");
            savedDeviceButtons.forEach(btn -> btn.setText("Load " + btn.getText().substring("Save in ".length())));
        }
    }

    public void checkBlobs() { Utils.openURL("https://verify.shsh.host"); }

    public void helpLabelHandler(Event evt) {
        if (Main.SHOW_BREAKPOINT) {
            return; // remember to put a breakpoint here
        }

        String labelID = ((Label) evt.getSource()).getId();

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
        alert.setContentText("blobsaver Copyright (c) 2019  airsquared\n\n" +
                "This program is licensed under GNU GPL v3.0-only");
        Utils.resizeAlertButtons(alert);

        alert.showAndWait();
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
        ((VBox) menuBar.getParent()).getChildren().remove(menuBar);
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
        boolean disableBackgroundSettings = !Prefs.anyBackgroundDevices();
        startBackgroundButton.setDisable(disableBackgroundSettings);
        forceCheckForBlobs.setDisable(disableBackgroundSettings);
        chooseTimeToRunButton.setDisable(disableBackgroundSettings);

        if (backgroundSettingsButton.isSelected()) {
            backgroundSettingsButton.setText("Back");
            savedDevicesVBox.setEffect(Utils.borderGlow);
            savedDeviceButtons.forEach(btn -> {
                if (Prefs.isDeviceInBackground(savedDeviceButtons.indexOf(btn) + 1)) {
                    btn.setText("Cancel " + btn.getText().substring("Load ".length()));
                } else {
                    btn.setText("Use " + btn.getText().substring("Load ".length()));
                }
            });
            if (Background.isBackgroundEnabled()) {
                startBackgroundButton.setText("Stop Background");
            }
        } else {
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
            Alert restartBackgroundAlert = new Alert(Alert.AlertType.INFORMATION,
                    "You will need to restart the background for changes to take effect.", new ButtonType("Stop Background"));
            restartBackgroundAlert.showAndWait();
            startBackgroundButton.fire();
        }
    }

    public void startBackgroundHandler() {
        if (Background.isBackgroundEnabled()) { //stops background if already in background
            Background.stopBackground();
            startBackgroundButton.setText("Start background");
        } else {
            Background.startBackground();
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Background saving has been enabled. You can now close this application and it will continue saving blobs at the interval you set, and won't use any resources when it is not running.", ButtonType.OK);
            alert.showAndWait();
            startBackgroundButton.setText("Stop background");
        }
    }

    public void forceCheckForBlobsHandler() {
        Background.runOnce();
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
            Main.exit();
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
                deviceModelChoiceBox.setValue(Devices.identifierToModel(deviceIdentifier));
            } catch (IllegalArgumentException e) {
                Utils.showReportableError("Unknown model: " + deviceIdentifier);
                return;
            }
            // read board config
            if (!boardConfigField.isDisabled()) {
                boardConfigField.setText(LibimobiledeviceUtil.getBoardConfig(true));
            }
        } catch (LibimobiledeviceUtil.LibimobiledeviceException e) {
            System.err.println(e.getMessage()); // don't need full stack trace
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
        if (result.isEmpty() || !result.get().equals(ButtonType.OK)) return;
        final Alert alert2 = new Alert(Alert.AlertType.INFORMATION, "Entering recovery mode...\n\n" +
                "This can take up to 60 seconds", ButtonType.FINISH);
        alert2.setHeaderText("Reading apnonce from connected device...");
        Utils.forEachButton(alert2, button -> button.setDisable(true));

        Task<String> getApnonceTask = LibimobiledeviceUtil.createGetApnonceTask();
        getApnonceTask.setOnSucceeded(event -> {
            apnonceField.setText(getApnonceTask.getValue());
            Utils.forEachButton(alert2, button -> button.setDisable(false));
        });
        getApnonceTask.setOnFailed(event -> {
            getApnonceTask.getException().printStackTrace();
            Utils.forEachButton(alert2, button -> button.setDisable(false));
            alert2.close();
        });

        alert2.contentTextProperty().bind(getApnonceTask.messageProperty());
        Utils.executeInThreadPool(getApnonceTask);
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
                .setEcid(ecidField.getText()).setSavePath(pathField.getText());
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
        if (t instanceof TSS.TSSException) {
            TSS.TSSException e = (TSS.TSSException) t;
            String message = t.getMessage();
            if (message.contains("not a valid identifier")) {
                identifierField.setEffect(Utils.errorBorder);
            } else if (message.contains("IPSW URL is not valid")) {
                ipswField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid ECID")) {
                ecidField.setEffect(Utils.errorBorder);
            } else if (message.contains("not a valid apnonce")) {
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
