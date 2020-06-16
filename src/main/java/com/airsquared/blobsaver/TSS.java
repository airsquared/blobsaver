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

import javafx.concurrent.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.airsquared.blobsaver.Utils.containsIgnoreCase;
import static com.airsquared.blobsaver.Utils.executeProgram;
import static com.airsquared.blobsaver.Utils.extractBuildManifest;
import static com.airsquared.blobsaver.Utils.getFirmwareList;
import static com.airsquared.blobsaver.Utils.getSignedFirmwares;

public class TSS extends Task<List<String>> {

    private final String deviceIdentifier;
    private final String ecid;
    private final String savePath;

    private final String boardConfig;

    private final String manualVersion;
    private final String manualIpswURL;

    private final String apnonce;

    /**
     * Private constructor; use {@link TSS.Builder} instead
     */
    private TSS(String deviceIdentifier, String ecid, String savePath, String boardConfig, String manualVersion, String manualIpswURL, String apnonce) {
        this.deviceIdentifier = deviceIdentifier;
        this.ecid = ecid;
        this.savePath = savePath;
        this.boardConfig = boardConfig;
        this.manualVersion = manualVersion;
        this.manualIpswURL = manualIpswURL;
        this.apnonce = apnonce;
    }

    private static ExecutorService executorService;

