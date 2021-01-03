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
import ball.swing.table.ListTableModel;
import ball.swing.table.MapTableModel;
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.io.IOUtils;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Inline {@link Taglet} to include a static {@link Class}
 * {@link java.lang.reflect.Field} or resource in the Javadoc output.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@TagletName("include")
@ServiceProviderFor({ Taglet.class })
@NoArgsConstructor @ToString
public class IncludeTaglet extends AbstractInlineTaglet {
    @Override
    public FluentNode toNode(UnknownInlineTagTree tag, Element element) throws Throwable {
        FluentNode node = null;
        var text = getText(tag).trim().split(Pattern.quote("#"), 2);

        if (text.length > 1) {
            node =
                field(tag, element,
                      asClass(isNotEmpty(text[0])
                                  ? getTypeElementFor(element, text[0])
                                  : getEnclosingTypeElement(element)),
                      text[1]);
        } else {
            Class<?> type = null;

            if (element instanceof PackageElement) {
                type = asPackageInfoClass((PackageElement) element);
            } else {
                type = asClass(getEnclosingTypeElement(element));
            }

            node = resource(tag, element, type, text[0]);
        }

        return node;
    }

    private FluentNode field(UnknownInlineTagTree tag, Element element,
                             Class<?> type, String name) throws Exception {
        Object object = type.getField(name).get(null);
        FluentNode node = null;

        if (object instanceof Collection<?>) {
            node =
                table(tag, element,
                      new ListTableModel(((Collection<?>) object)
                                         .stream()
                                         .collect(toList()),
                                         "Element"));
        } else if (object instanceof Map<?,?>) {
            node =
                table(tag, element,
                      new MapTableModel((Map<?,?>) object, "Key", "Value"));
        } else {
            node = pre(String.valueOf(object));
        }

        return div(attr("class", "block"), node);
    }

    private FluentNode resource(UnknownInlineTagTree tag, Element element,
                                Class<?> type, String name) throws Exception {
        String string = null;

        if (type == null) {
            type = getClass();
        }

        try (var in = type.getResourceAsStream(name)) {
            string = IOUtils.toString(in, "UTF-8");
        }

        return pre(string);
    }
}
