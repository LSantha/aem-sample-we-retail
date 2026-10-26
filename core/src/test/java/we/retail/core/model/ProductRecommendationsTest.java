/*
 *   Copyright 2026 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package we.retail.core.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProductRecommendationsTest {

    private static final String PRODUCT_PAGE = "/content/we-retail/us/en/products/product-page.html/";

    @Test
    public void testListsTheRelatedProductsUpToTheConfiguredMaximum() throws Exception {
        ProductCarousel relatedProducts = mock(ProductCarousel.class);
        List<ProductListItem> items = items("Portland Hooded Jacket", "Slopeside Coat", "Brooklyn Coat");
        when(relatedProducts.getProducts()).thenReturn(items);

        ProductRecommendations recommendations = recommendations(relatedProducts, "2");

        assertEquals(2, recommendations.getProducts().size());
        ProductGridItem first = recommendations.getProducts().get(0);
        assertEquals("Portland Hooded Jacket", first.getName());
        assertEquals("$75.00", first.getPrice());
        assertEquals(PRODUCT_PAGE + "portland-hooded-jacket.html", first.getPath());
        assertEquals("Slopeside Coat", recommendations.getProducts().get(1).getName());
    }

    @Test
    public void testListsSixProductsByDefault() throws Exception {
        ProductCarousel relatedProducts = mock(ProductCarousel.class);
        List<ProductListItem> items = items("A", "B", "C", "D", "E", "F", "G");
        when(relatedProducts.getProducts()).thenReturn(items);

        assertEquals(6, recommendations(relatedProducts, null).getProducts().size());
    }

    @Test
    public void testListsConfigurableProductsFetchedWithoutOptions() throws Exception {
        ProductCarousel relatedProducts = mock(ProductCarousel.class);
        List<ProductListItem> items = items("Portland Hooded Jacket", "Slopeside Coat");
        for (ProductListItem item : items) {
            ConfigurableProduct product = mock(ConfigurableProduct.class);
            when(product.getConfigurableOptions()).thenReturn(null);
            when(item.getProduct()).thenReturn(product);
        }
        when(relatedProducts.getProducts()).thenReturn(items);

        assertEquals(2, recommendations(relatedProducts, "6").getProducts().size());
    }

    @Test
    public void testSkipsABrokenProduct() throws Exception {
        ProductCarousel relatedProducts = mock(ProductCarousel.class);
        List<ProductListItem> items = items("Portland Hooded Jacket", "Slopeside Coat");
        when(items.get(0).getTitle()).thenThrow(new IllegalStateException("broken product"));
        when(relatedProducts.getProducts()).thenReturn(items);

        ProductRecommendations recommendations = recommendations(relatedProducts, "6");

        assertEquals(1, recommendations.getProducts().size());
        assertEquals("Slopeside Coat", recommendations.getProducts().get(0).getName());
    }

    @Test
    public void testListsNothingWhenCommerceDataIsUnavailable() throws Exception {
        ProductCarousel relatedProducts = mock(ProductCarousel.class);
        when(relatedProducts.getProducts()).thenThrow(new IllegalStateException("GraphQL unavailable"));

        assertTrue(recommendations(relatedProducts, "6").getProducts().isEmpty());
        assertTrue(recommendations(null, "6").getProducts().isEmpty());
    }

    @Test
    public void testDoesNotQueryWithoutAProductInTheUrl() throws Exception {
        ProductCarousel relatedProducts = mock(ProductCarousel.class);

        assertTrue(recommendations(relatedProducts, "6", false).getProducts().isEmpty());
        verify(relatedProducts, never()).getProducts();
    }

    @Test
    public void testDoesNotQueryWhenNoProductsAreWanted() throws Exception {
        ProductCarousel relatedProducts = mock(ProductCarousel.class);

        assertTrue(recommendations(relatedProducts, "0").getProducts().isEmpty());
        verify(relatedProducts, never()).getProducts();
    }

    private ProductRecommendations recommendations(ProductCarousel relatedProducts, String max) throws Exception {
        return recommendations(relatedProducts, max, true);
    }

    private ProductRecommendations recommendations(ProductCarousel relatedProducts, String max, boolean productInUrl)
        throws Exception {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        UrlProvider urlProvider = mock(UrlProvider.class);
        UnaryOperator<ProductAttributeFilterInput> productFilter = productInUrl ? UnaryOperator.<ProductAttributeFilterInput>identity() : null;
        when(urlProvider.getProductFilterHook(request)).thenReturn(productFilter);
        Resource resource = mock(Resource.class);
        Map<String, Object> properties = new HashMap<String, Object>();
        if (max != null) {
            properties.put("max", max);
        }
        when(resource.getValueMap()).thenReturn(new ValueMapDecorator(properties));

        ProductRecommendations recommendations = new ProductRecommendations();
        setField(recommendations, "request", request);
        setField(recommendations, "urlProvider", urlProvider);
        setField(recommendations, "resource", resource);
        setField(recommendations, "cifRelatedProducts", relatedProducts);
        Method initMethod = ProductRecommendations.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(recommendations);
        return recommendations;
    }

    private List<ProductListItem> items(String... titles) {
        List<ProductListItem> items = new ArrayList<ProductListItem>();
        for (String title : titles) {
            ProductListItem item = mock(ProductListItem.class);
            Price price = mock(Price.class);
            when(price.isEmpty()).thenReturn(false);
            when(price.isRange()).thenReturn(Boolean.FALSE);
            when(price.getFormattedFinalPrice()).thenReturn("$75.00");
            when(item.getTitle()).thenReturn(title);
            when(item.getPriceRange()).thenReturn(price);
            when(item.getURL()).thenReturn(PRODUCT_PAGE + title.toLowerCase().replace(' ', '-') + ".html");
            items.add(item);
        }
        return items;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
