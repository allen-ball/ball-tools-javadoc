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
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.TreeMap;
import javax.lang.model.element.PackageElement;
import lombok.NoArgsConstructor;

import static java.net.http.HttpResponse.BodyHandlers;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

/**
 * package::URI {@link java.util.Map} for externally-linked Javadoc.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor
public class Extern extends TreeMap<String,URI> {
    private static final long serialVersionUID = 8076330291756669682L;

    private static final String ELEMENT_LIST = "element-list";
    private static final String PACKAGE_LIST = "package-list";

    private static final String MODULE_PREFIX = "module:";

    /**
     * Method to configure an external Javadoc document for linking.
     * ("{code -link}" option).
     *
     * @param   javadoc         The {@link URI} of the external Javadoc.
     */
    public void link(URI javadoc) throws InterruptedException,
                                         IOException {
        link(javadoc, javadoc);
    }

    /**
     * Method to configure an external Javadoc document for linking
     * ("{code -linkoffline}" option).
     *
     * @param   javadoc         The {@link URI} of the external Javadoc.
     * @param   packageList     The {@link URI} of the folder containing the
     *                          package list.
     */
    public void link(URI javadoc, URI packageList) throws InterruptedException,
                                                          IOException {
        List<String> list = null;

        for (String name : List.of(ELEMENT_LIST, PACKAGE_LIST)) {
            var uri = packageList.resolve(name);

            try {
                if (uri.getScheme().equals("file")) {
                    list = Files.lines(Paths.get(uri), UTF_8).collect(toList());
                    break;
                } else {
                    var client = HttpClient.newHttpClient();
                    var request = HttpRequest.newBuilder().uri(uri).build();
                    var response = client.send(request, BodyHandlers.ofLines());

                    if (response.statusCode() == 200) {
                        list = response.body().collect(toList());

                        if (! list.isEmpty()) {
                            break;
                        } else {
                            throw new IOException("No packages found in " + uri);
                        }
                    } else {
                        throw new IOException(String.valueOf(uri));
                    }
                }
            } catch (IOException exception) {
                continue;
            }
        }

        if (! (list == null || list.isEmpty())) {
            var value = javadoc;

            for (var key : list) {
                if (key.startsWith(MODULE_PREFIX)) {
                    value =
                        javadoc
                        .resolve(key.substring(MODULE_PREFIX.length()) + "/")
                        .normalize();
                } else {
                    putIfAbsent(key, value);
                }
            }
        } else {
            throw new IOException("Cannot get package list from " + packageList);
        }
    }

    @Override
    public URI get(Object key) {
        URI value = null;

        if (key instanceof Class<?>) {
            value = get(((Class<?>) key).getPackage());
        } else if (key instanceof Package) {
            value = get(((Package) key).getName());
        } else if (key instanceof PackageElement) {
            var element = (PackageElement) key;

            if (! element.isUnnamed()) {
                value = get(element.getQualifiedName().toString());
            }
        } else {
            value = (key != null) ? super.get(key) : null;
        }

        return value;
    }
}
