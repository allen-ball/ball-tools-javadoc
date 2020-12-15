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
import com.sun.javadoc.Doc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.internal.toolkit.Configuration;
import com.sun.tools.doclets.internal.toolkit.Content;
import com.sun.tools.doclets.internal.toolkit.taglets.Taglet;
import com.sun.tools.doclets.internal.toolkit.taglets.TagletWriter;
import java.io.IOException;
import java.io.Writer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static lombok.AccessLevel.PROTECTED;

/**
 * Default methods for legacy {@link Taglet} implementations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface SunToolsInternalToolkitTaglet extends Taglet {
    void set(Configuration configuration);

    @Override
    default Content getTagletOutput(Doc doc,
                                    TagletWriter writer) throws IllegalArgumentException {
        throw new IllegalArgumentException(doc.position() + ": "
                                           + "Method not supported in taglet "
                                           + getName() + ".");
    }

    @Override
    default Content getTagletOutput(Tag tag,
                                    TagletWriter writer) throws IllegalArgumentException {
        set(writer.configuration());

        Content content = writer.getOutputInstance();
        String string = ((AbstractTaglet) this).toString(tag);

        content.addContent(new Raw(string));

        return content;
    }

    @RequiredArgsConstructor(access = PROTECTED) @ToString
    public class Raw extends Content {
        private static final String NL = System.getProperty("line.separator");

        @NonNull private final String string;

        @Override
        public void addContent(Content content) {
            throw new RuntimeException("not supported");
        }

        @Override
        public void addContent(String stringContent) {
            throw new RuntimeException("not supported");
        }

        @Override
        public boolean write(Writer out, boolean atNL) throws IOException {
            out.write(string);

            return string.endsWith(NL);
        }

        @Override
        public boolean isEmpty() { return string.isEmpty(); }

        @Override
        public int charCount() { return string.length(); }
    }
}
