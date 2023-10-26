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

import airsquared.blobsaver.app.LibimobiledeviceUtil.LibimobiledeviceException;
import picocli.CommandLine;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.stream.Collectors;

@Command(name = "blobsaver", versionProvider = CLI.VersionProvider.class, mixinStandardHelpOptions = true,
        sortOptions = false, usageHelpAutoWidth = true, sortSynopsis = false, abbreviateSynopsis = true,
        optionListHeading = "  You can separate options and their parameters with either a space or '='.%n",
        commandListHeading = "Commands:%n  See @|bold,white blobsaver help [COMMAND]|@ for more information about each command.%n%n",
        subcommands = HelpCommand.class)
public class CLI implements Callable<Void> {

    @Option(names = {"-s", "--save-blobs"})
    boolean saveBlobs;

    @Option(names = "--save-device", paramLabel = "<name>", description = "Create a new saved device.")
    String saveDevice;

    @Option(names = "--remove-device", paramLabel = "<Saved Device>", description = "Remove a saved device.")
    Prefs.SavedDevice removeDevice;

    @Option(names = "--enable-background", paramLabel = "<Saved Device>",
            description = "Enable background saving for a device.%nUse '--start-background-service' once devices are added.")
    Prefs.SavedDevice enableBackground;

    @Option(names = "--disable-background", paramLabel = "<Saved Device>",
            description = "Disable background saving for a device.")
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

