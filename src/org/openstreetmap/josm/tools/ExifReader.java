package org.openstreetmap.josm.tools;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * Read out exif file information from a jpeg file
 * @author Imi
 */
public class ExifReader {

	@SuppressWarnings("unchecked") public static Date readTime(File filename) {
		try {
	        Metadata metadata = JpegMetadataReader.readMetadata(filename);
	        for (Iterator<Directory> dirIt = metadata.getDirectoryIterator(); dirIt.hasNext();) {
	            for (Iterator<Tag> tagIt = dirIt.next().getTagIterator(); tagIt.hasNext();) {
	                Tag tag = tagIt.next();
	                if (tag.getTagType() == 0x132 || tag.getTagType() == 0x9003 || tag.getTagType() == 0x9004)
	                	return DateParser.parse(tag.getDescription());
	            }
	        }
        } catch (Exception e) {
	        e.printStackTrace();
        }
		return null;
	}
}
