/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common.configuration;

import io.prometheus.jmx.common.util.Precondition;
import io.prometheus.jmx.common.yaml.YamlMapAccessor;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class to convert an Object to a Map, throwing a RuntimeException
 * from the Supplier if there is a ClassCastException
 */
@SuppressWarnings("unchecked")
public class ConvertToMapAccessor implements Function<Object, YamlMapAccessor> {

    private Supplier<? extends RuntimeException> supplier;

    /**
     * Constructor
     *
     * @param supplier supplier
     */
    public ConvertToMapAccessor(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    /**
     * Method to apply a function
     *
     * @param value value
     * @return the return value
     */
    @Override
    public YamlMapAccessor apply(Object value) {
        try {
            return new YamlMapAccessor((Map<Object, Object>) value);
        } catch (ClassCastException e) {
            throw supplier.get();
        }
    }
}
