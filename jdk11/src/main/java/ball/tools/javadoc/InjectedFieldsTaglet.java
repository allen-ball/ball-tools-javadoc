package ball.tools.javadoc;
/*-
 * ##########################################################################
 * Utilities
 * %%
 * Copyright (C) 2020 - 2023 Allen D. Ball
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
import com.sun.source.doctree.UnknownInlineTagTree;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import jdk.javadoc.doclet.Taglet;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Inline {@link jdk.javadoc.doclet.Taglet} to provide a report of members
 * whose values are injected.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@TagletName("injected.fields")
@ServiceProviderFor({ Taglet.class })
@NoArgsConstructor @ToString
public class InjectedFieldsTaglet extends AbstractInlineTaglet {
    private static final String[] NAMES = new String[] {
        "jakarta.annotation.Resource",
        "jakarta.annotation.Resources",
        "javax.annotation.Resource",
        "javax.annotation.Resources",
        "javax.inject.Inject",
        "javax.inject.Named",
        "org.springframework.beans.factory.annotation.Autowired",
        "org.springframework.beans.factory.annotation.Value",
        "org.springframework.boot.web.server.LocalServerPort"
    };

    @Override
    public FluentNode toNode(UnknownInlineTagTree tag, Element context) throws Throwable {
        TypeElement type = null;
        var argv = getText(tag).trim().split("[\\p{Space}]+", 2);

        if (isNotEmpty(argv[0])) {
            type = getTypeElementFor(context, argv[0]);
        } else {
            type = getEnclosingTypeElement(context);
        }

        Set<Class<? extends Annotation>> set = new HashSet<>();

        for (String name : NAMES) {
            Class<? extends Annotation> annotation = null;

            try {
                annotation = Class.forName(name, false, getClassLoader()).asSubclass(Annotation.class);
            } catch (Exception exception) {
            }

            if (annotation != null) {
                var retention = annotation.getAnnotation(Retention.class);

                if (retention == null) {
                    throw new IllegalStateException(annotation.getCanonicalName()
                                                    + " does not specify a retention policy");
                }

                switch (retention.value()) {
                case RUNTIME:
                    break;

                case CLASS:
                case SOURCE:
                default:
                    throw new IllegalStateException(annotation.getCanonicalName()
                                                    + " specifies " + retention.value() + " retention policy");
                    /* break; */
                }

                set.add(annotation);
            }
        }

        if (set.isEmpty()) {
            throw new IllegalStateException("No annotations to map");
        }

        return div(attr("class", "summary"),
                   h3("Injected Field Summary"),
                   table(tag, context, asClass(type), set));
    }

    private FluentNode table(UnknownInlineTagTree tag, Element context, Class<?> type, Set<Class<? extends Annotation>> set) {
        return table(thead(tr(th("Annotation(s)"), th("Field"))),
                     tbody(Stream.of(type.getDeclaredFields())
                           .filter(t -> (Stream.of(t.getAnnotations())
                                         .filter(a -> set.contains(a.annotationType()))
                                         .findFirst().isPresent()))
                           .map(t -> tr(tag, context, t, set))));
    }

    private FluentNode tr(UnknownInlineTagTree tag, Element context, Field field, Set<Class<? extends Annotation>> set) {
        return tr(td(fragment(Stream.of(field.getAnnotations())
                              .filter(t -> set.contains(t.annotationType()))
                              .map(t -> annotation(tag, context, t)))),
                  td(declaration(tag, context, field)));
    }
}
