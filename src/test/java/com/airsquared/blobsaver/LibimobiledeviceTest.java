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

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.junit.Test;

import static com.airsquared.blobsaver.Libimobiledevice.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LibimobiledeviceTest {

    @Test
    public void getECIDNative() {
        //noinspection ConstantConditions
        if (true) return; // disable test unless device is connected
        PointerByReference device = new PointerByReference();
        assertEquals(0, idevice_new(device, Pointer.NULL));
        PointerByReference client = new PointerByReference();
        assertEquals(0, lockdownd_client_new(device.getValue(), client, "blobsaver"));
        assertEquals(0, lockdownd_pair(client.getValue(), Pointer.NULL));
        PointerByReference plist_value = new PointerByReference();
        assertEquals(0, lockdownd_get_value(client.getValue(), Pointer.NULL, "UniqueChipID", plist_value));
        PointerByReference xml_doc = new PointerByReference();
        Libplist.toXml(plist_value.getValue(), xml_doc, new PointerByReference());
        String ecidString = xml_doc.getValue().getString(0, "UTF-8");
        assertNotNull(ecidString);
        ecidString = ecidString.substring(ecidString.indexOf("<integer>") + "<integer>".length(), ecidString.indexOf("</integer>"));
        long ecid = Long.parseLong(ecidString);
//         assertEquals(0L, ecid);                                                  // change to your device's ecid
    }


    @Test
    public void getKeyFromConnectedDeviceTest() {
        //noinspection ConstantConditions
        if (true) return; // disable test unless device is connected
        System.out.println(getKeyFromConnectedDevice("UniqueChipID", PlistType.INTEGER, false));
    }

    @Test
    public void enterRecovery() {
        //noinspection ConstantConditions
        if (true) return; // disable test unless device is connected
        PointerByReference device = new PointerByReference();
        assertEquals(0, idevice_new(device, Pointer.NULL));
        PointerByReference client = new PointerByReference();
        assertEquals(0, lockdownd_client_new(device.getValue(), client, "blobsaver"));
        assertEquals(0, lockdownd_pair(client.getValue(), Pointer.NULL));
        assertEquals(0, lockdownd_enter_recovery(client.getValue()));
        idevice_free(device.getValue());
        lockdownd_client_free(client.getValue());
    }

    @Test
    public void getNonce() {
        //noinspection ConstantConditions
        if (true) return; // disable test unless device is connected
        PointerByReference irecvClient = new PointerByReference();
        assertEquals(0, Libirecovery.irecv_open_with_ecid(irecvClient, 0));
        Libirecovery.irecv_device_info deviceInfo = Libirecovery.irecv_get_device_info(irecvClient.getValue());
        StringBuilder apnonce = new StringBuilder();
        System.out.println("deviceInfo.ap_nonce = " + deviceInfo.ap_nonce);
        for (byte b : ((Pointer) deviceInfo.readField("ap_nonce")).getByteArray(0, deviceInfo.ap_nonce_size)) {
            apnonce.append(String.format("%02x", b));
        }
//        assertEquals("", apnonce.toString());                                   // change to your device's apnonce
        System.out.println("apnonce = " + apnonce);
        assertEquals(0, Libirecovery.irecv_setenv(irecvClient.getValue(), "auto-boot", "true"));
        assertEquals(0, Libirecovery.irecv_saveenv(irecvClient.getValue()));
        assertEquals(0, Libirecovery.irecv_reboot(irecvClient.getValue()));
        assertEquals(0, Libirecovery.irecv_close(irecvClient.getValue()));
    }

}
