/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe Systems Incorporated
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
package we.retail.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.apache.sling.models.factory.ModelFactory;
import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import we.retail.core.commerce.cif.models.CifModelAdapter;
import we.retail.core.commerce.cif.models.CifProductViewSupport;
import we.retail.core.commerce.cif.models.GenericRouteSupport;
import we.retail.core.commerce.cif.models.LegacyCommercePageSupport;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = com.adobe.cq.wcm.core.components.models.List.class,
    resourceType = "weretail/components/content/productgrid")
public class ProductGrid implements com.adobe.cq.wcm.core.components.models.List {
    private static final String PN_CATEGORY = "category";
    private static final String PN_LIST_FROM = "listFrom";
    private static final String PN_PRODUCT = "product";
    private static final String LIST_FROM_PRODUCTS = "products";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @Self
    @Via(type = ResourceSuperType.class)
    private com.adobe.cq.wcm.core.components.models.List delegate;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private PageManager pageManager;

    @OSGiService
    private ModelFactory modelFactory;

    @OSGiService
    private UrlProvider urlProvider;

    private java.util.List<ProductGridItem> items = Collections.emptyList();

    @PostConstruct
    private void initModel() {
        if (isExplicitProductListMode() || hasExplicitCifProductSelection()) {
            items = buildSelectedProductItems();
            return;
        }

        boolean routeCategoryPage = LegacyCommercePageSupport.isReferencedRoutePage(currentPage, "cq:cifCategoryPage");
        boolean explicitCategorySelection = hasExplicitCifCategorySelection();

        if (routeCategoryPage || explicitCategorySelection) {
            items = buildRouteItems();
            if (hasUsableRouteItems(items) || explicitCategorySelection) {
                return;
            }

            if (routeCategoryPage) {
                items = buildLegacyRouteItems();
                if (!items.isEmpty()) {
                    return;
                }
            }
        }

        items = buildLegacyItems();
    }

    public Collection<ProductGridItem> getProducts() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public Collection<ListItem> getListItems() {
        return delegate != null ? delegate.getListItems() : Collections.<ListItem>emptyList();
    }

    private java.util.List<ProductGridItem> buildSelectedProductItems() {
        java.util.List<ProductGridItem> selectedItems = new ArrayList<ProductGridItem>();
        for (SelectedProductSelection selection : getConfiguredProductSelections()) {
            try {
                Product product = CifModelAdapter.adaptToProduct(modelFactory, request, resource, selection.getBaseSku());
                if (product == null || !Boolean.TRUE.equals(product.getFound()) || GenericRouteSupport.isPlaceholderProductName(product.getName())) {
                    continue;
                }

                String routeUrl = buildSelectedProductUrl(product, selection.getBaseSku(), selection.getVariantSku());
                ProductGridItem item = ProductGridItem.fromProduct(product, null, request, routeUrl, selection.getVariantSku());
                if (item.exists()) {
                    selectedItems.add(item);
                }
            } catch (RuntimeException e) {
                // Ignore broken selections so one bad picker value does not fail the entire grid.
            }
        }
        return selectedItems;
    }

    private java.util.List<ProductGridItem> buildRouteItems() {
        java.util.List<ProductGridItem> routeItems = new ArrayList<ProductGridItem>();
        try {
            ProductList productList = CifModelAdapter.adaptToProductList(modelFactory, request, resource);
            if (productList == null || productList.getCategoryRetriever() == null) {
                return routeItems;
            }
            productList.getCategoryRetriever().extendProductQueryWith(query -> query
                .shortDescription(description -> description.html())
                .categories(category -> category.name().urlPath())
                .onConfigurableProduct(configurableProduct -> configurableProduct.configurableOptions(option -> option
                    .attributeCode()
                    .label()
                    .values(value -> value
                        .label()
                        .valueIndex()
                        .defaultLabel()))));
            if (GenericRouteSupport.isPlaceholderCategoryTitle(productList.getTitle())) {
                return Collections.emptyList();
            }

            for (ProductListItem productListItem : productList.getProducts()) {
                try {
                    String productUrl = buildRouteProductUrl(productListItem);
                    ProductGridItem item = ProductGridItem.fromProductListItem(productListItem,
                        resolveProductPage(productUrl),
                        request,
                        productUrl);
                    if (item.exists()) {
                        routeItems.add(item);
                    }
                } catch (RuntimeException e) {
                    // Keep rendering the rest of the catalog if one product has bad URL metadata.
                }
            }
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }

        return routeItems;
    }

