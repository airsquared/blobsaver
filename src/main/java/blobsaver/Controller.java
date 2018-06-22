package blobsaver;

import com.sun.javafx.PlatformUtil;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.prefs.Preferences;

public class Controller {


    @FXML private ChoiceBox deviceTypeChoiceBox;
    @FXML private ChoiceBox deviceModelChoiceBox;

    @FXML private TextField ecidField;
    @FXML private TextField boardConfigField;
    @FXML private TextField apnonceField;
    @FXML private TextField versionField;
    @FXML private TextField identifierField;
    @FXML private TextField pathField;
    @FXML private TextField buildManifestField;
    @FXML private TextField buildIDField;

    @FXML private CheckBox apnonceCheckBox;
    @FXML private CheckBox versionCheckBox;
    @FXML private CheckBox identifierCheckBox;
    @FXML private CheckBox betaCheckBox;

    @FXML private Label versionLabel;

    @FXML private ToggleButton savePresetButton;
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

    @FXML private ScrollPane scrollPane;

    @FXML private Button goButton;
    @FXML private Button plistPickerButton;

    private boolean boardConfig = false;
    private boolean editingPresets = false;

    private DropShadow errorBorder = new DropShadow();

    private ButtonType redditPM = new ButtonType("PM on Reddit");
    private ButtonType githubIssue = new ButtonType("Create Issue on Github");

    private URI githubIssueURI;
    private URI redditPMURI;

