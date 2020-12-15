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
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Inline {@link jdk.javadoc.doclet.Taglet} to provide {@link.this} links.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@TagletName("link.this")
@ServiceProviderFor({ Taglet.class })
@NoArgsConstructor @ToString
public class LinkThisTaglet extends AbstractInlineTaglet {
    @Override
    public FluentNode toNode(UnknownInlineTagTree tag, Element element) throws Throwable {
        if (isNotEmpty(getText(tag).trim())) {
            throw new IllegalArgumentException("Invalid argument");
        }

        return a(tag, element, getEnclosingTypeElement(element), code("this"));
    }
}
