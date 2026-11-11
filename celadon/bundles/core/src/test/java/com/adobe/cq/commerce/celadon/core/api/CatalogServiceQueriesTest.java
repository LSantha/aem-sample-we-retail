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

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import com.google.gson.GsonBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

/**
 * Product filters, category lookups, facets and metadata answered by the catalog service, run through the engine
 * against the fixture catalog: candace-dress (configurable: fashion_color Lilac/Peach, fashion_size L, material
 * Polyester) and angelina-tank-dress (simple: price 35, featured, rating 4.5, 3 units per pack, material Cotton, also
 * in venia-tops), both in venia-dresses.
 */
public class CatalogServiceQueriesTest {
    private static final String CANDACE = "candace-dress-sku";
    private static final String ANGELINA = "angelina-tank-dress-sku";

    @Test
    public void shouldFilterSelectAttributesByOptionLabelOrValueIndex() {
        CeladonGraphqlEngine engine = engineWithManifest(
                AttributeEntry.of("fashion_color", "Fashion Color", NormalizedType.SELECT,
                        AttributeScope.VARIANT, true, false, 10));

        Assert.assertEquals(Set.of(CANDACE), skus(engine, "fashion_color:{ eq:\"lilac\" }"));
        Assert.assertEquals(Set.of(CANDACE), skus(engine, "fashion_color:{ in:[\"93\"] }"));
        Assert.assertEquals(Set.of(), skus(engine, "fashion_color:{ eq:\"Blue\" }"));
    }

    @Test
    public void shouldFilterBooleanNumericAndStringAttributes() {
        CeladonGraphqlEngine engine = engineWithManifest(
                AttributeEntry.of("featured", "Featured", NormalizedType.BOOLEAN, AttributeScope.PRODUCT, true, false, 10),
                AttributeEntry.of("rating", "Rating", NormalizedType.FLOAT, AttributeScope.PRODUCT, true, false, 20),
                AttributeEntry.of("units_per_pack", "Units Per Pack", NormalizedType.INT, AttributeScope.PRODUCT, true, false, 30),
                AttributeEntry.of("msrp", "MSRP", NormalizedType.PRICE, AttributeScope.PRODUCT, true, false, 40),
                AttributeEntry.of("material", "Material", NormalizedType.STRING, AttributeScope.PRODUCT, true, false, 50));

        Assert.assertEquals(Set.of(ANGELINA), skus(engine, "featured:{ eq:\"true\" }"));
        Assert.assertEquals(Set.of(), skus(engine, "featured:{ eq:\"false\" }"));
        Assert.assertEquals(Set.of(ANGELINA), skus(engine, "rating:{ from:\"4\" }"));
        Assert.assertEquals(Set.of(), skus(engine, "rating:{ from:\"4.6\" }"));
        Assert.assertEquals(Set.of(ANGELINA), skus(engine, "units_per_pack:{ from:\"2\", to:\"3\" }"));
        // PRICE attributes compare the displayed minimum price, not their own element.
        Assert.assertEquals(Set.of(CANDACE), skus(engine, "msrp:{ from:\"40\" }"));
        Assert.assertEquals(Set.of(ANGELINA), skus(engine, "material:{ match:\"cot\" }"));
        Assert.assertEquals(Set.of(CANDACE), skus(engine, "material:{ match:\"poly\" }"));
    }

