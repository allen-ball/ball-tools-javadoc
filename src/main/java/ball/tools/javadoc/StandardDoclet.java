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
import ball.annotation.CompileTimeCheck;
import ball.lang.reflect.DefaultInvocationHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.tools.Diagnostic;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.tools.Diagnostic.Kind.WARNING;

/**
 * {@link jdk.javadoc.doclet.StandardDoclet} subclass which provides
 * additional services to {@link AbstractTaglet} implementations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString
public class StandardDoclet extends jdk.javadoc.doclet.StandardDoclet {
    private static final String ELEMENT_LIST = "element-list";
    private static final String PACKAGE_LIST = "package-list";

    private static final String SN =
        "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    @CompileTimeCheck
    private static final Pattern FQN =
        Pattern.compile(SN + "(" + Pattern.quote(".") + SN + ")*");

    private final Map<URI,URI> links = new TreeMap<>();
    private final Map<String,URI> external = new TreeMap<>();
    private Locale locale = null;
    private Reporter reporter = null;

    @Override
    public void init(Locale locale, Reporter reporter) {
        this.locale = locale;
        this.reporter = reporter;

        super.init(locale, reporter);
    }

    @Override
    public Set<Option> getSupportedOptions() {
        Set<Option> set =
            super.getSupportedOptions()
            .stream()
            .map(t -> new OptionWrapper(t).newProxyInstance(Option.class))
            .collect(toSet());

        return set;
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        for (URI key : links.keySet()) {
            URI value = links.computeIfAbsent(key, k -> key);
            List<String> list = null;

            for (String name : List.of(ELEMENT_LIST, PACKAGE_LIST)) {
                URI uri = value.resolve(name);

                try (InputStream in = uri.toURL().openStream()) {
                    list =
                        new BufferedReader(new InputStreamReader(in, UTF_8))
                        .lines()
                        .filter(t -> FQN.matcher(t).matches())
                        .collect(toList());

                    if (! list.isEmpty()) {
                        break;
                    } else {
                        throw new IOException("No packages found");
                    }
                } catch (IOException exception) {
                    /* print(WARNING, "Cannot read %s", uri); */
                }
            }

            if (list != null && (! list.isEmpty())) {
                list.stream()
                    .forEach(t -> external.computeIfAbsent(t, k -> key));
            } else {
                print(WARNING, "Cannot get package list for %s", value);
            }
        }

        return super.run(docEnv);
    }

    /**
     * Method to print a diagnostic message.
     *
     * @param   kind            The {@link javax.tools.Diagnostic.Kind}.
     * @param   format          The message format {@link String}.
     * @param   argv            Optional arguments to the message format
     *                          {@link String}.
     *
     * @see Reporter#print(Diagnostic.Kind,String)
     */
    protected void print(Diagnostic.Kind kind, String format, Object... argv) {
        reporter.print(kind, String.format(format, argv));
    }

    @RequiredArgsConstructor
    private class OptionWrapper extends DefaultInvocationHandler {
        private final Option option;

        @Override
        public Object invoke(Object proxy, Method method, Object[] argv) throws Throwable {
            try {
                getClass()
                    .getMethod(method.getName(), method.getParameterTypes())
                    .invoke(this, argv);
            } catch (Exception exception) {
            }

            Object result = null;

            if (method.isDefault()) {
                result = super.invoke(proxy, method, argv);
            } else if (method.getDeclaringClass().isAssignableFrom(option.getClass())) {
                result = method.invoke(option, argv);
            } else {
                result = super.invoke(proxy, method, argv);
            }

            return result;
        }

        public void process(String string, List<String> list) {
            if (option.getNames().contains("-link")) {
                links.put(asURI(list.get(0)), null);
            } else if (option.getNames().contains("-linkoffline")) {
                links.put(asURI(list.get(0)), new File(list.get(1)).toURI());
            }
        }

        private URI asURI(String string) {
            return URI.create(string + "/").normalize();
        }
    }
}
