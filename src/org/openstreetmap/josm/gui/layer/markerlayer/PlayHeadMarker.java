// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.PlayHeadDragMode;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.tools.AudioPlayer;

/**
 * Singleton marker class to track position of audio.
 * 
 * @author David Earl<david@frankieandshadow.com>
 *
 */
public class PlayHeadMarker extends Marker {

	private Timer timer = null;
	private double animationInterval = 0.0; // seconds
	// private Rectangle audioTracer = null;
	// private Icon audioTracerIcon = null;
	static private PlayHeadMarker playHead = null;
	private MapMode oldMode = null;
	private EastNorth oldEastNorth;
	private boolean enabled;
	private boolean wasPlaying = false;
	private int dropTolerance = 50; /* pixels */
	
	public static PlayHeadMarker create() {
		if (playHead == null) {
			try {
				playHead = new PlayHeadMarker();
			} catch (Exception ex) {
				return null;
			}
		}
		return playHead;
	}
	
	private PlayHeadMarker() {
		super(new LatLon(0.0,0.0), "", 
			  Main.pref.get("marker.audiotracericon", "audio-tracer"), 
			  null, -1.0, 0.0);
		enabled = Main.pref.getBoolean("marker.traceaudio", true);
		if (! enabled) return;
		try { dropTolerance = Integer.parseInt(Main.pref.get("marker.playHeadDropTolerance", "50")); }
		catch(NumberFormatException x) { dropTolerance = 50; }
		Main.map.mapView.addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent ev) {
				Point p = ev.getPoint();
				if (ev.getButton() != MouseEvent.BUTTON1 || p == null)
					return;
				if (playHead.containsPoint(p)) {
					/* when we get a click on the marker, we need to switch mode to avoid
					 * getting confused with other drag operations (like select) */
					oldMode = Main.map.mapMode;
					oldEastNorth = eastNorth;
					PlayHeadDragMode playHeadDragMode = new PlayHeadDragMode(playHead);
					Main.map.selectMapMode(playHeadDragMode);
					playHeadDragMode.mousePressed(ev);
				}
			}
		});
	}

	@Override public boolean containsPoint(Point p) {
		Point screen = Main.map.mapView.getPoint(eastNorth);
		Rectangle r = new Rectangle(screen.x, screen.y, symbol.getIconWidth(), symbol.getIconHeight());
		return r.contains(p);
	}

	/**
	 * called back from drag mode to say when we started dragging for real 
	 * (at least a short distance)
	 */
	public void startDrag() {
		if (timer != null) 
			timer.stop();
		wasPlaying = AudioPlayer.playing();
		if (wasPlaying) {
			try { AudioPlayer.pause(); } 
		    catch (Exception ex) { AudioPlayer.audioMalfunction(ex);}
		}
	}

	/**
	 * reinstate the old map mode after swuitching temporarily to do a play head drag 
	 */
	private void endDrag(boolean reset) {
		if (! wasPlaying || reset) 
			try { AudioPlayer.pause(); }
			catch (Exception ex) { AudioPlayer.audioMalfunction(ex);}
		if (reset)
			eastNorth = oldEastNorth;
		Main.map.selectMapMode(oldMode);
		Main.map.mapView.repaint();
		timer.start();
	}
	
	/**
	 * apply the new position resulting from a drag in progress
	 * @param en the new position in map terms 
	 */
	public void drag(EastNorth en) {
		eastNorth = en;
		Main.map.mapView.repaint();
	}

	/**
	 * Find the closest track point within the pixelTolerance of the screen point pNear 
	 * @param pNear : the point in screen coordinates near which to find a track point 
	 * @param pixelTolerance : only accept the point if within this number of pixels of en
	 * @return the nearest trackpoint or null if nothing nearby
	 * 
	 * XXX seems unused, F.R. 2008-03-15
	private WayPoint getClosestTrackPoint(Point pNear, double pixelTolerance) {
		WayPoint cw = null;
		AudioMarker recentlyPlayedMarker = AudioMarker.recentlyPlayedMarker();
		if (recentlyPlayedMarker != null) {
			// Find the track point closest to letting go of the play head 
			double minDistance = pixelTolerance;
			GpxLayer trackLayer = recentlyPlayedMarker.parentLayer.fromLayer;
			if (trackLayer.data.tracks == null) 
				return null;
			
			for (GpxTrack track : trackLayer.data.tracks) {
				if (track.trackSegs == null) 
					continue;

				for (Collection<WayPoint> trackseg : track.trackSegs) {
					for (WayPoint w : trackseg) {
						Point p = Main.map.mapView.getPoint(w.eastNorth);
						double distance = p.distance(pNear);
						if (distance <= minDistance) {
							cw = w;
							minDistance = distance;
						}
					}
				}
			}
		}
		return cw;
	}
	*/
	
	/**
	 * reposition the play head at the point on the track nearest position given,
	 * providing we are within reasonable distance from the track; otherwise reset to the
	 * original position.
	 * @param en the position to start looking from
	 */
	public void reposition(EastNorth en) {
		// eastNorth = en;
		WayPoint cw = null;
		AudioMarker recent = AudioMarker.recentlyPlayedMarker();
		if (recent != null && recent.parentLayer != null && recent.parentLayer.fromLayer != null) {
			/* work out EastNorth equivalent of 50 (default) pixels tolerance */ 
			Point p = Main.map.mapView.getPoint(en);
			EastNorth enPlus25px = Main.map.mapView.getEastNorth(p.x+dropTolerance, p.y);
			cw = recent.parentLayer.fromLayer.nearestPointOnTrack(en, enPlus25px.east() - en.east());
		}
		
		AudioMarker ca = null; 
		/* Find the prior audio marker (there should always be one in the 
		 * layer, even if it is only one at the start of the track) to 
		 * offset the audio from */ 
		if (cw != null) {
			if (recent != null || recent.parentLayer != null) {
				for (Marker m : recent.parentLayer.data) {
					if (m instanceof AudioMarker) {
						AudioMarker a = (AudioMarker) m;
						if (a.time > cw.time)
							break;
						ca = a;
					}
				}
			}
		}

		if (ca == null) {
			/* Not close enough to track, or no audio marker found for some other reason */
			JOptionPane.showMessageDialog(Main.parent, tr("You need to Drag the play head near to the GPX track whose associated sound track you were playing."));
			endDrag(true);
		} else {
			eastNorth = cw.eastNorth;
			ca.play(cw.time - ca.time);
			endDrag(false);
		}
	}

	/**
	 * Synchronize the audio at the position where the play head was paused before 
	 * dragging with the position on the track where it was dropped. 
	 * If this is quite near an audio marker, we use that 
	 * marker as the sync. location, otherwise we create a new marker at the 
	 * trackpoint nearest the end point of the drag point to apply the 
	 * sync to.
	 * @param en : the EastNorth end point of the drag
	 */
	public void synchronize(EastNorth en) {
		/* First, see if we dropped onto an existing audio marker in the layer being played */
		Point startPoint = Main.map.mapView.getPoint(en);
		AudioMarker ca = null; 
		AudioMarker recent = AudioMarker.recentlyPlayedMarker(); 		
		if (recent != null || recent.parentLayer != null) {
			double closestAudioMarkerDistanceSquared = 1.0E100;
			for (Marker m : AudioMarker.recentlyPlayedMarker().parentLayer.data) {
				if (m instanceof AudioMarker) {
					double distanceSquared = m.eastNorth.distanceSq(en);
					if (distanceSquared < closestAudioMarkerDistanceSquared) {
						ca = (AudioMarker) m;
						closestAudioMarkerDistanceSquared = distanceSquared;
					}
				}
			}
		}

		/* We found the closest marker: did we actually hit it? */
		if (ca != null && ! ca.containsPoint(startPoint)) ca = null;
		
		/* If we didn't hit an audio marker, we need to create one at the nearest point on the track */
		if (ca == null && recent != null) {
			/* work out EastNorth equivalent of 50 (default) pixels tolerance */ 
			Point p = Main.map.mapView.getPoint(en);
			EastNorth enPlus25px = Main.map.mapView.getEastNorth(p.x+dropTolerance, p.y);
			WayPoint cw = recent.parentLayer.fromLayer.nearestPointOnTrack(en, enPlus25px.east() - en.east());
			if (cw == null) {
				JOptionPane.showMessageDialog(Main.parent, tr("You need to SHIFT-Drag the play head onto an audio marker or onto the track point where you want to synchronize."));
				endDrag(true);
				return;
			}
			ca = recent.parentLayer.addAudioMarker(cw.time, cw.eastNorth);
		}

		/* Actually do the synchronization */
		if (recent.parentLayer.synchronizeAudioMarkers(ca)) {
			JOptionPane.showMessageDialog(Main.parent, tr("Audio synchronized at point ") + ca.text);
			eastNorth = ca.eastNorth;
			endDrag(false);
		} else {
			JOptionPane.showMessageDialog(Main.parent,tr("Unable to synchronize in layer being played."));
			endDrag(true);
		}
	}
	
	public void paint(Graphics g, MapView mv /*, boolean mousePressed */) {
		if (time < 0.0) return;
		Point screen = mv.getPoint(eastNorth);
		symbol.paintIcon(mv, g, screen.x, screen.y);
	}

	public void animate() {
		if (! enabled) return;
		if (timer == null) {
			animationInterval = Double.parseDouble(Main.pref.get("marker.audioanimationinterval", "1")); //milliseconds
			timer = new Timer((int)(animationInterval * 1000.0), new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					timerAction();
				}
			});
			timer.setInitialDelay(0);
		} else {
			timer.stop();
		}
		timer.start();
	}
	
	/**
	 * callback for moving play head marker according to audio player position 
	 */
	public void timerAction() {
		AudioMarker recentlyPlayedMarker = AudioMarker.recentlyPlayedMarker();
		if (recentlyPlayedMarker == null)
			return;
		double audioTime = recentlyPlayedMarker.time + 
			AudioPlayer.position() - 
			recentlyPlayedMarker.offset -
			recentlyPlayedMarker.syncOffset;
		if (Math.abs(audioTime - time) < animationInterval)
			return;
		if (recentlyPlayedMarker.parentLayer == null) return;
		GpxLayer trackLayer = recentlyPlayedMarker.parentLayer.fromLayer;
		if (trackLayer == null)
			return;
		/* find the pair of track points for this position (adjusted by the syncOffset)
		 * and interpolate between them 
		 */
		WayPoint w1 = null;
		WayPoint w2 = null;

		for (GpxTrack track : trackLayer.data.tracks) {
			for (Collection<WayPoint> trackseg : track.trackSegs) {
				for (Iterator<WayPoint> it = trackseg.iterator(); it.hasNext();) {
					WayPoint w = it.next();
					if (audioTime < w.time) {
						w2 = w;
						break;
					}
					w1 = w;
				}
				if (w2 != null) break;
			}
			if (w2 != null) break;
		}
		
		if (w1 == null)
			return;
		eastNorth = w2 == null ? 
			w1.eastNorth : 
			w1.eastNorth.interpolate(w2.eastNorth, 
					(audioTime - w1.time)/(w2.time - w1.time));
		time = audioTime;
		Main.map.mapView.repaint();
	}
}
