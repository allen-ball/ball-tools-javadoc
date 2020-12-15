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
import ball.util.ant.taskdefs.AntTask;
import ball.xml.FluentNode;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Antlib;
import org.w3c.dom.Node;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.tools.ant.MagicNames.ANTLIB_PREFIX;

/**
 * Inline {@link Taglet} to document {@link.uri http://ant.apache.org/ Ant}
 * {@link Task}s.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@TagletName("ant.task")
@ServiceProviderFor({ Taglet.class })
@NoArgsConstructor @ToString
public class AntTaskTaglet extends AbstractInlineTaglet
                           implements SunToolsInternalToolkitTaglet {
    private static final AntTaskTaglet INSTANCE = new AntTaskTaglet();

    public static void register(Map<Object,Object> map) {
        register(map, INSTANCE);
    }

    private static final String NO = "no";
    private static final String YES = "yes";

    private static final String INDENTATION = "  ";

    private static final String DOCUMENTED = "DOCUMENTED";

    @Override
    public FluentNode toNode(Tag tag) throws Throwable {
        ClassDoc doc = null;
        String name = tag.text().trim();

        if (isNotEmpty(name)) {
            doc = getClassDocFor(tag, name);
        } else {
            doc = containingClass(tag);
        }

        Class<?> type = getClassFor(doc);

        if (! Task.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(type.getCanonicalName()
                                               + " is not a subclass of "
                                               + Task.class.getCanonicalName());
        }

        String template =
            render(template(tag, type), INDENTATION.length())
            .replaceAll(Pattern.quote(DOCUMENTED + "=\"\""), "...");

        return div(attr("class", "block"), pre("xml", template));
    }

    private FluentNode template(Tag tag, Class<?> type) {
        String name = null;

        if (name == null) {
            Project project = new Project();
            String pkg = type.getPackage().getName();

            while (pkg != null) {
                URL url =
                    type.getResource("/"
                                     + String.join("/", pkg.split(Pattern.quote(".")))
                                     + "/antlib.xml");

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

            ComponentHelper helper =
                ComponentHelper.getComponentHelper(project);

            name =
                helper.getTaskDefinitions().entrySet()
                .stream()
                .filter(t -> t.getValue().equals(type))
                .map(t -> t.getKey())
                .findFirst().orElse(null);
        }

        if (name == null) {
            AntTask annotation = type.getAnnotation(AntTask.class);

            name = (annotation != null) ? annotation.value() : null;
        }

        if (name == null) {
            name = type.getSimpleName();
        }

        return type(0, new HashSet<>(), tag, new SimpleEntry<>(name, type));
    }

    private FluentNode type(int depth, Set<Map.Entry<?,?>> set,
                            Tag tag, Map.Entry<String,Class<?>> entry) {
        IntrospectionHelper helper =
            IntrospectionHelper.getHelper(entry.getValue());
        FluentNode node = element(entry.getKey());

        if (set.add(entry)
            && (! entry.getValue().getName()
                  .startsWith(Task.class.getPackage().getName()))) {
            node
                .add(attributes(tag, helper))
                .add(content(depth + 1, set, tag, helper));

            if (helper.supportsCharacters()) {
                String content = "... text ...";

                if (node.hasChildNodes()) {
                    content =
                        "\n" + repeat(INDENTATION, depth + 1) + content + "\n";
                }

                node.add(text(content));
            }
        } else {
            node.add(attr(DOCUMENTED));
        }

        return node;
    }

    private Node[] attributes(Tag tag, IntrospectionHelper helper) {
        Node[] array =
            helper.getAttributeMap().entrySet()
            .stream()
            .map(t -> attr(t.getKey(), t.getValue().getSimpleName()))
            .toArray(Node[]::new);

        return array;
    }

    private FluentNode content(int depth, Set<Map.Entry<?,?>> set,
                               Tag tag, IntrospectionHelper helper) {
        return fragment(helper.getNestedElementMap().entrySet()
                        .stream()
                        .map(t -> type(depth, set, tag, t)));
    }
}
