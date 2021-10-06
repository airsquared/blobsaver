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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest extends BlobsaverTest {

    private static final String ipswUrl = "http://updates-http.cdn-apple.com/2020WinterFCS/fullrestores/061-20302/454ACB32-D3F6-4984-92CB-27C8FA368165/iPhone11,8,iPhone12,1_13.4_17E255_Restore.ipsw";

    @Test
    public void extractBuildManifest() throws IOException {
        Path buildManifest = Utils.extractBuildManifest(ipswUrl);

        try (var reader = Files.newBufferedReader(buildManifest)) {
            assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", reader.readLine());
            assertEquals("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">", reader.readLine());
            assertEquals("<plist version=\"1.0\">", reader.readLine());
            assertEquals("<dict>", reader.readLine());
        }
    }

    @Test
    @DisabledOnOs(value = OS.MAC, disabledReason = "Randomly fails in CI on macOS")
    public void checkForUpdates() throws IOException {
        var version = Utils.LatestVersion.request();
        assertTrue(version.changelog().contains("Changelog"), "Changelog does not contain 'Changelog'");
    }

}
