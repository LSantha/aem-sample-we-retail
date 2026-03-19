package we.retail.core.commerce.cif.models;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenericRouteSupportTest {

    @Test
    public void testExtractRoutePathUsesRequestSuffix() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        RequestPathInfo pathInfo = mock(RequestPathInfo.class);

        when(request.getRequestPathInfo()).thenReturn(pathInfo);
        when(pathInfo.getSuffix()).thenReturn("/me/coats.html");

        assertEquals("me/coats", GenericRouteSupport.extractRoutePath(request));
    }

    @Test
    public void testFindConfiguredRouteWalksUpThePageTree() {
        Page currentPage = mock(Page.class);
        Page rootPage = mock(Page.class);
        Resource currentContent = mock(Resource.class);
        Resource rootContent = mock(Resource.class);

        when(currentPage.getContentResource()).thenReturn(currentContent);
        when(currentPage.getParent()).thenReturn(rootPage);
        when(rootPage.getContentResource()).thenReturn(rootContent);
        when(rootPage.getParent()).thenReturn(null);
        when(currentContent.getValueMap()).thenReturn(valueMap());
        when(rootContent.getValueMap()).thenReturn(valueMap("cq:cifCategoryPage", "/content/we-retail/us/en/products/category-page"));

        assertEquals("/content/we-retail/us/en/products/category-page",
            GenericRouteSupport.findConfiguredRoute(currentPage, "cq:cifCategoryPage"));
    }

    @Test
    public void testIsReferencedRoutePageMatchesConfiguredPagePath() {
        Page currentPage = mock(Page.class);
        Page siteRoot = mock(Page.class);
        Resource currentContent = mock(Resource.class);
        Resource rootContent = mock(Resource.class);

        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/product-page");
        when(currentPage.getContentResource()).thenReturn(currentContent);
        when(currentPage.getParent()).thenReturn(siteRoot);
        when(siteRoot.getContentResource()).thenReturn(rootContent);
        when(siteRoot.getParent()).thenReturn(null);
        when(currentContent.getValueMap()).thenReturn(valueMap());
        when(rootContent.getValueMap()).thenReturn(valueMap("cq:cifProductPage", "/content/we-retail/us/en/products/product-page"));

        assertTrue(GenericRouteSupport.isReferencedRoutePage(currentPage, "cq:cifProductPage"));
    }

    private ValueMapDecorator valueMap() {
        return new ValueMapDecorator(new HashMap<String, Object>());
    }

    private ValueMapDecorator valueMap(String key, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key, value);
        return new ValueMapDecorator(values);
    }
}
