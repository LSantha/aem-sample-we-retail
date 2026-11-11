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

import com.adobe.cq.commerce.celadon.aem.attribute.source.MagentoIntrospector;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeEntry;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeScope;
import com.adobe.cq.commerce.celadon.core.api.attribute.NormalizedType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class OptionDefinitionWriterTest {
    private static final String MODEL = "/conf/venia/settings/dam/cfm/models/celadon-option-definition";
    private static final String CATALOG = "/content/dam/celadon/venia";

    @Rule public final SlingContext context = new SlingContext();

    private ContentFragmentFakes fakes;
    private OptionDefinitionWriter writer;

    @Before
    public void setUp() throws Exception {
        context.create().resource(MODEL);
        Resource catalog = context.create().resource(CATALOG);
        fakes = new ContentFragmentFakes(context, MODEL,
                "label", "attributeCode", "productField", "swatchType", "values", "configurableOptions");
        writer = new OptionDefinitionWriter(context.resourceResolver(), catalog, context.resourceResolver().getResource(MODEL));
    }

    @Test
    public void constructorCreatesTitledOptionsFolder() {
        Resource folder = context.resourceResolver().getResource(CATALOG + "/_options");
        assertNotNull(folder);
        assertEquals("sling:OrderedFolder", folder.getValueMap().get("jcr:primaryType", String.class));
        assertEquals("Configurable Options", folder.getChild("jcr:content").getValueMap().get("jcr:title", String.class));
    }

    @Test
    public void fromMagentoEncodesValuesAndSwatchType() {
        OptionDefinitionWriter.OptionInput input = OptionDefinitionWriter.fromMagento(Map.of(
                "label", "Fashion Color",
                "attribute_code", "fashion_color",
                "values", List.of(
                        Map.of("value_index", 92, "label", "Lilac",
                                "swatch_data", Map.of("__typename", "ColorSwatchData", "value", "#c8a2c8")),
                        Map.of("value_index", "93.0", "label", "Peach; light",
                                "swatch_data", Map.of("__typename", "UnknownSwatch", "value", "a;b")),
                        Map.of("value_index", "garbage", "label", "Plain"),
                        "not-a-map")));

        assertEquals("Fashion Color", input.label());
        assertEquals("fashion_color", input.attributeCode());
        assertEquals("fashion_color", input.productField());
        assertEquals("color", input.swatchType());
        assertEquals(List.of("92;Lilac;#c8a2c8", "93;Peach, light;a,b", "0;Plain"), input.values());
        assertEquals(List.of("Lilac", "Peach, light", "Plain"), input.valueLabels());
        assertFalse(input.isEmpty());
    }

    @Test
    public void fromMagentoMapsTextAndImageSwatches() {
        assertEquals("text", OptionDefinitionWriter.fromMagento(swatchOption("TextSwatchData")).swatchType());
        assertEquals("image", OptionDefinitionWriter.fromMagento(swatchOption("ImageSwatchData")).swatchType());
        assertEquals("", OptionDefinitionWriter.fromMagento(Map.of("label", "Size", "values", "none")).swatchType());
    }

    @Test
    public void optionInputDefaultsNullsAndReportsEmpty() {
        OptionDefinitionWriter.OptionInput input = new OptionDefinitionWriter.OptionInput(null, null, null, null, null);
        assertEquals("", input.label());
        assertEquals("", input.attributeCode());
        assertEquals("", input.productField());
        assertEquals("", input.swatchType());
        assertEquals(List.of(), input.values());
        assertTrue(input.isEmpty());
        assertTrue(new OptionDefinitionWriter.OptionInput("Size", "size", "size", "", List.of()).isEmpty());
        assertEquals(List.of(""), new OptionDefinitionWriter.OptionInput("Size", "size", "size", "", List.of("1")).valueLabels());
    }

    @Test
    public void ensureOptionWritesFragmentAndReusesIdenticalDefinitions() throws Exception {
        OptionDefinitionWriter.OptionInput input = new OptionDefinitionWriter.OptionInput(
                "Fashion Color", "fashion_color", "fashion_color", "color", List.of("92;Lilac;#c8a2c8", "93;Peach"));

        String path = writer.ensureOption(input);

        assertEquals(CATALOG + "/_options/fashion-color--lilac-peach", path);
        assertEquals("Fashion Color: Lilac, Peach",
                context.resourceResolver().getResource(path + "/jcr:content").getValueMap().get("jcr:title", String.class));
        ContentFragmentFakes.Fragment fragment = fakes.fragment(path);
        assertEquals("Fashion Color", fragment.element("label").content);
        assertEquals("fashion_color", fragment.element("attributeCode").content);
        assertEquals("color", fragment.element("swatchType").content);
        assertArrayEquals(new String[]{"92;Lilac;#c8a2c8", "93;Peach"}, (String[]) fragment.element("values").value);

        OptionDefinitionWriter.OptionInput reordered = new OptionDefinitionWriter.OptionInput(
                "Color", "fashion_color", "fashion_color", "color", List.of("93;Peach", "92;Lilac;#c8a2c8"));
        assertEquals(path, writer.ensureOption(reordered));
        assertEquals(1, fakes.createdFragments);
    }

    @Test
    public void ensureOptionSuffixesCollidingNodeNames() throws Exception {
        String first = writer.ensureOption(new OptionDefinitionWriter.OptionInput(
                "Size", "size", "size", "", List.of("1;S", "2;M")));
        String second = writer.ensureOption(new OptionDefinitionWriter.OptionInput(
                "Size", "size", "size", "text", List.of("1;S", "2;M")));
        String third = writer.ensureOption(new OptionDefinitionWriter.OptionInput(
                "Size", "size", "size", "image", List.of("1;S", "2;M")));

        assertEquals(CATALOG + "/_options/size--s-m", first);
        assertEquals(CATALOG + "/_options/size--s-m-2", second);
        assertEquals(CATALOG + "/_options/size--s-m-3", third);
    }

    @Test
    public void ensureOptionAbbreviatesLongValueListsAndTitlesEmptyOnes() throws Exception {
        String path = writer.ensureOption(new OptionDefinitionWriter.OptionInput(
                "Size", "size.code", "size", "", List.of("1;XS", "2;S", "3;M", "4;L", "5;XL", "6;XXL")));
        assertEquals(CATALOG + "/_options/sizecode--xs-s-m-l-plus2", path);
        assertEquals("Size: XS, S, M, L, ... (6 values)",
                context.resourceResolver().getResource(path + "/jcr:content").getValueMap().get("jcr:title", String.class));

        String noValues = writer.ensureOption(new OptionDefinitionWriter.OptionInput("Size", "size", "size", "", List.of()));
        assertEquals(CATALOG + "/_options/size--", noValues);
        assertEquals("Size",
                context.resourceResolver().getResource(noValues + "/jcr:content").getValueMap().get("jcr:title", String.class));
    }

    @Test
    public void ensureOptionFromManifestUsesNumericValueIndexOrPosition() throws Exception {
        AttributeEntry entry = AttributeEntry.of("fashion_size", "Fashion Size", NormalizedType.SELECT,
                AttributeScope.VARIANT, true, true, 0);

        String path = writer.ensureOptionFromManifest(entry, List.of(
                new MagentoIntrospector.OptionValue("137", "L"),
                new MagentoIntrospector.OptionValue("x", "M"),
                new MagentoIntrospector.OptionValue(null, "S")));

        ContentFragmentFakes.Fragment fragment = fakes.fragment(path);
        assertEquals("Fashion Size", fragment.element("label").content);
        assertEquals("fashion_size", fragment.element("productField").content);
        assertArrayEquals(new String[]{"137;L", "2;M", "3;S"}, (String[]) fragment.element("values").value);
    }

    @Test
    public void ensureOptionFromManifestValuesFallsBackToCodeAsLabel() throws Exception {
        AttributeEntry entry = AttributeEntry.of("material", null, NormalizedType.SELECT,
                AttributeScope.PRODUCT, true, true, 0);

        String path = writer.ensureOptionFromManifestValues(entry, new LinkedHashSet<>(List.of("Cotton", "Silk")));

        assertEquals(CATALOG + "/_options/material--cotton-silk", path);
        ContentFragmentFakes.Fragment fragment = fakes.fragment(path);
        assertEquals("material", fragment.element("label").content);
        assertArrayEquals(new String[]{"1;Cotton", "2;Silk"}, (String[]) fragment.element("values").value);
    }

    @Test
    public void writeProductReferencesStoresOptionPaths() throws Exception {
        Resource product = AemContentFragmentSupport.ensureFragment(context.resourceResolver(),
                context.resourceResolver().getResource(MODEL), context.resourceResolver().getResource(CATALOG),
                "product", "Product");

        writer.writeProductReferences(product, List.of("/a", "/b"));
        assertArrayEquals(new String[]{"/a", "/b"},
                (String[]) fakes.fragment(product.getPath()).element("configurableOptions").value);

        writer.writeProductReferences(product, null);
        assertArrayEquals(new String[0],
                (String[]) fakes.fragment(product.getPath()).element("configurableOptions").value);
    }

    private static Map<String, Object> swatchOption(String typename) {
        return Map.of("label", "Option", "attribute_code", "option",
                "values", List.of(Map.of("value_index", 1L, "label", "One",
                        "swatch_data", Map.of("__typename", typename, "value", "v"))));
    }
}
