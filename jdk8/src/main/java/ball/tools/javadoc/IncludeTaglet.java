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
import ball.annotation.ServiceProviderFor;
import ball.swing.table.ListTableModel;
import ball.swing.table.MapTableModel;
import ball.xml.FluentNode;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.io.IOUtils;

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
public class IncludeTaglet extends AbstractInlineTaglet
                           implements SunToolsInternalToolkitTaglet {
    private static final IncludeTaglet INSTANCE = new IncludeTaglet();

    public static void register(Map<Object,Object> map) {
        register(map, INSTANCE);
    }

    @Override
    public FluentNode toNode(Tag tag) throws Throwable {
        FluentNode node = null;
        String[] text = tag.text().trim().split(Pattern.quote("#"), 2);

        if (text.length > 1) {
            node =
                field(tag,
                      getClassFor(isNotEmpty(text[0])
                                      ? getClassDocFor(tag, text[0])
                                      : containingClass(tag)),
                      text[1]);
        } else {
            Class<?> type = null;

            if (tag.holder() instanceof PackageDoc) {
                type = getClassFor((PackageDoc) tag.holder());
            } else {
                type = getClassFor(containingClass(tag));
            }

            node = resource(tag, type, text[0]);
        }

        return node;
    }

    private FluentNode field(Tag tag,
                             Class<?> type, String name) throws Exception {
        Object object = type.getField(name).get(null);
        FluentNode node = null;

        if (object instanceof Collection<?>) {
            node =
                table(tag,
                      new ListTableModel(((Collection<?>) object)
                                         .stream()
                                         .collect(Collectors.toList()),
                                         "Element"));
        } else if (object instanceof Map<?,?>) {
            node =
                table(tag,
                      new MapTableModel((Map<?,?>) object, "Key", "Value"));
        } else {
            node = pre(String.valueOf(object));
        }

        return div(attr("class", "block"), node);
    }

    private FluentNode resource(Tag tag,
                                Class<?> type, String name) throws Exception {
        String string = null;

        if (type == null) {
            type = getClass();
        }

        try (InputStream in = type.getResourceAsStream(name)) {
            string = IOUtils.toString(in, "UTF-8");
        }

        return pre(string);
    }
}
