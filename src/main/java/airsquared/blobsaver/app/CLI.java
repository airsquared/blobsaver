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
import picocli.CommandLine.*;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.stream.Collectors;

@Command(name = "blobsaver", versionProvider = CLI.VersionProvider.class, header = CLI.warning,
        optionListHeading = "  You can separate options and their parameters with either a space or '='.%n",
        mixinStandardHelpOptions = true, sortOptions = false, usageHelpAutoWidth = true, sortSynopsis = false,
        abbreviateSynopsis = true, synopsisSubcommandLabel = "")
public class CLI implements Callable<Void> {

    public static final String warning = "Warning: blobsaver's CLI is in alpha. Commands, options, and exit codes may change at any time.%n";

    @Option(names = {"-s", "--save-blobs"})
    boolean saveBlobs;

    @Option(names = "--save-device", paramLabel = "<name>", description = "Create a new saved device.")
    String saveDevice;

    @Option(names = "--remove-device", paramLabel = "<Saved Device>", description = "Remove a saved device.")
    Prefs.SavedDevice removeDevice;

    @Option(names = "--enable-background", paramLabel = "<Saved Device>", description = "Enable background saving for a device.%nUse '--start-background-service' once devices are added.")
    Prefs.SavedDevice enableBackground;

    @Option(names = "--disable-background", paramLabel = "<Saved Device>", description = "Disable background saving for a device.")
    Prefs.SavedDevice disableBackground;

    @ArgGroup
    BackgroundControls backgroundControls = new BackgroundControls();
    static class BackgroundControls {
        @Option(names = "--start-background-service", description = "Register background saving service with the OS.")
        boolean startBackground;
        @Option(names = "--stop-background-service", description = "Deregister background saving service from the OS.")
        boolean stopBackground;

        @Option(names = "--background-autosave", description = "Save blobs for all devices configured to save in background. All other options have no effect on this option. Useful in scripts.")
        boolean backgroundAutosave;
    }

    @Option(names = "--export", paramLabel = "<path>", description = "Export saved devices in XML format to the directory.")
    File exportPath;

    @Option(names = "--import", paramLabel = "<path>", description = "Import saved devices from a blobsaver XML file.")
    File importPath;

    @Option(names = "--identifier")
    String device;
    @Option(names = "--ecid")
    String ecid;

    @Option(names = "--boardconfig")
    String boardConfig;

    @Option(names = "--apnonce")
    String apnonce;

    @Option(names = "--generator")
    String generator;

    @Option(names = "--save-path", paramLabel = "<path>",
            description = "Directory to save blobs in. Can use the following variables: " +
                    "$${DeviceIdentifier}, $${BoardConfig}, $${APNonce}, $${Generator}, $${DeviceModel}, $${ECID}, $${FullVersionString}, $${BuildID}, and $${MajorVersion}.")
    File savePath;

    @ArgGroup
    Version version;
    static class Version {
        @Option(names = "--ios-version", paramLabel = "<version>")
        String manualVersion;
        @Option(names = "--ipsw-url", paramLabel = "<url>", description = "Either a URL to an IPSW file or a build manifest. Local 'file:' URLs are accepted.")
        String manualIpswURL;

        @Option(names = {"-b", "--include-betas"})
        boolean includeBetas;
    }

    @Spec CommandSpec spec;

    @Override
    public Void call() throws TSS.TSSException, IOException, InvalidPreferencesFormatException {
        if (importPath != null) {
            Prefs.importXML(importPath);
            System.out.println("Successfully imported saved devices.");
        }
        if (saveBlobs) {
            checkArgs("identifier", "ecid", "save-path");
            var tss = new TSS.Builder()
                    .setDevice(device).setEcid(ecid).setSavePath(savePath.getCanonicalPath()).setBoardConfig(boardConfig)
                    .setManualVersion(version.manualVersion).setManualIpswURL(version.manualIpswURL).setApnonce(apnonce)
                    .setGenerator(generator).setIncludeBetas(version.includeBetas).build();
            System.out.println(tss.call());
        }
        if (removeDevice != null) {
            removeDevice.delete();
            System.out.println("Deleted " + removeDevice + ".");
        }
        if (saveDevice != null) {
            checkArgs("ecid", "save-path", "identifier");
            var saved = new Prefs.SavedDeviceBuilder(saveDevice)
                    .setIdentifier(device).setEcid(ecid).setSavePath(savePath.getCanonicalPath()).setBoardConfig(boardConfig)
                    .setApnonce(apnonce).setGenerator(generator).setIncludeBetas(version.includeBetas).save();
            System.out.println("Saved " + saved + ".");
        }
        if (enableBackground != null) {
            if (!saveBlobs) {
                System.out.println("Testing device\n");
                Background.saveBlobs(enableBackground);
            }
            enableBackground.setBackground(true);
            System.out.println("Enabled background for " + enableBackground + ".");
        }
        if (disableBackground != null) {
            disableBackground.setBackground(false);
            System.out.println("Disabled background for " + enableBackground + ".");
        }
        if (backgroundControls.startBackground) {
            Background.startBackground();
            if (Background.isBackgroundEnabled()) {
                System.out.println("A background saving task has been scheduled.");
            } else {
                throw new ExecutionException(spec.commandLine(), "Error: Unable to enable background saving.");
            }
        } else if (backgroundControls.stopBackground) {
            Background.stopBackground();
        } else if (backgroundControls.backgroundAutosave) {
            Background.saveAllBackgroundBlobs();
        }
        if (exportPath != null) {
            if (!exportPath.isFile() && !exportPath.getName().endsWith(".xml") && !exportPath.toString().startsWith("/dev")) {
                exportPath.mkdirs();
                exportPath = new File(exportPath, "blobsaver.xml");
            }
            Prefs.export(exportPath);
            System.out.println("Successfully exported saved devices.");
        }
        return null;
    }

