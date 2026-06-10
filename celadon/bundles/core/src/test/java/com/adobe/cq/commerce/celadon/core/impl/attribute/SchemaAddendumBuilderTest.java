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
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchemaAddendumBuilderTest {

    @Test
    public void emptyManifestProducesEmptyAddendum() {
        String s = SchemaAddendumBuilder.fromManifest(AttributeManifest.empty("venia"));
        assertEquals("", s.trim());
    }

    @Test
    public void unfilterableEntriesAreSkipped() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("description", "Description", NormalizedType.TEXT,
                        AttributeScope.PRODUCT, false, false, 5)));
        String s = SchemaAddendumBuilder.fromManifest(m);
        assertFalse(s.contains("description"));
    }

    @Test
    public void filterableSelectAddsFilterMatchTypeInput() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("fashion_color", "Color", NormalizedType.SELECT,
                        AttributeScope.VARIANT, true, true, 20)));
        String s = SchemaAddendumBuilder.fromManifest(m);
        assertTrue(s.contains("extend input ProductAttributeFilterInput"));
        assertTrue(s.contains("fashion_color: FilterEqualTypeInput"));
    }

    @Test
    public void filterableIntUsesFilterRangeTypeInput() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("weight", "Weight", NormalizedType.INT,
                        AttributeScope.PRODUCT, true, true, 30)));
        String s = SchemaAddendumBuilder.fromManifest(m);
        assertTrue(s.contains("weight: FilterRangeTypeInput"));
    }

    @Test
    public void filterableStringUsesFilterMatchTypeInput() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("material", "Material", NormalizedType.STRING,
                        AttributeScope.PRODUCT, true, false, 40)));
        String s = SchemaAddendumBuilder.fromManifest(m);
        assertTrue(s.contains("material: FilterMatchTypeInput"));
    }

    @Test
    public void filterableBooleanUsesFilterEqualTypeInput() {
        AttributeManifest m = new AttributeManifest("venia", List.of(
                AttributeEntry.of("in_stock", "In Stock", NormalizedType.BOOLEAN,
                        AttributeScope.PRODUCT, true, true, 50)));
        String s = SchemaAddendumBuilder.fromManifest(m);
        assertTrue(s.contains("in_stock: FilterEqualTypeInput"));
    }
}
