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

import com.sun.javafx.PlatformUtil;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;

public class BlobsaverTest {

    @BeforeEach
    public void setJNALibraryPath() {
        if (!PlatformUtil.isMac() && !PlatformUtil.isWindows()) {
            return;
        }
        File path = Main.jarDirectory.getParentFile().getParentFile();
        if (path.getName().equals("build")) {
            path = path.getParentFile();
        }
        path = new File(path, PlatformUtil.isMac() ? "dist/macos/Frameworks" : "dist/windows/lib");
        System.out.println("path = " + path.getAbsolutePath());
        System.setProperty("jna.boot.library.path", path.getAbsolutePath()); // path for jnidispatch lib
        System.setProperty("jna.library.path", path.getAbsolutePath());
        // disable getting library w/ auto unpacking / classpath since it will never be in jar/classpath
        System.setProperty("jna.noclasspath", "true");
        System.setProperty("jna.nounpack", "true");
    }
}
