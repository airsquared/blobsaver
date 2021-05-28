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

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

final class NativeUtils {

    private static final Map<String, ?> libraryOptions = Map.of(Library.OPTION_CLASSLOADER, NativeUtils.class.getClassLoader(),
            Library.OPTION_FUNCTION_MAPPER, (FunctionMapper) (lib, method) ->
                    method.isAnnotationPresent(CFunctionName.class) ?
                            method.getAnnotation(CFunctionName.class).value() : method.getName());

    /**
     * Same exact implementation as Native.register(Class, String), except it
     * also includes a function mapper for methods annotated with @CFunctionName
     */
    static void register(Class<?> cls, String libName) {
        Native.register(cls, NativeLibrary.getInstance(libName, libraryOptions));
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface CFunctionName {
        String value();
    }
}
