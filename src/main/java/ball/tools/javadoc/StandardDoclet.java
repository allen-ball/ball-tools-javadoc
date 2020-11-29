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
import ball.lang.reflect.InterceptingInvocationHandler;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.tools.Diagnostic;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    private final Map<URI,URI> links = new TreeMap<>();
    private final Extern extern = new Extern();
    private Locale locale = null;
    private Reporter reporter = null;

    protected Extern extern() { return extern; }

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
            .map(OptionWrapper::new)
            .map(t -> (Option) t.newProxyInstance(Option.class))
            .collect(toSet());

        return set;
    }

    @Override
    public boolean run(DocletEnvironment docEnv) {
        for (URI key : links.keySet()) {
            URI value = links.computeIfAbsent(key, k -> key);

            try {
                extern.link(key, value);
            } catch (IOException exception) {
                print(WARNING, "%s", exception.getMessage());
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

    private class OptionWrapper extends InterceptingInvocationHandler<Option> {
        public OptionWrapper(Option option) { super(option); }

        public void process(String option, List<String> argv) {
            if (getTarget().getNames().contains("-link")) {
                links.put(asURI(argv.get(0)), null);
            } else if (getTarget().getNames().contains("-linkoffline")) {
                links.put(asURI(argv.get(0)), new File(argv.get(1)).toURI());
            }
        }

        private URI asURI(String string) {
            return URI.create(string + "/").normalize();
        }
    }
}
