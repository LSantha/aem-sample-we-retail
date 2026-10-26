/*
 *   Copyright 2026 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package we.retail.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ForcedResourceType;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;

/**
 * Lists the related products of the current product page through the CIF related products model.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class ProductRecommendations {

    private static final String CIF_RELATED_PRODUCTS_RESOURCE_TYPE = "core/cif/components/commerce/relatedproducts/v1/relatedproducts";
    private static final String PN_MAX = "max";
    private static final int DEFAULT_MAX = 6;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    @Via(type = ForcedResourceType.class, value = CIF_RELATED_PRODUCTS_RESOURCE_TYPE)
    private ProductCarousel cifRelatedProducts;

    @OSGiService
    private UrlProvider urlProvider;

    private List<ProductGridItem> products = Collections.emptyList();

    @PostConstruct
    private void initModel() {
        int max = resource.getValueMap().get(PN_MAX, DEFAULT_MAX);
        if (cifRelatedProducts == null || max <= 0 || urlProvider.getProductFilterHook(request) == null) {
            return;
        }

        List<ProductGridItem> items = new ArrayList<ProductGridItem>();
        try {
            for (ProductListItem productListItem : cifRelatedProducts.getProducts()) {
                if (items.size() >= max) {
                    break;
                }
                try {
                    ProductGridItem item = ProductGridItem.fromProductListItem(productListItem, request);
                    if (item.exists()) {
                        items.add(item);
                    }
                } catch (RuntimeException e) {
                    // Skip a product with broken data instead of dropping all recommendations.
                }
            }
        } catch (RuntimeException e) {
            // Render the product page without recommendations when commerce data is unavailable.
            return;
        }
        products = items;
    }

    public List<ProductGridItem> getProducts() {
        return Collections.unmodifiableList(products);
    }
}
