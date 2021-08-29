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

import airsquared.blobsaver.app.natives.Libimobiledevice;
import airsquared.blobsaver.app.natives.Libirecovery;
import airsquared.blobsaver.app.natives.Libplist;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.junit.jupiter.api.Test;

public class NativesTest extends BlobsaverTest {

    @Test
    public void loadPlist() {
        Libplist.free(Libplist.newArray());
    }

    @Test
    public void loadIdevice() {
        PointerByReference ref = new PointerByReference();
        Libimobiledevice.idevice_new(ref, Pointer.NULL);
        Libimobiledevice.idevice_free(ref.getValue());
    }

    @Test
    public void loadRecovery() {
        PointerByReference ref = new PointerByReference();
        Libirecovery.open(ref);
        Libirecovery.close(ref.getValue());
    }
}
