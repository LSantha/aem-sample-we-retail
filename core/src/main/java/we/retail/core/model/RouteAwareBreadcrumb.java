/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe Systems Incorporated
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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;

import we.retail.core.commerce.cif.models.CifModelAdapter;
import we.retail.core.commerce.cif.models.GenericRouteSupport;
import we.retail.core.commerce.cif.models.RouteCategorySupport;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Breadcrumb.class,
    resourceType = "weretail/components/content/breadcrumb")
public class RouteAwareBreadcrumb implements Breadcrumb {

    @Self
    private SlingHttpServletRequest request;

    @Self
    @Via(type = ResourceSuperType.class)
    private Breadcrumb delegate;

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @org.apache.sling.models.annotations.injectorspecific.OSGiService
    private ModelFactory modelFactory;

    @org.apache.sling.models.annotations.injectorspecific.OSGiService
    private UrlProvider urlProvider;

    @Override
    public Collection<NavigationItem> getItems() {
        Collection<NavigationItem> cifProductItems = buildCifProductItems();
        if (!cifProductItems.isEmpty()) {
            return cifProductItems;
        }

        Collection<NavigationItem> categoryRouteItems = buildCategoryRouteItems();
        if (!categoryRouteItems.isEmpty()) {
            return categoryRouteItems;
        }

        return delegate != null ? delegate.getItems() : Collections.<NavigationItem>emptyList();
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId() : null;
    }

    @Override
    public String getAppliedCssClasses() {
        return delegate != null ? delegate.getAppliedCssClasses() : null;
    }

    @Override
    public String getExportedType() {
        return resource != null ? resource.getResourceType() : null;
    }

    private Collection<NavigationItem> buildCifProductItems() {
        if (modelFactory == null || !GenericRouteSupport.isReferencedRoutePage(currentPage, "cq:cifProductPage")) {
            return Collections.emptyList();
        }

        String categoryRoute = GenericRouteSupport.findConfiguredRoute(currentPage, "cq:cifCategoryPage");
        if (StringUtils.isBlank(categoryRoute)) {
            return Collections.emptyList();
        }

        Collection<NavigationItem> adaptedItems = buildAdaptedCifProductItems(categoryRoute);
        if (!adaptedItems.isEmpty()) {
            return adaptedItems;
        }

        return buildProductCategoryItems(categoryRoute);
    }

    private Collection<NavigationItem> buildAdaptedCifProductItems(String categoryRoute) {
        Breadcrumb cifBreadcrumb;
        try {
            cifBreadcrumb = CifModelAdapter.adaptToBreadcrumb(modelFactory, request, resource);
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }

        if (cifBreadcrumb == null || cifBreadcrumb == this) {
            return Collections.emptyList();
        }

        Collection<NavigationItem> cifItems = cifBreadcrumb.getItems();
        if (cifItems == null || cifItems.isEmpty()) {
            return Collections.emptyList();
        }

        String categoryRoutePrefix = categoryRoute + ".html";
        List<NavigationItem> items = new ArrayList<NavigationItem>();
        for (NavigationItem item : cifItems) {
            if (item != null && StringUtils.startsWith(item.getURL(), categoryRoutePrefix)) {
                items.add(item);
            }
        }

        return items.isEmpty() ? Collections.<NavigationItem>emptyList() : Collections.unmodifiableList(items);
    }

    private Collection<NavigationItem> buildCategoryRouteItems() {
        if (!GenericRouteSupport.isReferencedRoutePage(currentPage, "cq:cifCategoryPage")) {
            return Collections.emptyList();
        }

        String categoryRoute = GenericRouteSupport.findConfiguredRoute(currentPage, "cq:cifCategoryPage");
        String routePath = GenericRouteSupport.extractRoutePath(request);
        if (StringUtils.isBlank(categoryRoute) || StringUtils.isBlank(routePath)) {
            return Collections.emptyList();
        }

        CategoryTree category = RouteCategorySupport.fetchCategoryByUrlPath(request, routePath);
        if (category == null) {
            return Collections.emptyList();
        }

        return buildCategoryParentItems(categoryRoute, category.getBreadcrumbs());
    }

