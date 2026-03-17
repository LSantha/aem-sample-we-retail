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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import we.retail.core.commerce.cif.models.GenericRouteSupport;
import we.retail.core.commerce.cif.models.LegacyCommercePageSupport;
import we.retail.core.util.WeRetailHelper;

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

    @ScriptVariable
    private PageManager pageManager;

    @Override
    public Collection<NavigationItem> getItems() {
        Collection<NavigationItem> routeItems = buildRouteItems();
        if (!routeItems.isEmpty()) {
            return routeItems;
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

    private Collection<NavigationItem> buildRouteItems() {
        Page productsRoot = GenericRouteSupport.findLegacyProductsRoot(pageManager, currentPage);
        if (productsRoot == null) {
            return Collections.emptyList();
        }

        Page targetPage = null;
        String routeProperty = null;
        if (LegacyCommercePageSupport.isReferencedRoutePage(currentPage, "cq:cifProductPage")) {
            targetPage = GenericRouteSupport.resolveLegacyProductPage(pageManager, currentPage, request);
            routeProperty = "cq:cifCategoryPage";
        } else if (LegacyCommercePageSupport.isReferencedRoutePage(currentPage, "cq:cifCategoryPage")) {
            targetPage = GenericRouteSupport.resolveLegacyCategoryPage(pageManager, currentPage, request);
            routeProperty = "cq:cifCategoryPage";
        }

        if (targetPage == null || StringUtils.isBlank(routeProperty)) {
            return Collections.emptyList();
        }

        String categoryRoute = GenericRouteSupport.findConfiguredRoute(currentPage, routeProperty);
        if (StringUtils.isBlank(categoryRoute)) {
            return Collections.emptyList();
        }

        List<Page> trailPages = new ArrayList<Page>();
        Page ancestor = targetPage.getParent();
        while (ancestor != null && !StringUtils.equals(ancestor.getPath(), productsRoot.getPath())) {
            trailPages.add(0, ancestor);
            ancestor = ancestor.getParent();
        }

        if (trailPages.isEmpty()) {
            return Collections.emptyList();
        }

        List<NavigationItem> items = new ArrayList<NavigationItem>();
        int level = 0;
        for (Page page : trailPages) {
            String relativePath = GenericRouteSupport.toCatalogRoutePath(
                GenericRouteSupport.relativeProductPath(productsRoot, page));
            String url = categoryRoute + ".html/" + relativePath + ".html";
            items.add(new RouteNavigationItem(page, url, level++));
        }
        return Collections.unmodifiableList(items);
    }

    private static final class RouteNavigationItem implements NavigationItem {
        private final Page page;
        private final String url;
        private final int level;

        private RouteNavigationItem(Page page, String url, int level) {
            this.page = page;
            this.url = url;
            this.level = level;
        }

        @Override
        public Page getPage() {
            return page;
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
            return WeRetailHelper.getTitle(page);
        }

        @Override
        public String getDescription() {
            return StringUtils.EMPTY;
        }

        @Override
        public Calendar getLastModified() {
            return page != null ? page.getLastModified() : null;
        }

        @Override
        public String getPath() {
            return page != null ? page.getPath() : StringUtils.EMPTY;
        }

        @Override
        public String getName() {
            return page != null ? page.getName() : StringUtils.EMPTY;
        }

        @Override
        public Resource getTeaserResource() {
            return null;
        }

        @Override
        public String getId() {
            return page != null ? page.getName() : null;
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
