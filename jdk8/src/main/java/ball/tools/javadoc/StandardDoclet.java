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
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * {@link Standard} subclass which provides additional services to
 * {@link AbstractTaglet} implementations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor @ToString
public class StandardDoclet extends Standard {

    /**
     * See
     * {@link com.sun.javadoc.Doclet#validOptions(String[][],DocErrorReporter)}.
     */
    public static String[][] options = null;

    /**
     * See
     * {@link com.sun.javadoc.Doclet#validOptions(String[][],DocErrorReporter)}.
     */
    public static DocErrorReporter reporter = null;

    /**
     * See {@link com.sun.javadoc.Doclet#start(RootDoc)}.
     */
    public static RootDoc root = null;

    /**
     * See
     * {@link com.sun.javadoc.Doclet#validOptions(String[][],DocErrorReporter)}.
     *
     * @param   options         The {@code options} and their arguments.
     * @param   reporter        The {@link DocErrorReporter}.
     *
     * @return  {@code true} if the {@code options} are valid; {@code false}
     *          otherwise.
     */
    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
        StandardDoclet.options = options;
        StandardDoclet.reporter = reporter;

        return Standard.validOptions(options, reporter);
    }

    /**
     * See {@link com.sun.javadoc.Doclet#start(RootDoc)}.
     *
     * @param   root            The {@link RootDoc}.
     *
     * @return  {@code true} on success; {@code false} otherwise.
     */
    public static boolean start(RootDoc root) {
        StandardDoclet.root = root;

        return Standard.start(root);
    }
}
