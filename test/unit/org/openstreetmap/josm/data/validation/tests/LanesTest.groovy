package org.openstreetmap.josm.data.validation.tests

import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.osm.OsmUtils

class LanesTest extends GroovyTestCase {

    Lanes lanes = new Lanes()

    @Override
    void setUp() {
        JOSMFixture.createUnitTestFixture().init()
        lanes.initialize()
        lanes.startTest(null)
    }

    void testLanesCount() {
        assert lanes.getLanesCount("") == 0
        assert lanes.getLanesCount("left") == 1
        assert lanes.getLanesCount("left|right") == 2
        assert lanes.getLanesCount("yes|no|yes") == 3
        assert lanes.getLanesCount("yes||") == 3
    }

    void test1() {
        lanes.check(OsmUtils.createPrimitive("way turn:lanes=left|right change:lanes=only_left|not_right|yes"))
        assert lanes.errors.get(0).getMessage() == "Number of lane dependent values inconsistent"
    }

    void test2() {
        lanes.check(OsmUtils.createPrimitive("way width:lanes:forward=1|2|3 psv:lanes:forward=no|designated"))
        assert lanes.errors.get(0).getMessage() == "Number of lane dependent values inconsistent in forward direction"
    }

    void test3() {
        lanes.check(OsmUtils.createPrimitive("way change:lanes:forward=yes|no turn:lanes:backward=left|right|left"))
        assert lanes.errors.isEmpty()
    }

    void test4() {
        lanes.check(OsmUtils.createPrimitive("way turn:lanes:forward=left|right change:lanes:forward=yes|no|yes width:backward=1|2|3"))
        assert lanes.errors.get(0).getMessage() == "Number of lane dependent values inconsistent in forward direction"
    }

    void test5() {
        lanes.check(OsmUtils.createPrimitive("way lanes:forward=5 turn:lanes:forward=left|right"))
        assert lanes.errors.get(0).getMessage() == "Number of lanes:forward greater than *:lanes:forward"
    }

    void test6() {
        lanes.check(OsmUtils.createPrimitive("way lanes:forward=foo|bar turn:lanes:forward=foo+bar"))
        assert lanes.errors.isEmpty()
    }

    void test7() {
        lanes.check(OsmUtils.createPrimitive("way lanes=3 lanes:forward=3 lanes:backward=7"))
        assert lanes.errors.get(0).getMessage() == "Number of lanes:forward+lanes:backward greater than lanes"
    }

    void test8() {
        lanes.check(OsmUtils.createPrimitive("way destination:country:lanes=X|Y;Z|none destination:ref:lanes=xyz|| destination:sign:lanes=none|airport|none"))
        assert lanes.errors.isEmpty()
    }

    void test9() {
        lanes.check(OsmUtils.createPrimitive("way highway=secondary lanes=2 source:lanes=survey"))
        assert lanes.errors.isEmpty()
    }
}
