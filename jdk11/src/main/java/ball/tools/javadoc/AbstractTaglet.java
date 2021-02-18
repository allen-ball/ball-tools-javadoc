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
import ball.annotation.processing.JavaxLangModelUtilities;
import ball.xml.FluentDocument;
import ball.xml.FluentDocumentBuilderFactory;
import ball.xml.FluentNode;
import ball.xml.XalanConstants;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
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

import static java.util.stream.Collectors.joining;
import static javax.tools.Diagnostic.Kind.WARNING;
import static javax.xml.transform.OutputKeys.INDENT;
import static javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION;
import static lombok.AccessLevel.PROTECTED;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
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
    private final Transformer transformer;
    private final FluentDocument document;
    private DocletEnvironment env = null;
    private Doclet doclet = null;
    /** See {@link DocletEnvironment#getDocTrees()}. */
    protected DocTrees trees = null;
    private Map<String,URI> extern = null;
    private transient ClassLoader loader = null;
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
            print(WARNING, tags.get(0), element, "%s", throwable.toString());
            node = toNode(tags.get(0), element, throwable);
        }

        return render(node);
    }

    /**
     * Abstract method to be overridden by subclass implementations.
     *
     * @param   tags            The list of instances of this tag.
     * @param   context         The element to which the enclosing comment
     *                          belongs.
     *
     * @return  The {@link Node} representing the output.
     *
     * @throws  Throwable       As required by the subclass.
     */
    protected abstract Node toNode(List<? extends DocTree> tags, Element context) throws Throwable;

    private FluentNode toNode(DocTree tag, Element context, Throwable throwable) {
        var string = "@" + getName();

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
        var writer = new StringWriter();

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
     * Method to print a diagnostic message.
     *
     * @param   kind            The {@link javax.tools.Diagnostic.Kind}.
     * @param   tag             The offending {@link DocTree}.
     * @param   element         The offending {@link Element}.
     * @param   format          The message format {@link String}.
     * @param   argv            Optional arguments to the message format
     *                          {@link String}.
     *
     * @see DocTrees#printMessage(Diagnostic.Kind,CharSequence,DocTree,DocCommentTree,CompilationUnitTree)
     */
    protected void print(Diagnostic.Kind kind, DocTree tag, Element element,
                         String format, Object... argv) {
        var comment = trees.getDocCommentTree(element);
        var path = trees.getPath(element);
        var unit = (path != null) ? path.getCompilationUnit() : null;

        print(kind, tag, comment, unit, format, argv);
    }

    /**
     * Method to print a diagnostic message.
     *
     * @param   kind            The {@link javax.tools.Diagnostic.Kind}.
     * @param   tag             The offending {@link DocTree}.
     * @param   comment         The offending {@link DocCommentTree}.
     * @param   unit            The offending {@link CompilationUnitTree}.
     * @param   format          The message format {@link String}.
     * @param   argv            Optional arguments to the message format
     *                          {@link String}.
     *
     * @see DocTrees#printMessage(Diagnostic.Kind,CharSequence,DocTree,DocCommentTree,CompilationUnitTree)
     */
    protected void print(Diagnostic.Kind kind, DocTree tag,
                         DocCommentTree comment, CompilationUnitTree unit,
                         String format, Object... argv) {
        trees.printMessage(kind, String.format(format, argv),
                           tag, comment, unit);
    }

    /**
     * Method to get a {@link Class}'s resource path.
     *
     * @param   type            The {@link Class}.
     *
     * @return  The {@link Class}'s resource path (as a {@link String}).
     */
    protected String getResourcePathOf(Class<?> type) {
        var path =
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
    protected ClassLoader getClassLoader() {
        if (loader == null) {
            loader = getClassPathClassLoader(fm, getClass().getClassLoader());
        }

        return loader;
    }

    @Override
    public URI href(DocTree tag, Element context, Object target) {
        URI uri = null;

        if (href == null) {
            href = new Object() { }.getClass().getEnclosingMethod();
        }

        if (target != null) {
            var parameters =
                Stream.of(DocTree.class, Element.class, target.getClass())
                .toArray(Class<?>[]::new);

            try {
                var method =
                    AbstractTaglet.class
                    .getDeclaredMethod(href.getName(), parameters);

                if (Objects.equals(href, method)
                    || (! href.getReturnType().isAssignableFrom(method.getReturnType()))) {
                    throw new NoSuchMethodException();
                }

                var arguments =
                    Stream.of(tag, context, target)
                    .toArray(Object[]::new);

                uri = (URI) method.invoke(this, arguments);
            } catch (Exception exception) {
                print(WARNING, tag, context,
                      "No method to get href for %s",
                      parameters[parameters.length - 1].getName());
            }
        }

        return uri;
    }

    private URI href(DocTree tag, Element context, Class<?> target) {
        return href(tag, context, target, null);
    }

    private URI href(DocTree tag, Element context, Constructor<?> target) {
        var type = target.getDeclaringClass();

        return href(tag, context, type,
                    type.getSimpleName() + signature(target).replaceAll("[(),]", "-"));
    }

    private URI href(DocTree tag, Element context, Field target) {
        return href(tag, context, target.getDeclaringClass(), target.getName());
    }

    private URI href(DocTree tag, Element context, Method target) {
        return href(tag, context, target.getDeclaringClass(),
                    target.getName() + signature(target).replaceAll("[(),]", "-"));
    }

    private URI href(DocTree tag, Element context, TypeElement target) {
        return href(tag, context, target, null);
    }

    private URI href(DocTree tag, Element context, ExecutableElement target) {
        URI href = null;

        if (target != null && env.isIncluded(target)) {
            Element enclosing = target.getEnclosingElement();

            if (enclosing instanceof TypeElement) {
                href =
                    href(tag, context, (TypeElement) enclosing,
                         target.getSimpleName() + signature(target).replaceAll("[(),]", "-"));
            }
        }

        return href;
    }

    private URI href(DocTree tag, Element context, VariableElement target) {
        URI href = null;

        if (target != null && env.isIncluded(target)) {
            var enclosing = target.getEnclosingElement();

            if (enclosing instanceof TypeElement) {
                href =
                    href(tag, context, (TypeElement) enclosing,
                         target.getSimpleName().toString());
            }
        }

        return href;
    }

    private URI href(DocTree tag, Element context, Class<?> target, String fragment) {
        var href = href(tag, context, asTypeElement(target), fragment);

        if (href == null) {
            href = extern(tag, context).get(target);

            if (href != null) {
                String path = target.getCanonicalName() + ".html";

                path =
                    Stream.concat(Stream.of(getComponentsOf(target.getPackage())),
                                  Stream.of(path))
                    .collect(joining("/"));

                href = href(href, path, fragment);
            }
        }

        return href;
    }

    private URI href(DocTree tag, Element context, TypeElement target, String fragment) {
        URI href = null;

        if (target != null) {
            var path = getCanonicalNameOf(target) + ".html";

            path =
                Stream.concat(Stream.of(getComponentsOf(elements.getPackageOf(target))),
                              Stream.of(path))
                .collect(joining("/"));

            if (env.isIncluded(target)) {
                int depth = getComponentsOf(elements.getPackageOf(context)).length;
                path =
                    Stream.concat(Stream.generate(() -> "..").limit(depth),
                                  Stream.of(path))
                    .collect(joining("/"));

                href = href(null, path, fragment);
            } else {
                href = extern(tag, context).get(elements.getPackageOf(target));

                if (href != null) {
                    href = href(href, path, fragment);
                }
            }
        }

        return href;
    }

    private URI href(URI base, String path, String fragment) {
        URI href = null;

        if (base != null) {
            href = base.resolve(path + "?is-external=true");
        } else {
            href = URI.create("./" + path);
        }

        href = href.normalize();

        if (fragment != null) {
            href = href.resolve("#" + fragment);
        }

        return href;
    }

    @SuppressWarnings({ "unchecked" })
    private Map<String,URI> extern(DocTree tag, Element context) {
        if (extern == null) {
            try {
                /*
                 * The Javadoc tool creates each Doclet and Taglet class in
                 * a different ClassLoader necessitating access by
                 * reflection look-up.
                 */
                extern =
                    (Map<String,URI>)
                    doclet.getClass().getField("extern").get(doclet);
            } catch (Exception exception) {
                extern = new Extern();
                print(WARNING, tag, context,
                      "Configure '-doclet %s' for external links",
                      StandardDoclet.class.getCanonicalName());
            }
        }

        return extern;
    }

    private String[] getComponentsOf(Package pkg) {
        return (pkg == null) ? new String[] { } : getComponentsOf(pkg.getName());
    }

    private String[] getComponentsOf(PackageElement element) {
        return element.isUnnamed() ? new String[] { } : getComponentsOf((QualifiedNameable) element);
    }

    private String[] getComponentsOf(QualifiedNameable element) {
        return getComponentsOf(element.getQualifiedName());
    }

    private String[] getComponentsOf(CharSequence sequence) {
        var strings = new String[] { };

        if (sequence != null && sequence.length() > 0) {
            strings = sequence.toString().split(Pattern.quote("."));
        }

        return strings;
    }

    private String getCanonicalNameOf(TypeElement element) {
        var string =
            Stream.of(getComponentsOf(element))
            .skip(getComponentsOf(elements.getPackageOf(element)).length)
            .collect(joining("."));

        return string;
    }

    @Override
    public FluentNode a(DocTree tag, Element element, TypeElement target, Node node) {
        URI href = href(tag, element, target);

        if (node == null) {
            var name =
                (href != null) ? target.getSimpleName() : target.getQualifiedName();

            node = code(name.toString());
        }

        return a(href, node);
    }

    @Override
    public FluentNode a(DocTree tag, Element element, Class<?> target, Node node) {
        var brackets = EMPTY;

        while (target.isArray()) {
            brackets = "[]" + brackets;
            target = target.getComponentType();
        }

        var href = href(tag, element, target);

        if (node == null) {
            var name = target.getCanonicalName();

            if (href != null) {
                name = target.getSimpleName();

                var enclosing = target.getEnclosingClass();

                while (enclosing != null) {
                    name = enclosing.getSimpleName() + "." + name;
                    enclosing = enclosing.getEnclosingClass();
                }
            }

            node = code(name + brackets);
        }

        return a(href, node);
    }

    @Override
    public FluentNode a(DocTree tag, Element element, Member member, Node node) {
        if (node == null) {
            node = code(member.getName());
        }

        return a(href(tag, element, member), node);
    }

    @Override
    public FluentNode a(DocTree tag, Element element, String name, Node node) {
        TypeElement target = null;

        if (node == null) {
            if (target != null) {
                node = code(target.getSimpleName().toString());
            } else {
                node = code(name);
            }
        }

        return a(tag, element, target, node);
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
        public String visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
            var text =
                node.getContent()
                .stream()
                .map(t -> t.accept(this, p))
                .filter(Objects::nonNull)
                .collect(joining(SPACE, EMPTY, EMPTY));

            return text.trim();
        }

        @Override
        public String visitText(TextTree node, Void p) {
            return node.getBody();
        }

        @Override
        protected String defaultAction(DocTree node, Void p) { return EMPTY; }
    }
}
