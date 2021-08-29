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

import airsquared.blobsaver.app.natives.NativeUtils.CFunctionName;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

public class Libplist {

    @CFunctionName("plist_new_array")
    public static native Pointer newArray();

    @CFunctionName("plist_array_append_item")
    public static native void arrayAppendItem(Pointer plist_t, Pointer plist_t_item);

    @CFunctionName("plist_new_string")
    public static native Pointer newString(String val);

    @CFunctionName("plist_dict_get_item")
    public static native Pointer dictGetItem(Pointer plist, String key);

    @CFunctionName("plist_get_data_ptr")
    public static native Pointer getDataPtr(Pointer plist, IntByReference length);

    @CFunctionName("plist_get_string_val")
    public static native void getStringVal(Pointer plist, PointerByReference value);

    @CFunctionName("plist_get_uint_val")
    public static native void getUintVal(Pointer plist, LongByReference value);

    @CFunctionName("plist_free")
    public static native void free(Pointer plist);

    @CFunctionName("plist_to_xml")
    public static native void toXml(Pointer plist, PointerByReference plist_xml, PointerByReference length);

    static {
        NativeUtils.register(Libplist.class, "plist");
    }
}