    private java.util.List<ProductGridItem> buildLegacyRouteItems() {
        Page categoryPage = GenericRouteSupport.resolveLegacyCategoryPage(pageManager, currentPage, request);
        Page productsRoot = GenericRouteSupport.findLegacyProductsRoot(pageManager, currentPage);
        if (categoryPage == null || productsRoot == null) {
            return Collections.emptyList();
        }

        java.util.List<ProductGridItem> routeItems = new ArrayList<ProductGridItem>();
        List<Page> productPages = GenericRouteSupport.collectProductPages(categoryPage);

        for (Page page : productPages) {
            String sku = LegacyCommercePageSupport.extractSku(page.getContentResource(), page).orElse(null);
            if (StringUtils.isBlank(sku)) {
                continue;
            }

            try {
                Product product = CifModelAdapter.adaptToProduct(modelFactory, request, resource, sku);
                if (product == null || !Boolean.TRUE.equals(product.getFound())) {
                    continue;
                }

                String routeUrl = buildLegacyRouteProductUrl(page, productsRoot);
                ProductGridItem item = ProductGridItem.fromProduct(product, page, request, routeUrl);
                if (item.exists()) {
                    routeItems.add(item);
                }
            } catch (RuntimeException e) {
                // Ignore broken route references so a single product does not fail the entire grid.
            }
        }

        return routeItems;
    }

    private java.util.List<ProductGridItem> buildLegacyItems() {
        java.util.List<ProductGridItem> legacyItems = new ArrayList<ProductGridItem>();
        if (delegate == null) {
            return legacyItems;
        }

        for (ListItem listItem : delegate.getListItems()) {
            Page page = pageManager.getPage(listItem.getPath());
            if (page == null) {
                continue;
            }

            String sku = LegacyCommercePageSupport.extractSku(page.getContentResource(), page).orElse(null);
            if (StringUtils.isBlank(sku)) {
                continue;
            }

            try {
                Product product = CifModelAdapter.adaptToProduct(modelFactory, request, resource, sku);
                if (product == null || !Boolean.TRUE.equals(product.getFound())) {
                    continue;
                }

                ProductGridItem item = ProductGridItem.fromProduct(product, page, request);
                if (item.exists()) {
                    legacyItems.add(item);
                }
            } catch (RuntimeException e) {
                // Ignore broken legacy references so a single page does not fail the entire grid.
            }
        }

        return legacyItems;
    }

    private Page resolveProductPage(String pathOrUrl) {
        if (StringUtils.isBlank(pathOrUrl) || pageManager == null) {
            return null;
        }

        String resolvedPath = StringUtils.substringBefore(pathOrUrl, "?");
        resolvedPath = StringUtils.substringBefore(resolvedPath, "#");
        resolvedPath = StringUtils.substringBefore(resolvedPath, ".html");
        return pageManager.getPage(resolvedPath);
    }

    private String rewriteRouteUrl(String pathOrUrl, String routePropertyName) {
        if (StringUtils.isBlank(pathOrUrl)) {
            return pathOrUrl;
        }

        String configuredRoute = findConfiguredRoute(routePropertyName);
        if (StringUtils.isBlank(configuredRoute)) {
            return pathOrUrl;
        }

        String configuredPrefix = configuredRoute + ".html";
        if (StringUtils.startsWith(pathOrUrl, configuredPrefix)) {
            return pathOrUrl;
        }

        int htmlIndex = pathOrUrl.indexOf(".html");
        if (htmlIndex < 0) {
            return pathOrUrl;
        }

        return configuredPrefix + pathOrUrl.substring(htmlIndex + ".html".length());
    }

    private String buildLegacyRouteProductUrl(Page productPage, Page productsRoot) {
        String configuredRoute = findConfiguredRoute("cq:cifProductPage");
        String relativePath = GenericRouteSupport.toCatalogRoutePath(
            GenericRouteSupport.relativeProductPath(productsRoot, productPage));
        if (StringUtils.isBlank(configuredRoute) || StringUtils.isBlank(relativePath)) {
            return StringUtils.EMPTY;
        }

        return configuredRoute + ".html/" + relativePath + ".html";
    }

    private String findConfiguredRoute(String routePropertyName) {
        Page page = currentPage;
        while (page != null) {
            Resource contentResource = page.getContentResource();
            if (contentResource != null) {
                String configuredRoute = contentResource.getValueMap().get(routePropertyName, String.class);
                if (StringUtils.isNotBlank(configuredRoute)) {
                    return configuredRoute;
                }
            }
            page = page.getParent();
        }
        return StringUtils.EMPTY;
    }

    private boolean hasExplicitCifProductSelection() {
        return !getConfiguredProductSelections().isEmpty();
    }

    private boolean isExplicitProductListMode() {
        return resource != null && StringUtils.equals(resource.getValueMap().get(PN_LIST_FROM, String.class), LIST_FROM_PRODUCTS);
    }

    private boolean hasExplicitCifCategorySelection() {
        return resource != null && StringUtils.isNotBlank(resource.getValueMap().get(PN_CATEGORY, String.class));
    }

