// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

/**
 * Immutable GPX track.
 * @since 2907
 */
public class ImmutableGpxTrack extends WithAttributes implements GpxTrack {

    private final List<GpxTrackSegment> segments;
    private final double length;
    private final Bounds bounds;

    /**
     * Constructs a new {@code ImmutableGpxTrack}.
     * @param trackSegs track segments
     * @param attributes track attributes
     */
    public ImmutableGpxTrack(Collection<Collection<WayPoint>> trackSegs, Map<String, Object> attributes) {
        List<GpxTrackSegment> newSegments = new ArrayList<>();
        for (Collection<WayPoint> trackSeg: trackSegs) {
            if (trackSeg != null && !trackSeg.isEmpty()) {
                newSegments.add(new ImmutableGpxTrackSegment(trackSeg));
            }
        }
        this.attr = Collections.unmodifiableMap(new HashMap<>(attributes));
        this.segments = Collections.unmodifiableList(newSegments);
        this.length = calculateLength();
        this.bounds = calculateBounds();
    }

    private double calculateLength() {
        double result = 0.0; // in meters

        for (GpxTrackSegment trkseg : segments) {
            result += trkseg.length();
        }
        return result;
    }

    private Bounds calculateBounds() {
        Bounds result = null;
        for (GpxTrackSegment segment: segments) {
            Bounds segBounds = segment.getBounds();
            if (segBounds != null) {
                if (result == null) {
                    result = new Bounds(segBounds);
                } else {
                    result.extend(segBounds);
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attr;
    }

    @Override
    public Bounds getBounds() {
        return bounds == null ? null : new Bounds(bounds);
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public Collection<GpxTrackSegment> getSegments() {
        return segments;
    }

    @Override
    public int getUpdateCount() {
        return 0;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + ((segments == null) ? 0 : segments.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableGpxTrack other = (ImmutableGpxTrack) obj;
        if (segments == null) {
            if (other.segments != null)
                return false;
        } else if (!segments.equals(other.segments))
            return false;
        return true;
    }
}