    public void executeInThreadPool() {
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }
        executorService.execute(this);
    }

    /**
     * @return a list of versions that blobs have been saved for
     * (an empty list if a build manifest was manually supplied)
     * @throws TSSException if blobs were not saved successfully
     */
    @Override
    protected List<String> call() throws TSSException {
        checkValues();

        ArrayList<String> ipswURLs = new ArrayList<>();
        ArrayList<String> versionStrings = new ArrayList<>();
        getIpswURLS(ipswURLs, versionStrings);

        String[] args = constructArgs();

        // can't use forEach() because exception won't be caught
        for (String ipswURL : ipswURLs) {
            try {
                args[args.length - 1] = extractBuildManifest(ipswURL).getAbsolutePath();
            } catch (IOException e) {
                throw new TSSException("Unable to extract BuildManifest.", true, e);
            }
            try {
                System.out.println("Running: " + Arrays.toString(args));
                String tssLog = executeProgram(args);
                parseTSSLog(tssLog);
            } catch (IOException e) {
                throw new TSSException("There was an error starting tsschecker.", true, e);
            }
        }
        return versionStrings;
    }

    // note: Matcher is NOT thread safe
    private static final Matcher ipswURLPattern = Pattern.compile("https?://.*apple.*\\.ipsw").matcher("");
    private static final Matcher versionPattern = Pattern.compile("[0-9]+\\.[0-9]+\\.?[0-9]*(?<!\\.)").matcher("");

    private void checkValues() throws TSSException {
        boolean hasCorrectIdentifierPrefix = deviceIdentifier.startsWith("iPad") || deviceIdentifier.startsWith("iPod")
                || deviceIdentifier.startsWith("iPhone") || deviceIdentifier.startsWith("AppleTV");
        if (!deviceIdentifier.contains(",") || !hasCorrectIdentifierPrefix) {
            throw new TSSException("\"" + deviceIdentifier + "\" is not a valid identifier", false);
        }
        if (manualIpswURL != null) { // check URL
            try {
                if (!ipswURLPattern.reset(manualIpswURL).matches()) {
                    throw new MalformedURLException("Doesn't match ipsw URL regex");
                }
                new URL(manualIpswURL);
            } catch (MalformedURLException e) {
                throw new TSSException("The IPSW URL is not valid.\n\nMake sure it starts with \"http://\" or \"https://\", has \"apple\" in it, and ends with \".ipsw\"", false, e);
            }
        } else if (manualVersion != null && !versionPattern.reset(manualVersion).matches()) {
            throw new TSSException("Invalid version. Make sure it follows the convention X.X.X or X.X, like \"13.1\" or \"13.5.5\"", false);
        }
    }

    /**
     * Adds to the arraylists, depending on the fields {@link #manualIpswURL} and {@link #manualVersion}.
     * <p>
     * If both of those fields are null, it adds all signed versions.
     * If {@link #manualIpswURL} is specified, the parameter {@code versionStrings} will not be modified.
     *
     * @param ipswURLS       an empty list that will be filled with URLs to IPSW(s)
     * @param versionStrings an empty list that will be filled with strings of iOS versions to be displayed to the user
     */
    private void getIpswURLS(ArrayList<String> ipswURLS, ArrayList<String> versionStrings) throws TSSException {
        try {
            if (manualVersion != null) {
                ipswURLS.add(getFirmwareList(deviceIdentifier).stream().filter(stringObjectMap ->
                        manualVersion.equals(stringObjectMap.get("version")))
                        .collect(Collectors.toList()).get(0).get("url").toString());
            } else if (manualIpswURL != null) {
                ipswURLS.add(manualIpswURL);
            } else { // all signed firmwares
                getSignedFirmwares(deviceIdentifier).forEach(firmware -> {
                    ipswURLS.add(firmware.get("url").toString());
                    versionStrings.add(firmware.get("version").toString());
                });
            }
        } catch (FileNotFoundException e) {
            throw new TSSException("The device \"" + deviceIdentifier + "\" could not be found.", false, e);
        } catch (IOException e) {
            throw new TSSException("Saving blobs failed. Check your internet connection.", false, e);
        }
    }

    private String[] constructArgs() {
        ArrayList<String> argsList = new ArrayList<>(13);
        String tsscheckerPath = Utils.getTsschecker().getAbsolutePath();
        new File(savePath).mkdirs();
        Collections.addAll(argsList, tsscheckerPath, "--nocache", "--save", "--device", deviceIdentifier, "--ecid", ecid, "--save-path", savePath);
        if (boardConfig != null) {
            Collections.addAll(argsList, "--boardconfig", boardConfig);
        }
        if (apnonce != null) {
            Collections.addAll(argsList, "--apnonce", apnonce);
        } else {
            Collections.addAll(argsList, "--generator", "0x1111111111111111");
        }
        Collections.addAll(argsList, "--build-manifest", "");

        return argsList.toArray(new String[0]);
    }

    private void parseTSSLog(String tsscheckerLog) throws TSSException {
        if (containsIgnoreCase(tsscheckerLog, "Saved shsh blobs")) {
            return; // success
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] manually specified ecid=" + ecid + ", but parsing failed")) {
            throw new TSSException("\"" + ecid + "\"" + " is not a valid ECID. Try using the 'Read from device' button.", false);
        } else if (containsIgnoreCase(tsscheckerLog, "[Error] [TSSC] manually specified ApNonce=" + apnonce + ", but parsing failed")
                || containsIgnoreCase(tsscheckerLog, "[Error] [TSSR] parsed APNoncelen != requiredAPNoncelen")) {
            throw new TSSException("\"" + apnonce + "\" is not a valid apnonce", false);
        } else if (containsIgnoreCase(tsscheckerLog, "could not get id0 for installType=Erase")
                && containsIgnoreCase(tsscheckerLog, "could not get id0 for installType=Update")
                && containsIgnoreCase(tsscheckerLog, "checking tss status failed")) {
            throw new TSSException("Saving blobs failed. Check the board configuration or try again later.", true, tsscheckerLog);
        } else if (containsIgnoreCase(tsscheckerLog, "Could not resolve host")) {
            throw new TSSException("Saving blobs failed. Check your internet connection.", false, tsscheckerLog);
        } else if (containsIgnoreCase(tsscheckerLog, "can't save shsh at")) {
            throw new TSSException("'" + savePath + "' is not a valid path", false);
        } else if (containsIgnoreCase(tsscheckerLog, "IS NOT being signed")) {
            throw new TSSException("The iOS/tvOS is not being signed for device " + deviceIdentifier, false);
        } else if (containsIgnoreCase(tsscheckerLog, "failed to load manifest")) {
            if (manualIpswURL != null) {
                throw new TSSException("Failed to load manifest. The IPSW URL might not be valid.\n\n" +
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
        private String device;
        private String ecid;
        private String savePath;
        private String boardConfig;
        private String manualVersion;
        private String manualIpswURL;
        private String apnonce;

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

        public TSS build() {
            // only manualVersion, manualIPSW, boardconfig, and apnonce can be null
            if (device == null || ecid == null || savePath == null) {
                throw new NullPointerException();
            }
            return new TSS(device, ecid, savePath, boardConfig, manualVersion, manualIpswURL, apnonce);
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

}
