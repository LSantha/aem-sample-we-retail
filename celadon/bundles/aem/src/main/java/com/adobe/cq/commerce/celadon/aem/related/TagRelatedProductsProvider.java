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

import com.adobe.cq.commerce.celadon.core.api.RelatedProductsProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Relates the products that share the most tags with a product, the way the classic We.Retail
 * "similar to current" recommendations did. Ties keep the catalog order.
 */
@Component(service = RelatedProductsProvider.class)
@Designate(ocd = TagRelatedProductsProvider.Configuration.class)
public class TagRelatedProductsProvider implements RelatedProductsProvider {

    @ObjectClassDefinition(
            name = "Celadon Tag-Based Related Products",
            description = "Serves related_products as the products sharing the most tags with a product")
    public @interface Configuration {
        @AttributeDefinition(
                name = "Maximum Related Products",
                description = "Maximum number of related products returned for a product")
        int maxResults() default 6;
    }

    /** Subservice name mapped to the {@code celadon-catalog-reader} system user. */
    private static final String SUBSERVICE_CATALOG_READER = "catalog-reader";
    private static final String TAGS_PROPERTY = "cq:tags";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private volatile int maxResults;

    @Activate
    @Modified
    protected void activate(Configuration configuration) {
        this.maxResults = Math.max(0, configuration.maxResults());
    }

    @Override
    public List<String> relatedProducts(String product, List<String> candidates) {
        int limit = maxResults;
        if (limit == 0 || candidates.isEmpty()) {
            return List.of();
        }
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(
                Map.of(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_CATALOG_READER))) {
            Set<String> tags = tags(resolver, product);
            if (tags.isEmpty()) {
                return List.of();
            }
            List<Match> matches = new ArrayList<>();
            for (String candidate : candidates) {
                if (!candidate.equals(product)) {
                    int sharedTags = (int) tags(resolver, candidate).stream().filter(tags::contains).count();
                    if (sharedTags > 0) {
                        matches.add(new Match(candidate, sharedTags));
                    }
                }
            }
            return rank(matches, limit);
        } catch (LoginException e) {
            throw new IllegalStateException("Failed to open the catalog resource resolver", e);
        }
    }

    /** Orders the matches by the number of shared tags; the sort is stable, so ties keep the catalog order. */
    static List<String> rank(List<Match> matches, int limit) {
        return matches.stream()
                .sorted(Comparator.comparingInt(Match::sharedTags).reversed())
                .limit(limit)
                .map(Match::product)
                .toList();
    }

    private static Set<String> tags(ResourceResolver resolver, String product) {
        Resource metadata = resolver.getResource(product + "/jcr:content/metadata");
        String[] tags = metadata == null ? null : metadata.getValueMap().get(TAGS_PROPERTY, String[].class);
        return tags == null ? Set.of() : new LinkedHashSet<>(Arrays.asList(tags));
    }

    record Match(String product, int sharedTags) {
    }
}
