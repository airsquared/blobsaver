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

package airsquared.blobsaver.app.natives;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;


/**
 * This class provides access to functions in the native library libimobiledevice.
 * <p>
 * See the libimobiledevice docs for
 * <a href="https://www.libimobiledevice.org/docs/html/lockdown_8h.html">lockdown.h</a>
 * and
 * <a href="https://www.libimobiledevice.org/docs/html/libimobiledevice_8h.html">libimobiledevice.h</a>
 * for information on the native functions. For information on the libplist functions,
 * look at the source code(files plist.c and xplist.c).
 */
public class Libimobiledevice {
    public static native int lockdownd_get_value(Pointer client, Pointer domain, String key, PointerByReference value);

    public static native int lockdownd_enter_recovery(Pointer client);

    public static native int idevice_new(PointerByReference device, Pointer udid);

    public static native int lockdownd_client_new(Pointer device, PointerByReference client, String label);

    public static native int lockdownd_pair(Pointer lockdownd_client, Pointer lockdownd_pair_record);

    public static native void lockdownd_client_free(Pointer client);

    public static native void idevice_free(Pointer idevice);

    static {
        Native.register(Libimobiledevice.class, "imobiledevice");
    }
}
