/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.HGV_ACCESSIBLE;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.MAX_AXLE_LOAD;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.MAX_HEIGHT;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.MAX_LENGTH;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.MAX_WEIGHT;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.MAX_WIDTH;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.MUNICIPALITY_CODE;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.routing.util.parsers.TagParserFactory;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers.BooleanParser;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers.DoubleParser;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers.IntParser;

public class LinkTagParserFactory implements TagParserFactory {

    @Override
    public TagParser create(EncodedValueLookup lookup, String name, PMap properties) {
        return switch (EncodedTag.withKey(name)) {
            case WAY_ID -> null;
            case MAX_WEIGHT -> new DoubleParser(lookup, MAX_WEIGHT);
            case MAX_WIDTH -> new DoubleParser(lookup, MAX_WIDTH);
            case MAX_LENGTH -> new DoubleParser(lookup, MAX_LENGTH);
            case MAX_AXLE_LOAD -> new DoubleParser(lookup, MAX_AXLE_LOAD);
            case MAX_HEIGHT -> new DoubleParser(lookup, MAX_HEIGHT);
            case MUNICIPALITY_CODE -> new IntParser(lookup, MUNICIPALITY_CODE);
            case HGV_ACCESSIBLE -> new BooleanParser(lookup, HGV_ACCESSIBLE);
        };
    }

}
