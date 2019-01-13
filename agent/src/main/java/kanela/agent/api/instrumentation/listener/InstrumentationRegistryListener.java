/*
 * =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

package kanela.agent.api.instrumentation.listener;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Value;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import kanela.agent.api.instrumentation.TypeTransformation;
import kanela.agent.bootstrap.dispatcher.Dispatcher;
import kanela.agent.util.classloader.ClassLoaderNameMatcher;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

public class InstrumentationRegistryListener extends AgentBuilder.Listener.Adapter {

    private final static String DISPATCHER_KEY = "InstrumentationRegistryListener";

    private Map<String, List<Tuple2<TypeTransformation, List<TypeDescription>>>> moduleTransformers = HashMap.empty();
    private Map<String, List<Throwable>> errors = HashMap.empty();

    public java.util.Map<String, java.util.Map<String, java.util.List<String>>> getRecorded() {
        return moduleTransformers.mapValues(
                value -> value.toMap(
                        t -> Tuple.of(t._1.getInstrumentationName(), t._2.map(TypeDescription::getCanonicalName).toJavaList())
                ).toJavaMap()
        ).toJavaMap();
    }

    public java.util.Map<String, java.util.List<Throwable>> getErrors() {
        return errors.mapValues(Value::toJavaList).toJavaMap();
    }

    public void register(String moduleName, TypeTransformation typeTransformation) {
        moduleTransformers = moduleTransformers.computeIfPresent(moduleName, (mn, tts) -> tts.append(Tuple.of(typeTransformation, List.empty())))._2;
        moduleTransformers = moduleTransformers.computeIfAbsent(moduleName, (m) -> List.of(Tuple.of(typeTransformation, List.empty())))._2;
    }

    public static InstrumentationRegistryListener instance() {
        return Dispatcher.computeIfAbsent(
                InstrumentationRegistryListener.DISPATCHER_KEY,
                k -> new InstrumentationRegistryListener()
        );
    }

    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        moduleTransformers = moduleTransformers.map(
                (moduleName, transformations) -> Tuple.of(moduleName, transformations.map(transformation -> {
                    if (transformation._1.getElementMatcher().map(em -> em.matches(typeDescription)).getOrElse(false) &&
                        ClassLoaderNameMatcher.RefinedClassLoaderMatcher.from(transformation._1.getClassLoaderRefiner()).matches(classLoader)
                    ) {
                        return transformation.update2(transformation._2.append(typeDescription));
                    } else {
                        return transformation;
                    }
                })));
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        errors = errors.computeIfPresent(typeName, (tn, errs) -> errs.append(throwable))._2;
        errors = errors.computeIfAbsent(typeName, (tn) -> List.of(throwable))._2;
    }
}

