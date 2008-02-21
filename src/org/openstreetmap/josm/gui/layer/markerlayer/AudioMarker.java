// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MapView;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Marker class with audio playback capability.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class AudioMarker extends ButtonMarker {

	private URL audioUrl;	
	private static AudioMarker recentlyPlayedMarker = null;
	public  double syncOffset;
	
	/**
	 * Verifies the parameter whether a new AudioMarker can be created and return
	 * one or return <code>null</code>.
	 */
	public static AudioMarker create(LatLon ll, String text, String url, MarkerLayer parentLayer, double time, double offset) {
		try {
			return new AudioMarker(ll, text, new URL(url), parentLayer, time, offset);
		} catch (Exception ex) {
			return null;
		}
	}

	private AudioMarker(LatLon ll, String text, URL audioUrl, MarkerLayer parentLayer, double time, double offset) {
		super(ll, text, "speech.png", parentLayer, time, offset);
		this.audioUrl = audioUrl;
		this.syncOffset = 0.0;
	}

	@Override public void actionPerformed(ActionEvent ev) {
		play();
	}

	public static AudioMarker recentlyPlayedMarker() {
		return recentlyPlayedMarker;
	}
	
	public URL url() {
		return audioUrl;
	}
		
	/**
	 * Starts playing the audio associated with the marker: used in response to pressing
	 * the marker as well as indirectly 
	 *
	 */
	public void play() {
		try {
			// first enable tracing the audio along the track
			if (Main.pref.getBoolean("marker.traceaudio") && parentLayer != null) {
				parentLayer.traceAudio();
			}

			AudioPlayer.play(audioUrl, offset + syncOffset);
			recentlyPlayedMarker = this;
		} catch (Exception e) {
			AudioPlayer.audioMalfunction(e);
		}
	}

	public void adjustOffset(double adjustment) {
		syncOffset = adjustment; // added to offset may turn out negative, but that's ok
	}
}
