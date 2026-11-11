/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.celadon.aem;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.FragmentData;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.dam.cfm.VariationDef;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.tagging.Tag;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

/**
 * In-memory stand-in for the AEM Content Fragment API, registered as Sling adapters: the model resource adapts to a
 * {@link FragmentTemplate} whose {@code createFragment} creates the fragment node, fragment nodes adapt to a
 * {@link ContentFragment} with the given elements, and the resolver adapts to an {@link AssetManager} that creates
 * plain asset nodes. Element, variation and data objects are dynamic proxies that keep their values in maps.
 */
final class ContentFragmentFakes {
    final Map<String, Fragment> fragments = new HashMap<>();
    final Map<String, byte[]> assets = new LinkedHashMap<>();
    int createdFragments;

    private final String modelPath;
    private final Set<String> elementNames;

    ContentFragmentFakes(SlingContext context, String modelPath, String... elementNames) {
        this.modelPath = modelPath;
        this.elementNames = Set.of(elementNames);
        context.registerAdapter(Resource.class, FragmentTemplate.class,
                (Function<Resource, FragmentTemplate>) resource ->
                        modelPath.equals(resource.getPath()) ? proxy(FragmentTemplate.class, this::template) : null);
        context.registerAdapter(Resource.class, ContentFragment.class,
                (Function<Resource, ContentFragment>) resource -> {
                    Fragment fragment = fragments.get(resource.getPath());
                    return fragment == null ? null : fragment.proxy;
                });
        context.registerAdapter(ResourceResolver.class, AssetManager.class,
                (Function<ResourceResolver, AssetManager>) resolver ->
                        proxy(AssetManager.class, (method, args) -> createAsset(resolver, method, args)));
    }

    Fragment fragment(String path) {
        return fragments.get(path);
    }

    private Object template(Method method, Object[] args) throws PersistenceException {
        if (!"createFragment".equals(method.getName())) {
            return null;
        }
        Resource parent = (Resource) args[0];
        String name = (String) args[1];
        ResourceResolver resolver = parent.getResourceResolver();
        Resource node = resolver.create(parent, name, Map.of("jcr:primaryType", "dam:Asset"));
        resolver.create(node, "jcr:content", Map.of("jcr:primaryType", "dam:AssetContent", "jcr:title", args[2]));
        Fragment fragment = new Fragment(node.getPath());
        fragments.put(node.getPath(), fragment);
        createdFragments++;
        return fragment.proxy;
    }

    private Object createAsset(ResourceResolver resolver, Method method, Object[] args) throws Exception {
        if (!"createAsset".equals(method.getName())) {
            return null;
        }
        String path = (String) args[0];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ((InputStream) args[1]).transferTo(bytes);
        assets.put(path, bytes.toByteArray());
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        resolver.create(resolver.getResource(parentPath), path.substring(path.lastIndexOf('/') + 1),
                Map.of("jcr:primaryType", "dam:Asset"));
        return null;
    }

    /** A fragment: its elements by name and its variations (name to title) with their tags. */
    final class Fragment {
        final Map<String, Element> elements = new LinkedHashMap<>();
        final Map<String, String> variations = new LinkedHashMap<>();
        final Map<String, Tag[]> variationTags = new LinkedHashMap<>();
        final ContentFragment proxy;

        Fragment(String path) {
            this.proxy = ContentFragmentFakes.proxy(ContentFragment.class, (method, args) -> switch (method.getName()) {
                case "getElement" -> elementNames.contains((String) args[0])
                        ? elements.computeIfAbsent((String) args[0], Element::new).proxy
                        : null;
                case "createVariation" -> {
                    variations.put((String) args[0], (String) args[1]);
                    yield ContentFragmentFakes.proxy(VariationTemplate.class, (m, a) -> named(m, (String) args[0]));
                }
                case "listAllVariations" -> {
                    List<VariationDef> definitions = new ArrayList<>();
                    for (String name : variations.keySet()) {
                        definitions.add(ContentFragmentFakes.proxy(VariationDef.class, (m, a) -> named(m, name)));
                    }
                    yield definitions.iterator();
                }
                case "removeVariation" -> {
                    variations.remove((String) args[0]);
                    elements.values().forEach(element -> element.variations.remove((String) args[0]));
                    yield null;
                }
                case "setVariationTags" -> {
                    variationTags.put((String) args[1], (Tag[]) args[0]);
                    yield null;
                }
                case "getName" -> path.substring(path.lastIndexOf('/') + 1);
                default -> null;
            });
        }

        Element element(String name) {
            return elements.get(name);
        }
    }

    /** An element or a variation of one: its text content and its typed value. */
    static class Value {
        String content;
        String mimeType;
        Object value;

        Object handle(Method method, Object[] args) {
            return switch (method.getName()) {
                case "setContent" -> {
                    content = (String) args[0];
                    mimeType = (String) args[1];
                    yield null;
                }
                case "getContent" -> content;
                case "getValue" -> {
                    Object[] current = {value};
                    yield proxy(FragmentData.class, (m, a) -> switch (m.getName()) {
                        case "getValue" -> current[0];
                        case "setValue" -> {
                            current[0] = a[0];
                            yield null;
                        }
                        default -> null;
                    });
                }
                case "setValue" -> {
                    value = ((FragmentData) args[0]).getValue();
                    yield null;
                }
                default -> null;
            };
        }
    }

    final class Element extends Value {
        final Map<String, Value> variations = new LinkedHashMap<>();
        final ContentElement proxy;

        Element(String name) {
            this.proxy = ContentFragmentFakes.proxy(ContentElement.class, (method, args) -> switch (method.getName()) {
                case "getName" -> name;
                case "getVariation" -> {
                    Value variation = variations.get((String) args[0]);
                    yield variation == null ? null : variationProxy(variation);
                }
                case "createVariation" -> {
                    Value variation = new Value();
                    variations.put(((VariationTemplate) args[0]).getName(), variation);
                    yield variationProxy(variation);
                }
                default -> handle(method, args);
            });
        }

        Value variation(String name) {
            return variations.get(name);
        }

        private ContentVariation variationProxy(Value variation) {
            return ContentFragmentFakes.proxy(ContentVariation.class, variation::handle);
        }
    }

    private static Object named(Method method, String name) {
        return "getName".equals(method.getName()) || "getTitle".equals(method.getName()) ? name : null;
    }

    interface Handler {
        Object handle(Method method, Object[] args) throws Exception;
    }

    private static <T> T proxy(Class<T> type, Handler handler) {
        InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                };
            }
            return handler.handle(method, args);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, invocationHandler));
    }
}
