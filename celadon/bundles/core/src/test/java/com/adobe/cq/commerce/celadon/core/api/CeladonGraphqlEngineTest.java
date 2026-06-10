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

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class CeladonGraphqlEngineTest {
    @Test
    public void shouldInitializeAndSerializeNulls() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Assert.assertNotNull(engine);
        Assert.assertTrue(engine.gson().toJson(TestCatalogFixtures.map("value", null)).contains("\"value\":null"));
    }

    @Test
    public void shouldServeIntrospectionAndSelectedOperations() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> introspection = engine.execute("{ __typename }", null, null);
        Assert.assertNotNull(introspection.get("data"));

        Map<String, Object> multi = engine.execute(
                "query A { storeConfig { root_category_uid } } query B { storeConfig { base_currency_code } }",
                "B",
                null
        );
        Map<String, Object> data = castMap(multi.get("data"));
        Assert.assertEquals("USD", castMap(data.get("storeConfig")).get("base_currency_code"));
    }

    @Test
    public void shouldReturnGraphqlErrorsForUnknownFieldsAndSyntaxErrors() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> unknownField = engine.execute("{ products { items { does_not_exist } } }", null, null);
        Assert.assertTrue(((List<?>) unknownField.get("errors")).size() >= 1);
        Assert.assertNotNull(unknownField.get("data"));

        Map<String, Object> malformed = engine.execute("{ products(", null, null);
        Assert.assertTrue(((List<?>) malformed.get("errors")).size() >= 1);
        Assert.assertNotNull(malformed.get("data"));
    }

    @Test
    public void shouldExcludeUnderscorePrefixedFoldersFromCategories() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList { uid name children { uid name } } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("/", roots.getFirst().get("uid"));
        List<Map<String, Object>> children = castList(roots.getFirst().get("children"));
        for (Map<String, Object> child : children) {
            Assert.assertFalse(
                    "Folders starting with '_' must be excluded from categories: " + child.get("uid"),
                    child.get("uid").toString().contains("_options")
            );
        }
        Assert.assertTrue(
                children.stream().anyMatch(child -> "venia-dresses".equals(child.get("uid")))
        );
    }

    @Test
    public void shouldExposeFolderThumbnailAsCategoryImage() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList(filters:{ category_uid:{ eq:\"venia-dresses\" } }) { uid image } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("venia-dresses", roots.getFirst().get("uid"));
        Assert.assertEquals(
                "http://localhost:4502/content/dam/celadon/venia/venia-dresses/jcr:content/folderThumbnail",
                roots.getFirst().get("image")
        );
    }

    @Test
    public void shouldExposeManualThumbnailAsCategoryImage() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList(filters:{ category_uid:{ eq:\"venia-tops\" } }) { uid image } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("venia-tops", roots.getFirst().get("uid"));
        Assert.assertEquals(
                "http://localhost:4502/content/dam/celadon/venia/venia-tops/jcr:content/manualThumbnail.jpg",
                roots.getFirst().get("image")
        );
    }

    @Test
    public void shouldLeaveCategoryImageBlankWhenNoFolderThumbnail() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ categoryList { uid image } }",
                null,
                null
        );
        List<Map<String, Object>> roots = castList(castMap(response.get("data")).get("categoryList"));
        Assert.assertEquals("/", roots.getFirst().get("uid"));
        Assert.assertEquals("", roots.getFirst().get("image"));
    }

    @Test
    public void shouldBuildCustomAttributeMetadataFromSchema() {
        CeladonGraphqlEngine engine = TestCatalogFixtures.engine();

        Map<String, Object> response = engine.execute(
                "{ customAttributeMetadata(attributes:[{attribute_code:\"sku\",entity_type:\"catalog_product\"}]) { items { attribute_code attribute_type input_type } } }",
                null,
                null
        );

        List<Map<String, Object>> items = castList(castMap(castMap(response.get("data")).get("customAttributeMetadata")).get("items"));
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("sku", items.getFirst().get("attribute_code"));
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
