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
package nu.ndw.nls.routingmapmatcher.network.init.annotation;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.routing.util.parsers.TagParserFactory;
import com.graphhopper.util.PMap;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory;
import nu.ndw.nls.routingmapmatcher.network.model.Link;


/**
 * Is a tag parser factory that wires all {@link nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory}s that are available in the spring
 * context and uses the field meta-data to resolve the correct {@link nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory} and instantiates an
 * instance of {@link AbstractEncodedMapper}, which is a GraphHopper {@link TagParser}
 *
 * @param <T> Link type
 */
@Slf4j
public class AnnotatedEncodedMapperFactory<T extends Link> implements TagParserFactory {

    private final EncodedValuesByTypeDto<T> encodedValuesByTypeDto;

    private final EncodedMapperFactoryRegistry classToEncodedMapperFactory;

    public AnnotatedEncodedMapperFactory(EncodedMapperFactoryRegistry classToEncodedMapperFactory,
            EncodedValuesByTypeDto<T> encodedValuesByTypeDto) {
        this.classToEncodedMapperFactory = classToEncodedMapperFactory ;
        this.encodedValuesByTypeDto = encodedValuesByTypeDto;
    }

    @Override
    public AbstractEncodedMapper<T, ?> create(EncodedValueLookup lookup, String name, PMap properties) {
        Class<?> type = encodedValuesByTypeDto.getValueTypeByKey(name).orElseThrow(() ->
                new IllegalStateException("No annotate encoded value configuration found by name: " + name));

        return createEncodedMapper(lookup, type, name);
    }

    private <U> AbstractEncodedMapper<T, U> createEncodedMapper(EncodedValueLookup lookup, Class<U> valueType,
            String name) {

        EncodedMapperFactory<U> encodedMapperFactory =
                classToEncodedMapperFactory.lookupEncodedMapperFactory(valueType).orElseThrow(
                () -> new IllegalStateException(
                        "No tag parser found for name: %s with type: %s".formatted(name, valueType)));

        return  encodedMapperFactory.create(lookup, encodedValuesByTypeDto.get(valueType, name));
    }


}
