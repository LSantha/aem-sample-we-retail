package we.retail.core.model;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogAwareNavigationTest {

    @Test
    public void testLimitCatalogBranchDepthResolvesCatalogRootFromUrl() {
        PageManager pageManager = mock(PageManager.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        SiteStructure siteStructure = mock(SiteStructure.class);
        Resource catalogResource = mock(Resource.class);
        Page catalogPage = page("products");

        NavigationItem equipmentLeaf = navItem("Hiking", "/content/we-retail/us/en/products/category-page.html/eq/hiking.html",
            Collections.<NavigationItem>emptyList(), null, 2);
        NavigationItem equipment = navItem("Equipment", "/content/we-retail/us/en/products/category-page.html/eq.html",
            Collections.singletonList(equipmentLeaf), null, 1);
        NavigationItem women = navItem("Women", "/content/we-retail/us/en/products/category-page.html/wo.html",
            Collections.<NavigationItem>emptyList(), null, 1);
        NavigationItem products = navItem("Products", "/content/we-retail/us/en/products.html", Arrays.asList(equipment, women), null,
            0);

        NavigationItem shorts = navItem("Shorts", "/content/we-retail/us/en/men/shorts.html",
            Collections.<NavigationItem>emptyList(), page("shorts"), 1);
        NavigationItem men = navItem("Men", "/content/we-retail/us/en/men.html",
            Collections.singletonList(shorts), page("men"), 0);

        when(resourceResolver.resolve("/content/we-retail/us/en/products.html")).thenReturn(catalogResource);
        when(pageManager.getContainingPage(catalogResource)).thenReturn(catalogPage);
        when(siteStructure.isCatalogPage(catalogPage)).thenReturn(true);

        List<NavigationItem> items = CatalogAwareNavigation.limitCatalogBranchDepth(Arrays.asList(products, men),
            siteStructure, pageManager, resourceResolver);

        assertEquals(2, items.size());
        assertEquals(2, items.get(0).getChildren().size());
        assertTrue(items.get(0).getChildren().get(0).getChildren().isEmpty());
        assertEquals(1, items.get(1).getChildren().size());
        assertEquals("Shorts", items.get(1).getChildren().get(0).getTitle());
    }

    private NavigationItem navItem(String title, String url, List<NavigationItem> children, Page page, int level) {
        return new TestNavigationItem(title, url, children, page, level);
    }

    private Page page(String name) {
        Page page = mock(Page.class);
        when(page.getName()).thenReturn(name);
        when(page.getPath()).thenReturn("/content/we-retail/us/en/" + name);
        when(page.getTitle()).thenReturn(name);
        return page;
    }

    private static final class TestNavigationItem implements NavigationItem {
        private final String title;
        private final String url;
        private final List<NavigationItem> children;
        private final Page page;
        private final int level;

        private TestNavigationItem(String title, String url, List<NavigationItem> children, Page page, int level) {
            this.title = title;
            this.url = url;
            this.children = children;
            this.page = page;
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
            return children;
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
            return null;
        }

        @Override
        public Calendar getLastModified() {
            return null;
        }

        @Override
        public String getPath() {
            return page != null ? page.getPath() : null;
        }

        @Override
        public String getName() {
            return page != null ? page.getName() : title;
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
