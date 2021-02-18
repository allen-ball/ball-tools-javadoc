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
import ball.annotation.CompileTimeCheck;
import ball.annotation.ServiceProviderFor;
import ball.util.PropertiesImpl;
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static lombok.AccessLevel.PROTECTED;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Abstract base class for inline {@link Taglet}s that load
 * {@link.uri https://maven.apache.org/index.html Maven} artifacts.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class MavenTaglet extends AbstractInlineTaglet {
    private static final XPath XPATH = XPathFactory.newInstance().newXPath();

    private static final String POM_XML = "pom.xml";
    private static final String DEPENDENCY = "dependency";
    private static final String GROUP_ID = "groupId";
    private static final String ARTIFACT_ID = "artifactId";
    private static final String VERSION = "version";

    protected XPathExpression compile(String format, Object... argv) {
        XPathExpression expression = null;

        try {
            expression = XPATH.compile(String.format(format, argv));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }

        return expression;
    }

    /**
     * Method to locate the POM from an {@link Element}.
     *
     * @param   tag             The {@link UnknownInlineTagTree}.
     * @param   context         The element to which the enclosing comment
     *                          belongs.
     *
     * @return  The POM {@link File}.
     *
     * @throws  Exception       If the POM {@link File} cannot be found.
     */
    protected File getPomFileFor(UnknownInlineTagTree tag, Element context) throws Exception {
        var name = defaultIfBlank(getText(tag).trim(), POM_XML);
        var parent =
            new File(trees.getPath(context).getCompilationUnit()
                     .getSourceFile().toUri())
            .getParentFile();
        var file = new File(parent, name);

        while (parent != null) {
            file = new File(parent, name);

            if (file.isFile()) {
                break;
            } else {
                file = null;
            }

            parent = parent.getParentFile();
        }

        if (! (file != null && file.isFile())) {
            throw new FileNotFoundException(name);
        }

        return file;
    }

    /**
     * Inline {@link Taglet} to provide a report of fields whose values are
     * configured by the
     * {@link.uri https://maven.apache.org/index.html Maven}
     * {@link.uri https://maven.apache.org/plugin-developers/index.html Plugin}
     * {@code plugin.xml}.
     */
    @TagletName("maven.plugin.fields")
    @ServiceProviderFor({ Taglet.class })
    @NoArgsConstructor @ToString
    public static class PluginFields extends MavenTaglet {
        private static final String PLUGIN_XML = "META-INF/maven/plugin.xml";

        @Override
        public FluentNode toNode(UnknownInlineTagTree tag, Element context) throws Throwable {
            TypeElement type = null;
            var argv = getText(tag).trim().split("[\\p{Space}]+", 2);

            if (isNotEmpty(argv[0])) {
                type = getTypeElementFor(context, argv[0]);
            } else {
                type = getEnclosingTypeElement(context);
            }

            var url = getResourceURLOf(asClass(type));
            var protocol = Protocol.of(url);
            Document document = null;

            switch (protocol) {
            case FILE:
                var root =
                    url.getPath()
                    .replaceAll(Pattern.quote(getResourcePathOf(asClass(type))), EMPTY);

                document =
                    DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(root + PLUGIN_XML);
                break;

            case JAR:
                var jar = protocol.getJarFile(url);
                var entry = jar.getEntry(PLUGIN_XML);

                try (var in = jar.getInputStream(entry)) {
                    document =
                        DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(in);
                }
                break;
            }

            if (document == null) {
                throw new IllegalStateException("Cannot find " + PLUGIN_XML);
            }

            var mojo =
                (Node)
                compile("/plugin/mojos/mojo[implementation='%s']",
                        type.getQualifiedName())
                .evaluate(document, NODE);

            return div(attr("class", "summary"),
                       h3("Maven Plugin Parameter Summary"),
                       table(tag, context, asClass(type), mojo,
                             asStream((NodeList)
                                      compile("parameters/parameter")
                                      .evaluate(mojo, NODESET))));
        }

        private FluentNode table(UnknownInlineTagTree tag, Element context,
                                 Class<?> type,
                                 Node mojo, Stream<Node> parameters) {
            return table(thead(tr(th(EMPTY), th("Field"),
                                  th("Default"), th("Property"),
                                  th("Required"), th("Editable"),
                                  th("Description"))),
                         tbody(parameters.map(t -> tr(tag, context, type, mojo, t))));
        }

        private FluentNode tr(UnknownInlineTagTree tag, Element context,
                              Class<?> type, Node mojo, Node parameter) {
            var tr = fragment();

            try {
                var name = compile("name").evaluate(parameter);
                var field = FieldUtils.getField(type, name, true);

                if (field != null) {
                    tr =
                        tr(td((! type.equals(field.getDeclaringClass()))
                                  ? type(tag, context, field.getDeclaringClass())
                                  : text(EMPTY)),
                           td(declaration(tag, context, field)),
                           td(code(compile("configuration/%s/@default-value", name)
                                   .evaluate(mojo))),
                           td(code(compile("configuration/%s", name)
                                   .evaluate(mojo))),
                           td(code(compile("required").evaluate(parameter))),
                           td(code(compile("editable").evaluate(parameter))),
                           td(p(compile("description").evaluate(parameter))));
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }

            return tr;
        }
    }

    /**
     * Inline {@jdk.javadoc.doclet.link Taglet} to include generated
     * {@link.uri https://maven.apache.org/index.html Maven}
     * {@link.uri https://maven.apache.org/plugin-developers/index.html Plugin}
     * help documentation.
     */
    @TagletName("maven.plugin.help")
    @ServiceProviderFor({ Taglet.class })
    @NoArgsConstructor @ToString
    public static class PluginHelp extends MavenTaglet {
        private static final String NAME = "plugin-help.xml";
        @CompileTimeCheck
        private static final Pattern PATTERN =
            Pattern.compile("META-INF/maven/(?<g>[^/]+)/(?<a>[^/]+)/"
                            + Pattern.quote(NAME));

        @Override
        public FluentNode toNode(UnknownInlineTagTree tag, Element context) throws Throwable {
            Class<?> type = null;

            if (context instanceof PackageElement) {
                type = asPackageInfoClass((PackageElement) context);
            } else {
                type = asClass(getEnclosingTypeElement(context));
            }

            var url = getResourceURLOf(type);
            var protocol = Protocol.of(url);
            Document document = null;

            switch (protocol) {
            case FILE:
                var root =
                    Paths.get(url.getPath()
                              .replaceAll(Pattern.quote(getResourcePathOf(type)), EMPTY));
                var path =
                    Files.walk(root, Integer.MAX_VALUE)
                    .filter(Files::isRegularFile)
                    .filter(t -> PATTERN.matcher(root.relativize(t).toString()).matches())
                    .findFirst().orElse(null);

                document =
                    DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(path.toFile());
                break;

            case JAR:
                try (var jar = protocol.getJarFile(url)) {
                    var entry =
                        jar.stream()
                        .filter(t -> PATTERN.matcher(t.getName()).matches())
                        .findFirst().orElse(null);

                    try (var in = jar.getInputStream(entry)) {
                        document =
                            DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(in);
                    }
                }
                break;
            }

            if (document == null) {
                throw new IllegalStateException("Cannot find " + NAME);
            }

            return div(attr("class", "summary"),
                       h3(compile("/plugin/name").evaluate(document)),
                       p(compile("/plugin/description").evaluate(document)),
                       table(tag, context,
                             asStream((NodeList)
                                      compile("/plugin/mojos/mojo")
                                      .evaluate(document, NODESET))));
        }

        private FluentNode table(UnknownInlineTagTree tag, Element context,
                                 Stream<Node> mojos) {
            return table(thead(tr(th("Goal"), th("Phase"), th("Description"))),
                         tbody(mojos.map(t -> tr(tag, context, t))));
        }

        private FluentNode tr(UnknownInlineTagTree tag, Element context,
                              Node mojo) {
            var tr = fragment();

            try {
                tr =
                    tr(td(a(tag, context,
                            compile("implementation").evaluate(mojo),
                            code(compile("goal").evaluate(mojo)))),
                       td(code(compile("phase").evaluate(mojo))),
                       td(p(code(compile("description").evaluate(mojo)))));
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }

            return tr;
        }
    }

    /**
     * {@link jdk.javadoc.doclet.Taglet} to provide
     * {@link.uri https://maven.apache.org/pom.html POM} coordinates as
     * a {@code <dependency/>} element to include this documented
     * {@link Class} or {@link Package}.
     *
     * <p>For example:</p>
     *
     * {@pom.coordinates}
     */
    @TagletName("pom.coordinates")
    @ServiceProviderFor({ Taglet.class })
    @NoArgsConstructor @ToString
    public static class Coordinates extends MavenTaglet {
        @CompileTimeCheck
        private static final Pattern PATTERN =
            Pattern.compile("META-INF/maven/(?<g>[^/]+)/(?<a>[^/]+)/pom[.]properties");

        @Override
        public FluentNode toNode(UnknownInlineTagTree tag, Element context) throws Throwable {
            var properties = new POMProperties();
            Class<?> type = null;

            if (context instanceof PackageElement) {
                type = asPackageInfoClass((PackageElement) context);
            } else {
                type = asClass(getEnclosingTypeElement(context));
            }

            var url = getResourceURLOf(type);
            var protocol = Protocol.of(url);

            switch (protocol) {
            case FILE:
                var document =
                    DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(getPomFileFor(tag, context));

                Stream.of(GROUP_ID, ARTIFACT_ID, VERSION)
                    .forEach(t -> properties.load(t, document, "/project/"));
                Stream.of(VERSION)
                    .forEach(t -> properties.load(t, document, "/project/parent/"));
                break;

            case JAR:
                try (var jar = protocol.getJarFile(url)) {
                    var entry =
                        jar.stream()
                        .filter(t -> PATTERN.matcher(t.getName()).matches())
                        .findFirst().orElse(null);

                    if (entry != null) {
                        try (var in = jar.getInputStream(entry)) {
                            properties.load(in);
                        }
                    }
                }
                break;
            }

            return pre("xml",
                       render(element(DEPENDENCY,
                                      Stream.of(GROUP_ID, ARTIFACT_ID, VERSION)
                                      .map(t -> element(t).content(properties.getProperty(t, "unknown")))),
                              2));
        }
    }

    private static enum Protocol {
        FILE, JAR;

        public static Protocol of(URL url) {
            return valueOf(url.getProtocol().toUpperCase());
        }

        public JarFile getJarFile(URL url) throws IOException {
            JarFile jar = null;

            switch (this) {
            case JAR:
                jar = ((JarURLConnection) url.openConnection()).getJarFile();
                break;

            default:
                throw new IllegalStateException();
                /* break; */
            }

            return jar;
        }
    }

    private static class POMProperties extends PropertiesImpl {
        private static final long serialVersionUID = 1354153174028868013L;

        public String load(String key, Document document, String prefix) {
            if (document != null) {
                computeIfAbsent(key, k -> evaluate(prefix + k, document));
            }

            return super.getProperty(key);
        }

        private String evaluate(String expression, Document document) {
            String value = null;

            try {
                value = XPATH.evaluate(expression, document);
            } catch (Exception exception) {
            }

            return isNotEmpty(value) ? value : null;
        }
    }
}
