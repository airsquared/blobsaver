/*
 * Copyright (c) 2022  airsquared
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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.prefs.InvalidPreferencesFormatException;

@Command(name = "blobsaver", version = Main.appVersion, mixinStandardHelpOptions = true)
public class CLI implements Callable<Void> {

    @Option(names = {"-f", "--save-blobs"})
    boolean saveBlobs;

    @Option(names = {"-s", "--save-device"})
    String saveDevice;

    @Option(names = {"--remove"})
    String removeDevice;

    @Option(names = "--enable-background")
    String enableBackground;

    @Option(names = "--export")
    File exportTo;

    @Option(names = "--import")
    File importFrom;

    @Option(names = {"-i", "--identifier"})
    String device;
    @Option(names = {"-e", "--ecid"})
    String ecid;

    @Option(names = {"-p", "--save-path"})
    String savePath;

    @Option(names = {"-b", "--boardconfig"})
    String boardConfig;

    @Option(names = {"-v", "--ios-version"})
    String manualVersion;
    @Option(names = {"-u", "--url"})
    String manualIpswURL;

    @Option(names = {"-a", "--apnonce"})
    String apnonce;

    @Option(names = {"-g", "--generator"})
    String generator;
    @Option(names = {"-t", "--include-betas"})
    boolean includeBetas;

    @Override
    public Void call() throws TSS.TSSException, IOException, InvalidPreferencesFormatException {
        if (importFrom != null) {
            Prefs.importXML(importFrom);
        }
        if (saveBlobs) {
            var builder = new TSS.Builder()
                    .setDevice(device).setEcid(ecid).setSavePath(savePath).setBoardConfig(boardConfig)
                    .setManualVersion(manualVersion).setManualIpswURL(manualIpswURL).setApnonce(apnonce)
                    .setGenerator(generator).setIncludeBetas(includeBetas);
            builder.build().call();
        }
        if (removeDevice != null) {
            Prefs.getSavedDevices().stream()
                    .filter(savedDevice -> savedDevice.getName().equalsIgnoreCase(removeDevice))
                    .findAny().get().delete();
        }
        if (saveDevice != null) {
            new Prefs.SavedDeviceBuilder(saveDevice)
                    .setIdentifier(device).setEcid(ecid).setSavePath(savePath).setBoardConfig(boardConfig)
                    .setApnonce(apnonce).setGenerator(generator).setIncludeBetas(includeBetas).save();
        }
        if (enableBackground != null) {

        }
        if (exportTo != null) {
            Prefs.export(exportTo);
        }
        return null;
    }

    /**
     * @return the exit code
     */
    public static int launch(String... args) {
        System.err.println("Warning: blobsaver's CLI is in alpha. Commands, options, and exit codes may change at any time.");
        var c = new CommandLine(new CLI());
        return c.execute(args);
    }
}
