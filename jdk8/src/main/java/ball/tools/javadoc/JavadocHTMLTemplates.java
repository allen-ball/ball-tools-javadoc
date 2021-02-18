package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2008 - 2021 Allen D. Ball
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
import ball.xml.FluentNode;
import ball.xml.HTMLTemplates;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.Tag;
import java.lang.annotation.Annotation;
/* import java.lang.reflect.AnnotatedElement; */
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Node;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Javadoc {@link HTMLTemplates}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface JavadocHTMLTemplates extends HTMLTemplates {

    /**
     * Method to create an {@link URI} {@code href} to the {@code target}
     * represented by the argument {@link Object}.
     *
     * @param   tag             The context {@link Tag}.
     * @param   target          The {@link URI} {@code target}.
     *
     * @return  The {@link URI} (may be {@code null}).
     */
    public URI href(Tag tag, Object target);

    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc type}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   target          The target {@link ProgramElementDoc}.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(Tag tag, ProgramElementDoc target, Node node);

    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc type}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   type            The target {@link Class}.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(Tag tag, Class<?> type, Node node);

    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc member}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   member          The target {@link Member}.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(Tag tag, Member member, Node node);

    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc type}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   name            The target {@link Class} name.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(Tag tag, String name, Node node);

    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc type}{@code ">}{@link com.sun.javadoc.ClassDoc#name() com.sun.javadoc.ClassDoc.name()}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   type            The target {@link Class}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(Tag tag, Class<?> type) {
        return a(tag, type, (String) null);
    }

    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc type}{@code ">}{@link #code(String) code(name)}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   type            The target {@link Class}.
     * @param   name            The link name.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(Tag tag, Class<?> type, String name) {
        return a(tag, type, (name != null) ? code(name) : null);
    }

    /**
     * {@code <a href="}{@link com.sun.javadoc.MemberDoc member}{@code ">}{@link com.sun.javadoc.MemberDoc#name() MemberDoc.name()}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   member          The target {@link Member}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(Tag tag, Member member) {
        return a(tag, member, (String) null);
    }

    /**
     * {@code <a href="}{@link com.sun.javadoc.MemberDoc member}{@code ">}{@link #code(String) code(name)}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   member          The target {@link Member}.
     * @param   name            The link name.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(Tag tag, Member member, String name) {
        return a(tag, member, (name != null) ? code(name) : null);
    }

    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc constant}{@code ">}{@link Enum#name() constant.name()}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   constant        The target {@link Enum}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(Tag tag, Enum<?> constant) {
        return a(tag, constant.getDeclaringClass(), constant.name());
    }

    /**
     * Dispatches call to {@link #declaration(Tag,Field)} or
     * {@link #declaration(Tag,Method)} as appropriate.
     *
     * @param   tag             The {@link Tag}.
     * @param   member          The target {@link Member}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(Tag tag, Member member) {
        FluentNode node = null;

        if (member instanceof Field) {
            node = declaration(tag, (Field) member);
        } else if (member instanceof Method) {
            node = declaration(tag, (Method) member);
        } else {
            throw new IllegalArgumentException(String.valueOf(member));
        }

        return node;
    }

    /**
     * Method to generate a {@link Field} declaration with javadoc
     * hyperlinks.
     *
     * @param   tag             The {@link Tag}.
     * @param   field           The target {@link Field}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(Tag tag, Field field) {
        return fragment(modifiers(field.getModifiers()),
                        type(tag, field.getGenericType()),
                        code(SPACE),
                        a(tag, field, (String) null));
    }

    /**
     * Method to generate a {@link Method} declaration with javadoc
     * hyperlinks.
     *
     * @param   tag             The {@link Tag}.
     * @param   method          The target {@link Method}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(Tag tag, Method method) {
        FluentNode node =
            fragment(modifiers(method.getModifiers()),
                     type(tag, method.getGenericReturnType()),
                     code(SPACE),
                     a(tag, method, (String) null));

        Parameter[] parameters = method.getParameters();

        node.add(code("("));

        for (int i = 0; i < parameters.length; i += 1) {
            if (i > 0) {
                node.add(code(", "));
            }

            node.add(declaration(tag, parameters[i]));
        }

        node.add(code(")"));

        return node;
    }

    /**
     * Method to generate a {@link Parameter} declaration with javadoc
     * hyperlinks.
     *
     * @param   tag             The {@link Tag}.
     * @param   parameter       The target {@link Parameter}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(Tag tag, Parameter parameter) {
        return fragment(modifiers(parameter.getModifiers()),
                        type(tag, parameter.getParameterizedType()),
                        code(SPACE),
                        code(parameter.getName()));
    }
    /*
     * public default FluentNode annotations(Tag tag, AnnotatedElement element) {
     *     return annotations(tag, element.getDeclaredAnnotations());
     * }
     *
     * public default FluentNode annotations(Tag tag, Annotation... annotations) {
     *     return fragment().add(Stream.of(annotations)
     *                           .map(t -> annotation(tag, t)));
     * }
     */
    /**
     * {@code <a href="}{@link com.sun.javadoc.ClassDoc annotation}{@code ">}{@link #code(String) code(String.valueOf(annotation))}{@code </a>}
     *
     * @param   tag             The {@link Tag}.
     * @param   annotation      The target {@link Annotation}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode annotation(Tag tag, Annotation annotation) {
        Class<?> type = annotation.annotationType();
        String string =
            String.valueOf(annotation)
            .replace(type.getCanonicalName(), type.getSimpleName())
            .replaceAll("[(][)]$", "");

        return fragment().add(a(href(tag, type), code(string)));
    }

    /**
     * Method to generate modifiers for {@code declaration()} methods.
     *
     * @param   modifiers       See {@link Modifier}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode modifiers(int modifiers) {
        FluentNode node = fragment();
        String string = Modifier.toString(modifiers);

        if (isNotEmpty(string)) {
            node.add(code(string + SPACE));
        }

        return node;
    }

    /**
     * Method to generate types for {@code declaration()} methods.
     *
     * @param   tag             The {@link Tag}.
     * @param   type            The target {@link Type}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode type(Tag tag, Type type) {
        FluentNode node = null;

        if (type instanceof ParameterizedType) {
            node =
                fragment(type(tag, ((ParameterizedType) type).getRawType()));

            Type[] types = ((ParameterizedType) type).getActualTypeArguments();

            node = node.add(code("<"));

            for (int i = 0; i < types.length; i += 1) {
                if (i > 0) {
                    node.add(code(","));
                }

                node.add(type(tag, types[i]));
            }

            node.add(code(">"));
        } else if (type instanceof Class<?>) {
            node = a(tag, (Class<?>) type);
        } else {
            node = code(type.getTypeName());
        }

        return node;
    }

    /**
     * {@code <table>}{@link TableModel model}{@code </table>}
     *
     * @param   tag             The {@link Tag}.
     * @param   model           The {@link TableModel} to use to create the
     *                          new table {@link org.w3c.dom.Element}.
     * @param   stream          The {@link Stream} of {@link Node}s to
     *                          append to the newly created
     *                          {@link org.w3c.dom.Element}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode table(Tag tag, TableModel model, Stream<Node> stream) {
        return table(tag, model, stream.toArray(Node[]::new));
    }

    /**
     * {@code <table>}{@link TableModel model}{@code </table>}
     *
     * @param   tag             The {@link Tag}.
     * @param   model           The {@link TableModel} to use to create the
     *                          new table {@link org.w3c.dom.Element}.
     * @param   nodes           The {@link Node}s to append to the newly
     *                          created
     *                          {@link org.w3c.dom.Element}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode table(Tag tag, TableModel model, Node... nodes) {
        FluentNode table = table();
        String[] names =
            IntStream.range(0, model.getColumnCount())
            .boxed()
            .map(model::getColumnName)
            .toArray(String[]::new);

        if (! isAllBlank(names)) {
            table.add(thead(tr(Stream.of(names).map(this::th))));
        }

        table
            .add(tbody(IntStream.range(0, model.getRowCount())
                       .boxed()
                       .map(y -> tr(IntStream.range(0, names.length)
                                    .boxed()
                                    .map(x -> td(toHTML(tag,
                                                        model.getValueAt(y, x))))))));

        return table.add(nodes);
    }

    /**
     * Method to get a Javadoc HTML representation of an {@link Object}.
     *
     * @param   tag             The {@link Tag}.
     * @param   object          The target {@link Object}.
     *
     * @return  {@link org.w3c.dom.Node}
     */
    public default FluentNode toHTML(Tag tag, Object object) {
        FluentNode node = null;

        if (object instanceof byte[]) {
            node =
                text(Stream.of(ArrayUtils.toObject((byte[]) object))
                     .map (t -> String.format("0x%02X", t))
                     .collect(Collectors.joining(", ", "[", "]")));
        } else if (object instanceof boolean[]) {
            node = text(Arrays.toString((boolean[]) object));
        } else if (object instanceof double[]) {
            node = text(Arrays.toString((double[]) object));
        } else if (object instanceof float[]) {
            node = text(Arrays.toString((float[]) object));
        } else if (object instanceof int[]) {
            node = text(Arrays.toString((int[]) object));
        } else if (object instanceof long[]) {
            node = text(Arrays.toString((long[]) object));
        } else if (object instanceof Object[]) {
            node = toHTML(tag, Arrays.asList((Object[]) object));
        } else if (object instanceof Type) {
            node = type(tag, (Type) object);
        } else if (object instanceof Enum<?>) {
            node = a(tag, (Enum<?>) object);
        } else if (object instanceof Field) {
            node = a(tag, (Field) object);
        } else if (object instanceof Constructor) {
            node = a(tag, (Constructor) object);
        } else if (object instanceof Method) {
            node = a(tag, (Method) object);
        } else if (object instanceof Collection<?>) {
            List<Node> nodes =
                ((Collection<?>) object)
                .stream()
                .map(t -> toHTML(tag, t))
                .collect(Collectors.toList());

            for (int i = nodes.size() - 1; i > 0; i -= 1) {
                nodes.add(i, text(", "));
            }

            node =
                fragment()
                .add(text("["))
                .add(nodes.stream())
                .add(text("]"));
        } else {
            node = text(String.valueOf(object));
        }

        return node;
    }
}
