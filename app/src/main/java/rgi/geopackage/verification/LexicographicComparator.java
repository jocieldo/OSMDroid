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

package rgi.geopackage.verification;

import java.util.Comparator;
import java.util.function.Predicate;

/**
 * @author Luke Lambert
 */
@SuppressWarnings("ComparatorNotSerializable")
public final class LexicographicComparator implements Comparator<String> {
    @Override
    public int compare(final String s1, final String s2) {
        int str1Index = 0;
        int str2Index = 0;

        final int s1Length = s1.length();
        final int s2Length = s2.length();

        while (str1Index < s1Length && str2Index < s2Length) {
            final String chunk1 = getChunk(s1, s1Length, str1Index);
            final String chunk2 = getChunk(s2, s2Length, str2Index);

            // If both chunks contain numeric characters, sort them numerically
            int result;

            if (Character.isDigit(chunk1.charAt(0)) &&
                    Character.isDigit(chunk2.charAt(0))) {
                // Compare chunks by length
                final int thisChunkLength = chunk1.length();
                result = Integer.compare(thisChunkLength, chunk2.length());

                // If equal, the first different number counts
                if (result == 0) {
                    for (int i = 0; i < thisChunkLength; ++i) {
                        result = Integer.compare(chunk1.charAt(i), chunk2.charAt(i));
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            } else {
                result = chunk1.compareTo(chunk2);
            }

            if (result != 0) {
                return result;
            }

            str1Index += chunk1.length();
            str2Index += chunk2.length();
        }

        return Integer.compare(s1Length, s2Length);
    }

    private static String getChunk(final String str,
                                   final int strLength,
                                   final int startIndex) {
        if (startIndex < strLength) {
            final char firstCharacter = str.charAt(startIndex);
            final Predicate<Character> predicate = Character.isDigit(firstCharacter) ? Character::isDigit
                    : ((Predicate<Character>) Character::isDigit).negate();

            final int endIndex = firstIndexNotOf(str,
                    strLength,
                    startIndex,
                    predicate);

            return str.substring(startIndex, endIndex);
        }

        return "";
    }

    private static int firstIndexNotOf(final CharSequence charSequence,
                                       final int charSequenceLength,
                                       final int beginIndex,
                                       final Predicate<Character> predicate) {
        int index = beginIndex;

        while (index < charSequenceLength && predicate.test(charSequence.charAt(index))) {
            ++index;
        }

        return index;
    }
}
