package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2008 - 2020 Allen D. Ball
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
import com.sun.tools.doclets.Taglet;

/**
 * Interface indicating {@link Taglet} is annotated with {@link TagletName}
 * and related annotations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedTaglet extends Taglet {

    /**
     * Method to get {@link TagletName#value()}.
     *
     * @return  {@link TagletName#value()} if {@link.this} {@link Taglet} is
     *          annotated; {@code null} otherwise.
     */
    @Override
    default String getName() {
        return getClass().getAnnotation(TagletName.class).value();
    }
}
