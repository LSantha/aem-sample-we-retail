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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Navigation;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Navigation.class,
    resourceType = "weretail/components/structure/navigation")
public class CatalogAwareNavigation implements Navigation {

    @Self
    @Via(type = ResourceSuperType.class)
    private Navigation delegate;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private SiteStructure siteStructure;

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private PageManager pageManager;

    @Override
    public List<NavigationItem> getItems() {
        if (delegate == null) {
            return Collections.emptyList();
        }
        return limitCatalogBranchDepth(delegate.getItems(), siteStructure, pageManager, resourceResolver);
    }

    @Override
    public String getAccessibilityLabel() {
        return delegate != null ? delegate.getAccessibilityLabel() : null;
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId() : null;
    }

    @Override
    public ComponentData getData() {
        return delegate != null ? delegate.getData() : null;
    }

    @Override
    public String getAppliedCssClasses() {
        return delegate != null ? delegate.getAppliedCssClasses() : null;
    }

    @Override
    public String getExportedType() {
        return resource != null ? resource.getResourceType() : null;
    }

    static List<NavigationItem> limitCatalogBranchDepth(List<NavigationItem> items, SiteStructure siteStructure,
        PageManager pageManager, ResourceResolver resourceResolver) {
        return wrapItems(items, false, siteStructure, pageManager, resourceResolver);
    }

    private static List<NavigationItem> wrapItems(List<NavigationItem> items, boolean trimDescendants,
        SiteStructure siteStructure, PageManager pageManager, ResourceResolver resourceResolver) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<NavigationItem> wrappedItems = new ArrayList<NavigationItem>();
        for (NavigationItem item : items) {
            wrappedItems.add(new CatalogNavigationItem(item, trimDescendants, siteStructure, pageManager, resourceResolver));
        }
        return Collections.unmodifiableList(wrappedItems);
    }

    private static Page resolveItemPage(NavigationItem item, PageManager pageManager, ResourceResolver resourceResolver) {
        if (item == null) {
            return null;
        }

        Page page = item.getPage();
        if (page != null) {
            return page;
        }
        if (pageManager == null || resourceResolver == null) {
            return null;
        }

        String url = StringUtils.substringBefore(StringUtils.substringBefore(item.getURL(), "?"), "#");
        if (StringUtils.isBlank(url) || !StringUtils.startsWith(url, "/")) {
            return null;
        }

        Resource resolvedResource = resourceResolver.resolve(url);
        if (resolvedResource != null) {
            page = pageManager.getContainingPage(resolvedResource);
            if (page != null) {
                return page;
            }
        }

        String pagePath = StringUtils.substringBefore(url, ".html");
        return StringUtils.isBlank(pagePath) ? null : pageManager.getContainingPage(pagePath);
    }

    private static final class CatalogNavigationItem implements NavigationItem {
        private final NavigationItem delegate;
        private final Page page;
        private final List<NavigationItem> children;

        private CatalogNavigationItem(NavigationItem delegate, boolean trimDescendants, SiteStructure siteStructure,
            PageManager pageManager, ResourceResolver resourceResolver) {
            this.delegate = delegate;
            this.page = resolveItemPage(delegate, pageManager, resourceResolver);

            if (delegate == null || trimDescendants) {
                this.children = Collections.emptyList();
            } else {
                boolean catalogRoot = siteStructure != null && siteStructure.isCatalogPage(page);
                this.children = wrapItems(delegate.getChildren(), catalogRoot, siteStructure, pageManager, resourceResolver);
            }
        }

        @Override
        public Page getPage() {
            return page;
        }

        @Override
        public boolean isActive() {
            return delegate != null && delegate.isActive();
        }

        @Override
        public boolean isCurrent() {
            return delegate != null && delegate.isCurrent();
        }

        @Override
        public List<NavigationItem> getChildren() {
            return children;
        }

        @Override
        public int getLevel() {
            return delegate != null ? delegate.getLevel() : 0;
        }

        @Override
        public Link getLink() {
            return delegate != null ? delegate.getLink() : null;
        }

        @Override
        public String getURL() {
            return delegate != null ? delegate.getURL() : null;
        }

        @Override
        public String getTitle() {
            return delegate != null ? delegate.getTitle() : null;
        }

        @Override
        public String getDescription() {
            return delegate != null ? delegate.getDescription() : null;
        }

        @Override
        public Calendar getLastModified() {
            if (delegate != null && delegate.getLastModified() != null) {
                return delegate.getLastModified();
            }
            return page != null ? page.getLastModified() : null;
        }

        @Override
        public String getPath() {
            if (delegate != null && StringUtils.isNotBlank(delegate.getPath())) {
                return delegate.getPath();
            }
            return page != null ? page.getPath() : null;
        }

        @Override
        public String getName() {
            if (delegate != null && StringUtils.isNotBlank(delegate.getName())) {
                return delegate.getName();
            }
            return page != null ? page.getName() : null;
        }

        @Override
        public Resource getTeaserResource() {
            return delegate != null ? delegate.getTeaserResource() : null;
        }

        @Override
        public String getId() {
            return delegate != null ? delegate.getId() : null;
        }

        @Override
        public ComponentData getData() {
            return delegate != null ? delegate.getData() : null;
        }

        @Override
        public String getAppliedCssClasses() {
            return delegate != null ? delegate.getAppliedCssClasses() : null;
        }

        @Override
        public String getExportedType() {
            return delegate != null ? delegate.getExportedType() : null;
        }
    }
}
