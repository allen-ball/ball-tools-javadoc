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
import ball.annotation.MatcherGroup;
import ball.annotation.PatternRegex;
import ball.annotation.ServiceProviderFor;
import ball.util.PatternMatcherBean;
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.io.File;
import javax.lang.model.element.Element;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Inline {@link Taglet} providing links to {@link.man man(1)} pages.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@TagletName("link.man")
@ServiceProviderFor({ Taglet.class })
@PatternRegex("(?is)(?<name>.+)[(](?<section>[\\p{Alnum}]+)[)]")
@NoArgsConstructor @ToString
public class LinkManTaglet extends AbstractInlineTaglet implements PatternMatcherBean {
    private String name = null;
    private String section = null;

    @MatcherGroup(1)
    protected void setName(String string) { name = string; }

    @MatcherGroup(2)
    protected void setSection(String string) { section = string; }

    @Override
    public FluentNode toNode(UnknownInlineTagTree tag, Element element) throws Throwable {
        PatternMatcherBean.super.initialize(getText(tag).trim());

        var path = new File(File.separator);

        path = new File(path, "usr");
        path = new File(path, "share");
        path = new File(path, "man");
        path = new File(path, "htmlman" + section);
        path = new File(path, name + "." + section + ".html");

        return a(path.toURI(), code(name + "(" + section + ")"));
    }
}