    @SuppressWarnings("unused")
    @Command(name = "clear-app-data", description = "Remove all of blobsaver's data including saved devices.")
    void clearAppData() {
        System.out.print("Are you sure you would like to permanently clear all blobsaver data? ");
        var answer = new Scanner(System.in).nextLine();
        if (answer.toLowerCase().matches("y|ye|yes")) {
            Utils.clearAppData();
            System.out.println("The application data has been removed.");
        }
    }

    @SuppressWarnings("unused")
    @Command(name = "donate", description = "https://www.paypal.me/airsqrd")
    void donate() {
        System.out.println("You can donate at https://www.paypal.me/airsqrd or with GitHub Sponsors at https://github.com/sponsors/airsquared.");
    }

    public static class VersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() {
            String[] output = {CLI.warning, "blobsaver " + Main.appVersion, Main.copyright, null};
            try {
                var newVersion = Utils.LatestVersion.request();
                if (Main.appVersion.equals(newVersion.toString())) {
                    output[3] = "You are on the latest version.";
                } else {
                    output[3] = "New Update Available: " + newVersion + ". Update at%n https://github.com/airsquared/blobsaver/releases";
                }
            } catch (Exception e) {
                output[3] = "Unable to check for updates.";
            }

            return output;
        }
    }

    private void checkArgs(String... names) {
        var missing = new HashSet<ArgSpec>();
        for (String name : names) {
            if (!spec.commandLine().getParseResult().hasMatchedOption(name)) {
                missing.add(spec.findOption(name));
            }
        }
        if (!missing.isEmpty()) {
            String miss = missing.stream().map(OptionSpec.class::cast)
                    .map(OptionSpec::longestName).collect(Collectors.joining(", "));
            throw new MissingParameterException(spec.commandLine(), missing, "Missing required options: " + miss);
        }
    }

    private static Prefs.SavedDevice savedDeviceConverter(String name) {
        return Prefs.getSavedDevices().stream()
                .filter(savedDevice -> savedDevice.getName().equalsIgnoreCase(name))
                .findAny().orElseThrow(() -> new TypeConversionException("Must be one of " + Prefs.getSavedDevices() + "\n"));
    }

    public static int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
        boolean messageOnly = ex instanceof ExecutionException
                // if either the exception is not reportable or there is a tssLog present
                || ex instanceof TSS.TSSException e && (!e.isReportable || e.tssLog != null);
        if (messageOnly) {
            cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));

            return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : cmd.getCommandSpec().exitCodeOnExecutionException();
        }
        throw ex;
    }

    public static int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        PrintWriter err = cmd.getErr();

        // if tracing at DEBUG level, show the location of the issue
        if ("DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"))) {
            err.println(cmd.getColorScheme().stackTraceText(ex));
        }

        err.println(cmd.getColorScheme().errorText(ex.getMessage())); // bold red
        UnmatchedArgumentException.printSuggestions(ex, err);
        err.print(cmd.getHelp().fullSynopsis());

        CommandSpec spec = cmd.getCommandSpec();
        err.printf("Try '%s --help' for more information.%n", spec.qualifiedName());

        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : spec.exitCodeOnInvalidInput();
    }

    /**
     * @return the exit code
     */
    public static int launch(String... args) {
        var c = new CommandLine(new CLI())
                .setExecutionExceptionHandler(CLI::handleExecutionException)
                .setParameterExceptionHandler(CLI::handleParseException)
                .registerConverter(Prefs.SavedDevice.class, CLI::savedDeviceConverter);
        return c.execute(args);
    }
}
