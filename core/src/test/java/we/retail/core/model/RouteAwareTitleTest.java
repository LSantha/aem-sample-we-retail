package we.retail.core.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.wcm.core.components.models.Title;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteAwareTitleTest {

    @Test
    public void testUsesCifCategoryNameForCategoryRoutePages() throws Exception {
        RouteAwareTitle title = new RouteAwareTitle();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        RequestPathInfo pathInfo = mock(RequestPathInfo.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        Title delegate = mock(Title.class);
        MagentoGraphqlClient client = mock(MagentoGraphqlClient.class);
        @SuppressWarnings("unchecked")
        GraphqlResponse<Query, Error> response = mock(GraphqlResponse.class);
        Query query = mock(Query.class);
        CategoryTree category = mock(CategoryTree.class);

        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/category-page");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(request.getRequestPathInfo()).thenReturn(pathInfo);
        when(request.adaptTo(MagentoGraphqlClient.class)).thenReturn(client);
        when(pathInfo.getSuffix()).thenReturn("/eq/snow-sports.html");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPageContent.getValueMap()).thenReturn(valueMap("cq:cifCategoryPage",
            "/content/we-retail/us/en/products/category-page"));
        when(delegate.getText()).thenReturn("Category Page");
        when(client.execute(any(String.class))).thenReturn(response);
        when(response.getErrors()).thenReturn(Collections.<Error>emptyList());
        when(response.getData()).thenReturn(query);
        when(query.getCategoryList()).thenReturn(Arrays.asList(category));
        when(category.getUrlPath()).thenReturn("eq/snow-sports");
        when(category.getName()).thenReturn("Snow Sports");

        setField(title, "request", request);
        setField(title, "resource", resource);
        setField(title, "currentPage", currentPage);
        setField(title, "delegate", delegate);

        assertEquals("Snow Sports", title.getText());
    }

    @Test
    public void testFallsBackToHumanizedRoutePathWhenCategoryLookupFails() throws Exception {
        RouteAwareTitle title = new RouteAwareTitle();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        RequestPathInfo pathInfo = mock(RequestPathInfo.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        Title delegate = mock(Title.class);

        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/category-page");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(request.getRequestPathInfo()).thenReturn(pathInfo);
        when(pathInfo.getSuffix()).thenReturn("/me/coats.html");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPageContent.getValueMap()).thenReturn(valueMap("cq:cifCategoryPage",
            "/content/we-retail/us/en/products/category-page"));
        when(delegate.getText()).thenReturn("Category Page");

        setField(title, "request", request);
        setField(title, "resource", resource);
        setField(title, "currentPage", currentPage);
        setField(title, "delegate", delegate);

        assertEquals("Coats", title.getText());
    }

    private ValueMapDecorator valueMap(String key, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key, value);
        return new ValueMapDecorator(values);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
