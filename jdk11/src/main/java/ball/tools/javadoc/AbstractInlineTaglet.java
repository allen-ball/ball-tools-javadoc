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
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.util.List;
import javax.lang.model.element.Element;
import lombok.NoArgsConstructor;
import org.w3c.dom.Node;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for inline {@link jdk.javadoc.doclet.Taglet}
 * implementations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractInlineTaglet extends AbstractTaglet {
    @Override
    public boolean isInlineTag() { return true; }

    @Override
    protected Node toNode(List<? extends DocTree> tags, Element element) throws Throwable {
        return toNode((UnknownInlineTagTree) tags.get(0), element);
    }

    /**
     * Abstract method to be overridden by subclass implementations.
     *
     * @param   tag             The instance of this tag.
     * @param   element         The element to which the enclosing comment
     *                          belongs.
     *
     * @return  The {@link Node} representing the output.
     *
     * @throws  Throwable       As required by the subclass.
     */
    protected abstract Node toNode(UnknownInlineTagTree tag, Element element) throws Throwable;
}
