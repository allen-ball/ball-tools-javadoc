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
import ball.annotation.processing.JavaxLangModelUtilities;
import ball.xml.FluentDocument;
import ball.xml.FluentDocumentBuilderFactory;
import ball.xml.FluentNode;
import ball.xml.XalanConstants;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;
import com.sun.source.util.TreePath;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileManager;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.w3c.dom.Node;

import static javax.tools.Diagnostic.Kind.WARNING;
import static javax.tools.StandardLocation.CLASS_PATH;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static lombok.AccessLevel.PROTECTED;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Abstract {@link jdk.javadoc.doclet.Taglet} base class.
 * See {@link #toNode(List,Element)}.
 *
 * <p>Note: {@link #getName()} implementation requires the subclass is
 * annotated with {@link TagletName}.</p>
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractTaglet extends JavaxLangModelUtilities
                                     implements AnnotatedTaglet,
                                                JavadocHTMLTemplates,
                                                XalanConstants {

    /**
     * Implementation method for
     * {@code public static void register(Map<Object,Object> map)}.
     *
     * @param   map             The {@link Map} to update.
     * @param   taglet          The {@link AbstractTaglet} instance to
     *                          register.
     */
    protected static void register(Map<Object,Object> map,
                                   AbstractTaglet taglet) {
        map.remove(taglet.getName());
        map.put(taglet.getName(), taglet);
    }

    private final Transformer transformer;
    private final FluentDocument document;
    private DocletEnvironment env = null;
    private Doclet doclet = null;
    /** See {@link DocletEnvironment#getDocTrees()}. */
    protected DocTrees trees = null;
    private transient Method href = null;

    {
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OMIT_XML_DECLARATION, YES);
            transformer.setOutputProperty(INDENT, NO);

            document =
                FluentDocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();
            document
                .add(element("html",
                             element("head",
                                     element("meta",
                                             attr("charset", "utf-8"))),
                             element("body")));
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @Override
    public FluentDocument document() { return document; }

    @Override
    public void init(DocletEnvironment env, Doclet doclet) {
        this.env = env;
        this.doclet = doclet;

        elements = env.getElementUtils();
        types = env.getTypeUtils();
        fm = env.getJavaFileManager();
        trees = env.getDocTrees();
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        Node node = null;

        try {
            node = toNode(tags, element);
        } catch (Throwable throwable) {
            node = warning(tags, element, throwable);
        }

        return render(node);
    }

    /**
     * Abstract method to be overridden by subclass implementations.
     *
     * @param   tags            The list of instances of this tag.
     * @param   element         The element to which the enclosing comment
     *                          belongs.
     *
     * @return  The {@link Node} representing the output.
     *
     * @throws  Throwable       As required by the subclass.
     */
    protected abstract Node toNode(List<? extends DocTree> tags,
                                   Element element) throws Throwable;

    /**
     * Method to render a {@link Node} to a {@link String} without
     * formatting or indentation.
     *
     * @param   node            The {@link Node}.
     *
     * @return  The {@link String} representation.
     *
     * @throws  RuntimeException
     *                          Instead of checked {@link Exception}.
     */
    protected String render(Node node) { return render(node, 0); }

    /**
     * Method to render a {@link Node} to a {@link String} with or without
     * formatting or indentation.
     *
     * @param   node            The {@link Node}.
     * @param   indent          The amount to indent; {@code <= 0} for no
     *                          indentation.
     *
     * @return  The {@link String} representation.
     *
     * @throws  RuntimeException
     *                          Instead of checked {@link Exception}.
     */
    protected String render(Node node, int indent) {
        StringWriter writer = new StringWriter();

        try {
            transformer
                .setOutputProperty(INDENT, (indent > 0) ? YES : NO);
            transformer
                .setOutputProperty(XALAN_INDENT_AMOUNT.toString(),
                                   String.valueOf(indent > 0 ? indent : 0));
            transformer
                .transform(new DOMSource(node),
                           new StreamResult(writer));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Error error) {
            throw error;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return writer.toString();
    }

    /**
     * {@code <}{@code p><b><u>}{@link DocTree tag}{@code </u></b></p}{@code >}
     * {@code <!}{@code -- }{@link Throwable stack trace}{@code --}{@code >}
     *
     * @param   tags            The list of instances of this tag.
     * @param   element         The element to which the enclosing comment
     *                          belongs.
     * @param   throwable       The {@link Throwable}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    protected FluentNode warning(List<? extends DocTree> tags, Element element,
                                 Throwable throwable) {
        return warning(tags.get(0), element, throwable);
    }

    /**
     * {@code <}{@code p><b><u>}{@link DocTree tag}{@code </u></b></p}{@code >}
     * {@code <!}{@code -- }{@link Throwable stack trace}{@code --}{@code >}
     *
     * @param   tag             The tag.
     * @param   element         The element to which the enclosing comment
     *                          belongs.
     * @param   throwable       The {@link Throwable}.
     *
     * @return  {@link org.w3c.dom.DocumentFragment}
     */
    protected FluentNode warning(DocTree tag, Element element,
                                 Throwable throwable) {
        DocCommentTree comment = trees.getDocCommentTree(element);

        trees.printMessage(WARNING, throwable.toString(), tag, comment, null);

        String string = "@" + getName();

        if (isNotEmpty(getText(tag))) {
            string += SPACE + getText(tag);
        }

        if (isInlineTag()) {
            string = "{" + string + "}";
        }

        return fragment(p(b(u(string))),
                        comment(ExceptionUtils.getStackTrace(throwable)));
    }

    /**
     * Method to get a {@link Class}'s resource path.
     *
     * @param   type            The {@link Class}.
     *
     * @return  The {@link Class}'s resource path (as a {@link String}).
     */
    protected String getResourcePathOf(Class<?> type) {
        String path =
            String.join("/", type.getName().split(Pattern.quote(".")))
            + ".class";

        return path;
    }

    /**
     * Method to get the {@link URL} to a {@link Class}.
     *
     * @param   type            The {@link Class}.
     *
     * @return  The {@link Class}'s {@link URL}.
     */
    protected URL getResourceURLOf(Class<?> type) {
        return type.getResource("/" + getResourcePathOf(type));
    }

    /**
     * See {@link Introspector#getBeanInfo(Class,Class)}.
     *
     * @param   start           The start {@link Class}.
     * @param   stop            The stop {@link Class}.
     *
     * @return  {@link BeanInfo}
     *
     * @throws  RuntimeException
     *                          Instead of checked {@link Exception}.
     */
    protected BeanInfo getBeanInfo(Class<?> start, Class<?> stop) {
        BeanInfo info = null;

        try {
            info = Introspector.getBeanInfo(start, stop);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Error error) {
            throw error;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return info;
    }

    /**
     * See {@link #getBeanInfo(Class,Class)}.
     *
     * @param   start           The start {@link Class}.
     *
     * @return  {@link BeanInfo}
     *
     * @throws  RuntimeException
     *                          Instead of checked {@link Exception}.
     */
    protected BeanInfo getBeanInfo(Class<?> start) {
        return getBeanInfo(start, Object.class);
    }

    @Override
    public URI href(DocTree tag, Object target) {
        URI uri = null;

        if (href == null) {
            href = new Object() { }.getClass().getEnclosingMethod();
        }

        if (target != null) {
            try {
                Method method =
                    AbstractTaglet.class
                    .getDeclaredMethod(href.getName(),
                                       DocTree.class, target.getClass());

                if (! Objects.equals(href, method)) {
                    uri = (URI) method.invoke(this, tag, target);
                }
            } catch (Exception exception) {
                trees.printMessage(WARNING,
                                   "No method to get href for "
                                   + target.getClass().getName(),
                                   tag, null, null);
            }
        }

        return uri;
    }

    @Override
    public FluentNode a(DocTree tag, TypeElement target, Node node) {
        URI href = href(tag, target);

        if (node == null) {
            Name name =
                (href != null) ? target.getSimpleName() : target.getQualifiedName();

            node = code(name.toString());
        }

        return a(href, node);
    }

    @Override
    public FluentNode a(DocTree tag, Class<?> target, Node node) {
        String brackets = EMPTY;

        while (target.isArray()) {
            brackets = "[]" + brackets;
            target = target.getComponentType();
        }

        URI href = href(tag, target);

        if (node == null) {
            String name =
                (href != null) ? target.getSimpleName() : target.getCanonicalName();

            node = code(name + brackets);
        }

        return a(href, node);
    }

    @Override
    public FluentNode a(DocTree tag, Member member, Node node) {
        if (node == null) {
            node = code(member.getName());
        }

        return a(href(tag, member), node);
    }

    @Override
    public FluentNode a(DocTree tag, String name, Node node) {
        TypeElement target = null;

        if (node == null) {
            if (target != null) {
                node = code(target.getSimpleName().toString());
            } else {
                node = code(name);
            }
        }

        return a(tag, target, node);
    }

    /**
     * Method to get the text ({@link String}) associated with a tag
     * ({link DocTree}).
     *
     * @param   tag             The {@link DocTree}.
     *
     * @return  The text.
     */
    protected String getText(DocTree tag) {
        return new GetTextVisitor().visit(tag, null);
    }

    @NoArgsConstructor @ToString
    private class GetTextVisitor extends SimpleDocTreeVisitor<String,Void> {
        @Override
        public String visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
            String text =
                node.getContent()
                .stream()
                .map(t -> node.accept(this, p))
                .findFirst().orElse(EMPTY);

            return text;
        }

        @Override
        public String visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
            String text =
                node.getContent()
                .stream()
                .map(t -> node.accept(this, p))
                .findFirst().orElse(EMPTY);

            return text;
        }

        @Override
        public String visitText(TextTree node, Void p) {
            return node.getBody();
        }

        @Override
        protected String defaultAction(DocTree node, Void p) { return EMPTY; }
    }
}
