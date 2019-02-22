/*
 * Copyright (c) 2019  airsquared
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
        PointerByReference device = new PointerByReference();
        assertEquals(0, idevice_new(device, Pointer.NULL));
        PointerByReference client = new PointerByReference();
        assertEquals(0, lockdownd_client_new(device.getValue(), client, "blobsaver"));
        assertEquals(0, lockdownd_pair(client.getValue(), Pointer.NULL));
        PointerByReference plist_value = new PointerByReference();
        assertEquals(0, lockdownd_get_value(client.getValue(), Pointer.NULL, "UniqueChipID", plist_value));
        PointerByReference xml_doc = new PointerByReference();
        plist_to_xml(plist_value.getValue(), xml_doc, new PointerByReference());
        String ecidString = xml_doc.getValue().getString(0, "UTF-8");
        assertNotNull(ecidString);
        ecidString = ecidString.substring(ecidString.indexOf("<integer>") + "<integer>".length(), ecidString.indexOf("</integer>"));
        long ecid = Long.valueOf(ecidString);
        // fill your real ecid here:
        // assertEquals(0L, ecid);
    }

    @Test
    public void getKeyFromConnectedDeviceTest() {
        System.out.println(getKeyFromConnectedDevice("UniqueChipID", PlistType.INTEGER, false));
    }

}
