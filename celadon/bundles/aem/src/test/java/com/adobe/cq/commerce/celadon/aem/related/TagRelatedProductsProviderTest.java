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
package com.adobe.cq.commerce.celadon.aem.related;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TagRelatedProductsProviderTest {

    private static final String CATALOG = "/content/dam/celadon/we-retail";
    private static final String EL_GORDO = CATALOG + "/men/coats/el-gordo";
    private static final String PORTLAND = CATALOG + "/men/coats/portland";
    private static final String SONJA = CATALOG + "/women/coats/sonja";
    private static final String TRAIL_PANTS = CATALOG + "/men/pants/trail-pants";
    private static final String TOWEL = CATALOG + "/equipment/swimming/towel";
    private static final String UNTAGGED = CATALOG + "/equipment/biking/pump";

    @Rule
    public final SlingContext context = new SlingContext();

    private TagRelatedProductsProvider provider;

    @Before
    public void setUp() throws Exception {
        product(EL_GORDO, "we-retail:gender/men", "we-retail:season/winter", "we-retail:apparel/coat");
        product(PORTLAND, "we-retail:gender/men", "we-retail:apparel/coat");
        product(SONJA, "we-retail:gender/women", "we-retail:season/winter", "we-retail:apparel/coat");
        product(TRAIL_PANTS, "we-retail:gender/men", "we-retail:apparel/pants");
        product(TOWEL, "we-retail:season/summer");
        context.create().resource(UNTAGGED + "/jcr:content/metadata");
        context.resourceResolver().commit();
        provider = provider(6);
    }

    @Test
    public void shouldRankByTheNumberOfSharedTagsKeepingCatalogOrderForTies() {
        List<String> related = provider.relatedProducts(EL_GORDO,
                List.of(TRAIL_PANTS, EL_GORDO, SONJA, TOWEL, PORTLAND, UNTAGGED));

        assertEquals(List.of(SONJA, PORTLAND, TRAIL_PANTS), related);
    }

    @Test
    public void shouldLimitTheRelatedProducts() throws Exception {
        provider = provider(2);

        assertEquals(List.of(SONJA, PORTLAND), provider.relatedProducts(EL_GORDO, List.of(TRAIL_PANTS, SONJA, PORTLAND)));
    }

    @Test
    public void shouldRelateNothingToAnUntaggedProduct() {
        assertTrue(provider.relatedProducts(UNTAGGED, List.of(EL_GORDO, PORTLAND)).isEmpty());
        assertTrue(provider.relatedProducts(CATALOG + "/missing", List.of(EL_GORDO, PORTLAND)).isEmpty());
    }

    @Test
    public void shouldNotReadTheRepositoryWhenNothingCanBeRelated() throws Exception {
        provider = provider(0, null);
        assertTrue(provider.relatedProducts(EL_GORDO, List.of(PORTLAND)).isEmpty());

        provider = provider(6, null);
        assertTrue(provider.relatedProducts(EL_GORDO, List.of()).isEmpty());
    }

    private TagRelatedProductsProvider provider(int maxResults) throws Exception {
        return provider(maxResults, context.getService(ResourceResolverFactory.class));
    }

    private TagRelatedProductsProvider provider(int maxResults, ResourceResolverFactory resourceResolverFactory) throws Exception {
        TagRelatedProductsProvider provider = new TagRelatedProductsProvider();
        Field factory = TagRelatedProductsProvider.class.getDeclaredField("resourceResolverFactory");
        factory.setAccessible(true);
        factory.set(provider, resourceResolverFactory);
        provider.activate((TagRelatedProductsProvider.Configuration) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { TagRelatedProductsProvider.Configuration.class },
                (proxy, method, args) -> "maxResults".equals(method.getName()) ? maxResults : method.getDefaultValue()));
        return provider;
    }

    private void product(String path, String... tags) {
        context.create().resource(path + "/jcr:content/metadata", Map.of("cq:tags", tags));
    }
}
