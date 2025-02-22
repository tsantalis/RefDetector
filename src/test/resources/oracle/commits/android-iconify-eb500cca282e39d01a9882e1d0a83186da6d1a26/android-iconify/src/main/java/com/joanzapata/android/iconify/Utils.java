/**
 * Copyright 2013 Joan Zapata
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * It uses FontAwesome font, licensed under OFL 1.1, which is compatible
 * with this library's license.
 *
 *     http://scripts.sil.org/cms/scripts/render_download.php?format=file&media_id=OFL_plaintext&filename=OFL.txt
 */
package com.joanzapata.android.iconify;

import android.R;
import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.util.TypedValue.*;
import static com.joanzapata.android.iconify.Iconify.IconValue;

class Utils {

    public static final String ICON_FONT_FOLDER = "icon_tmp";

    private Utils() {
        // Prevents instantiation
    }

    static int convertDpToPx(Context context, float dp) {
        return (int) applyDimension(COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    static boolean isEnabled(int[] stateSet) {
        for (int state : stateSet)
            if (state == R.attr.state_enabled)
                return true;
        return false;
    }

    static File resourceToFile(Context context, String resourceName) throws IOException {
        File f = null;
        if(context.getFilesDir() == null)
            f = new File(context.getCacheDir(), ICON_FONT_FOLDER);
        else
            f = new File(context.getFilesDir(), ICON_FONT_FOLDER);
        if (!f.exists())
            if (!f.mkdirs()) {
                Log.e(Iconify.TAG, "Font folder creation failed");
                throw new IllegalStateException("Cannot create Iconify font destination folder");
            }

        String filename = resourceName;
        int separatorIndex = resourceName.indexOf(':');
        if (separatorIndex != -1) {
            filename = resourceName.substring(separatorIndex + 1);
        }

        File outPath = new File(f, filename);
        if (outPath.exists()) return outPath;

        InputStream inputStream = null;
        try {
            if (resourceName.startsWith("asset:")) {
                inputStream = context.getAssets().open(filename);
                copy(inputStream, outPath);
                return outPath;
            }

            inputStream = Iconify.class.getClassLoader().getResourceAsStream(filename);
            copy(inputStream, outPath);
            return outPath;
        } finally {
            closeQuietly(inputStream);
        }
    }

    private static void copy(InputStream inputStream, File outputFile) throws IOException {
        BufferedOutputStream bos = null;
        try {
            byte[] buffer = new byte[inputStream.available()];
            bos = new BufferedOutputStream(new FileOutputStream(outputFile));
            int l;
            while ((l = inputStream.read(buffer)) > 0) {
                bos.write(buffer, 0, l);
            }
        } finally {
            closeQuietly(bos);
            closeQuietly(inputStream);
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Don't care
            }
        }
    }

    public static StringBuilder replaceIcons(StringBuilder text) {
        return replaceIcons(IconValue.fa_500px, text);
    }

    /**
     * Replace icons placeholder by the appropriate character
     *
     * @param icon an arbitrary instance of {@link BaseIconValue} in order to have a reference on the icon font to use
     * @param text the text to process
     * @return the processed text
     */
    public static <T extends Enum<T> & BaseIconValue> StringBuilder replaceIcons(T icon, StringBuilder text) {
        int startIndex = text.indexOf("{" + icon.getPrefix());
        if (startIndex == -1) {
            return text;
        }

        int endIndex = text.indexOf("}", startIndex) + 1;

        String iconString = text.substring(startIndex + 1, endIndex - 1);
        try {
            BaseIconValue value = icon.iconFrom(iconString.replaceAll("-", "_"));
            String iconValue = String.valueOf(value.character());

            text = text.replace(startIndex, endIndex, iconValue);
            return replaceIcons(icon, text);
        } catch (IllegalArgumentException e) {
            Log.w(Iconify.TAG, "Wrong icon name: " + iconString);
            return text;
        }
    }
}
