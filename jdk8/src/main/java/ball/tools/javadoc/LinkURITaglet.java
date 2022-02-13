package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * %%
 * Copyright (C) 2008 - 2022 Allen D. Ball
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
import ball.annotation.ServiceProviderFor;
import ball.xml.FluentNode;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Inline {@link Taglet} to provide external links.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@TagletName("link.uri")
@ServiceProviderFor({ Taglet.class })
@NoArgsConstructor @ToString
public class LinkURITaglet extends AbstractInlineTaglet implements SunToolsInternalToolkitTaglet {
    private static final LinkURITaglet INSTANCE = new LinkURITaglet();

    public static void register(Map<Object,Object> map) {
        register(map, INSTANCE);
    }

    private static final String SPACES = "[\\p{Space}]+";

    @Override
    public FluentNode toNode(Tag tag) throws Throwable {
        String text = tag.text().trim();
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
                   .add(map.entrySet().stream()
                        .map(t -> attr(t.getKey(), t.getValue())));
    }
}