    private Collection<NavigationItem> buildProductCategoryItems(String categoryRoute) {
        ProductInterface productData = fetchRouteProduct();
        CategoryInterface category = selectCategory(productData, resolveContextCategoryPath());
        if (category == null) {
            return Collections.emptyList();
        }

        return buildCategoryItems(categoryRoute, category.getUrlPath(), category.getName(), category.getBreadcrumbs());
    }

    private Collection<NavigationItem> buildCategoryItems(String categoryRoute, String categoryUrlPath, String categoryName,
        List<com.adobe.cq.commerce.magento.graphql.Breadcrumb> breadcrumbs) {
        if (StringUtils.isBlank(categoryRoute) || StringUtils.isBlank(categoryUrlPath) || StringUtils.isBlank(categoryName)) {
            return Collections.emptyList();
        }

        List<NavigationItem> items = new ArrayList<NavigationItem>();
        int level = 0;
        if (breadcrumbs != null) {
            for (com.adobe.cq.commerce.magento.graphql.Breadcrumb breadcrumb : breadcrumbs) {
                if (breadcrumb == null || StringUtils.isBlank(breadcrumb.getCategoryUrlPath())
                    || StringUtils.isBlank(breadcrumb.getCategoryName())) {
                    continue;
                }
                items.add(new CommerceNavigationItem(
                    breadcrumb.getCategoryName(),
                    buildCategoryRouteUrl(categoryRoute, breadcrumb.getCategoryUrlPath()),
                    level++));
            }
        }

        items.add(new CommerceNavigationItem(categoryName, buildCategoryRouteUrl(categoryRoute, categoryUrlPath), level));
        return Collections.unmodifiableList(items);
    }

    private Collection<NavigationItem> buildCategoryParentItems(String categoryRoute,
        List<com.adobe.cq.commerce.magento.graphql.Breadcrumb> breadcrumbs) {
        if (StringUtils.isBlank(categoryRoute) || breadcrumbs == null || breadcrumbs.isEmpty()) {
            return Collections.emptyList();
        }

        List<NavigationItem> items = new ArrayList<NavigationItem>();
        int level = 0;
        for (com.adobe.cq.commerce.magento.graphql.Breadcrumb breadcrumb : breadcrumbs) {
            String categoryUrlPath = breadcrumb != null ? breadcrumb.getCategoryUrlPath() : null;
            String categoryName = breadcrumb != null ? breadcrumb.getCategoryName() : null;
            if (StringUtils.isBlank(categoryUrlPath) || StringUtils.isBlank(categoryName) || StringUtils.equals(categoryUrlPath, "/")) {
                continue;
            }
            items.add(new CommerceNavigationItem(
                categoryName,
                buildCategoryRouteUrl(categoryRoute, categoryUrlPath),
                level++));
        }

        return items.isEmpty() ? Collections.<NavigationItem>emptyList() : Collections.unmodifiableList(items);
    }

    private String resolveContextCategoryPath() {
        String suffix = extractRouteSuffix();
        return StringUtils.contains(suffix, "/") ? StringUtils.substringBeforeLast(suffix, "/") : StringUtils.EMPTY;
    }

    private String resolveRouteProductUrlKey() {
        String suffix = extractRouteSuffix();
        return StringUtils.contains(suffix, "/") ? StringUtils.substringAfterLast(suffix, "/") : suffix;
    }

    private String extractRouteSuffix() {
        RequestPathInfo pathInfo = request != null ? request.getRequestPathInfo() : null;
        String suffix = pathInfo != null ? pathInfo.getSuffix() : null;
        return StringUtils.removeStart(StringUtils.removeEnd(suffix, ".html"), "/");
    }

