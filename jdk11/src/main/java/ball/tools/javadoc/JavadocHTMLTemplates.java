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
import ball.xml.FluentNode;
import ball.xml.HTMLTemplates;
import com.sun.source.doctree.DocTree;
import java.lang.annotation.Annotation;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Node;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
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
     * @param   tag             The context {@link DocTree}.
     * @param   context         The context {@link Element}.
     * @param   target          The {@link URI} {@code target}.
     *
     * @return  The {@link URI} (may be {@code null}).
     */
    public URI href(DocTree tag, Element context, Object target);

    /**
     * {@code <a href="}{@link TypeElement type}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link TypeElement}.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(DocTree tag, Element element, TypeElement target, Node node);

    /**
     * {@code <a href="}{@link TypeElement type}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Class}.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(DocTree tag, Element element, Class<?> target, Node node);

    /**
     * {@code <a href="}{@link Member}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Member}.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(DocTree tag, Element element, Member target, Node node);

    /**
     * {@code <a href="}{@link TypeElement type}{@code ">}{@link Node node}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Class} name.
     * @param   node            The child {@link Node} (may be
     *                          {@code null}).
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public FluentNode a(DocTree tag, Element element, String target, Node node);

    /**
     * {@code <a href="}{@link TypeElement type}{@code ">}{@code name}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Class}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(DocTree tag, Element element, Class<?> target) {
        return a(tag, element, target, (String) null);
    }

    /**
     * {@code <a href="}{@link Class}{@code ">}{@link #code(String) code(name)}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Class}.
     * @param   name            The link name.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(DocTree tag, Element element, Class<?> target, String name) {
        return a(tag, element, target, (name != null) ? code(name) : null);
    }

    /**
     * {@code <a href="}{@link Member}{@code ">}{@link Member#getName()}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Member}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(DocTree tag, Element element, Member target) {
        return a(tag, element, target, (String) null);
    }

    /**
     * {@code <a href="}{@link Member}{@code ">}{@link #code(String) code(name)}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Member}.
     * @param   name            The link name.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(DocTree tag, Element element, Member target, String name) {
        return a(tag, element, target, (name != null) ? code(name) : null);
    }

    /**
     * {@code <a href="}{@link Enum}{@code ">}{@link Enum#name() constant.name()}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   target          The target {@link Enum}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode a(DocTree tag, Element element, Enum<?> target) {
        return a(tag, element, target.getDeclaringClass(), target.name());
    }

    /**
     * Dispatches call to {@link #declaration(DocTree,Element,Field)} or
     * {@link #declaration(DocTree,Element,Method)} as appropriate.
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   member          The target {@link Member}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(DocTree tag, Element element, Member member) {
        FluentNode node = null;

        if (member instanceof Field) {
            node = declaration(tag, element, (Field) member);
        } else if (member instanceof Method) {
            node = declaration(tag, element, (Method) member);
        } else {
            throw new IllegalArgumentException(String.valueOf(member));
        }

        return node;
    }

    /**
     * Method to generate a {@link Field} declaration with javadoc
     * hyperlinks.
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   field           The target {@link Field}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(DocTree tag, Element element, Field field) {
        return fragment(modifiers(field.getModifiers()),
                        type(tag, element, field.getGenericType()),
                        code(SPACE),
                        a(tag, element, field, (String) null));
    }

    /**
     * Method to generate a {@link Method} declaration with javadoc
     * hyperlinks.
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   method          The target {@link Method}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(DocTree tag, Element element, Method method) {
        var node =
            fragment(modifiers(method.getModifiers()),
                     type(tag, element, method.getGenericReturnType()),
                     code(SPACE),
                     a(tag, element, method, (String) null));
        var parameters = method.getParameters();

        node.add(code("("));

        for (int i = 0; i < parameters.length; i += 1) {
            if (i > 0) {
                node.add(code(", "));
            }

            node.add(declaration(tag, element, parameters[i]));
        }

        node.add(code(")"));

        return node;
    }

    /**
     * Method to generate a {@link Parameter} declaration with javadoc
     * hyperlinks.
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   parameter       The target {@link Parameter}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode declaration(DocTree tag, Element element, Parameter parameter) {
        return fragment(modifiers(parameter.getModifiers()),
                        type(tag, element, parameter.getParameterizedType()),
                        code(SPACE),
                        code(parameter.getName()));
    }
    /*
     * public default FluentNode annotations(DocTree tag, AnnotatedElement element) {
     *     return annotations(tag, element.getDeclaredAnnotations());
     * }
     *
     * public default FluentNode annotations(DocTree tag, Annotation... annotations) {
     *     return fragment().add(Stream.of(annotations)
     *                           .map(t -> annotation(tag, t)));
     * }
     */
    /**
     * {@code <a href="}{@link Annotation}{@code ">}{@link #code(String) code(String.valueOf(annotation))}{@code </a>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   annotation      The target {@link Annotation}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode annotation(DocTree tag, Element element, Annotation annotation) {
        var type = annotation.annotationType();
        var string =
            String.valueOf(annotation)
            .replace(type.getCanonicalName(), type.getSimpleName())
            .replaceAll("[(][)]$", "");

        return fragment().add(a(href(tag, element, type), code(string)));
    }

    /**
     * Method to generate modifiers for {@code declaration()} methods.
     *
     * @param   modifiers       See {@link Modifier}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode modifiers(int modifiers) {
        var node = fragment();
        var string = Modifier.toString(modifiers);

        if (isNotEmpty(string)) {
            node.add(code(string + SPACE));
        }

        return node;
    }

    /**
     * Method to generate types for {@code declaration()} methods.
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   type            The target {@link Type}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    public default FluentNode type(DocTree tag, Element element, Type type) {
        FluentNode node = null;

        if (type instanceof ParameterizedType) {
            node =
                fragment(type(tag, element, ((ParameterizedType) type).getRawType()));

            var types = ((ParameterizedType) type).getActualTypeArguments();

            node = node.add(code("<"));

            for (int i = 0; i < types.length; i += 1) {
                if (i > 0) {
                    node.add(code(","));
                }

                node.add(type(tag, element, types[i]));
            }

            node.add(code(">"));
        } else if (type instanceof Class<?>) {
            node = a(tag, element, (Class<?>) type);
        } else {
            node = code(type.getTypeName());
        }

        return node;
    }

    /**
     * {@code <table>}{@link TableModel model}{@code </table>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   model           The {@link TableModel} to use to create the
     *                          new table {@link org.w3c.dom.Element}.
     * @param   stream          The {@link Stream} of {@link Node}s to
     *                          append to the newly created
     *                          {@link org.w3c.dom.Element}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode table(DocTree tag, Element element,
                                    TableModel model, Stream<Node> stream) {
        return table(tag, element, model, stream.toArray(Node[]::new));
    }

    /**
     * {@code <table>}{@link TableModel model}{@code </table>}
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   model           The {@link TableModel} to use to create the
     *                          new table {@link org.w3c.dom.Element}.
     * @param   nodes           The {@link Node}s to append to the newly
     *                          created
     *                          {@link org.w3c.dom.Element}.
     *
     * @return  {@link org.w3c.dom.Element}
     */
    public default FluentNode table(DocTree tag, Element element,
                                    TableModel model, Node... nodes) {
        var table = table();
        var names =
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
                                    .map(x -> td(toHTML(tag, element,
                                                        model.getValueAt(y, x))))))));

        return table.add(nodes);
    }

    /**
     * Method to get a Javadoc HTML representation of an {@link Object}.
     *
     * @param   tag             The context {@link DocTree}.
     * @param   element         The context {@link Element}.
     * @param   object          The target {@link Object}.
     *
     * @return  {@link org.w3c.dom.Node}
     */
    public default FluentNode toHTML(DocTree tag, Element element, Object object) {
        FluentNode node = null;

        if (object instanceof byte[]) {
            node =
                text(Stream.of(ArrayUtils.toObject((byte[]) object))
                     .map (t -> String.format("0x%02X", t))
                     .collect(joining(", ", "[", "]")));
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
            node = toHTML(tag, element, Arrays.asList((Object[]) object));
        } else if (object instanceof Type) {
            node = type(tag, element, (Type) object);
        } else if (object instanceof Enum<?>) {
            node = a(tag, element, (Enum<?>) object);
        } else if (object instanceof Field) {
            node = a(tag, element, (Field) object);
        } else if (object instanceof Constructor) {
            node = a(tag, element, (Constructor) object);
        } else if (object instanceof Method) {
            node = a(tag, element, (Method) object);
        } else if (object instanceof Collection<?>) {
            List<Node> nodes =
                ((Collection<?>) object)
                .stream()
                .map(t -> toHTML(tag, element, t))
                .collect(toList());

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
