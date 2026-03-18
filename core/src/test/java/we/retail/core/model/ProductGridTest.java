package we.retail.core.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productlist.CategoryRetriever;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

public class ProductGridTest {

    @Test
    public void testUsesExplicitCifCategorySelectionOnCatalogRoot() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        Page productRoutePage = mock(Page.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        ModelFactory modelFactory = mock(ModelFactory.class);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever categoryRetriever = mock(CategoryRetriever.class);
        ProductListItem productListItem = mock(ProductListItem.class);
        ProductInterface product = mock(ProductInterface.class);
        Price price = mock(Price.class);
        CategoryInterface category = createCategory("Men");
        PageManager pageManager = mock(PageManager.class);

        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resource.getValueMap()).thenReturn(valueMap("category", "/"));
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/cif-products");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(currentPageContent.getValueMap()).thenReturn(valueMap("cq:cifProductPage",
            "/content/we-retail/us/en/cif-products/product-page"));
        when(pageManager.getPage("/content/we-retail/us/en/cif-products/product-page")).thenReturn(productRoutePage);
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(ProductList.class))).thenReturn(productList);
        when(productList.getCategoryRetriever()).thenReturn(categoryRetriever);
        when(productList.getTitle()).thenReturn("Default Category");
        when(productList.getProducts()).thenReturn(Collections.singletonList(productListItem));

        when(productListItem.getProduct()).thenReturn(product);
        when(productListItem.getTitle()).thenReturn("Corona Shorts");
        when(productListItem.getURL()).thenReturn("/content/we-retail/us/en/category-page.html/me/shorts/corona-shorts.html");
        when(productListItem.getPath()).thenReturn("/content/we-retail/us/en/category-page.html/me/shorts/corona-shorts.html");
        when(productListItem.getImageURL()).thenReturn("/content/dam/celadon/we-retail/me/shorts/mehisucos_img_0.jpeg");
        when(productListItem.getPriceRange()).thenReturn(price);
        when(product.getUrlPath()).thenReturn("me/shorts/corona-shorts");

        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn("$39.00");

        when(product.getCategories()).thenReturn(Collections.singletonList(category));
        when(resourceResolver.map(request, "/content/dam/celadon/we-retail/me/shorts/mehisucos_img_0.jpeg"))
            .thenReturn("/content/dam/celadon/we-retail/me/shorts/mehisucos_img_0.jpeg");

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "pageManager", pageManager);
        setField(productGrid, "modelFactory", modelFactory);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        Collection<ProductGridItem> items = productGrid.getProducts();
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("/content/we-retail/us/en/cif-products/product-page.html/me/shorts/corona-shorts.html",
            items.iterator().next().getPath());
    }

    @Test
    public void testBuildRouteProductUrlUsesProductUrlPathWhenItemUrlFails() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        ProductListItem productListItem = mock(ProductListItem.class);
        ProductInterface product = mock(ProductInterface.class);

        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(currentPageContent.getValueMap()).thenReturn(valueMap("cq:cifProductPage",
            "/content/we-retail/us/en/cif-products/product-page"));
        when(productListItem.getProduct()).thenReturn(product);
        when(productListItem.getURL()).thenThrow(new RuntimeException("root category url generation should not be required"));
        when(product.getUrlPath()).thenReturn("eq/biking/eqbisublp");

        setField(productGrid, "currentPage", currentPage);

        Method routeUrlMethod = ProductGrid.class.getDeclaredMethod("buildRouteProductUrl", ProductListItem.class);
        routeUrlMethod.setAccessible(true);
        String routeUrl = (String) routeUrlMethod.invoke(productGrid, productListItem);

        assertEquals("/content/we-retail/us/en/cif-products/product-page.html/eq/biking/eqbisublp.html", routeUrl);
    }

    @Test
    public void testExtendsProductQueryBeforeReadingTitle() throws Exception {
        ProductGrid productGrid = new ProductGrid();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageContent = mock(Resource.class);
        ModelFactory modelFactory = mock(ModelFactory.class);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever categoryRetriever = mock(CategoryRetriever.class);

        when(resource.getValueMap()).thenReturn(valueMap("category", "/"));
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/cif-products");
        when(currentPage.getContentResource()).thenReturn(currentPageContent);
        when(currentPageContent.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<String, Object>()));
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(ProductList.class))).thenReturn(productList);
        when(productList.getCategoryRetriever()).thenReturn(categoryRetriever);
        when(productList.getTitle()).thenReturn("Default Category");
        when(productList.getProducts()).thenReturn(Collections.<ProductListItem>emptyList());

        setField(productGrid, "request", request);
        setField(productGrid, "resource", resource);
        setField(productGrid, "currentPage", currentPage);
        setField(productGrid, "modelFactory", modelFactory);

        Method initMethod = ProductGrid.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productGrid);

        InOrder inOrder = inOrder(categoryRetriever, productList);
        inOrder.verify(categoryRetriever).extendProductQueryWith(any());
        inOrder.verify(productList).getTitle();
    }

    private CategoryInterface createCategory(String name) {
        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getName()).thenReturn(name);
        return category;
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