    private ProductInterface fetchRouteProduct() {
        String urlKey = resolveRouteProductUrlKey();
        if (StringUtils.isBlank(urlKey) || request == null) {
            return null;
        }

        MagentoGraphqlClient client = request.adaptTo(MagentoGraphqlClient.class);
        if (client == null) {
            return null;
        }

        String query = Operations.query(queryRoot -> {
            ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setUrlKey(new FilterEqualTypeInput().setEq(urlKey));
            QueryQuery.ProductsArgumentsDefinition searchArgs = search -> search.filter(filter);
            ProductsQueryDefinition queryArgs = products -> products.items(item -> item
                .name()
                .urlKey()
                .urlPath()
                .categories(category -> category
                    .uid()
                    .urlPath()
                    .name()
                    .breadcrumbs(breadcrumb -> breadcrumb
                        .categoryUid()
                        .categoryUrlPath()
                        .categoryName())));
            queryRoot.products(searchArgs, queryArgs);
        }).toString();

        try {
            GraphqlResponse<Query, Error> response = client.execute(query);
            if (response == null || response.getErrors() != null && !response.getErrors().isEmpty()) {
                return null;
            }

            Query data = response.getData();
            Products products = data != null ? data.getProducts() : null;
            if (products == null || products.getItems() == null) {
                return null;
            }

            ProductInterface firstProduct = null;
            for (ProductInterface product : products.getItems()) {
                if (product == null) {
                    continue;
                }
                if (StringUtils.equals(urlKey, product.getUrlKey())) {
                    return product;
                }
                if (firstProduct == null) {
                    firstProduct = product;
                }
            }
            return firstProduct;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private CategoryInterface selectCategory(ProductInterface productData, String contextCategoryPath) {
        if (productData == null || productData.getCategories() == null || productData.getCategories().isEmpty()) {
            return null;
        }

        CategoryInterface selectedCategory = null;
        int bestDepth = -1;
        for (CategoryInterface category : productData.getCategories()) {
            String categoryPath = category != null ? category.getUrlPath() : null;
            if (StringUtils.isBlank(categoryPath)) {
                continue;
            }

            if (StringUtils.equals(contextCategoryPath, categoryPath)) {
                return category;
            }

            int depth = StringUtils.countMatches(categoryPath, "/");
            if (selectedCategory == null || depth > bestDepth) {
                selectedCategory = category;
                bestDepth = depth;
            }
        }
        return selectedCategory;
    }

    private String buildCategoryRouteUrl(String categoryRoute, String categoryUrlPath) {
        String providerUrl = CommerceLinkSupport.resolveCategoryUrl(request, currentPage, urlProvider, categoryUrlPath,
            CommerceLinkSupport.URL_PATH);
        if (StringUtils.isNotBlank(providerUrl) && !StringUtils.equals(providerUrl, CommerceLinkSupport.DEFAULT_LINK)) {
            return providerUrl;
        }
        return categoryRoute + ".html/" + categoryUrlPath + ".html";
    }

    private static final class CommerceNavigationItem implements NavigationItem {
        private final String title;
        private final String url;
        private final int level;

        private CommerceNavigationItem(String title, String url, int level) {
            this.title = title;
            this.url = url;
            this.level = level;
        }

        @Override
        public Page getPage() {
            return null;
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean isCurrent() {
            return false;
        }

        @Override
        public List<NavigationItem> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public Link getLink() {
            return null;
        }

        @Override
        public String getURL() {
            return url;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            return StringUtils.EMPTY;
        }

        @Override
        public Calendar getLastModified() {
            return null;
        }

        @Override
        public String getPath() {
            return StringUtils.EMPTY;
        }

        @Override
        public String getName() {
            return title;
        }

        @Override
        public Resource getTeaserResource() {
            return null;
        }

        @Override
        public String getId() {
            return title;
        }

        @Override
        public String getAppliedCssClasses() {
            return null;
        }

        @Override
        public String getExportedType() {
            return null;
        }
    }
}
