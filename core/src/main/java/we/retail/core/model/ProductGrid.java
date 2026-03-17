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

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import we.retail.core.commerce.cif.models.CifModelAdapter;
import we.retail.core.commerce.cif.models.LegacyCommercePageSupport;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = com.adobe.cq.wcm.core.components.models.List.class,
    resourceType = "weretail/components/content/productgrid")
public class ProductGrid implements com.adobe.cq.wcm.core.components.models.List {

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

    private java.util.List<ProductGridItem> items = Collections.emptyList();

    @PostConstruct
    private void initModel() {
        if (LegacyCommercePageSupport.isReferencedRoutePage(currentPage, "cq:cifCategoryPage")) {
            items = buildRouteItems();
            if (!items.isEmpty()) {
                return;
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

            for (ProductListItem productListItem : productList.getProducts()) {
                ProductGridItem item = ProductGridItem.fromProductListItem(productListItem,
                    resolveProductPage(StringUtils.defaultIfBlank(productListItem.getURL(), productListItem.getPath())), request);
                if (item.exists()) {
                    routeItems.add(item);
                }
            }
        } catch (RuntimeException e) {
            return Collections.emptyList();
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
}
