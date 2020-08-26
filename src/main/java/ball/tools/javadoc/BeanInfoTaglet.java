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
import ball.beans.PropertyDescriptorsTableModel;
import ball.xml.FluentNode;
import com.sun.source.doctree.UnknownInlineTagTree;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Inline {@link jdk.javadoc.doclet.Taglet} to provide a table of bean
 * properties.
 *
 * For example:
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@TagletName("bean.info")
@NoArgsConstructor @ToString
public class BeanInfoTaglet extends AbstractInlineTaglet {
    private static final BeanInfoTaglet INSTANCE = new BeanInfoTaglet();

    public static void register(Map<Object,Object> map) {
        register(map, INSTANCE);
    }

    @Override
    public FluentNode toNode(UnknownInlineTagTree tag,
                             Element element) throws Throwable {
        TypeElement type = null;
        String name = getText(tag);

        if (isNotEmpty(name)) {
            type = getTypeElementFor(element, name);
        } else {
            type = getEnclosingTypeElement(element);
        }

        PropertyDescriptorsTableModel model =
            new PropertyDescriptorsTableModel(getBeanInfo(asClass(type))
                                              .getPropertyDescriptors());

        return div(attr("class", "summary"),
                   h3("Bean Property Summary"),
                   table(tag, model));
    }
}
