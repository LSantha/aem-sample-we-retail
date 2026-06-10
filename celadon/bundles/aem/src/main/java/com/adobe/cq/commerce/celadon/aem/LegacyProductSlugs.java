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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure (Sling-free, JCR-free) parser that turns a source AEM QueryBuilder result into an
 * {@code identifier -> slug} map for the legacy importer's {@code BLUEPRINT} mode.
 *
 * <p>Classic AEM commerce keeps the editorial slug and the commerce identifier in two parallel
 * JCR trees. The authored product-page tree ({@code /content/<site>/.../products}) carries the
 * human slug as the page node name and links to the commerce data node via
 * {@code jcr:content/cq:productMaster}. The commerce tree ({@code /var/commerce/products/...})
 * names its nodes after the technical identifier (the SKU) — exactly the {@code productKey} the
 * importer walk passes to {@code importLegacyProduct}.
 *
 * <p>A single source QueryBuilder call returns one hit per product page with its {@code jcr:path}
 * and {@code jcr:content/cq:productMaster}. This parser keys the map by the {@code cq:productMaster}
 * leaf (the identifier) and stores the {@code jcr:path} leaf (the slug), because page-tree placement
 * does not mirror commerce-tree placement — only the identifier reliably joins the two.
 */
public final class LegacyProductSlugs {

    private final Map<String, String> slugByIdentifier;

    private LegacyProductSlugs(Map<String, String> slugByIdentifier) {
        this.slugByIdentifier = slugByIdentifier;
    }

    /** The authored editorial slug for a commerce identifier, or {@code null} when none was harvested. */
    public String slugFor(String identifier) {
        return identifier == null ? null : slugByIdentifier.get(identifier);
    }

    /** Number of harvested identifier→slug mappings. */
    public int size() {
        return slugByIdentifier.size();
    }

    /**
     * Parse a QueryBuilder {@code .json} result into an {@code identifier -> slug} map. Each hit
     * contributes {@code cq:productMaster-leaf -> jcr:path-leaf}. Hits missing either leaf are
     * skipped; on duplicate identifiers the first hit wins (deterministic by source order).
     */
    public static LegacyProductSlugs parse(Map<String, Object> queryBuilderJson) {
        Map<String, String> map = new LinkedHashMap<>();
        if (queryBuilderJson != null) {
            for (Map<String, Object> hit : listOfMaps(queryBuilderJson.get("hits"))) {
                String slug = leaf(stringValue(hit.get("jcr:path")));
                String identifier = leaf(stringValue(asMap(hit.get("jcr:content")).get("cq:productMaster")));
                if (slug.isBlank() || identifier.isBlank()) {
                    continue;
                }
                map.putIfAbsent(identifier, slug);
            }
        }
        return new LegacyProductSlugs(map);
    }

    /** Coerce a value into a {@code List<Map<String,Object>>}, dropping non-map entries. */
    private static List<Map<String, Object>> listOfMaps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?>) {
                    result.add(asMap(entry));
                }
            }
        }
        return result;
    }

    /** Coerce a value into a {@code Map<String,Object>} with string keys; empty for non-maps. */
    private static Map<String, Object> asMap(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return result;
    }

    /** Trailing-slash-tolerant final path segment; empty for blank input. */
    private static String leaf(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
