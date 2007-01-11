package org.openstreetmap.josm.gui.annotation;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import javax.swing.Action;

import junit.framework.TestCase;

import org.openstreetmap.josm.gui.annotation.AnnotationPreset.Check;
import org.openstreetmap.josm.gui.annotation.AnnotationPreset.Combo;
import org.openstreetmap.josm.gui.annotation.AnnotationPreset.Key;
import org.openstreetmap.josm.gui.annotation.AnnotationPreset.Label;
import org.openstreetmap.josm.gui.annotation.AnnotationPreset.Text;

public class AnnotationPresetTest extends TestCase {

	public void testAnnotationPresetLoads() throws Exception {
		InputStream in = getClass().getResourceAsStream("annotation-test.xml");
		List<AnnotationPreset> all = AnnotationPreset.readAll(in);

		assertEquals(1, all.size());
		AnnotationPreset a = all.get(0);
		assertEquals("Highway", a.getValue(Action.NAME));
		Field dataField = a.getClass().getDeclaredField("data");
		dataField.setAccessible(true);
		List<?> data = (List<?>)dataField.get(a);
		assertEquals(5, data.size());

		Label label = (Label)data.get(0);
		assertEquals("Inserting a highway in UK", label.text);

		Text text = (Text)data.get(1);
		assertEquals("name", text.key);
		assertEquals("Highway (e.g. M3)", text.text);
		assertFalse(text.delete_if_empty);
		assertNull(text.default_);

		Combo combo = (Combo)data.get(2);
		assertEquals("highway", combo.key);
		assertEquals("Type", combo.text);
		assertEquals("major,minor", combo.values);
		assertTrue(combo.delete_if_empty);
		assertTrue(combo.editable);
		assertNull(combo.default_);

		Check check = (Check)data.get(3);
		assertEquals("oneway", check.key);
		assertEquals("Oneway", check.text);
		assertTrue(check.default_);

		Key key = (Key)data.get(4);
		assertEquals("class", key.key);
		assertEquals("highway", key.value);
	}
}
