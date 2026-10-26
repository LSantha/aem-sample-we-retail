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
package com.adobe.cq.commerce.celadon.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class RelatedProductsTest {

    private static final String QUERY = """
            {
              products(filter:{ url_key:{ eq:"candace-dress" } }) {
                items {
                  sku
                  related_products { sku name url_key }
                }
              }
            }
            """;

    @Test
    public void shouldResolveTheProvidedCatalogProducts() {
        List<String> requested = new ArrayList<>();
        List<String> candidates = new ArrayList<>();
        CeladonGraphqlEngine engine = engine((product, catalogProducts) -> {
            requested.add(product);
            candidates.addAll(catalogProducts);
            return List.of("unknown/product", product, "venia-dresses/angelina-tank-dress", "venia-dresses/angelina-tank-dress");
        });

        Map<String, Object> response = engine.execute(QUERY, null, null);

        assertNull(response.get("errors"));
        assertEquals(List.of("venia-dresses/candace-dress"), requested);
        assertEquals(List.of("venia-dresses/candace-dress", "venia-dresses/angelina-tank-dress"), candidates);
        List<Map<String, Object>> related = relatedProducts(response);
        assertEquals(1, related.size());
        assertEquals("angelina-tank-dress-sku", related.get(0).get("sku"));
        assertEquals("Angelina Tank Dress", related.get(0).get("name"));
        assertEquals("angelina-tank-dress", related.get(0).get("url_key"));
    }

    @Test
    public void shouldNotCallTheProviderUnlessRelatedProductsAreSelected() {
        List<String> requested = new ArrayList<>();
        CeladonGraphqlEngine engine = engine((product, catalogProducts) -> {
            requested.add(product);
            return List.of();
        });

        engine.execute("{ products(filter:{ url_key:{ eq:\"candace-dress\" } }) { items { sku } } }", null, null);

        assertTrue(requested.isEmpty());
    }

    @Test
    public void shouldReturnNoRelatedProductsWithoutAProvider() {
        Map<String, Object> response = TestCatalogFixtures.engine().execute(QUERY, null, null);

        assertNull(response.get("errors"));
        assertTrue(relatedProducts(response).isEmpty());
    }

    @Test
    public void shouldReturnNoRelatedProductsWhenTheProviderFails() {
        CeladonGraphqlEngine engine = engine((product, catalogProducts) -> {
            throw new IllegalStateException("provider unavailable");
        });

        Map<String, Object> response = engine.execute(QUERY, null, null);

        assertNull(response.get("errors"));
        assertTrue(relatedProducts(response).isEmpty());
        assertFalse(firstProduct(response).isEmpty());
    }

    private static CeladonGraphqlEngine engine(RelatedProductsProvider provider) {
        FetcherContext context = new FetcherContext("celadon/venia");
        return new CeladonGraphqlEngine(context, new GsonBuilder().serializeNulls().create(),
                CatalogGatewayFactory.fixed(new TestCatalogFixtures.FixtureCatalogGateway(context)), null, provider);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstProduct(Map<String, Object> response) {
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> products = (Map<String, Object>) data.get("products");
        return ((List<Map<String, Object>>) products.get("items")).get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> relatedProducts(Map<String, Object> response) {
        return (List<Map<String, Object>>) firstProduct(response).get("related_products");
    }
}
