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

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageHeadMetadataTest {

    private final SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    private final Page currentPage = mock(Page.class);
    private final CatalogPage catalogPage = mock(CatalogPage.class);

    @Test
    public void testTakesTheProductPageMetadataFromTheProduct() throws Exception {
        Product product = mock(Product.class);
        when(product.getMetaTitle()).thenReturn("El Gordo Down Jacket");
        when(product.getMetaDescription()).thenReturn("A warm down jacket");
        when(product.getCanonicalUrl()).thenReturn("/content/we-retail/us/en/products/product-page.html/el-gordo-down-jacket.html");
        when(catalogPage.getProduct()).thenReturn(product);

        PageHeadMetadata metadata = metadata();

        assertEquals("El Gordo Down Jacket", metadata.getTitle());
        assertEquals("A warm down jacket", metadata.getDescription());
        assertEquals("/content/we-retail/us/en/products/product-page.html/el-gordo-down-jacket.html", metadata.getCanonicalUrl());
    }

    @Test
    public void testTakesTheCategoryPageMetadataFromTheProductList() throws Exception {
        ProductList productList = mock(ProductList.class);
        when(productList.getMetaTitle()).thenReturn("Coats");
        when(productList.getMetaDescription()).thenReturn("Coats for men");
        when(productList.getCanonicalUrl()).thenReturn("/content/we-retail/us/en/products/category-page.html/men/coats.html");
        when(catalogPage.getProductList()).thenReturn(productList);

        PageHeadMetadata metadata = metadata();

        assertEquals("Coats", metadata.getTitle());
        assertEquals("Coats for men", metadata.getDescription());
        assertEquals("/content/we-retail/us/en/products/category-page.html/men/coats.html", metadata.getCanonicalUrl());
    }

    @Test
    public void testFallsBackToThePageWithoutCommerceData() throws Exception {
        when(currentPage.getTitle()).thenReturn("Summer Sale");
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/summer-sale");

        PageHeadMetadata metadata = metadata();

        assertEquals("Summer Sale", metadata.getTitle());
        assertEquals("/content/we-retail/us/en/summer-sale.html", metadata.getCanonicalUrl());
    }

    private PageHeadMetadata metadata() throws Exception {
        PageHeadMetadata metadata = new PageHeadMetadata();
        setField(metadata, "request", request);
        setField(metadata, "currentPage", currentPage);
        setField(metadata, "catalogPage", catalogPage);
        Method initModel = PageHeadMetadata.class.getDeclaredMethod("initModel");
        initModel.setAccessible(true);
        initModel.invoke(metadata);
        return metadata;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
