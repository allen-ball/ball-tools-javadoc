package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2020, 2021 Allen D. Ball
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
import ball.annotation.ServiceProviderFor;
import ball.annotation.processing.AnnotatedNoAnnotationProcessor;
import ball.annotation.processing.ForElementKinds;
import ball.annotation.processing.ForSubclassesOf;
import ball.annotation.processing.WithoutModifiers;
import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * {@link Processor} implementation to verify concrete implementations of
 * {@link Taglet} implement the required static {@code register()} method.
 * See
 * {@link.uri https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/taglet/overview.html Taglet Overview}
 * and
 * {@link.uri https://docs.oracle.com/javase/8/docs/jdk/api/javadoc/taglet/com/sun/tools/doclets/Taglet.html Taglet}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Processor.class })
@ForElementKinds({ CLASS })
@ForSubclassesOf(Taglet.class)
@WithoutModifiers({ ABSTRACT })
@NoArgsConstructor @ToString
public class TagletProcessor extends AnnotatedNoAnnotationProcessor {
    private static abstract class PROTOTYPE {
        public static void register(Map<Object,Object> map) { }
    }

    private static final Method PROTOTYPE =
        PROTOTYPE.class.getDeclaredMethods()[0];

    @Override
    protected void process(RoundEnvironment roundEnv, Element element) {
        var type = (TypeElement) element;
        var method = getMethod(type, PROTOTYPE);

        if (! (method != null
               && method.getModifiers().containsAll(getModifiers(PROTOTYPE)))) {
            print(WARNING, element,
                  "%s implements %s but does not implement '%s'",
                  element.getKind(), Taglet.class.getName(),
                  declaration(PROTOTYPE));
        }
    }
}
