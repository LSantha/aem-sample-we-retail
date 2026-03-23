package we.retail.core.commerce.cif.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommerceSiteStructureSupportTest {

    @Test
    public void testFindCategoryRouteUsesAssociatedCatalogPage() {
        SiteStructure siteStructure = mock(SiteStructure.class);
        SiteStructure.Entry entry = mock(SiteStructure.Entry.class);
        Page currentPage = page("/content/we-retail/us/en/products/category-page");
        Page catalogPage = page("/content/we-retail/us/en/products",
            "cq:cifCategoryPage", "/content/we-retail/us/en/products/category-page");

        when(siteStructure.getEntry(currentPage)).thenReturn(entry);
        when(entry.getCatalogPage()).thenReturn(catalogPage);

        assertEquals("/content/we-retail/us/en/products/category-page",
            CommerceSiteStructureSupport.findCategoryRoute(siteStructure, currentPage));
    }

    @Test
    public void testFindProductRouteFallsBackToLandingPageConfiguration() {
        SiteStructure siteStructure = mock(SiteStructure.class);
        Page currentPage = page("/content/we-retail/us/en/men");
        Page landingPage = page("/content/we-retail/us/en",
            "cq:cifProductPage", "/content/we-retail/us/en/products/product-page");

        when(siteStructure.getEntry(currentPage)).thenReturn(null);
        when(siteStructure.getLandingPage()).thenReturn(landingPage);

        assertEquals("/content/we-retail/us/en/products/product-page",
            CommerceSiteStructureSupport.findProductRoute(siteStructure, currentPage));
    }

    @Test
    public void testIsProductRoutePageMatchesSpecificRoutePageOnly() {
        SiteStructure siteStructure = mock(SiteStructure.class);
        Page productRoutePage = page("/content/we-retail/us/en/products/product-page");
        SiteStructure.Entry productEntry = mock(SiteStructure.Entry.class);

        when(productEntry.getPage()).thenReturn(productRoutePage);
        when(siteStructure.getProductPages()).thenReturn(Collections.singletonList(productEntry));

        assertTrue(CommerceSiteStructureSupport.isProductRoutePage(siteStructure, productRoutePage));
    }

    private Page page(String path, String... keyValues) {
        Page page = mock(Page.class);
        Resource contentResource = mock(Resource.class);
        when(page.getPath()).thenReturn(path);
        when(page.getContentResource()).thenReturn(contentResource);
        when(contentResource.getValueMap()).thenReturn(valueMap(keyValues));
        return page;
    }

    private ValueMapDecorator valueMap(String... keyValues) {
        Map<String, Object> values = new HashMap<String, Object>();
        for (int i = 0; i < keyValues.length; i += 2) {
            values.put(keyValues[i], keyValues[i + 1]);
        }
        return new ValueMapDecorator(values);
    }
}
