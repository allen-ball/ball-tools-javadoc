package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * %%
 * Copyright (C) 2020 - 2022 Allen D. Ball
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.util.ElementFilter.fieldsIn;
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
public class IncludeTaglet extends AbstractInlineTaglet {
    @Override
    public FluentNode toNode(UnknownInlineTagTree tag, Element context) throws Throwable {
        FluentNode node = null;
        var argv = getText(tag).trim().split("[\\p{Space}]+", 2);

        if (isNotEmpty(argv[0])) {
            var target = argv[0].trim().split(Pattern.quote("#"), 2);

            if (target.length > 1) {
                var type =
                    isNotEmpty(target[0])
                        ? getTypeElementFor(context, target[0])
                        : getEnclosingTypeElement(context);
                var field =
                    fieldsIn(type.getEnclosedElements()).stream()
                    .filter(t -> t.getSimpleName().contentEquals(target[1]))
                    .findFirst().get();

                node = field(tag, context, field);
            } else {
                var type =
                    (context instanceof PackageElement)
                        ? asPackageInfoClass((PackageElement) context)
                        : asClass(getEnclosingTypeElement(context));

                node = resource(tag, context, type, target[0]);
            }
        } else {
            node = field(tag, context, (VariableElement) context);
        }

        return node;
    }

    private FluentNode field(UnknownInlineTagTree tag, Element context, VariableElement element) throws Exception {
        FluentNode node = null;
        var type = asClass((TypeElement) element.getEnclosingElement());
        var field = type.getDeclaredField(element.getSimpleName().toString());

        field.setAccessible(true);

        var value = field.get(null);

        if (value instanceof Collection<?>) {
            node =
                table(tag, context, new ListTableModel(((Collection<?>) value).stream().collect(toList()), "Element"));
        } else if (value instanceof Map<?,?>) {
            node = table(tag, context, new MapTableModel((Map<?,?>) value, "Key", "Value"));
        } else {
            node = pre(String.valueOf(value));
        }

        return div(attr("class", "block"), node);
    }

    private FluentNode resource(UnknownInlineTagTree tag, Element context, Class<?> type, String name) throws Exception {
        String string = null;

        if (type == null) {
            type = getClass();
        }

        try (var in = type.getResourceAsStream(name)) {
            string =
                new BufferedReader(new InputStreamReader(in, UTF_8)).lines()
                .collect(joining("\n"));
        }

        return pre(string);
    }
}
