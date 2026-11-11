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

import com.day.cq.tagging.Tag;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class AemContentFragmentSupportTest {
    private static final String MODEL = "/conf/venia/settings/dam/cfm/models/celadon-product";
    private static final String CATALOG = "/content/dam/celadon/venia";

    @Rule public final SlingContext context = new SlingContext();

    private ContentFragmentFakes fakes;
    private ResourceResolver resolver;

    @Before
    public void setUp() {
        context.create().resource(MODEL);
        context.create().resource(CATALOG);
        fakes = new ContentFragmentFakes(context, MODEL, "name", "price", "material", "tags");
        resolver = context.resourceResolver();
    }

    @Test
    public void ensureFragmentCreatesOnceAndUpdatesTitle() throws Exception {
        Resource created = ensure("candace-dress", "Candace Dress");
        Resource again = ensure("candace-dress", "Candace Dress (renamed)");

        assertEquals(CATALOG + "/candace-dress", created.getPath());
        assertEquals(created.getPath(), again.getPath());
        assertEquals(1, fakes.createdFragments);
        assertEquals("Candace Dress (renamed)", again.getChild("jcr:content").getValueMap().get("jcr:title", String.class));
    }

    @Test
    public void ensureFragmentReplacesNodesThatAreNotFragments() throws Exception {
        context.create().resource(CATALOG + "/candace-dress", "stale", true);

        Resource fragment = ensure("candace-dress", "Candace Dress");

        assertNull(fragment.getValueMap().get("stale"));
        assertNotNull(fakes.fragment(fragment.getPath()));
    }

    @Test
    public void ensureFragmentFailsWithoutAFragmentModel() {
        context.create().resource("/conf/other-model");
        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                AemContentFragmentSupport.ensureFragment(resolver, resolver.getResource("/conf/other-model"),
                        resolver.getResource(CATALOG), "x", "X"));
        assertTrue(error.getMessage().contains("FragmentTemplate"));
    }

    @Test
    public void writeTextAndTypedSkipUnknownElements() throws Exception {
        Resource fragment = ensure("candace-dress", "Candace Dress");

        AemContentFragmentSupport.writeText(fragment, "name", "Candace", "text/plain");
        AemContentFragmentSupport.writeText(fragment, "material", null, "text/plain");
        AemContentFragmentSupport.writeText(fragment, "unknown", "ignored", "text/plain");
        AemContentFragmentSupport.writeTyped(fragment, "unknown", 1);

        ContentFragmentFakes.Fragment fake = fakes.fragment(fragment.getPath());
        assertEquals("Candace", fake.element("name").content);
        assertEquals("text/plain", fake.element("name").mimeType);
        assertEquals("", fake.element("material").content);
        assertNull(fake.element("unknown"));
    }

    @Test
    public void writeTypedCoercesToTheCurrentValueShape() throws Exception {
        Resource fragment = ensure("candace-dress", "Candace Dress");
        ContentFragmentFakes.Element price = fakes.fragment(fragment.getPath()).elements.computeIfAbsent("price",
                name -> fakes.new Element(name));

        price.value = 0.0d;
        AemContentFragmentSupport.writeTyped(fragment, "price", 49);
        assertEquals(49.0d, price.value);

        AemContentFragmentSupport.writeTyped(fragment, "price", "59.5");
        assertEquals(59.5d, price.value);

        ContentFragmentFakes.Element tags = fakes.fragment(fragment.getPath()).elements.computeIfAbsent("tags",
                name -> fakes.new Element(name));
        tags.value = List.of();
        AemContentFragmentSupport.writeTyped(fragment, "tags", Arrays.asList("a", null, "b"));
        assertEquals(List.of("a", "b"), tags.value);

        AemContentFragmentSupport.writeTyped(fragment, "tags", null);
        assertNull(tags.value);
    }

    @Test
    public void writesVariationValuesAndRemovesStaleVariations() throws Exception {
        Resource fragment = ensure("candace-dress", "Candace Dress");

        AemContentFragmentSupport.writeVariationText(fragment, "lilac-l", "Lilac L", "name", "Candace Lilac", "text/plain");
        AemContentFragmentSupport.writeVariationText(fragment, "lilac-l", "Lilac L", "material", null, "text/plain");
        AemContentFragmentSupport.writeVariationTyped(fragment, "lilac-l", "Lilac L", "price", 59.99d);
        AemContentFragmentSupport.writeVariationTyped(fragment, "peach-l", "Peach L", "price", 69.99d);
        AemContentFragmentSupport.writeVariationText(fragment, "peach-l", "Peach L", "unknown", "x", "text/plain");

        ContentFragmentFakes.Fragment fake = fakes.fragment(fragment.getPath());
        assertEquals("Candace Lilac", fake.element("name").variation("lilac-l").content);
        assertEquals("", fake.element("material").variation("lilac-l").content);
        assertEquals(59.99d, fake.element("price").variation("lilac-l").value);
        assertEquals(69.99d, fake.element("price").variation("peach-l").value);
        assertEquals(List.of("lilac-l", "peach-l"), List.copyOf(fake.variations.keySet()));

        List<String> removed = AemContentFragmentSupport.removeVariationsExcept(fragment, Set.of("lilac-l"));

        assertEquals(List.of("peach-l"), removed);
        assertEquals(Set.of("lilac-l"), fake.variations.keySet());
        assertNull(fake.element("price").variation("peach-l"));
    }

    @Test
    public void setsVariationTagsOnlyWhenNeeded() throws Exception {
        Resource fragment = ensure("candace-dress", "Candace Dress");
        ContentFragmentFakes.Fragment fake = fakes.fragment(fragment.getPath());

        AemContentFragmentSupport.setVariationTags(fragment, List.of("lilac-l"), new Tag[0]);
        assertTrue(fake.variationTags.isEmpty());

        Tag[] tags = new Tag[1];
        AemContentFragmentSupport.setVariationTags(fragment, List.of("lilac-l", "peach-l"), tags);
        assertSame(tags, fake.variationTags.get("lilac-l"));
        assertSame(tags, fake.variationTags.get("peach-l"));
    }

    @Test
    public void createOrUpdateAssetReplacesExistingAsset() throws Exception {
        String path = CATALOG + "/images/dress.jpg";
        context.create().resource(path, "old", true);

        AemContentFragmentSupport.createOrUpdateAsset(resolver, path, "jpeg".getBytes(StandardCharsets.UTF_8), "image/jpeg");

        assertArrayEquals("jpeg".getBytes(StandardCharsets.UTF_8), fakes.assets.get(path));
        assertNull(resolver.getResource(path).getValueMap().get("old"));
    }

    @Test
    public void requiresAFragmentForFragmentOperations() {
        Resource plain = context.create().resource(CATALOG + "/plain");
        assertThrows(IllegalStateException.class,
                () -> AemContentFragmentSupport.writeText(plain, "name", "x", "text/plain"));
    }

    private Resource ensure(String name, String title) throws Exception {
        return AemContentFragmentSupport.ensureFragment(resolver, resolver.getResource(MODEL),
                resolver.getResource(CATALOG), name, title);
    }
}
