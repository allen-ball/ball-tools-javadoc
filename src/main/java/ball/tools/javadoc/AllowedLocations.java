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
import ball.annotation.ServiceProviderFor;
import ball.annotation.processing.AnnotatedProcessor;
import ball.annotation.processing.For;
import ball.annotation.processing.TargetMustExtend;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * {@link java.lang.annotation.Annotation} to mark {@link Taglet}s with
 * their {@link Taglet#getAllowedLocations() allowed locations}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE })
@TargetMustExtend(AnnotatedTaglet.class)
public @interface AllowedLocations {
    Taglet.Location[] value();

    /**
     * {@link Processor} implementation.
     */
    @ServiceProviderFor({ Processor.class })
    @For({ AllowedLocations.class })
    @NoArgsConstructor @ToString
    public class ProcessorImpl extends AnnotatedProcessor {
        @Override
        protected void process(RoundEnvironment roundEnv,
                               TypeElement annotation, Element element) {
            super.process(roundEnv, annotation, element);

            var mirror = getAnnotationMirror(element, annotation);
            var value = getAnnotationValue(mirror, "value");

            if (isEmptyArray(value)) {
                print(ERROR, element, mirror, value, "value() is empty");
            }
        }
    }
}
