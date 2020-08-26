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
import ball.util.PropertiesImpl;
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
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
     * @param   element         The {@link Element}.
     *
     * @return  The POM {@link File}.
     *
     * @throws  Exception       If the POM {@link File} cannot be found.
     */
    protected File getPomFileFor(UnknownInlineTagTree tag,
                                 Element element) throws Exception {
        String name = defaultIfBlank(getText(tag).trim(), POM_XML);
        File parent =
            new File(trees.getPath(element).getCompilationUnit()
                     .getSourceFile().toUri())
            .getParentFile();
        File file = new File(parent, name);

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
     * Inline {@link jdk.javadoc.doclet.Taglet} to provide a report of
     * fields whose values are configured by the
     * {@link.uri https://maven.apache.org/index.html Maven}
     * {@link.uri https://maven.apache.org/plugin-developers/index.html Plugin}
     * {@code plugin.xml}.
     */
    @TagletName("maven.plugin.fields")
    @NoArgsConstructor @ToString
    public static class PluginFields extends MavenTaglet {
        private static final PluginFields INSTANCE = new PluginFields();

        public static void register(Map<Object,Object> map) {
            register(map, INSTANCE);
        }

        private static final String PLUGIN_XML = "META-INF/maven/plugin.xml";

        @Override
        public FluentNode toNode(UnknownInlineTagTree tag,
                                 Element element) throws Throwable {
            TypeElement type = null;
            String[] argv = getText(tag).trim().split("[\\p{Space}]+", 2);

            if (isNotEmpty(argv[0])) {
                type = getTypeElementFor(element, argv[0]);
            } else {
                type = getEnclosingTypeElement(element);
            }

            URL url = getResourceURLOf(asClass(type));
            Protocol protocol = Protocol.of(url);
            Document document = null;

            switch (protocol) {
            case FILE:
                String root =
                    url.getPath()
                    .replaceAll(Pattern.quote(getResourcePathOf(asClass(type))), EMPTY);

                document =
                    DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(root + PLUGIN_XML);
                break;

            case JAR:
                JarFile jar = protocol.getJarFile(url);
                ZipEntry entry = jar.getEntry(PLUGIN_XML);

                try (InputStream in = jar.getInputStream(entry)) {
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

            Node mojo =
                (Node)
                compile("/plugin/mojos/mojo[implementation='%s']",
                        type.getQualifiedName())
                .evaluate(document, NODE);

            return div(attr("class", "summary"),
                       h3("Maven Plugin Parameter Summary"),
                       table(tag, asClass(type), mojo,
                             asStream((NodeList)
                                      compile("parameters/parameter")
                                      .evaluate(mojo, NODESET))));
        }

        private FluentNode table(UnknownInlineTagTree tag, Class<?> type,
                                 Node mojo, Stream<Node> parameters) {
            return table(thead(tr(th(EMPTY), th("Field"),
                                  th("Default"), th("Property"),
                                  th("Required"), th("Editable"),
                                  th("Description"))),
                         tbody(parameters.map(t -> tr(tag, type, mojo, t))));
        }

        private FluentNode tr(UnknownInlineTagTree tag, Class<?> type,
                              Node mojo, Node parameter) {
            FluentNode tr = fragment();

            try {
                String name = compile("name").evaluate(parameter);
                Field field = FieldUtils.getField(type, name, true);

                if (field != null) {
                    tr =
                        tr(td((! type.equals(field.getDeclaringClass()))
                                  ? type(tag, field.getDeclaringClass())
                                  : text(EMPTY)),
                           td(declaration(tag, field)),
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
    @NoArgsConstructor @ToString
    public static class PluginHelp extends MavenTaglet {
        private static final PluginHelp INSTANCE = new PluginHelp();

        public static void register(Map<Object,Object> map) {
            register(map, INSTANCE);
        }

        private static final String NAME = "plugin-help.xml";
        private static final Pattern PATTERN =
            Pattern.compile("META-INF/maven/(?<g>[^/]+)/(?<a>[^/]+)/"
                            + Pattern.quote(NAME));

        @Override
        public FluentNode toNode(UnknownInlineTagTree tag,
                                 Element element) throws Throwable {
            Class<?> type = null;

            if (element instanceof PackageElement) {
                type = asPackageInfoClass((PackageElement) element);
            } else {
                type = asClass(getEnclosingTypeElement(element));
            }

            URL url = getResourceURLOf(type);
            Protocol protocol = Protocol.of(url);
            Document document = null;

            switch (protocol) {
            case FILE:
                Path root =
                    Paths.get(url.getPath()
                              .replaceAll(Pattern.quote(getResourcePathOf(type)), EMPTY));
                Path path =
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
                try (JarFile jar = protocol.getJarFile(url)) {
                    JarEntry entry =
                        jar.stream()
                        .filter(t -> PATTERN.matcher(t.getName()).matches())
                        .findFirst().orElse(null);

                    try (InputStream in = jar.getInputStream(entry)) {
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
                       table(tag,
                             asStream((NodeList)
                                      compile("/plugin/mojos/mojo")
                                      .evaluate(document, NODESET))));
        }

        private FluentNode table(UnknownInlineTagTree tag,
                                 Stream<Node> mojos) {
            return table(thead(tr(th("Goal"), th("Phase"), th("Description"))),
                         tbody(mojos.map(t -> tr(tag, t))));
        }

        private FluentNode tr(UnknownInlineTagTree tag, Node mojo) {
            FluentNode tr = fragment();

            try {
                tr =
                    tr(td(a(tag,
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
     * {@link.uri https://maven.apache.org/pom.html POM} POM coordinates as
     * a {@code <dependency/>} element to include this documented
     * {@link Class} or {@link Package}.
     *
     * <p>For example:</p>
     *
     * {@pom.coordinates}
     */
    @TagletName("pom.coordinates")
    @NoArgsConstructor @ToString
    public static class Coordinates extends MavenTaglet {
        private static final Coordinates INSTANCE = new Coordinates();

        public static void register(Map<Object,Object> map) {
            register(map, INSTANCE);
        }

        private static final Pattern PATTERN =
            Pattern.compile("META-INF/maven/(?<g>[^/]+)/(?<a>[^/]+)/pom[.]properties");

        @Override
        public FluentNode toNode(UnknownInlineTagTree tag,
                                 Element element) throws Throwable {
            POMProperties properties = new POMProperties();
            Class<?> type = null;

            if (element instanceof PackageElement) {
                type = asPackageInfoClass((PackageElement) element);
            } else {
                type = asClass(getEnclosingTypeElement(element));
            }

            URL url = getResourceURLOf(type);
            Protocol protocol = Protocol.of(url);

            switch (protocol) {
            case FILE:
                Document document =
                    DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(getPomFileFor(tag, element));

                Stream.of(GROUP_ID, ARTIFACT_ID, VERSION)
                    .forEach(t -> properties.load(t, document, "/project/"));
                Stream.of(VERSION)
                    .forEach(t -> properties.load(t, document, "/project/parent/"));
                break;

            case JAR:
                try (JarFile jar = protocol.getJarFile(url)) {
                    JarEntry entry =
                        jar.stream()
                        .filter(t -> PATTERN.matcher(t.getName()).matches())
                        .findFirst().orElse(null);

                    if (entry != null) {
                        try (InputStream in = jar.getInputStream(entry)) {
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
