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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LegacyProductSlugsTest {

    /** Builds an ordered map from alternating key/value pairs. */
    private static Map<String, Object> m(Object... kv) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i].toString(), kv[i + 1]);
        }
        return map;
    }

    /** One QueryBuilder hit: jcr:path (page) + jcr:content/cq:productMaster (commerce node). */
    private static Map<String, Object> hit(String pagePath, String productMaster) {
        return m("jcr:path", pagePath, "jcr:content", m("cq:productMaster", productMaster));
    }

    private static Map<String, Object> result(Map<String, Object>... hits) {
        return m("success", Boolean.TRUE, "hits", List.of(hits));
    }

    @Test
    public void parse_keysByProductMasterLeaf_notByPageLeaf() {
        // Page slug leaf (bpa-free-water-bottle) differs from the identifier leaf (eqrusubpe);
        // the map must key on the identifier so the SKU-keyed importer walk can join.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/we-retail/us/en/products/equipment/running/bpa-free-water-bottle",
                        "/var/commerce/products/we-retail/eq/running/eqrusubpe")));
        assertEquals(1, slugs.size());
        assertEquals("bpa-free-water-bottle", slugs.slugFor("eqrusubpe"));
        // The page-leaf is not a key.
        assertNull(slugs.slugFor("bpa-free-water-bottle"));
    }

    @Test
    public void parse_pageTreePlacementDivergesFromCommerceTree() {
        // The authored page lives under .../products/men/coats while the commerce node lives under
        // .../we-retail/me/coats; only the productMaster leaf reliably joins the two trees.
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/we-retail/us/en/products/men/coats/portland-hooded-jacket",
                        "/var/commerce/products/we-retail/me/coats/meotwipot")));
        assertEquals("portland-hooded-jacket", slugs.slugFor("meotwipot"));
    }

    @Test
    public void parse_skipsHitsMissingEitherLeaf() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/has-both", "/var/commerce/products/site/skuok"),
                m("jcr:path", "/content/site/products/no-master"),                 // missing productMaster
                m("jcr:content", m("cq:productMaster", "/var/commerce/x/orphan")))); // missing jcr:path
        assertEquals(1, slugs.size());
        assertEquals("has-both", slugs.slugFor("skuok"));
    }

    @Test
    public void parse_duplicateIdentifierFirstWins() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/first-slug", "/var/commerce/products/site/dup"),
                hit("/content/site/products/second-slug", "/var/commerce/products/site/dup")));
        assertEquals(1, slugs.size());
        assertEquals("first-slug", slugs.slugFor("dup"));
    }

    @Test
    public void parse_toleratesTrailingSlashInPaths() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/trailing/", "/var/commerce/products/site/skux/")));
        assertEquals("trailing", slugs.slugFor("skux"));
    }

    @Test
    public void parse_nullOrEmptyJsonYieldsEmptyMap() {
        assertEquals(0, LegacyProductSlugs.parse(null).size());
        assertEquals(0, LegacyProductSlugs.parse(Map.of()).size());
    }

    @Test
    public void slugFor_unknownOrNullIdentifierReturnsNull() {
        LegacyProductSlugs slugs = LegacyProductSlugs.parse(result(
                hit("/content/site/products/known", "/var/commerce/products/site/known")));
        assertNull(slugs.slugFor("absent"));
        assertNull(slugs.slugFor(null));
    }
}
