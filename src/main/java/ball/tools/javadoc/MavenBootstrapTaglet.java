package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2020 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import java.util.Map;
import java.util.ServiceLoader;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * {@link AbstractTaglet} whose {@link #register(Map)} method loads all
 * {@link Taglet}s specified through a {@link ServiceLoader}.  Designed to
 * be specified as {@code <tagletClass/>} in a Maven {@code pom.xml}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PRIVATE)
public abstract class MavenBootstrapTaglet extends AbstractTaglet {

    /**
     * Static method to register all service providers for {@link Taglet}.
     *
     * @param   map             The {@link Map} of {@link Taglet}s.
     */
    public static void register(Map<Object,Object> map) {
        try {
            ServiceLoader.load(Taglet.class,
                               MavenBootstrapTaglet.class.getClassLoader())
                .iterator()
                .forEachRemaining(t -> map.putIfAbsent(t.getName(), t));
        } catch (Throwable throwable) {
            throwable.printStackTrace(System.err);

            if (throwable instanceof Error) {
                throw (Error) throwable;
            } else {
                throw new ExceptionInInitializerError(throwable);
            }
        }
    }
}
