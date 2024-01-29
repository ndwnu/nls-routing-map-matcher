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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory;
import org.springframework.stereotype.Component;


/**
 * Wires all {@link EncodedMapperFactory} instances that are found in the spring boot context and allows you to
 * look up an {@link EncodedMapperFactory} for a specific java class type.
 */
@Component
public class EncodedMapperFactoryRegistry {

    private final Map<Class<?>, EncodedMapperFactory<?>> classToEncodedMapperFactory;

    public EncodedMapperFactoryRegistry(List<? extends EncodedMapperFactory<?>> encodedMapperFactories) {
        this.classToEncodedMapperFactory = encodedMapperFactories.stream().collect(
                Collectors.toMap(EncodedMapperFactory::getType, encodedMapperFactory -> encodedMapperFactory));
    }

    /**
     * Resolves an {@link EncodedMapperFactory} for a specific java type. Uses unchecked cast, but type safety is
     * guaranteed, because the constructor code guarantees that each class key matches the {@link EncodedMapperFactory}
     * type class.
     *
     * @param aClass type
     * @return {@link EncodedMapperFactory} for a specific java type
     * @param <T> type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<EncodedMapperFactory<T>> lookupEncodedMapperFactory(Class<T> aClass) {
        return Optional.ofNullable((EncodedMapperFactory<T>) this.classToEncodedMapperFactory.get(aClass));
    }

}
