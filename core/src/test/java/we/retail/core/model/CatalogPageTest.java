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
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.CategoryRetriever;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogPageTest {

    private static final String PRODUCT_RESOURCE_TYPE = "weretail/components/structure/product";
    private static final String PRODUCT_GRID_RESOURCE_TYPE = "weretail/components/content/productgrid";
    private static final String TEMPLATE_PATH = "/conf/we-retail/settings/wcm/templates/product-page";
    private static final ProductInterface PRODUCT = mock(ProductInterface.class);
    private static final CategoryInterface CATEGORY = mock(CategoryInterface.class);

    private final SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    private final ResourceResolver resourceResolver = mock(ResourceResolver.class);
    private final ModelFactory modelFactory = mock(ModelFactory.class);
    private final SiteStructure siteStructure = mock(SiteStructure.class);
    private final Page currentPage = mock(Page.class);

    @Test
    public void testTakesTheProductFromTheProductComponentModel() throws Exception {
        Product product = productPageWith(pageWithComponent(PRODUCT_RESOURCE_TYPE), retrieverFinding(PRODUCT), Boolean.TRUE);

        CatalogPage catalogPage = catalogPage();

        assertSame(product, catalogPage.getProduct());
        assertNull(catalogPage.getProductList());
    }

    @Test
    public void testFindsTheProductComponentInTheTemplateStructure() throws Exception {
        pageWithComponent("weretail/components/content/text");
        Template template = mock(Template.class);
        when(template.getPath()).thenReturn(TEMPLATE_PATH);
        when(currentPage.getTemplate()).thenReturn(template);
        Resource structure = parentOf(component(PRODUCT_RESOURCE_TYPE));
        when(resourceResolver.getResource(TEMPLATE_PATH + "/structure/jcr:content")).thenReturn(structure);
        Product product = productPageWith(structure.getChildren().iterator().next(), retrieverFinding(PRODUCT), Boolean.TRUE);

        assertSame(product, catalogPage().getProduct());
    }

    @Test
    public void testTakesTheProductListFromTheProductGridModel() throws Exception {
        ProductList productList = categoryPageWith(pageWithComponent(PRODUCT_GRID_RESOURCE_TYPE), retrieverFinding(CATEGORY), 0);

        CatalogPage catalogPage = catalogPage();

        assertSame(productList, catalogPage.getProductList());
        assertNull(catalogPage.getProduct());
    }

    @Test
    public void testIsNotFoundWhenTheCatalogDoesNotHaveTheProductTheUrlNames() throws Exception {
        // CIF reports placeholder data as found on author.
        productPageWith(pageWithComponent(PRODUCT_RESOURCE_TYPE), retrieverFinding((ProductInterface) null), Boolean.TRUE);

        assertTrue(catalogPage().isNotFound());
    }

    @Test
    public void testIsFoundWhenTheCatalogHasTheProductTheUrlNames() throws Exception {
        productPageWith(pageWithComponent(PRODUCT_RESOURCE_TYPE), retrieverFinding(PRODUCT), Boolean.TRUE);

        assertFalse(catalogPage().isNotFound());
    }

    @Test
    public void testFollowsCifOnTheProductTemplatePage() throws Exception {
        productPageWith(pageWithComponent(PRODUCT_RESOURCE_TYPE), null, Boolean.FALSE);
        assertTrue(catalogPage().isNotFound());

        productPageWith(pageWithComponent(PRODUCT_RESOURCE_TYPE), null, Boolean.TRUE);
        assertFalse(catalogPage().isNotFound());
    }

    @Test
    public void testIsNotFoundWhenTheCatalogDoesNotHaveTheCategoryTheUrlNames() throws Exception {
        // CIF lists placeholder products on author.
        categoryPageWith(pageWithComponent(PRODUCT_GRID_RESOURCE_TYPE), retrieverFinding((CategoryInterface) null), 2);

        assertTrue(catalogPage().isNotFound());
    }

    @Test
    public void testIsFoundWhenTheCatalogHasTheCategoryTheUrlNames() throws Exception {
        categoryPageWith(pageWithComponent(PRODUCT_GRID_RESOURCE_TYPE), retrieverFinding(CATEGORY), 0);

        assertFalse(catalogPage().isNotFound());
    }

    @Test
    public void testFollowsCifOnTheCategoryTemplatePage() throws Exception {
        categoryPageWith(pageWithComponent(PRODUCT_GRID_RESOURCE_TYPE), null, 0);
        assertTrue(catalogPage().isNotFound());

        categoryPageWith(pageWithComponent(PRODUCT_GRID_RESOURCE_TYPE), null, 2);
        assertFalse(catalogPage().isNotFound());
    }

    @Test
    public void testIsFoundWithoutACommerceComponent() throws Exception {
        pageWithComponent("weretail/components/content/text");
        when(siteStructure.isProductPage(currentPage)).thenReturn(true);

        CatalogPage catalogPage = catalogPage();

        assertNull(catalogPage.getProduct());
        assertFalse(catalogPage.isNotFound());
    }

    private Product productPageWith(Resource productComponent, AbstractProductRetriever retriever, Boolean found) throws Exception {
        when(siteStructure.isProductPage(currentPage)).thenReturn(true);
        Product product = mock(Product.class);
        when(product.getProductRetriever()).thenReturn(retriever);
        when(product.getFound()).thenReturn(found);
        ProductModel productModel = new ProductModel();
        setField(productModel, "cifProduct", product);
        when(modelFactory.getModelFromWrappedRequest(request, productComponent, ProductModel.class)).thenReturn(productModel);
        return product;
    }

    private ProductList categoryPageWith(Resource productGrid, CategoryRetriever categoryRetriever, int products) throws Exception {
        when(siteStructure.isCategoryPage(currentPage)).thenReturn(true);
        ProductList productList = mock(ProductList.class);
        when(productList.getCategoryRetriever()).thenReturn(categoryRetriever);
        List<ProductListItem> items = Collections.nCopies(products, mock(ProductListItem.class));
        when(productList.getProducts()).thenReturn(items);
        ProductGrid productGridModel = new ProductGrid();
        setField(productGridModel, "cifProductList", productList);
        when(modelFactory.getModelFromWrappedRequest(request, productGrid, ProductGrid.class)).thenReturn(productGridModel);
        return productList;
    }

    private AbstractProductRetriever retrieverFinding(ProductInterface product) {
        AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);
        when(retriever.fetchProduct()).thenReturn(product);
        return retriever;
    }

    private CategoryRetriever retrieverFinding(CategoryInterface category) {
        CategoryRetriever retriever = mock(CategoryRetriever.class);
        when(retriever.fetchCategory()).thenReturn(category);
        return retriever;
    }

    private Resource pageWithComponent(String resourceType) {
        Resource component = component(resourceType);
        Resource content = parentOf(component);
        when(currentPage.getContentResource()).thenReturn(content);
        return component;
    }

    private Resource component(String resourceType) {
        Resource component = mock(Resource.class);
        when(component.isResourceType(resourceType)).thenReturn(true);
        when(component.getChildren()).thenReturn(Collections.<Resource>emptyList());
        return component;
    }

    private Resource parentOf(Resource child) {
        Resource parent = mock(Resource.class);
        when(parent.getChildren()).thenReturn(Collections.singletonList(child));
        return parent;
    }

    private CatalogPage catalogPage() throws Exception {
        CatalogPage catalogPage = new CatalogPage();
        setField(catalogPage, "request", request);
        setField(catalogPage, "resourceResolver", resourceResolver);
        setField(catalogPage, "modelFactory", modelFactory);
        setField(catalogPage, "siteStructure", siteStructure);
        setField(catalogPage, "currentPage", currentPage);
        Method initModel = CatalogPage.class.getDeclaredMethod("initModel");
        initModel.setAccessible(true);
        initModel.invoke(catalogPage);
        return catalogPage;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
