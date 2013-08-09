/*
 * Copyright 2002-2012 Drew Noakes
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 * More information about this project is available at:
 *
 *    http://drewnoakes.com/code/exif/
 *    http://code.google.com/p/metadata-extractor/
 */

package com.drew.metadata.exif;

import com.drew.lang.annotations.NotNull;
import com.drew.metadata.Directory;

import java.util.HashMap;

/**
 * Describes tags specific to Sony cameras that use the Sony Type 6 makernote tags.
 *
 * @author Drew Noakes http://drewnoakes.com
 */
public class SonyType6MakernoteDirectory extends Directory
{
    public static final int TAG_MAKER_NOTE_THUMB_OFFSET = 0x0513;
    public static final int TAG_MAKER_NOTE_THUMB_LENGTH = 0x0514;
    public static final int TAG_UNKNOWN_1 = 0x0515;
    public static final int TAG_MAKER_NOTE_THUMB_VERSION = 0x2000;

    @NotNull
    protected static final HashMap<Integer, String> _tagNameMap = new HashMap<Integer, String>();

    static
    {
        _tagNameMap.put(TAG_MAKER_NOTE_THUMB_OFFSET, "Maker Note Thumb Offset");
        _tagNameMap.put(TAG_MAKER_NOTE_THUMB_LENGTH, "Maker Note Thumb Length");
        _tagNameMap.put(TAG_UNKNOWN_1, "Sony-6-0x0203");
        _tagNameMap.put(TAG_MAKER_NOTE_THUMB_VERSION, "Maker Note Thumb Version");
    }

    public SonyType6MakernoteDirectory()
    {
        this.setDescriptor(new SonyType6MakernoteDescriptor(this));
    }

    @NotNull
    public String getName()
    {
        return "Sony Makernote";
    }

    @NotNull
    protected HashMap<Integer, String> getTagNameMap()
    {
        return _tagNameMap;
    }
}
