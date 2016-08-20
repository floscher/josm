/*
 * Copyright 2002-2016 Drew Noakes
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
 *    https://drewnoakes.com/code/exif/
 *    https://github.com/drewnoakes/metadata-extractor
 */

package com.drew.metadata.exif;

import com.drew.lang.annotations.NotNull;

/**
 * Provides human-readable string representations of tag values stored in a {@link ExifIFD0Directory}.
 *
 * @author Drew Noakes https://drewnoakes.com
 */
public class ExifIFD0Descriptor extends ExifDescriptorBase<ExifIFD0Directory>
{
    public ExifIFD0Descriptor(@NotNull ExifIFD0Directory directory)
    {
        super(directory);
    }
}
