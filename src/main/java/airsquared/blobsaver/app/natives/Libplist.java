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

package airsquared.blobsaver.app.natives;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class Libplist {

    public static void getStringVal(Pointer plist, PointerByReference value) {
        plist_get_string_val(plist, value);
    }

    public static void free(Pointer plist) {
        plist_free(plist);
    }

    public static void toXml(Pointer plist, PointerByReference plist_xml, PointerByReference length) {
        plist_to_xml(plist, plist_xml, length);
    }

    public static native Pointer plist_new_array();

    public static native void plist_array_append_item(Pointer plist_t, Pointer plist_t_item);

    public static native Pointer plist_new_string(String val);

    public static native Pointer plist_dict_get_item(Pointer plist, String key);

    public static native Pointer plist_get_data_ptr(Pointer plist, IntByReference length);

    private static native void plist_get_string_val(Pointer plist, PointerByReference value);

    private static native void plist_free(Pointer plist);

    private static native void plist_to_xml(Pointer plist, PointerByReference plist_xml, PointerByReference length);

    static {
        Native.register(Libplist.class, "plist");
    }
}