    @Option(names = "--export", paramLabel = "<path>", description = "Export saved devices in XML format to a directory.")
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
                    "$${DeviceIdentifier}, $${BoardConfig}, $${APNonce}, $${Generator}, $${DeviceModel}, $${ECID}, $${FullVersionString}, $${BuildID}, $${MajorVersion} and $${Name} (if using a saved device).")
    File savePath;

    @ArgGroup
    Version version = new Version();
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
            System.out.println(success("Successfully imported saved devices."));
        }
        if (saveBlobs) {
            checkArgs("identifier", "ecid", "save-path");
            var tss = new TSS.Builder()
                    .setDevice(device).setEcid(ecid).setSavePath(savePath.getCanonicalPath()).setBoardConfig(boardConfig)
                    .setManualVersion(version.manualVersion).setManualIpswURL(version.manualIpswURL).setApnonce(apnonce)
                    .setGenerator(generator).setIncludeBetas(version.includeBetas).build();
            System.out.println(success("\n" + tss.call()));
        }
        if (removeDevice != null) {
            removeDevice.delete();
            System.out.println(STR."Deleted \{removeDevice}.");
        }
        if (saveDevice != null) {
            checkArgs("ecid", "save-path", "identifier");
            var saved = new Prefs.SavedDeviceBuilder(saveDevice)
                    .setIdentifier(device).setEcid(ecid).setSavePath(savePath.getCanonicalPath()).setBoardConfig(boardConfig)
                    .setApnonce(apnonce).setGenerator(generator).setIncludeBetas(version.includeBetas).save();
            System.out.println(success(STR."Saved \{saved}."));
        }
        if (enableBackground != null) {
            if (!saveBlobs) {
                System.out.println("Testing device\n");
                Background.saveBlobs(enableBackground);
            }
            enableBackground.setBackground(true);
            System.out.println(success(STR."\nEnabled background for \{enableBackground}."));
        }
        if (disableBackground != null) {
            disableBackground.setBackground(false);
            System.out.println(STR."Disabled background for \{enableBackground}.");
        }
        if (backgroundControls.startBackground) {
            Background.startBackground();
            if (Background.isBackgroundEnabled()) {
                System.out.println(success("A background saving task has been scheduled."));
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
            if (exportPath.toString().startsWith("/dev")) {
                System.err.println(success("Successfully exported saved devices."));
            } else {
                System.out.println(success("Successfully exported saved devices."));
            }
        }
        return null;
    }

    @Command(name = "clear-app-data", description = "Remove all of blobsaver's data including saved devices.")
    void clearAppData() {
        System.out.print("Are you sure you would like to permanently clear all blobsaver data? ");
        var answer = new Scanner(System.in).nextLine();
        if (answer.matches("(?i)y|ye|yes")) {
            Utils.clearAppData();
            System.out.println("The application data has been removed.");
        }
    }

    @Command(description = "Help support me and the development of this application! (I'm only a student)")
    void donate() {
        System.out.println("""
                You can donate with GitHub Sponsors at
                https://github.com/sponsors/airsquared
                or with PayPal at
                https://www.paypal.me/airsqrd.
                Thank you!""");
    }

    @Command(name = "read-info", description = "Reads ECID, identifier, board configuration, device type, and device name.")
    void readInfo() throws LibimobiledeviceException {
        long ecid = LibimobiledeviceUtil.getECID();
        System.out.println("ECID (hex): " + Long.toHexString(ecid).toUpperCase());
        System.out.println("ECID (dec): " + ecid);
        String identifier = LibimobiledeviceUtil.getDeviceModelIdentifier();
        System.out.println("Identifier: " + identifier);
        System.out.println("Board Configuration: " + LibimobiledeviceUtil.getBoardConfig()
                + (Devices.doesRequireBoardConfig(identifier) ? " (Required)" : " (Not Required)"));
        System.out.println("Device Type: " + Devices.getDeviceType(identifier));
        System.out.println("Device Name: " + Devices.identifierToModel(identifier));

        Analytics.readInfo();
    }

    @Command(name = "read-apnonce", description = "Enters recovery mode to read APNonce, and freezes it if needed.")
    void readAPNonce(@Option(names = "--force-new", description = "Generate a new APNonce, even if it is already frozen.") boolean forceNew) throws LibimobiledeviceException {
        var task = new LibimobiledeviceUtil.GetApnonceTask(forceNew) {
            @Override
            protected void updateMessage(String message) {
                System.out.println(message);
            }
        };
        task.call();
        System.out.println("APNonce: " + task.getApnonceResult());
        System.out.println("Generator: " + task.getGeneratorResult());
    }

    @Command(name = "exit-recovery")
    void exitRecovery() throws LibimobiledeviceException {
        LibimobiledeviceUtil.exitRecovery();
        Analytics.exitRecovery();
    }

    @Command(description = "Read a key from lockdownd.", showDefaultValues = true)
    void read(@Parameters(paramLabel = "<key>") String key,
              @Parameters(paramLabel = "[output-type]", defaultValue = "xml", description = "Can be any of [xml, string, integer, base64]") PlistOutputType type) throws LibimobiledeviceException {
        var plist = LibimobiledeviceUtil.getLockdownValuePlist(key);
        System.out.println(switch (type) {
            case XML -> LibimobiledeviceUtil.plistToXml(plist);
            case STRING -> LibimobiledeviceUtil.getPlistString(plist);
            case INTEGER -> LibimobiledeviceUtil.getPlistLong(plist);
            case BASE64 -> Base64.getEncoder().encodeToString(LibimobiledeviceUtil.plistDataBytes(plist));
        });
    }
    enum PlistOutputType {XML, STRING, INTEGER, BASE64}

    public static class VersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() {
            String[] output = {"blobsaver " + Main.appVersion, Main.copyright, "Licence: GNU GPL v3.0-only", null,
                    "%nHomepage: https://github.com/airsquared/blobsaver"};
            try {
                var newVersion = Utils.LatestVersion.request();
                if (Main.appVersion.equals(newVersion.version())) {
                    output[3] = "%nYou are on the latest version.";
                } else {
                    output[3] = STR."%nNew Update Available: \{newVersion}. Update using your package manager or at%n https://github.com/airsquared/blobsaver/releases";
                }
            } catch (Exception e) {
                output[3] = "%nUnable to check for updates.";
            }

            return output;
        }
    }

    private static String success(String s) {
        return Help.Ansi.AUTO.string(STR."@|bold,green \{s}|@");
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

    private static int handleExecutionException(Exception ex, CommandLine cmd, ParseResult __) throws Exception {
        boolean messageOnly = ex instanceof ExecutionException
                // if either the exception is not reportable or there is a tssLog present
                || ex instanceof TSS.TSSException e && (!e.isReportable || e.tssLog != null)
                || ex instanceof LibimobiledeviceException;
        if (messageOnly) {
            cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));

            return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : cmd.getCommandSpec().exitCodeOnExecutionException();
        }
        throw ex;
    }

    private static int handleParameterException(ParameterException ex, String[] __) {
        CommandLine cmd = ex.getCommandLine();
        CommandSpec spec = cmd.getCommandSpec();
        boolean isRootCommand = spec == spec.root();
        PrintWriter err = cmd.getErr();

        // if tracing at DEBUG level, show the location of the issue
        if ("DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"))) {
            err.println(cmd.getColorScheme().stackTraceText(ex));
        }

        err.println(cmd.getColorScheme().errorText(ex.getMessage())); // bold red
        UnmatchedArgumentException.printSuggestions(ex, err);
        if (isRootCommand) {
            err.print(cmd.getHelp().fullSynopsis());
            err.printf("Try '%s help' for more information.%n", spec.name());
        } else {
            cmd.usage(err); // print full help
        }

        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : spec.exitCodeOnInvalidInput();
    }

    /**
     * @return the exit code
     */
    public static int launch(String... args) {
        Analytics.startup();
        var c = new CommandLine(new CLI())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionExceptionHandler(CLI::handleExecutionException)
                .setParameterExceptionHandler(CLI::handleParameterException)
                .registerConverter(Prefs.SavedDevice.class, CLI::savedDeviceConverter);
        if (args.length == 0) { // happens when environment variable $BLOBSAVER_CLI_ONLY is set to some value
            args = new String[]{"help"};
        }
        return c.execute(args);
    }

    /**
     * Private Constructor; Use {@link CLI#launch(String...)} instead
     */
    private CLI() {}
}
