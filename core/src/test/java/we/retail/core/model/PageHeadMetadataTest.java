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
import java.util.Collections;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageHeadMetadataTest {

    private static final String PRODUCT_RESOURCE_TYPE = "weretail/components/structure/product";
    private static final String PRODUCT_GRID_RESOURCE_TYPE = "weretail/components/content/productgrid";

    private final SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    private final ModelFactory modelFactory = mock(ModelFactory.class);
    private final SiteStructure siteStructure = mock(SiteStructure.class);
    private final Page currentPage = mock(Page.class);

    @Test
    public void testTakesTheProductPageMetadataFromTheProductComponentModel() throws Exception {
        Resource productComponent = pageWithComponent(PRODUCT_RESOURCE_TYPE);
        when(siteStructure.isProductPage(currentPage)).thenReturn(true);
        Product cifProduct = mock(Product.class);
        when(cifProduct.getMetaTitle()).thenReturn("El Gordo Down Jacket");
        when(cifProduct.getMetaDescription()).thenReturn("A warm down jacket");
        when(cifProduct.getCanonicalUrl()).thenReturn("/content/we-retail/us/en/products/product-page.html/el-gordo-down-jacket.html");
        ProductModel productModel = new ProductModel();
        setField(productModel, "cifProduct", cifProduct);
        when(modelFactory.getModelFromWrappedRequest(request, productComponent, ProductModel.class)).thenReturn(productModel);

        PageHeadMetadata metadata = metadata();

        assertEquals("El Gordo Down Jacket", metadata.getTitle());
        assertEquals("A warm down jacket", metadata.getDescription());
        assertEquals("/content/we-retail/us/en/products/product-page.html/el-gordo-down-jacket.html", metadata.getCanonicalUrl());
    }

    @Test
    public void testTakesTheCategoryPageMetadataFromTheProductGridModel() throws Exception {
        Resource productGrid = pageWithComponent(PRODUCT_GRID_RESOURCE_TYPE);
        when(siteStructure.isCategoryPage(currentPage)).thenReturn(true);
        ProductList cifProductList = mock(ProductList.class);
        when(cifProductList.getMetaTitle()).thenReturn("Coats");
        when(cifProductList.getMetaDescription()).thenReturn("Coats for men");
        when(cifProductList.getCanonicalUrl()).thenReturn("/content/we-retail/us/en/products/category-page.html/men/coats.html");
        ProductGrid productGridModel = new ProductGrid();
        setField(productGridModel, "cifProductList", cifProductList);
        when(modelFactory.getModelFromWrappedRequest(request, productGrid, ProductGrid.class)).thenReturn(productGridModel);

        PageHeadMetadata metadata = metadata();

        assertEquals("Coats", metadata.getTitle());
        assertEquals("Coats for men", metadata.getDescription());
        assertEquals("/content/we-retail/us/en/products/category-page.html/men/coats.html", metadata.getCanonicalUrl());
    }

    @Test
    public void testFallsBackToThePageWithoutACommerceComponent() throws Exception {
        pageWithComponent("weretail/components/content/text");
        when(siteStructure.isProductPage(currentPage)).thenReturn(true);
        when(currentPage.getTitle()).thenReturn("Summer Sale");
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/product-page");

        PageHeadMetadata metadata = metadata();

        assertEquals("Summer Sale", metadata.getTitle());
        assertEquals("/content/we-retail/us/en/products/product-page.html", metadata.getCanonicalUrl());
    }

    private Resource pageWithComponent(String resourceType) {
        Resource component = mock(Resource.class);
        when(component.isResourceType(resourceType)).thenReturn(true);
        when(component.getChildren()).thenReturn(Collections.<Resource>emptyList());
        Resource content = mock(Resource.class);
        when(content.getChildren()).thenReturn(Collections.singletonList(component));
        when(currentPage.getContentResource()).thenReturn(content);
        return component;
    }

    private PageHeadMetadata metadata() throws Exception {
        PageHeadMetadata metadata = new PageHeadMetadata();
        setField(metadata, "request", request);
        setField(metadata, "modelFactory", modelFactory);
        setField(metadata, "siteStructure", siteStructure);
        setField(metadata, "currentPage", currentPage);
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
