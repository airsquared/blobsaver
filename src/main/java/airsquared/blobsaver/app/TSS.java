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

import com.google.gson.Gson;
import javafx.concurrent.Task;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static airsquared.blobsaver.app.Utils.containsIgnoreCase;
import static airsquared.blobsaver.app.Utils.executeProgram;
import static airsquared.blobsaver.app.Utils.extractBuildManifest;
import static airsquared.blobsaver.app.Utils.getFirmwareList;
import static airsquared.blobsaver.app.Utils.getSignedBetas;
import static airsquared.blobsaver.app.Utils.getSignedFirmwares;
import static airsquared.blobsaver.app.Utils.isNumeric;

public class TSS extends Task<String> {

    private static final Pattern ipswURLPattern = Pattern.compile("(https?://|file:/).*\\.(ipsw|plist)");
    private static final Pattern versionPattern = Pattern.compile("[0-9]+\\.[0-9]+\\.?[0-9]*(?<!\\.)");

    private final String deviceIdentifier;
    private final String ecid;
    private final String savePath;

    private final String boardConfig;

    private final boolean includeBetas;
    private final String manualVersion;
    private final String manualIpswURL;

    private final String apnonce, generator;

    private final boolean saveToTSSSaver, saveToSHSHHost;

    /**
     * Private constructor; use {@link TSS.Builder} instead
     */
    private TSS(String deviceIdentifier, String ecid, String savePath, String boardConfig, boolean includeBetas, String manualVersion, String manualIpswURL, String apnonce, String generator, boolean saveToTSSSaver, boolean saveToSHSHHost) {
        this.deviceIdentifier = deviceIdentifier;
        this.ecid = ecid;
        this.boardConfig = boardConfig;
        this.includeBetas = includeBetas;
        this.manualVersion = manualVersion;
        this.manualIpswURL = manualIpswURL;
        this.apnonce = apnonce;
        this.generator = generator;
        this.saveToTSSSaver = saveToTSSSaver;
        this.saveToSHSHHost = saveToSHSHHost;
        this.savePath = parsePath(savePath);
    }

    /**
     * @return a printable multiline string describing the blobs saved
     * @throws TSSException if blobs were not saved successfully
     */
    @Override
    protected String call() throws TSSException {
        checkInputs();

        List<Utils.IOSVersion> iosVersions = getIOSVersions();
        System.out.println("iosVersions = " + iosVersions);
        ArrayList<String> args = constructArgs();

        var alreadySaved = new StringJoiner(", ");
        var newlySaved = new StringJoiner(", ");
        for (Utils.IOSVersion iosVersion : iosVersions) {
            if (!Prefs.getAlwaysSaveNewBlobs() && checkAlreadySaved(iosVersion)) {
                alreadySaved.add(iosVersion.versionString());
                continue;
            }

            try {
                saveFor(iosVersion, args);
            } catch (TSSException e) {
                if ((manualVersion == null && manualIpswURL == null) && e.getMessage().contains("not being signed")) {
                    System.out.println("Warning: ignoring unsigned version; API is likely out of date");
                    continue; // ignore not being signed (API might not be updated)
                }
                throw e;
            }

            if (iosVersion.versionString() != null) {
                newlySaved.add(iosVersion.versionString());
            }
        }
        var responseBuilder = new StringBuilder();
        if (manualIpswURL != null || newlySaved.length() > 0) {
            responseBuilder.append("Successfully saved blobs in\n").append(savePath);
            if (newlySaved.length() > 0) {
                responseBuilder.append("\n\nFor version").append(iosVersions.size() == 1 ? " " : "s ").append(newlySaved);
            }
            if (alreadySaved.length() > 0) {
                responseBuilder.append("\n\n");
            }
        }
        if (alreadySaved.length() > 0) {
            responseBuilder.append("Already saved for ").append(alreadySaved);
        }

        if (saveToTSSSaver || saveToSHSHHost) {
            responseBuilder.append("\n\n");
        }
        if (saveToTSSSaver) {
            try {
                saveBlobsTSSSaver(responseBuilder);
            } catch (Exception e) {
                e.printStackTrace();
                responseBuilder.append("Error encountered while trying to save blobs to TSS Saver: ").append(e.getMessage());
            }
        }
        if (saveToSHSHHost) {
            try {
                saveBlobsSHSHHost(responseBuilder);
            } catch (Exception e) {
                e.printStackTrace();
                responseBuilder.append("Error encountered while trying to save blobs to SHSH Host: ").append(e.getMessage());
            }
        }

        Analytics.saveBlobs();
        return responseBuilder.toString();
    }