    private List<SelectedProductSelection> getConfiguredProductSelections() {
        if (resource == null) {
            return Collections.emptyList();
        }

        String[] configuredProducts = resource.getValueMap().get(PN_PRODUCT, String[].class);
        if (configuredProducts == null) {
            String configuredProduct = resource.getValueMap().get(PN_PRODUCT, String.class);
            if (StringUtils.isNotBlank(configuredProduct)) {
                configuredProducts = new String[] { configuredProduct };
            }
        }

        if (configuredProducts == null || configuredProducts.length == 0) {
            return Collections.emptyList();
        }

        List<SelectedProductSelection> selections = new ArrayList<SelectedProductSelection>();
        for (String configuredProduct : configuredProducts) {
            SelectedProductSelection selection = SelectedProductSelection.from(configuredProduct);
            if (selection != null) {
                selections.add(selection);
            }
        }
        return selections;
    }

    private String buildRouteProductUrl(ProductListItem productListItem) {
        ProductInterface product = productListItem != null ? productListItem.getProduct() : null;
        String routeUrl = buildRouteProductUrl(product != null ? product.getUrlPath() : null, null);
        if (StringUtils.isNotBlank(routeUrl)) {
            return routeUrl;
        }

        String safeUrl = StringUtils.EMPTY;
        if (productListItem != null) {
            try {
                safeUrl = productListItem.getURL();
            } catch (RuntimeException e) {
                safeUrl = StringUtils.EMPTY;
            }
        }
        return rewriteRouteUrl(StringUtils.defaultIfBlank(safeUrl, productListItem != null ? productListItem.getPath() : StringUtils.EMPTY),
            "cq:cifProductPage");
    }

    private String buildSelectedProductUrl(Product product, String baseSku, String variantSku) {
        String providerUrl = buildSelectedProductUrlFromProvider(baseSku, variantSku);
        if (StringUtils.isNotBlank(providerUrl)) {
            return providerUrl;
        }

        ProductInterface productData = CifProductViewSupport.fetchProduct(product);
        return buildRouteProductUrl(productData != null ? productData.getUrlPath() : null, variantSku);
    }

    private String buildSelectedProductUrlFromProvider(String baseSku, String variantSku) {
        if (urlProvider == null || StringUtils.isBlank(baseSku)) {
            return StringUtils.EMPTY;
        }

        String routeUrl = urlProvider.toProductUrl(request, currentPage, baseSku);
        if (StringUtils.isBlank(routeUrl)) {
            return StringUtils.EMPTY;
        }

        if (StringUtils.isBlank(variantSku)) {
            return routeUrl;
        }

        return StringUtils.substringBefore(routeUrl, "#") + "#" + variantSku;
    }

    private String buildRouteProductUrl(String urlPath, String variantSku) {
        String configuredRoute = findConfiguredRoute("cq:cifProductPage");
        if (StringUtils.isBlank(configuredRoute) || StringUtils.isBlank(urlPath)) {
            return StringUtils.EMPTY;
        }

        String routeUrl = configuredRoute + ".html/" + StringUtils.removeStart(urlPath, "/") + ".html";
        if (StringUtils.isNotBlank(variantSku)) {
            return routeUrl + "#" + variantSku;
        }
        return routeUrl;
    }

    private boolean hasUsableRouteItems(List<ProductGridItem> routeItems) {
        if (routeItems.isEmpty()) {
            return false;
        }

        for (ProductGridItem item : routeItems) {
            if (!GenericRouteSupport.isPlaceholderProductListItem(item.getName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean linkItems() {
        return delegate != null && delegate.linkItems();
    }

    @Override
    public boolean showDescription() {
        return delegate != null && delegate.showDescription();
    }

    @Override
    public boolean showModificationDate() {
        return delegate != null && delegate.showModificationDate();
    }

    @Override
    public String getDateFormatString() {
        return delegate != null ? delegate.getDateFormatString() : StringUtils.EMPTY;
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }

    private static final class SelectedProductSelection {

        private final String baseSku;
        private final String variantSku;

        private SelectedProductSelection(String baseSku, String variantSku) {
            this.baseSku = baseSku;
            this.variantSku = variantSku;
        }

        private static SelectedProductSelection from(String configuredProduct) {
            String normalizedSelection = StringUtils.trimToEmpty(configuredProduct);
            if (StringUtils.isBlank(normalizedSelection)) {
                return null;
            }
            if (normalizedSelection.startsWith("/")) {
                normalizedSelection = StringUtils.substringAfterLast(normalizedSelection, "/");
            }

            CombinedSku combinedSku = CombinedSku.parse(normalizedSelection);
            if (StringUtils.isBlank(combinedSku.getBaseSku())) {
                return null;
            }
            return new SelectedProductSelection(combinedSku.getBaseSku(), combinedSku.getVariantSku());
        }

        private String getBaseSku() {
            return baseSku;
        }

        private String getVariantSku() {
            return variantSku;
        }
    }
}
