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

public class Libfragmentzip {

    public static int downloadFile(String url, String remotePath, String savePath) {
        Pointer fragmentzip = open(url);
        int err = downloadFile(fragmentzip, remotePath, savePath);
        close(fragmentzip);
        return err;
    }

    public static Pointer open(String url) {
        return fragmentzip_open(url);
    }

    public static int downloadFile(Pointer fragmentzip_t, String remotePath, String savePath) {
        return fragmentzip_download_file(fragmentzip_t, remotePath, savePath, null);
    }

    public static void close(Pointer fragmentzip_t) {
        fragmentzip_close(fragmentzip_t);
    }

    /**
     * Native declaration:
     * <code>fragmentzip_t *fragmentzip_open(const char *url);</code>
     */
    private static native Pointer fragmentzip_open(String url);

    /**
     * Native declaration:
     * <code>int fragmentzip_download_file(fragmentzip_t *info, const char *remotepath, const char *savepath, fragmentzip_process_callback_t callback);</code>
     */
    private static native int fragmentzip_download_file(Pointer fragmentzip_t, String remotePath, String savePath,
                                                        Pointer fragmentzip_process_callback_t);

    private static native void fragmentzip_close(Pointer fragmentzip_t);

    static {
        Native.register(Libfragmentzip.class, "fragmentzip");
    }
}