    private boolean checkAlreadySaved(Utils.IOSVersion ios) {
        if (ios.versionString() == null) {
            return false;
        }
        var versionStringOnly = ios.versionString().trim().replaceFirst(" .*", ""); // strip out 'beta' labels
        // https://github.com/1Conan/tsschecker/blob/0bc6174c3c2f77a0de525b71e7d8ec0987f07aa1/tsschecker/tsschecker.c#L1262
        String fileName = "%s_%s_%s_%s-%s_%s.shsh2"
                .formatted(parseECID(), deviceIdentifier, getBoardConfig(), versionStringOnly, ios.buildid(), apnonce);

        if (Files.exists(Path.of(parsePathWithVersion(ios), fileName))) {
            System.out.println("Already Saved: " + fileName);
            return true;
        }
        return false;
    }

    private long parseECID() {
        return isNumeric(ecid) ? Long.parseLong(ecid)
                : Long.parseLong(ecid.startsWith("0x") ? ecid.substring(2) : ecid, 16);
    }

    private String parsePath(String input) {
        if (!input.contains("${")) return input;
        String template = input;

        var variables = Map.of("${DeviceIdentifier}", deviceIdentifier,
                            "${BoardConfig}", getBoardConfig(),
                            "${APNonce}", Utils.defIfNull(apnonce, "UnknownAPNonce"),
                            "${Generator}", Utils.defIfNull(generator, "UnknownGenerator"),
                            "${DeviceModel}", Devices.identifierToModel(deviceIdentifier),
                            "${ECID}", ecid);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace(entry.getKey(), entry.getValue());
        }
        return template;
    }

    private String parsePathWithVersion(Utils.IOSVersion ios) {
        if (!savePath.contains("${")) return savePath;
        var template = savePath;

        var majorVersion = ios.versionString() != null ? ios.versionString().replaceFirst("\\..*", "") : "UnknownVersion";
        Map<String, String> variables = Map.of(
                "${FullVersionString}", Utils.defIfNull(ios.versionString(), "UnknownVersion"),
                "${BuildID}", Utils.defIfNull(ios.buildid(), "UnknownBuildID"),
                "${MajorVersion}", majorVersion);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace(entry.getKey(), entry.getValue());
        }

        return template;
    }

    private void saveFor(Utils.IOSVersion iosVersion, ArrayList<String> args) throws TSSException {
        final int urlIndex = args.size() - 1;
        final int pathIndex = args.size() - 3;
        Path manifest;
        try {
            manifest = extractBuildManifest(iosVersion.ipswURL());
            args.set(urlIndex, manifest.toString());
        } catch (IOException e) {
            throw new TSSException("Unable to extract BuildManifest.", true, e);
        }
        try {
            args.set(pathIndex, parsePathWithVersion(iosVersion));
            Files.createDirectories(Path.of(args.get(pathIndex)));
        } catch (IOException e) {
            throw new TSSException("Unable to create save directory. Try with a different save path. If you are using variables, make sure they are spelled correctly.", false, e);
        }
        try {
            System.out.println("Running: " + args);
            String tssLog = executeProgram(args);
            parseTSSLog(tssLog);
        } catch (IOException e) {
            throw new TSSException("There was an error starting tsschecker.", true, e);
        } finally {
            deleteIfPossible(manifest);
        }
    }

    private void deleteIfPossible(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private void checkInputs() throws TSSException {
        try {
            Devices.getDeviceType(deviceIdentifier);
        } catch (IllegalArgumentException e) {
            throw new TSSException("\"" + deviceIdentifier + "\" is not a valid identifier", false);
        }
        if (boardConfig == null && Devices.doesRequireBoardConfig(deviceIdentifier)) {
            throw new TSSException("A board configuration is required for this device.", false);
        }
        if (manualIpswURL != null) { // check URL
            try {
                if (!ipswURLPattern.matcher(manualIpswURL).matches()) {
                    throw new MalformedURLException("Doesn't match ipsw URL regex");
                }
                new URL(manualIpswURL); // check URL
            } catch (MalformedURLException e) {
                throw new TSSException("The IPSW URL is not valid.\n\nMake sure it's a valid URL that ends with \".ipsw\" or \".plist\"", false, e);
            }
            if (manualIpswURL.startsWith("file:")) try {
                Path.of(new URI(manualIpswURL)).toRealPath(); // check URI
            } catch (IllegalArgumentException | URISyntaxException | IOException e) {
                throw new TSSException("The IPSW URL is not valid.\n\nMake sure it's a valid file URL to a local .ipsw or .plist file.", false, e);
            }
        } else if (manualVersion != null && !versionPattern.matcher(manualVersion).matches()) {
            throw new TSSException("Invalid version. Make sure it follows the convention X.X.X or X.X, like \"13.1\" or \"13.5.5\"", false);
        }
    }

    /**
     * Adds to the arraylists, depending on the fields {@link #manualIpswURL} and {@link #manualVersion}.
     * <p>
     * If both of those fields are null, it adds all signed versions.
     *
     * @return a list of {@link Utils.IOSVersion} to save blobs for
     */
    private List<Utils.IOSVersion> getIOSVersions() throws TSSException {
        try {
            if (manualVersion != null) {
                return Collections.singletonList(getFirmwareList(deviceIdentifier).filter(iosVersion ->
                                manualVersion.equals(iosVersion.versionString())).findFirst()
                        .orElseThrow(() -> new TSSException("No versions found.", false)));
            } else if (manualIpswURL != null) {
                return Collections.singletonList(new Utils.IOSVersion(null, null, manualIpswURL, null));
            } else if (includeBetas) {
                var signedFirmwares = getSignedFirmwares(deviceIdentifier);
                Stream<Utils.IOSVersion> signedBetas;
                try {
                    signedBetas = getSignedBetas(deviceIdentifier);
                } catch (IOException e) {
                    throw new TSSException("There was an error with the beta API; try without including beta versions.", false, e);
                }
                return Stream.concat(signedFirmwares, signedBetas).toList();
            } else { // all signed firmwares
                return getSignedFirmwares(deviceIdentifier).toList();
            }
        } catch (FileNotFoundException e) {
            var message = "The device \"" + deviceIdentifier + "\" could not be found.";
            if (includeBetas) {
                message += " This device may not have any beta versions available; try without including beta versions.";
            }
            throw new TSSException(message, false, e);
        } catch (IOException e) {
            var message = "Saving blobs failed. Check your internet connection.";
            throw new TSSException(message, false, e);
        }
    }

    private ArrayList<String> constructArgs() {
        ArrayList<String> args = new ArrayList<>(17);
        String tsscheckerPath = Utils.getTsschecker().getAbsolutePath();
        Collections.addAll(args, tsscheckerPath, "--nocache", "--save", "--device", deviceIdentifier, "--ecid", ecid);
        Collections.addAll(args, "--boardconfig", getBoardConfig());
        if (apnonce != null) {
            Collections.addAll(args, "--apnonce", apnonce);
            if (generator != null) {
                Collections.addAll(args, "--generator", generator);
            }
        } else {
            Collections.addAll(args, "--generator", "0x1111111111111111");
        }

        Collections.addAll(args, "--save-path", "will be replaced in loop",
                "--build-manifest", "will be replaced in loop");

        return args;
    }

    private String getBoardConfig() {
        return Utils.defIfNull(boardConfig, Devices.getBoardConfig(deviceIdentifier));
    }

    private void parseTSSLog(String tsscheckerLog) throws TSSException {
        if (containsIgnoreCase(tsscheckerLog, "Saved shsh blobs")) {
            return; // success
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] manually specified ecid=" + ecid + ", but parsing failed")) {
            throw new TSSException("\"" + ecid + "\"" + " is not a valid ECID. Try using the 'Read from device' button.", false);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] manually specified ApNonce=" + apnonce + ", but parsing failed")
                || containsIgnoreCase(tsscheckerLog, "[Error] [TSSR] parsed APNoncelen != requiredAPNoncelen")) {
            throw new TSSException("\"" + apnonce + "\" is not a valid APNonce", false);
        } else if (containsIgnoreCase(tsscheckerLog, "could not get BuildIdentity for installType=Erase")
                && containsIgnoreCase(tsscheckerLog, "could not get BuildIdentity for installType=Update")
                && containsIgnoreCase(tsscheckerLog, "checking tss status failed")) {
            throw new TSSException("Saving blobs failed. Check the board configuration or try again later.", true, tsscheckerLog);
        } else if (containsIgnoreCase(tsscheckerLog, "Could not resolve host")) {
            throw new TSSException("Saving blobs failed. Check your internet connection.", false, tsscheckerLog);
        } else if (containsIgnoreCase(tsscheckerLog, "can't save shsh at")) {
            throw new TSSException("'" + savePath + "' is not a valid path. If you are using variables, make sure they are spelled correctly.", false);
        } else if (containsIgnoreCase(tsscheckerLog, "IS NOT being signed")) {
            if (manualVersion == null) {
                throw new TSSException("The " + Devices.getOSNameForType(Devices.getDeviceType(deviceIdentifier)) + " version is not being signed for device " + deviceIdentifier, false);
            } else {
                throw new TSSException("%s %s is not being signed for device %s".formatted(
                        Devices.getOSNameForIdentifier(deviceIdentifier), manualVersion, deviceIdentifier), false);
            }

        } else if (containsIgnoreCase(tsscheckerLog, "failed to load manifest")) {
            if (manualIpswURL != null) {
                throw new TSSException("Failed to load manifest. The IPSW or build manifest URL is not valid.\n\n", false);
            } else {
                throw new TSSException("Failed to load manifest.", true, tsscheckerLog);
            }
        } else if (containsIgnoreCase(tsscheckerLog, "selected device can't be used with that buildmanifest")) {
            throw new TSSException("Device and build manifest don't match.", false);
        }
        throw new TSSException("An unknown error occurred.", true, tsscheckerLog);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        private String device, ecid, savePath, boardConfig, manualVersion, manualIpswURL, apnonce, generator;
        private boolean includeBetas, saveToTSSSaver, saveToSHSHHost;

        public Builder setDevice(String device) {
            this.device = device;
            return this;
        }

        public Builder setEcid(String ecid) {
            this.ecid = ecid;
            return this;
        }

        public Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public Builder setBoardConfig(String boardConfig) {
            this.boardConfig = boardConfig;
            return this;
        }

        public Builder setManualVersion(String manualVersion) {
            manualIpswURL = null;
            this.manualVersion = manualVersion;
            return this;
        }

        public Builder setManualIpswURL(String manualIpswURL) {
            manualVersion = null;
            this.manualIpswURL = manualIpswURL;
            return this;
        }

        public Builder setApnonce(String apnonce) {
            this.apnonce = apnonce;
            return this;
        }

        public Builder setGenerator(String generator) {
            this.generator = generator;
            return this;
        }

        public Builder setIncludeBetas(boolean includeBetas) {
            this.includeBetas = includeBetas;
            return this;
        }

        public Builder saveToTSSSaver(boolean saveToTSSSaver) {
            this.saveToTSSSaver = saveToTSSSaver;
            return this;
        }

        public Builder saveToSHSHHost(boolean saveToSHSHHost) {
            this.saveToSHSHHost = saveToSHSHHost;
            return this;
        }

        public TSS build() {
            return new TSS(Objects.requireNonNull(device, "Device"),
                    Objects.requireNonNull(ecid, "ECID"),
                    Objects.requireNonNull(savePath, "Save Path"),
                    boardConfig, includeBetas, manualVersion, manualIpswURL, apnonce, generator, saveToTSSSaver, saveToSHSHHost);
        }
    }


    /**
     * Checked exception for all TSS related errors.
     */
    static class TSSException extends Exception {

        public final String tssLog;
        public final boolean isReportable;

        public TSSException(String message, boolean isReportable) {
            super(message);
            this.isReportable = isReportable;
            this.tssLog = null;
        }

        public TSSException(String message, boolean isReportable, String tssLog) {
            super(message);
            this.isReportable = isReportable;
            this.tssLog = tssLog;
        }

        public TSSException(String message, boolean isReportable, Throwable cause) {
            super(message, cause);
            this.isReportable = isReportable;
            this.tssLog = null;
        }

        public void showErrorAlert() {
            if (isReportable && tssLog != null) {
                Utils.showReportableError(getMessage(), tssLog);
            } else if (isReportable) {
                Utils.showReportableError(getMessage(), Utils.exceptionToString(this));
            } else {
                Utils.showUnreportableError(getMessage());
            }
        }
    }

    private void saveBlobsTSSSaver(StringBuilder responseBuilder) throws IOException, InterruptedException {
        Map<Object, Object> deviceParameters = new HashMap<>();

        deviceParameters.put("ecid", String.valueOf(parseECID()));
        deviceParameters.put("deviceIdentifier", deviceIdentifier);
        deviceParameters.put("boardConfig", getBoardConfig());

        if (generator != null) {
            deviceParameters.put("generator", generator);
        }
        if (apnonce != null) {
            deviceParameters.put("apnonce", apnonce);
        }

        System.out.println(deviceParameters);
        var headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        HttpResponse<String> response =
                Network.makePOSTRequest("https://tsssaver.1conan.com/v2/api/save.php", deviceParameters, headers, true);
        System.out.println(response.body());

        @SuppressWarnings("rawtypes") Map responseBody = new Gson().fromJson(response.body(), Map.class);

        if (responseBody == null) {
            responseBuilder.append("Error encountered while trying to save blobs to TSSSaver: ").append("Response code=").append(response.statusCode());
        } else if (responseBody.containsKey("errors")) {
            responseBuilder.append("Error encountered while trying to save blobs to TSSSaver: ").append(responseBody.get("errors"));
        } else {
            responseBuilder.append("Also saved blobs online to TSS Saver.");
        }
    }

    private void saveBlobsSHSHHost(StringBuilder responseBuilder) throws IOException, InterruptedException {
        if (saveToTSSSaver) {
            responseBuilder.append("\n");
        }

        Map<Object, Object> deviceParameters = new HashMap<>();

        deviceParameters.put("ecid", ecid);
        deviceParameters.put("boardconfig", getBoardConfig());
        deviceParameters.put("device", deviceIdentifier);
        deviceParameters.put("selected_firmware", "All");

        if (generator != null) {
            deviceParameters.put("generator", generator);
        }
        if (apnonce != null) {
            deviceParameters.put("apnonce", apnonce);
        }

        System.out.println(deviceParameters);
        String userAgent = "Blobsaver " + Main.appVersion;
        var headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("User-Agent", userAgent);
        headers.put("X-CPU-STATE", "0000000000000000000000000000000000000000");

        HttpResponse<String> response =
                Network.makePOSTRequest("https://api.arx8x.net/shsh3/", deviceParameters, headers, false);
        System.out.println(response.body());

        @SuppressWarnings("rawtypes") Map responseBody = new Gson().fromJson(response.body(), Map.class);

        if (responseBody.get("code").equals((double) 0)) {
            responseBuilder.append("Also saved blobs online to SHSH Host.");
        } else {
            responseBuilder.append("Error encountered while trying to save blobs to SHSH Host: ").append(responseBody.get("message"));
        }
    }
}
