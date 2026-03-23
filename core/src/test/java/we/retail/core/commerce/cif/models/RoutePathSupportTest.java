package we.retail.core.commerce.cif.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoutePathSupportTest {

    @Test
    public void testExtractRoutePathUsesRequestSuffix() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        RequestPathInfo pathInfo = mock(RequestPathInfo.class);

        when(request.getRequestPathInfo()).thenReturn(pathInfo);
        when(pathInfo.getSuffix()).thenReturn("/me/coats.html");

        assertEquals("me/coats", RoutePathSupport.extractRoutePath(request));
    }
}
