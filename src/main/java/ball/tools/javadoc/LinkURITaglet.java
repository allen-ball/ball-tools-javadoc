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
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.Element;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Inline {@link jdk.javadoc.doclet.Taglet} to provide external links.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@TagletName("link.uri")
@NoArgsConstructor @ToString
public class LinkURITaglet extends AbstractInlineTaglet {
    private static final LinkURITaglet INSTANCE = new LinkURITaglet();

    public static void register(Map<Object,Object> map) {
        register(map, INSTANCE);
    }

    private static final String SPACES = "[\\p{Space}]+";

    @Override
    public FluentNode toNode(UnknownInlineTagTree tag,
                             Element element) throws Throwable {
        String text = getText(tag).trim();
        String[] argv = text.split(SPACES, 2);
        URI href = new URI(argv[0]);

        text = (argv.length > 1) ? argv[1] : null;

        LinkedHashMap<String,String> map = new LinkedHashMap<>();

        if (text != null) {
            for (;;) {
                argv = text.split(SPACES, 2);

                String[] nvp = argv[0].split("=", 2);

                if (argv.length > 1 && nvp.length > 1) {
                    map.put(nvp[0], nvp[1]);
                    text = argv[1];
                } else {
                    break;
                }
            }
        }

        return a(href, text)
                   .add(map.entrySet()
                        .stream()
                        .map(t -> attr(t.getKey(), t.getValue())));
    }
}
