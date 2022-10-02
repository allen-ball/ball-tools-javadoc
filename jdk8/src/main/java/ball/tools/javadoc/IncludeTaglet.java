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
import ball.annotation.ServiceProviderFor;
import ball.swing.table.ListTableModel;
import ball.swing.table.MapTableModel;
import ball.xml.FluentNode;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Inline {@link Taglet} to include a static {@link Class}
 * {@link java.lang.reflect.Field} or resource in the Javadoc output.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@TagletName("include")
@ServiceProviderFor({ Taglet.class })
@NoArgsConstructor @ToString
public class IncludeTaglet extends AbstractInlineTaglet implements SunToolsInternalToolkitTaglet {
    private static final IncludeTaglet INSTANCE = new IncludeTaglet();

    public static void register(Map<Object,Object> map) {
        register(map, INSTANCE);
    }

    @Override
    public FluentNode toNode(Tag tag) throws Throwable {
        FluentNode node = null;
        String[] argv = tag.text().trim().split("[\\p{Space}]+", 2);

        if (isNotEmpty(argv[0])) {
            String[] target = argv[0].trim().split(Pattern.quote("#"), 2);

            if (target.length > 1) {
                node =
                    field(tag,
                          getClassFor(isNotEmpty(target[0]) ? getClassDocFor(tag, target[0]) : containingClass(tag)),
                          target[1]);
            } else {
                Class<?> type = null;

                if (tag.holder() instanceof PackageDoc) {
                    type = getClassFor((PackageDoc) tag.holder());
                } else {
                    type = getClassFor(containingClass(tag));
                }

                node = resource(tag, type, target[0]);
            }
        } else {
            Class<?> type = getClassFor(containingClass(tag));

            node = field(tag, type, tag.holder().name());
        }

        return node;
    }

    private FluentNode field(Tag tag, Class<?> type, String name) throws Exception {
        FluentNode node = null;
        Object value = type.getDeclaredField(name).get(null);

        if (value instanceof Collection<?>) {
            node = table(tag, new ListTableModel(((Collection<?>) value).stream().collect(toList()), "Element"));
        } else if (value instanceof Map<?,?>) {
            node = table(tag, new MapTableModel((Map<?,?>) value, "Key", "Value"));
        } else {
            node = pre(String.valueOf(value));
        }

        return div(attr("class", "block"), node);
    }

    private FluentNode resource(Tag tag, Class<?> type, String name) throws Exception {
        String string = null;

        if (type == null) {
            type = getClass();
        }

        try (InputStream in = type.getResourceAsStream(name)) {
            string =
                new BufferedReader(new InputStreamReader(in, UTF_8)).lines()
                .collect(joining("\n"));
        }

        return pre(string);
    }
}
