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
import ball.util.ant.taskdefs.AntTask;
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Antlib;
import org.w3c.dom.Node;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.tools.ant.MagicNames.ANTLIB_PREFIX;

/**
 * Inline {@link Taglet} to document {@link.uri http://ant.apache.org/ Ant}
 * {@link Task}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@TagletName("ant.task")
@ServiceProviderFor({ Taglet.class })
@NoArgsConstructor @ToString
public class AntTaskTaglet extends AbstractInlineTaglet {
    private static final String NO = "no";
    private static final String YES = "yes";

    private static final String INDENTATION = "  ";

    private static final String DOCUMENTED = "DOCUMENTED";

    @Override
    public FluentNode toNode(UnknownInlineTagTree tag, Element context) throws Throwable {
        FluentNode node = null;
        TypeElement type = null;
        var name = getText(tag).trim();

        if (isNotEmpty(name)) {
            type = getTypeElementFor(context, name);
        } else {
            type = getEnclosingTypeElement(context);
        }

        if (! isAssignableTo(Task.class).test(type)) {
            throw new IllegalArgumentException(type.getQualifiedName()
                                               + " is not a subclass of " + Task.class.getCanonicalName());
        }

        var template =
            render(template(tag, context, asClass(type)), INDENTATION.length())
            .replaceAll(Pattern.quote(DOCUMENTED + "=\"\""), "...");

        return div(attr("class", "block"), pre("xml", template));
    }

    private FluentNode template(UnknownInlineTagTree tag, Element context, Class<?> type) {
        String name = null;

        if (name == null) {
            var project = new Project();
            var pkg = type.getPackage().getName();

            while (pkg != null) {
                var url = type.getResource("/" + String.join("/", pkg.split(Pattern.quote("."))) + "/antlib.xml");

                if (url != null) {
                    try {
                        Antlib.createAntlib(project, url, ANTLIB_PREFIX + pkg)
                            .execute();
                        break;
                    } catch (Exception exception) {
                    }
                }

                int index = pkg.lastIndexOf(".");

                if (! (index < 0)) {
                    pkg = pkg.substring(0, index);
                } else {
                    pkg = null;
                }
            }

            var helper = ComponentHelper.getComponentHelper(project);

            name =
                helper.getTaskDefinitions().entrySet().stream()
                .filter(t -> t.getValue().equals(type))
                .map(t -> t.getKey())
                .findFirst().orElse(null);
        }

        if (name == null) {
            var annotation = type.getAnnotation(AntTask.class);

            name = (annotation != null) ? annotation.value() : null;
        }

        if (name == null) {
            name = type.getSimpleName();
        }

        return type(0, new HashSet<>(), tag, context, new SimpleEntry<>(name, type));
    }

    private FluentNode type(int depth, Set<Map.Entry<?,?>> set, UnknownInlineTagTree tag, Element context, Map.Entry<String,Class<?>> entry) {
        var helper = IntrospectionHelper.getHelper(entry.getValue());
        var node = element(entry.getKey());

        if (set.add(entry) && (! entry.getValue().getName().startsWith(Task.class.getPackage().getName()))) {
            node
                .add(attributes(tag, context, helper))
                .add(content(depth + 1, set, tag, context, helper));

            if (helper.supportsCharacters()) {
                var content = "... text ...";

                if (node.hasChildNodes()) {
                    content =
                        Stream.generate(() -> INDENTATION)
                        .limit(depth + 1)
                        .collect(joining(EMPTY, "\n", content + "\n"));
                }

                node.add(text(content));
            }
        } else {
            node.add(attr(DOCUMENTED));
        }

        return node;
    }

    private Node[] attributes(UnknownInlineTagTree tag, Element context, IntrospectionHelper helper) {
        var array =
            helper.getAttributeMap().entrySet().stream()
            .map(t -> attr(t.getKey(), t.getValue().getSimpleName()))
            .toArray(Node[]::new);

        return array;
    }

    private FluentNode content(int depth, Set<Map.Entry<?,?>> set, UnknownInlineTagTree tag, Element context, IntrospectionHelper helper) {
        return fragment(helper.getNestedElementMap().entrySet().stream()
                        .map(t -> type(depth, set, tag, context, t)));
    }
}
