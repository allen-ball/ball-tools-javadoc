package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * %%
 * Copyright (C) 2008 - 2022 Allen D. Ball
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

/**
 * Abstract base class for inline
 * {@link com.sun.tools.doclets.internal.toolkit.taglets.Taglet}
 * implementations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
public abstract class AbstractInlineTaglet extends AbstractTaglet {

    /**
     * Sole constructor.
     */
    protected AbstractInlineTaglet() {
        super(true, true, true, true, true, true, true);
    }
}