    static void setPresetButtonNames() {
        Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
        for (int i = 1; i < 11; i++) {
            if (!appPrefs.get("Name Preset" + i, "").equals("")) {
                Button btn = (Button) Main.primaryStage.getScene().lookup("#preset" + i);
                btn.setText("Load " + appPrefs.get("Name Preset" + i, ""));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @FXML
    public void initialize() {
        final ObservableList iPhones = FXCollections.observableArrayList("iPhone 3G[S]", "iPhone 4 (GSM)",
                "iPhone 4 (GSM 2012)", "iPhone 4 (CDMA)", "iPhone 4[S]", "iPhone 5 (GSM)", "iPhone 5 (Global)",
                "iPhone 5c (GSM)", "iPhone 5c (Global)", "iPhone 5s (GSM)", "iPhone 5s (Global)",
                "iPhone 6+", "iPhone 6", "iPhone 6s", "iPhone 6s+", "iPhone SE", "iPhone 7 (Global)(iPhone9,1)",
                "iPhone 7+ (Global)(iPhone9,2)", "iPhone 7 (GSM)(iPhone9,3)", "iPhone 7+ (GSM)(iPhone9,4)",
                "iPhone 8 (iPhone10,1)", "iPhone 8+ (iPhone10,2)", "iPhone X (iPhone10,3)", "iPhone 8 (iPhone10,4)",
                "iPhone 8+ (iPhone10,5)", "iPhone X (iPhone10,6)");
        final ObservableList iPods = FXCollections.observableArrayList("iPod Touch 3", "iPod Touch 4", "iPod Touch 5", "iPod Touch 6");
        final ObservableList iPads = FXCollections.observableArrayList("iPad 1", "iPad 2 (WiFi)", "iPad 2 (GSM)",
                "iPad 2 (CDMA)", "iPad 2 (Mid 2012)", "iPad Mini (Wifi)", "iPad Mini (GSM)", "iPad Mini (Global)",
                "iPad 3 (WiFi)", "iPad 3 (CDMA)", "iPad 3 (GSM)", "iPad 4 (WiFi)", "iPad 4 (GSM)", "iPad 4 (Global)",
                "iPad Air (Wifi)", "iPad Air (Cellular)", "iPad Air (China)", "iPad Mini 2 (WiFi)", "iPad Mini 2 (Cellular)",
                "iPad Mini 2 (China)", "iPad Mini 3 (WiFi)", "iPad Mini 3 (Cellular)", "iPad Mini 3 (China)",
                "iPad Mini 4 (Wifi)", "iPad Mini 4 (Cellular)", "iPad Air 2 (WiFi)", "iPad Air 2 (Cellular)",
                "iPad Pro 9.7 (Wifi)", "iPad Pro 9.7 (Cellular)", "iPad Pro 12.9 (WiFi)", "iPad Pro 12.9 (Cellular)",
                "iPad 5 (Wifi)", "iPad 5 (Cellular)", "iPad Pro 2 12.9 (WiFi)(iPad7,1)", "iPad Pro 2 12.9 (Cellular)(iPad7,2)",
                "iPad Pro 10.5 (WiFi)(iPad7,3)", "iPad 10.5 (Cellular)(iPad7,4)", "iPad 6 (WiFi)(iPad 7,5)", "iPad 6 (Cellular)(iPad7,6)");
        final ObservableList AppleTVs = FXCollections.observableArrayList("Apple TV 2G", "Apple TV 3", "Apple TV 3 (2013)", "Apple TV 4 (2015)", "Apple TV 4K");
        deviceTypeChoiceBox.setItems(FXCollections.observableArrayList("iPhone", "iPod", "iPad", "AppleTV"));

        deviceTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            deviceTypeChoiceBox.setEffect(null);
            if (newValue == null) {
                versionLabel.setText("Version");
                return;
            }
            final String v = (String) newValue;
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
            deviceModelChoiceBox.setEffect(null);
            if (newValue == null) {
                boardConfigField.setEffect(null);
                boardConfig = false;
                boardConfigField.setText("");
                boardConfigField.setDisable(true);
                return;
            }
            final String v = (String) newValue;
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
            identifierField.setEffect(null);
            if (newValue.equals("iPhone8,1") || newValue.equals("iPhone8,2") || newValue.equals("iPhone8,4")) {
                final int depth = 20;
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
        ecidField.textProperty().addListener((observable, oldValue, newValue) -> ecidField.setEffect(null));
        versionField.textProperty().addListener((observable, oldValue, newValue) -> versionField.setEffect(null));
        boardConfigField.textProperty().addListener((observable, oldValue, newValue) -> boardConfigField.setEffect(null));
        apnonceField.textProperty().addListener((observable, oldValue, newValue) -> apnonceField.setEffect(null));
        pathField.textProperty().addListener((observable, oldValue, newValue) -> pathField.setEffect(null));

        deviceTypeChoiceBox.setValue("iPhone");

        goButton.setDefaultButton(true);

        errorBorder.setOffsetY(0f);
        errorBorder.setOffsetX(0f);
        errorBorder.setColor(Color.RED);
        errorBorder.setWidth(20);
        errorBorder.setHeight(20);

        presetButtons = new ArrayList<>(Arrays.asList(preset1Button, preset2Button, preset3Button, preset4Button, preset5Button, preset6Button, preset7Button, preset8Button, preset9Button, preset10Button));
        presetButtons.forEach((Button btn) -> btn.setOnAction(this::presetButtonHandler));

        try {
            githubIssueURI = new URI("https://github.com/airsquared/blobsaver/issues/new");
            redditPMURI = new URI("https://www.reddit.com//message/compose?to=01110101_00101111&subject=Blobsaver+Bug+Report");
        } catch (URISyntaxException ignored) {
        }

        final String url = getClass().getResource("Controller.class").toString();
        String path = url.substring(0, url.length() - "blobsaver/Controller.class".length());
        if (path.startsWith("jar:")) {
            path = path.substring("jar:".length(), path.length() - 2);
        }
        if (path.startsWith("file:")) {
            path = path.substring("file:".length(), path.length());
        }
        path = new File(path).getParentFile().toString();
        if (PlatformUtil.isWindows() && path.endsWith("\\")) {
            path = path + "Blobs";
        } else if (PlatformUtil.isWindows()) {
            path = path + "\\Blobs";
        } else if (path.endsWith("/")) {
            path = path + "Blobs";
        } else {
            path = path + "/Blobs";
        }
        pathField.setText(path);

        checkForUpdates();
    }

    public void checkForUpdates() {
        Service<Void> service = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        StringBuilder response = new StringBuilder();
                        try {
                            URLConnection urlConnection = new URL("https://api.github.com/repos/airsquared/blobsaver/releases/latest").openConnection();
                            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String inputLine;
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();
                        } catch (FileNotFoundException ignored) {
                            return null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String newVersion;
                        String changelog;
                        try {
                            newVersion = new JSONObject(response.toString()).getString("tag_name");
                            changelog = new JSONObject(response.toString()).getString("body");
                            changelog = changelog.substring(changelog.indexOf("Changelog"));
                        } catch (JSONException e) {
                            newVersion = Main.appVersion;
                            changelog = "";
                        }
                        Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
                        if (!newVersion.equals(Main.appVersion) && !appPrefs.get("Ignore Version", "").equals(newVersion)) {
                            final CountDownLatch latch = new CountDownLatch(1);
                            final String finalNewVersion = newVersion;
                            final String finalChangelog = changelog;
                            Platform.runLater(() -> {
                                try {
                                    ButtonType downloadNow = new ButtonType("Download now");
                                    ButtonType ignore = new ButtonType("Ignore this update");
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "You have version " + Main.appVersion + "\n\n" + finalChangelog, downloadNow, ignore, ButtonType.CANCEL);
                                    alert.setHeaderText("New Update Available: " + finalNewVersion);
                                    alert.setTitle("New Update Available");
                                    alert.initModality(Modality.NONE);
                                    Button dlButton = (Button) alert.getDialogPane().lookupButton(downloadNow);
                                    dlButton.setDefaultButton(true);
                                    alert.showAndWait();
                                    if (alert.getResult().equals(downloadNow) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                                        try {
                                            Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver/releases/latest"));
                                        } catch (IOException | URISyntaxException ee) {
                                            ee.printStackTrace();
                                        }
                                    } else if (alert.getResult().equals(ignore)) {
                                        appPrefs.put("Ignore Version", finalNewVersion);
                                    }
                                } finally {
                                    latch.countDown();
                                }
                            });
                            latch.await();
                        }
                        return null;
                    }
                };
            }
        };
        service.start();
    }

