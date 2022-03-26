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

import javafx.concurrent.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static airsquared.blobsaver.app.Utils.containsIgnoreCase;
import static airsquared.blobsaver.app.Utils.executeProgram;
import static airsquared.blobsaver.app.Utils.extractBuildManifest;
import static airsquared.blobsaver.app.Utils.getFirmwareList;
import static airsquared.blobsaver.app.Utils.getSignedFirmwares;

public class TSS extends Task<String> {

    // note: Matcher is NOT thread safe
    private static final Matcher ipswURLMatcher = Pattern.compile("(https?://|file:/).*\\.ipsw").matcher("");
    private static final Matcher versionMatcher = Pattern.compile("[0-9]+\\.[0-9]+\\.?[0-9]*(?<!\\.)").matcher("");

    private final String deviceIdentifier;
    private final String ecid;
    private final String savePath;

    private final String boardConfig;

    private final String manualVersion;
    private final String manualIpswURL;

    private final String apnonce, generator;
    private final boolean saveToTSSSaver, saveToSHSHHost;

    /**
     * Private constructor; use {@link TSS.Builder} instead
     */
    private TSS(String deviceIdentifier, String ecid, String savePath, String boardConfig, String manualVersion, String manualIpswURL, String apnonce, String generator, boolean saveToTSSSaver, boolean saveToSHSHHost) {
        this.deviceIdentifier = deviceIdentifier;
        this.ecid = ecid;
        this.savePath = savePath;
        this.boardConfig = boardConfig;
        this.manualVersion = manualVersion;
        this.manualIpswURL = manualIpswURL;
        this.apnonce = apnonce;
        this.generator = generator;
        this.saveToTSSSaver = saveToTSSSaver;
        this.saveToSHSHHost = saveToSHSHHost;
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
        final int urlIndex = args.size() - 1;

        StringBuilder sb = new StringBuilder("Successfully saved blobs in\n").append(savePath);
        if (manualIpswURL == null) {
            sb.append(iosVersions.size() == 1 ? "\n\nFor version " : "\n\nFor versions ");
        }

        // can't use forEach() because exception won't be caught
        for (Utils.IOSVersion iosVersion : iosVersions) {
            try {
                args.set(urlIndex, extractBuildManifest(iosVersion.ipswURL()).toString());
            } catch (IOException e) {
                throw new TSSException("Unable to extract BuildManifest.", true, e);
            }
            try {
                System.out.println("Running: " + args);
                String tssLog = executeProgram(args);
                parseTSSLog(tssLog);
            } catch (IOException e) {
                throw new TSSException("There was an error starting tsschecker.", true, e);
            }

            if (iosVersion.versionString() != null) {
                sb.append(iosVersion.versionString());
                if (iosVersion != iosVersions.get(iosVersions.size() - 1))
                    sb.append(", ");
            }
        }

        Analytics.saveBlobs();
        return sb.toString();
    }

    private void checkInputs() throws TSSException {
        boolean hasCorrectIdentifierPrefix = deviceIdentifier.startsWith("iPad") || deviceIdentifier.startsWith("iPod")
                || deviceIdentifier.startsWith("iPhone") || deviceIdentifier.startsWith("AppleTV");
        if (!deviceIdentifier.contains(",") || !hasCorrectIdentifierPrefix) {
            throw new TSSException("\"" + deviceIdentifier + "\" is not a valid identifier", false);
        }
        if (manualIpswURL != null) { // check URL
            try {
                if (!ipswURLMatcher.reset(manualIpswURL).matches()) {
                    throw new MalformedURLException("Doesn't match ipsw URL regex");
                }
                new URL(manualIpswURL); // check URL
            } catch (MalformedURLException e) {
                throw new TSSException("The IPSW URL is not valid.\n\nMake sure it's a valid URL that ends with \".ipsw\"", false, e);
            }
            if (manualIpswURL.startsWith("file:")) try {
                Path.of(new URI(manualIpswURL)).toRealPath(); // check URI
            } catch (IllegalArgumentException | URISyntaxException | IOException e) {
                throw new TSSException("The IPSW URL is not valid.\n\nMake sure it's a valid file URL to a local .ipsw file.", false, e);
            }
        } else if (manualVersion != null && !versionMatcher.reset(manualVersion).matches()) {
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
                return Collections.singletonList(new Utils.IOSVersion(null, manualIpswURL, null));
            } else { // all signed firmwares
                return getSignedFirmwares(deviceIdentifier).toList();
            }
        } catch (FileNotFoundException e) {
            throw new TSSException("The device \"" + deviceIdentifier + "\" could not be found.", false, e);
        } catch (IOException e) {
            throw new TSSException("Saving blobs failed. Check your internet connection.", false, e);
        }
    }

    private ArrayList<String> constructArgs() {
        ArrayList<String> args = new ArrayList<>(17);
        String tsscheckerPath = Utils.getTsschecker().getAbsolutePath();
        //noinspection ResultOfMethodCallIgnored
        new File(savePath).mkdirs();
        Collections.addAll(args, tsscheckerPath, "--nocache", "--save", "--device", deviceIdentifier, "--ecid", ecid, "--save-path", savePath);
        Collections.addAll(args, "--boardconfig", getBoardConfig());
        if (apnonce != null) {
            Collections.addAll(args, "--apnonce", apnonce);
            if (generator != null) {
                Collections.addAll(args, "--generator", generator);
            }
        } else {
            Collections.addAll(args, "--generator", "0x1111111111111111");
        }
        Collections.addAll(args, "--build-manifest", "will be replaced in loop");

        return args;
    }

    private String getBoardConfig() {
        return Objects.requireNonNullElse(boardConfig, Devices.getBoardConfig(deviceIdentifier));
    }

    @SuppressWarnings("TextBlockMigration")
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
            throw new TSSException("'" + savePath + "' is not a valid path", false);
        } else if (containsIgnoreCase(tsscheckerLog, "IS NOT being signed")) {
            if (manualVersion == null) {
                throw new TSSException("The " + Devices.getOSNameForType(Devices.getDeviceType(deviceIdentifier)) + " version is not being signed for device " + deviceIdentifier, false);
            } else {
                throw new TSSException("%s %s is not being signed for device %s".formatted(
                        Devices.getOSNameForIdentifier(deviceIdentifier), manualVersion, deviceIdentifier), false);
            }

        } else if (containsIgnoreCase(tsscheckerLog, "failed to load manifest")) {
            if (manualIpswURL != null) {
                throw new TSSException("Failed to load manifest. The IPSW URL is not valid.\n\n" +
                        "Make sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"", false);
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
        private boolean saveToTSSSaver, saveToSHSHHost;

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
                    boardConfig, manualVersion, manualIpswURL, apnonce, generator, saveToTSSSaver, saveToSHSHHost);
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

    }

    HttpResponse<String> saveBlobsTSSSaver() throws Exception {
        Map<Object, Object> deviceParameters = new HashMap<>();

        deviceParameters.put("ecid", String.valueOf(Long.parseLong(ecid, 16)));
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
        return Network.makePOSTRequest("https://tsssaver.1conan.com/v2/api/save.php", deviceParameters, headers, true);
    }

    HttpResponse<String> saveBlobsSHSHHost() throws Exception {
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
        String UserAgent = "Blobsaver " + Main.appVersion;
        var headers = new HashMap<String, String>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("User-Agent", UserAgent);
        headers.put("X-CPU-STATE", "0000000000000000000000000000000000000000");
        return Network.makePOSTRequest("https://api.arx8x.net/shsh3/", deviceParameters, headers, false);
    }
}
