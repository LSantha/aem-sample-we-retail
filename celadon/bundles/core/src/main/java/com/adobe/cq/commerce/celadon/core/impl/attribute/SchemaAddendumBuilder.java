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
package com.adobe.cq.commerce.celadon.core.impl.attribute;

import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import java.util.Set;

public final class SchemaAddendumBuilder {

    /**
     * Attribute codes that already exist on the base Magento
     * {@code ProductAttributeFilterInput}. They must NOT be re-emitted by the
     * addendum — graphql-java treats that as a redefinition error and refuses
     * to build the schema.
     */
    private static final Set<String> BASE_SCHEMA_FIELDS = Set.of(
            "sku", "name", "price", "url_key", "url_path",
            "category_id", "category_uid", "description", "short_description"
    );

    private SchemaAddendumBuilder() {}

    public static String fromManifest(AttributeManifest manifest) {
        List<AttributeEntry> filterable = manifest.filterable().stream()
                .filter(e -> !BASE_SCHEMA_FIELDS.contains(e.code()))
                .toList();
        if (filterable.isEmpty()) return "";

        StringBuilder b = new StringBuilder();
        b.append("extend input ProductAttributeFilterInput {\n");
        for (AttributeEntry e : filterable) {
            b.append("  ").append(e.code()).append(": ").append(inputTypeFor(e.type())).append("\n");
        }
        b.append("}\n");
        return b.toString();
    }

    private static String inputTypeFor(NormalizedType type) {
        return switch (type) {
            case INT, FLOAT, PRICE, DATE -> "FilterRangeTypeInput";
            case SELECT, MULTISELECT, BOOLEAN -> "FilterEqualTypeInput";
            case STRING, TEXT, IMAGE_URL -> "FilterMatchTypeInput";
        };
    }
}
