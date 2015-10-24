// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests

import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Way

class HighwaysTest extends GroovyTestCase {

    @Override
    void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    public static Way createTestSetting(String highway, String highwayLink) {
        def ds = new DataSet()

        def n00 = new Node(new LatLon(0, 0))
        def n10 = new Node(new LatLon(1, 0))
        def n20 = new Node(new LatLon(2, 0))
        def n01 = new Node(new LatLon(0, 1))
        def n11 = new Node(new LatLon(1, 1))
        def n21 = new Node(new LatLon(2, 1))

        ds.addPrimitive(n00)
        ds.addPrimitive(n10)
        ds.addPrimitive(n20)
        ds.addPrimitive(n01)
        ds.addPrimitive(n11)
        ds.addPrimitive(n21)

        def major = new Way()
        major.addNode(n00)
        major.addNode(n10)
        major.addNode(n20)
        major.put("highway", highway)
        def link = new Way()
        link.addNode(n10)
        link.addNode(n11)
        link.put("highway", highwayLink)
        def unclassified = new Way()
        unclassified.addNode(n01)
        unclassified.addNode(n11)
        unclassified.addNode(n21)
        unclassified.put("highway", "unclassified")

        ds.addPrimitive(major)
        ds.addPrimitive(link)
        ds.addPrimitive(unclassified)

        return link
    }

    void testCombinations() {
        assert Highways.isHighwayLinkOkay(createTestSetting("primary", "primary_link"))
        assert Highways.isHighwayLinkOkay(createTestSetting("primary", "primary"))
        assert !Highways.isHighwayLinkOkay(createTestSetting("primary", "secondary_link"))
        assert !Highways.isHighwayLinkOkay(createTestSetting("secondary", "primary_link"))
        assert !Highways.isHighwayLinkOkay(createTestSetting("secondary", "tertiary_link"))
        assert Highways.isHighwayLinkOkay(createTestSetting("residential", "residential"))
    }

    void testSourceMaxSpeedUnitedKingdom() {
        def link = createTestSetting("primary", "primary")
        link.put("maxspeed", "60 mph")
        link.put("source:maxspeed", "UK:nsl_single")
        def test = new Highways()
        test.visit(link)
        assert test.errors.size() == 1
        def error = test.errors.get(0)
        assert error.isFixable()
        assert error.getFix().executeCommand()
        assert "GB:nsl_single".equals(link.get("source:maxspeed"))
    }
}
