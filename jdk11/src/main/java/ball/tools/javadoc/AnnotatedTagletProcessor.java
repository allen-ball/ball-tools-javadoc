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
import ball.annotation.processing.MustImplement;
import javax.annotation.processing.Processor;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static javax.lang.model.element.ElementKind.CLASS;

/**
 * {@link Processor} implementation to verify concrete implementations of
 * {@link AnnotatedTaglet} are also subclasses of {@link Taglet}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Processor.class })
@ForElementKinds({ CLASS })
@ForSubclassesOf(AnnotatedTaglet.class)
@MustImplement({ Taglet.class })
@NoArgsConstructor @ToString
public class AnnotatedTagletProcessor extends AnnotatedNoAnnotationProcessor {
}
