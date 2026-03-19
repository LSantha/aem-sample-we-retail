package we.retail.core.model;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Test;

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteAwareBreadcrumbTest {

    @Test
    public void testUsesCifBreadcrumbItemsForProductRoutePages() throws Exception {
        RouteAwareBreadcrumb breadcrumb = new RouteAwareBreadcrumb();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        ModelFactory modelFactory = mock(ModelFactory.class);
        Breadcrumb delegate = mock(Breadcrumb.class);
        Breadcrumb cifBreadcrumb = mock(Breadcrumb.class);
        NavigationItem catalogItem = navigationItem("Products", "/content/we-retail/us/en/products.html");
        NavigationItem womenItem = navigationItem("Women", "/content/we-retail/us/en/products/category-page.html/wo.html");
        NavigationItem coatsItem = navigationItem("Coats", "/content/we-retail/us/en/products/category-page.html/wo/coats.html");
        NavigationItem productItem = navigationItem("Sonja Insulated Jacket",
            "/content/we-retail/us/en/products/product-page.html/wo/coats/wootwisot.html");

        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/product-page");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPageContent.getValueMap()).thenReturn(valueMap(
            "cq:cifProductPage", "/content/we-retail/us/en/products/product-page",
            "cq:cifCategoryPage", "/content/we-retail/us/en/products/category-page"));
        when(modelFactory.getModelFromWrappedRequest(any(), any(), any(Class.class))).thenReturn(cifBreadcrumb);
        when(cifBreadcrumb.getItems()).thenReturn(Arrays.asList(catalogItem, womenItem, coatsItem, productItem));
        when(delegate.getItems()).thenReturn(Collections.<NavigationItem>emptyList());

        setField(breadcrumb, "request", request);
        setField(breadcrumb, "resource", resource);
        setField(breadcrumb, "currentPage", currentPage);
        setField(breadcrumb, "modelFactory", modelFactory);
        setField(breadcrumb, "delegate", delegate);

        assertTrue(we.retail.core.commerce.cif.models.LegacyCommercePageSupport.isReferencedRoutePage(currentPage, "cq:cifProductPage"));
        assertEquals("/content/we-retail/us/en/products/category-page",
            we.retail.core.commerce.cif.models.GenericRouteSupport.findConfiguredRoute(currentPage, "cq:cifCategoryPage"));

        Collection<NavigationItem> items = breadcrumb.getItems();

        assertEquals(2, items.size());
        NavigationItem[] breadcrumbItems = items.toArray(new NavigationItem[0]);
        assertEquals("Women", breadcrumbItems[0].getTitle());
        assertEquals("Coats", breadcrumbItems[1].getTitle());
    }

    @Test
    public void testFallsBackToCifProductCategoriesForCanonicalProductRoutePages() throws Exception {
        RouteAwareBreadcrumb breadcrumb = new RouteAwareBreadcrumb();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        RequestPathInfo pathInfo = mock(RequestPathInfo.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        ModelFactory modelFactory = mock(ModelFactory.class);
        Breadcrumb delegate = mock(Breadcrumb.class);
        Breadcrumb cifBreadcrumb = mock(Breadcrumb.class);
        MagentoGraphqlClient client = mock(MagentoGraphqlClient.class);
        @SuppressWarnings("unchecked")
        GraphqlResponse<Query, Error> response = mock(GraphqlResponse.class);
        Query query = mock(Query.class);
        Products products = mock(Products.class);
        ProductInterface productData = mock(ProductInterface.class);
        CategoryInterface womenCategory = mock(CategoryInterface.class);
        CategoryInterface pantsCategory = mock(CategoryInterface.class);
        com.adobe.cq.commerce.magento.graphql.Breadcrumb womenBreadcrumb = mock(com.adobe.cq.commerce.magento.graphql.Breadcrumb.class);

        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/product-page");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(request.getRequestPathInfo()).thenReturn(pathInfo);
        when(request.adaptTo(MagentoGraphqlClient.class)).thenReturn(client);
        when(pathInfo.getSuffix()).thenReturn("/wo/pants/faba_running_pants.html");
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(currentPageContent.getValueMap()).thenReturn(valueMap(
            "cq:cifProductPage", "/content/we-retail/us/en/products/product-page",
            "cq:cifCategoryPage", "/content/we-retail/us/en/products/category-page"));
        when(modelFactory.getModelFromWrappedRequest(any(), any(), any(Class.class))).thenReturn(cifBreadcrumb);
        when(cifBreadcrumb.getItems()).thenReturn(Collections.<NavigationItem>emptyList());
        when(client.execute(any(String.class))).thenReturn(response);
        when(response.getErrors()).thenReturn(Collections.<Error>emptyList());
        when(response.getData()).thenReturn(query);
        when(query.getProducts()).thenReturn(products);
        when(products.getItems()).thenReturn(Arrays.asList(productData));
        when(productData.getUrlKey()).thenReturn("faba_running_pants");
        when(productData.getCategories()).thenReturn(Arrays.asList(womenCategory, pantsCategory));
        when(womenCategory.getUrlPath()).thenReturn("wo");
        when(womenCategory.getName()).thenReturn("Women");
        when(womenCategory.getBreadcrumbs()).thenReturn(Collections.<com.adobe.cq.commerce.magento.graphql.Breadcrumb>emptyList());
        when(pantsCategory.getUrlPath()).thenReturn("wo/pants");
        when(pantsCategory.getName()).thenReturn("Pants");
        when(pantsCategory.getBreadcrumbs()).thenReturn(Arrays.asList(womenBreadcrumb));
        when(womenBreadcrumb.getCategoryUrlPath()).thenReturn("wo");
        when(womenBreadcrumb.getCategoryName()).thenReturn("Women");
        when(delegate.getItems()).thenReturn(Collections.<NavigationItem>emptyList());

        setField(breadcrumb, "request", request);
        setField(breadcrumb, "resource", resource);
        setField(breadcrumb, "currentPage", currentPage);
        setField(breadcrumb, "modelFactory", modelFactory);
        setField(breadcrumb, "delegate", delegate);

        Collection<NavigationItem> items = breadcrumb.getItems();

        assertEquals(2, items.size());
        NavigationItem[] breadcrumbItems = items.toArray(new NavigationItem[0]);
        assertEquals("Women", breadcrumbItems[0].getTitle());
        assertEquals("/content/we-retail/us/en/products/category-page.html/wo.html", breadcrumbItems[0].getURL());
        assertEquals("Pants", breadcrumbItems[1].getTitle());
        assertEquals("/content/we-retail/us/en/products/category-page.html/wo/pants.html", breadcrumbItems[1].getURL());
    }

    private NavigationItem navigationItem(String title, String url) {
        NavigationItem item = mock(NavigationItem.class);
        when(item.getTitle()).thenReturn(title);
        when(item.getURL()).thenReturn(url);
        return item;
    }

    private ValueMapDecorator valueMap(String... keyValues) {
        Map<String, Object> values = new HashMap<String, Object>();
        for (int i = 0; i < keyValues.length; i += 2) {
            values.put(keyValues[i], keyValues[i + 1]);
        }
        return new ValueMapDecorator(values);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
