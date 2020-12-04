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

import airsquared.blobsaver.app.natives.Libirecovery;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("disable tests unless device is connected")
public class LibimobiledeviceTest extends BlobsaverTest {

    @Test
    public void getECID() throws LibimobiledeviceUtil.LibimobiledeviceException {
        long ecid = LibimobiledeviceUtil.getECID(false);
        assertEquals(0L, ecid);  // change to your device's ecid
    }

    @Test
    public void enterRecovery() throws LibimobiledeviceUtil.LibimobiledeviceException {
        LibimobiledeviceUtil.enterRecovery(false);
    }

    @Test
    public void getNonce() {
        PointerByReference irecvClient = new PointerByReference();
        assertEquals(0, Libirecovery.open(irecvClient));
        Libirecovery.irecv_device_info deviceInfo = Libirecovery.getDeviceInfo(irecvClient.getValue());
        StringBuilder apnonce = new StringBuilder();
        System.out.println("deviceInfo.ap_nonce = " + deviceInfo.ap_nonce);
        for (byte b : ((Pointer) deviceInfo.readField("ap_nonce")).getByteArray(0, deviceInfo.ap_nonce_size)) {
            apnonce.append(String.format("%02x", b));
        }
//        assertEquals("", apnonce.toString());                                   // change to your device's apnonce
        System.out.println("apnonce = " + apnonce);
        assertEquals(0, Libirecovery.setEnv(irecvClient.getValue(), "auto-boot", "true"));
        assertEquals(0, Libirecovery.saveEnv(irecvClient.getValue()));
        assertEquals(0, Libirecovery.reboot(irecvClient.getValue()));
        assertEquals(0, Libirecovery.close(irecvClient.getValue()));
    }

}
