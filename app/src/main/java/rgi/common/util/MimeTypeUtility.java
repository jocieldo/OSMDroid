/* The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package rgi.common.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import other.MimeType;
import other.MimeTypeParseException;

/**
 * Utility to simplify the creation and interaction with sets of
 * {@link MimeType}s
 *
 * @author Luke Lambert
 */
public class MimeTypeUtility {
    /**
     * Create a set of {@link MimeType} objects from their corresponding string
     * designations
     *
     * @param types Mime type strings
     * @return A set of MimeType objects
     */
    public static Set<MimeType> createMimeTypeSet(final String... types) {
        if (types == null) {
            throw new IllegalArgumentException("The mime type strings cannot be null.");
        }

        final Set<MimeType> imageFormats = new HashSet<>();

        for (final String type : types) {
            try {
                imageFormats.add(new MimeType(type));
            } catch (final MimeTypeParseException | NullPointerException ex) {
                // This method was specifically created to avoid checked exceptions
            }
        }
        return imageFormats;
    }

    /**
     * Checks if a specific {@link MimeType} is in a collection of mime types
     *
     * @param mimeTypes A collection of {@link MimeType}s
     * @param mimeType  The mime type to test
     * @return True if the mime type is in the collection
     */
    public static boolean contains(final Collection<MimeType> mimeTypes, final MimeType mimeType) {
        if (mimeTypes == null) {
            throw new IllegalArgumentException("The collection of mimeTypes cannot be null.");
        }

        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType cannot be null.");
        }

        return mimeTypes.stream().filter(format -> format != null).anyMatch(allowedImageOutputFormat -> allowedImageOutputFormat.match(mimeType));
    }
}