    public void newGithubIssue() {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(githubIssueURI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendRedditPM() {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(redditPMURI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void reportError(Alert alert) {
        if (alert.getResult().equals(githubIssue)) {
            newGithubIssue();
        } else if (alert.getResult().equals(redditPM)) {
            sendRedditPM();
        }
    }

    private void reportError(Alert alert, String toCopy) {
        StringSelection stringSelection = new StringSelection(toCopy);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        reportError(alert);
    }

    private void newReportableError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nPlease create a new issue on Github or PM me on Reddit.", githubIssue, redditPM, ButtonType.CANCEL);
        alert.showAndWait();
        reportError(alert);
    }

    private void newReportableError(String msg, String toCopy) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg + "\n\nPlease create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.", githubIssue, redditPM, ButtonType.CANCEL);
        alert.showAndWait();
        reportError(alert, toCopy);
    }

    private void newUnreportableError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private void run(String device) {
        File file;
        try {
            InputStream input;
            if (PlatformUtil.isWindows()) {
                input = getClass().getResourceAsStream("tsschecker.exe");
                file = File.createTempFile("tsschecker", ".tmp.exe");
            } else {
                input = getClass().getResourceAsStream("tsschecker");
                file = File.createTempFile("tsschecker", ".tmp");
            }
            OutputStream out = new FileOutputStream(file);
            int read;
            byte[] bytes = new byte[1024];

            while ((read = input.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        file.deleteOnExit();

        if (!file.setExecutable(true, false)) {
            newReportableError("There was an error setting tsschecker as executable.");
            return;
        }

        File locationToSaveBlobs = new File(pathField.getText());
        locationToSaveBlobs.mkdirs();
        ArrayList<String> args;
        args = new ArrayList<>(Arrays.asList(file.getPath(), "-d", device, "-s", "-e", ecidField.getText(), "--save-path", pathField.getText()));
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
        } else if (betaCheckBox.isSelected()) {
            args.add("-i");
            args.add(versionField.getText());
            args.add("--beta");
            args.add("--buildid");
            args.add(buildIDField.getText());
            args.add("-m");
            args.add(buildManifestField.getText());
        } else {
            args.add("-i");
            args.add(versionField.getText());
        }
        Process proc = null;
        try {
            proc = new ProcessBuilder(args).start();
        } catch (IOException e) {
            newReportableError("There was an error starting tsschecker.", e.toString());
            e.printStackTrace();
        }
        String tsscheckerLog;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            StringBuilder logBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line + "\n");
                logBuilder.append(line).append("\n");
            }
            tsscheckerLog = logBuilder.toString();
        } catch (IOException e) {
            newReportableError("There was an error getting the tsschecker result", e.toString());
            e.printStackTrace();
            return;
        }

        if (tsscheckerLog.contains("Saved shsh blobs")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Successfully saved blobs in\n" + pathField.getText(), ButtonType.OK);
            alert.showAndWait();
        } else if (tsscheckerLog.contains("[Error] [TSSC] manually specified ecid=" + ecidField.getText() + ", but parsing failed")) {
            newUnreportableError("\"" + ecidField.getText() + "\"" + " is not a valid ECID. Try getting it from iTunes");
            ecidField.setEffect(errorBorder);
        } else if (tsscheckerLog.contains("[Error] [TSSC] device " + device + " could not be found in devicelist")) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "tsschecker could not find device: \"" + device +
                    "\"\n\nPlease create a new Github issue or PM me on Reddit if you used the dropdown menu", githubIssue, redditPM, ButtonType.CANCEL);
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
                    "Saving blobs failed. Check the board configuration.\n\nIf this doesn't work, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                    githubIssue, redditPM, ButtonType.OK);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (tsscheckerLog.contains("[Error] ERROR: TSS request failed: Could not resolve host:")) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Saving blobs failed. Check your internet connection.\n\nIf your internet is working and you can connect to apple.com in your browser, please create a new issue on Github or PM me on Reddit. The log has been copied to your clipboard.",
                    githubIssue, redditPM, ButtonType.OK);
            alert.showAndWait();
            reportError(alert, tsscheckerLog);
        } else if (tsscheckerLog.contains("[Error] [Error] can't save shsh at " + pathField.getText())) {
            newUnreportableError("\'" + pathField.getText() + "\' is not a valid path");
            pathField.setEffect(errorBorder);
        } else if (tsscheckerLog.contains("iOS " + versionField.getText() + " for device " + device + " IS NOT being signed!")) {
            newUnreportableError("iOS/tvOS " + versionField.getText() + " is not being signed for device " + device);
            versionField.setEffect(errorBorder);
        } else if (tsscheckerLog.contains("[Error] [TSSC] failed to load manifest")) {
            newUnreportableError("\'" + buildManifestField.getText() + "\' is not a valid manifest");
        } else if (tsscheckerLog.contains("[Error]")) {
            newReportableError("Saving blobs failed.", tsscheckerLog);
        } else {
            newReportableError("Unknown result.", tsscheckerLog);
        }
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            newReportableError("The tsschecker process was interrupted.", e.toString());
        }


        if (!file.delete()) {
            newUnreportableError("\"There was an error deleting the temporary file.\"");
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
            buildManifestField.setDisable(false);
            int depth = 20;
            DropShadow borderGlow = new DropShadow();
            borderGlow.setOffsetY(0f);
            borderGlow.setOffsetX(0f);
            borderGlow.setColor(Color.DARKCYAN);
            borderGlow.setWidth(depth);
            borderGlow.setHeight(depth);
            buildManifestField.setEffect(borderGlow);
            plistPickerButton.setDisable(false);
            buildIDField.setDisable(false);
            buildIDField.setEffect(borderGlow);
            if (versionCheckBox.isSelected()) {
                versionCheckBox.fire();
            }
        } else {
            buildManifestField.setEffect(null);
            buildManifestField.setText("");
            buildManifestField.setDisable(true);
            plistPickerButton.setDisable(true);
            buildIDField.setEffect(null);
            buildIDField.setText("");
            buildIDField.setDisable(true);
        }
    }

    public void plistPickerHandler() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select the BuildManifest.plist");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PLIST", "*.plist"));
        File result = fileChooser.showOpenDialog(Main.primaryStage);
        if (result != null) {
            buildManifestField.setText(result.toString());
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
        File result = dirChooser.showDialog(Main.primaryStage);
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
        if (!prefs.get("Path", "").equals("")) {
            pathField.setText(prefs.get("Path", ""));
        }
        if (prefs.get("Device Model", "").equals("none")) {
            identifierCheckBox.setSelected(true);
            identifierCheckBoxHandler();
            identifierField.setText(prefs.get("Device Identifier", ""));
        } else {
            identifierCheckBox.setSelected(false);
            identifierCheckBoxHandler();
            deviceTypeChoiceBox.setValue(prefs.get("Device Type", ""));
            deviceModelChoiceBox.setValue(prefs.get("Device Model", ""));
        }
        if (!prefs.get("Board Config", "").equals("none")) {
            boardConfigField.setText(prefs.get("Board Config", ""));
        }

    }

    private void presetButtonHandler(ActionEvent evt) {
        Button btn = (Button) evt.getTarget();
        int preset = Integer.valueOf(btn.getId().substring("preset".length()));
        if (editingPresets) {
            savePreset(preset);
            savePresetButton.fire();
        } else {
            loadPreset(preset);
        }
    }

    private void savePreset(int preset) {
        boolean doReturn = false;
        if (ecidField.getText().equals("")) {
            ecidField.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && ((deviceModelChoiceBox.getValue() == null) || (deviceModelChoiceBox.getValue().equals("")))) {
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
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Name Preset " + preset);
        textInputDialog.setHeaderText("Name Preset");
        textInputDialog.setContentText("Please enter a name for the preset:");
        textInputDialog.showAndWait();
        if (!textInputDialog.getResult().equals("") || !(textInputDialog.getResult() == null)) {
            Preferences appPrefs = Preferences.userRoot().node("airsquared/blobsaver/prefs");
            appPrefs.put("Name Preset" + preset, textInputDialog.getResult());
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
        if (boardConfig) {
            presetPrefs.put("Board Config", boardConfigField.getText());
        } else {
            presetPrefs.put("Board Config", "none");
        }
    }

    public void savePresetHandler() {
        editingPresets = !editingPresets;
        if (editingPresets) {
            goButton.setVisible(false);
            goButton.setManaged(false);
            savePresetButton.setText("Cancel");
            scrollPane.setMaxWidth(410.0);
            scrollPane.setPrefWidth(410.0);
            presetButtons.forEach((Button btn) -> btn.setText("Save in " + btn.getText().substring("Load ".length())));
        } else {
            savePresetButton.setText("Save");
            scrollPane.setMaxWidth(385.0);
            scrollPane.setPrefWidth(385.0);
            goButton.setManaged(true);
            goButton.setVisible(true);
            presetButtons.forEach((Button btn) -> btn.setText("Load " + btn.getText().substring("Save in ".length())));
        }
    }

    public void checkBlobs() {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://tsssaver.1conan.com/check.php"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public void aboutMenuHandler() {
        ButtonType githubRepo = new ButtonType("Github Repo");
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "About text here", githubRepo, ButtonType.OK);
        alert.setTitle("About");
        alert.setHeaderText("Blobsaver " + Main.appVersion);
        alert.showAndWait();
        if (alert.getResult().equals(githubRepo) && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/airsquared/blobsaver"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public void go() {
        boolean doReturn = false;
        if (ecidField.getText().equals("")) {
            ecidField.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && ((deviceTypeChoiceBox.getValue() == null) || (deviceTypeChoiceBox.getValue().equals("")))) {
            deviceTypeChoiceBox.setEffect(errorBorder);
            doReturn = true;
        }
        if (!identifierCheckBox.isSelected() && ((deviceModelChoiceBox.getValue() == null) || (deviceModelChoiceBox.getValue().equals("")))) {
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
        if (pathField.getText().equals("")) {
            pathField.setEffect(errorBorder);
            doReturn = true;
        }
        if (!versionCheckBox.isSelected() && versionField.getText().equals("")) {
            versionField.setEffect(errorBorder);
            doReturn = true;
        }
        if (betaCheckBox.isSelected() && buildIDField.getText().equals("")) {
            buildIDField.setEffect(errorBorder);
            doReturn = true;
        }
        if (betaCheckBox.isSelected() && buildManifestField.getText().equals("")) {
            buildManifestField.setEffect(errorBorder);
            doReturn = true;
        }
        if (doReturn) {
            return;
        }
        String deviceModel = (String) deviceModelChoiceBox.getValue();
        if (deviceModel == null) {
            deviceModel = "";
        }
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
                    if (identifierText.startsWith("iPad") || identifierText.startsWith("iPod") || identifierText.startsWith("iPhone") || identifierText.startsWith("AppleTV")) {
                        run(identifierField.getText());
                    } else {
                        newUnreportableError("\"" + identifierText + "\" is not a valid identifier");
                        return;
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    newUnreportableError("\"" + identifierText + "\" is not a valid identifier");
                    return;
                }
                break;
            default:
                newReportableError("Could not find: \"" + deviceModel + "\"");
                break;
        }
    }
}