    @Test
    public void shouldApplyTheBaseSchemaFilters() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Assert.assertEquals(Set.of(ANGELINA),
                skus(engine, "category_uid:{ eq:\"venia-dresses\" }, name:{ match:\"tank\" }"));
        Assert.assertEquals(Set.of(CANDACE), skus(engine, "price:{ from:\"40\", to:\"100\" }"));
        Assert.assertEquals(Set.of(CANDACE, ANGELINA),
                skus(engine, "sku:{ in:[\"" + ANGELINA + "\", \"" + CANDACE + "\", \"missing-sku\"] }"));
        Assert.assertEquals(Set.of(CANDACE), skus(engine, "url_key:{ in:[\"candace-dress\"] }"));
        Assert.assertEquals(Set.of(ANGELINA), skus(engine, "category_id:{ eq:\"venia-tops\" }"));
    }

    @Test
    public void shouldResolveCategoryByNumericIdAndDefaultToTheRoot() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();
        Map<String, Object> listed = data(engine.execute(
                "{ categoryList(filters:{ category_uid:{ eq:\"venia-dresses\" } }) { id uid } }", null, null));
        Object id = castList(listed.get("categoryList")).getFirst().get("id");

        Map<String, Object> byId = data(engine.execute("{ category(id:" + id + ") { uid name } }", null, null));
        Assert.assertEquals("venia-dresses", castMap(byId.get("category")).get("uid"));
        Assert.assertEquals("Dresses", castMap(byId.get("category")).get("name"));

        Map<String, Object> root = data(engine.execute("{ category { uid } }", null, null));
        Assert.assertEquals("/", castMap(root.get("category")).get("uid"));
    }

    @Test
    public void shouldListChildCategoriesByParentOrUrlPath() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> byParent = castMap(data(engine.execute(
                "{ categories(filters:{ parent_category_uid:{ eq:\"/\" } }) { total_count items { uid } } }",
                null, null)).get("categories"));
        Set<Object> children = castList(byParent.get("items")).stream().map(item -> item.get("uid")).collect(Collectors.toSet());
        Assert.assertTrue(children.containsAll(Set.of("venia-dresses", "venia-tops")));

        Map<String, Object> leaf = castMap(data(engine.execute(
                "{ categories(filters:{ url_path:{ eq:\"venia-dresses\" } }) { total_count } }",
                null, null)).get("categories"));
        Assert.assertEquals(0, ((Number) leaf.get("total_count")).intValue());
    }

    @Test
    public void shouldAggregateBooleanAndNumericAttributes() {
        CeladonGraphqlEngine engine = engineWithManifest(
                AttributeEntry.of("featured", "Featured", NormalizedType.BOOLEAN, AttributeScope.PRODUCT, true, true, 10),
                AttributeEntry.of("rating", "Rating", NormalizedType.FLOAT, AttributeScope.PRODUCT, true, true, 20));

        Map<String, Object> products = castMap(data(engine.execute(
                "{ products(filter:{ category_uid:{ eq:\"venia-dresses\" } }) "
                        + "{ aggregations { attribute_code label options { label value count } } } }",
                null, null)).get("products"));
        Map<Object, Map<String, Object>> byCode = castList(products.get("aggregations")).stream()
                .collect(Collectors.toMap(aggregation -> aggregation.get("attribute_code"), aggregation -> aggregation));

        Map<String, Object> featured = byCode.get("featured");
        Assert.assertNotNull("boolean facet", featured);
        Assert.assertTrue(castList(featured.get("options")).stream()
                .anyMatch(option -> ((Number) option.get("count")).intValue() == 1));
        Assert.assertNotNull("numeric facet", byCode.get("rating"));
    }

    @Test
    public void shouldDescribeManifestAttributesInCustomAttributeMetadata() {
        CeladonGraphqlEngine engine = engineWithManifest(
                AttributeEntry.of("featured", "Featured", NormalizedType.BOOLEAN, AttributeScope.PRODUCT, true, false, 10),
                AttributeEntry.of("rating", "Rating", NormalizedType.FLOAT, AttributeScope.PRODUCT, true, false, 20));

        Map<String, Object> metadata = castMap(data(engine.execute(
                "{ customAttributeMetadata(attributes:[{ attribute_code:\"featured\", entity_type:\"4\" },"
                        + " { attribute_code:\"rating\", entity_type:\"4\" }]) "
                        + "{ items { attribute_code attribute_type input_type } } }",
                null, null)).get("customAttributeMetadata"));
        Map<Object, Map<String, Object>> byCode = castList(metadata.get("items")).stream()
                .collect(Collectors.toMap(item -> item.get("attribute_code"), item -> item));

        Assert.assertEquals("boolean", byCode.get("featured").get("input_type"));
        Assert.assertEquals("Float", byCode.get("rating").get("attribute_type"));
        Assert.assertEquals("text", byCode.get("rating").get("input_type"));
    }

    @Test
    public void shouldServeTheStorefrontInstanceContext() {
        Map<String, Object> context = castMap(data(TestCatalogFixtures.engine().execute(
                "{ dataServicesStorefrontInstanceContext { store_code store_view_code website_code store_url } }",
                null, null)).get("dataServicesStorefrontInstanceContext"));

        Assert.assertEquals("default", context.get("store_code"));
        Assert.assertEquals("default", context.get("store_view_code"));
        Assert.assertEquals("base", context.get("website_code"));
        Assert.assertTrue(context.get("store_url").toString().endsWith("/us/en.html"));
    }

    private static CeladonGraphqlEngine engineWithManifest(AttributeEntry... entries) {
        FetcherContext context = new FetcherContext("celadon/venia");
        return new CeladonGraphqlEngine(context, new GsonBuilder().serializeNulls().create(),
                CatalogGatewayFactory.fixed(new TestCatalogFixtures.FixtureCatalogGateway(context)),
                new AttributeManifest("venia", List.of(entries)));
    }

    private static Set<Object> skus(CeladonGraphqlEngine engine, String filter) {
        Map<String, Object> response = engine.execute(
                "{ products(filter:{ " + filter + " }) { total_count items { sku } } }", null, null);
        Assert.assertNull("errors for " + filter + ": " + response.get("errors"), response.get("errors"));
        Map<String, Object> products = castMap(data(response).get("products"));
        Set<Object> skus = castList(products.get("items")).stream().map(item -> item.get("sku")).collect(Collectors.toSet());
        Assert.assertEquals(skus.size(), ((Number) products.get("total_count")).intValue());
        return skus;
    }

    private static Map<String, Object> data(Map<String, Object> response) {
        return castMap(response.get("data"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
