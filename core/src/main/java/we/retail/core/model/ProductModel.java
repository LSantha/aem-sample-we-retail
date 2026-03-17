/*
 *   Copyright 2016 Adobe Systems Incorporated
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

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.factory.ModelFactory;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import we.retail.core.commerce.cif.models.CifModelAdapter;
import we.retail.core.commerce.cif.models.GenericRouteSupport;
import we.retail.core.commerce.cif.models.LegacyCommercePageSupport;
import we.retail.core.commerce.cif.models.LegacyProductPresentationSupport;

@Model(adaptables = SlingHttpServletRequest.class)
public class ProductModel {

    @SlingObject
    private Resource resource;

    @SlingObject
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private PageManager pageManager;

    @OSGiService
    private ModelFactory modelFactory;

    private ProductItem productItem;

    @PostConstruct
    private void initModel() {
        try {
            if (LegacyCommercePageSupport.isReferencedRoutePage(currentPage, "cq:cifProductPage")) {
                Product routeProduct = CifModelAdapter.adaptToProduct(modelFactory, request, resource, null);
                if (isUsableRouteProduct(routeProduct)) {
                    productItem = new ProductItem(routeProduct, request, currentPage, resource);
                    return;
                }

                Page legacyProductPage = GenericRouteSupport.resolveLegacyProductPage(pageManager, currentPage, request);
                if (legacyProductPage != null) {
                    String routeSku = LegacyCommercePageSupport.extractSku(legacyProductPage.getContentResource(), legacyProductPage)
                        .orElse(null);
                    Product legacyRouteProduct = CifModelAdapter.adaptToProduct(modelFactory, request, resource, routeSku);
                    if (legacyRouteProduct != null && Boolean.TRUE.equals(legacyRouteProduct.getFound())) {
                        productItem = new ProductItem(legacyRouteProduct, request, legacyProductPage,
                            LegacyProductPresentationSupport.productResource(legacyProductPage));
                        return;
                    }
                }
            }

            String sku = LegacyCommercePageSupport.extractSku(resource, currentPage).orElse(null);
            Product product = CifModelAdapter.adaptToProduct(modelFactory, request, resource, sku);
            if (product != null && Boolean.TRUE.equals(product.getFound()) && !GenericRouteSupport.isPlaceholderProductName(product.getName())) {
                productItem = new ProductItem(product, request, currentPage, resource);
            }
        } catch (RuntimeException e) {
            // Fail soft so route pages can render surrounding authored content even when commerce data is unavailable.
        }
    }

    public ProductItem getProductItem() {
        return productItem;
    }

    public boolean hasVariants() {
        return productItem != null && !productItem.getVariants().isEmpty();
    }

    public String getAddToCartUrl() {
        return StringUtils.EMPTY;
    }

    public String getProductTrackingPath() {
        if (productItem != null && StringUtils.isNotBlank(productItem.getPagePath())) {
            return StringUtils.substringBefore(productItem.getPagePath(), ".html");
        }
        return currentPage != null ? currentPage.getPath() : resource.getPath();
    }

    private boolean isUsableRouteProduct(Product product) {
        return product != null
            && Boolean.TRUE.equals(product.getFound())
            && !GenericRouteSupport.isPlaceholderProductName(product.getName());
    }
}
