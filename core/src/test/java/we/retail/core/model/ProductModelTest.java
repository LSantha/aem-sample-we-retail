package we.retail.core.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductModelTest {

    @Test
    public void testAdaptsToCifBackedProductModel() throws Exception {
        ProductModel productModel = new ProductModel();
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        Resource resource = mock(Resource.class);
        Page currentPage = mock(Page.class);
        Resource currentPageResource = mock(Resource.class);
        ModelFactory modelFactory = mock(ModelFactory.class);
        Product product = createProduct();

        when(request.getRequestURI()).thenReturn("/content/we-retail/us/en/products/product-page.html/fleet-cross-training-shoe.html");
        when(resource.getPath()).thenReturn("/content/we-retail/us/en/products/product-page/jcr:content/root/product");
        when(resource.getChildren()).thenReturn(Collections.<Resource>emptyList());
        when(resource.getValueMap()).thenReturn(mock(ValueMap.class));
        when(currentPage.getContentResource()).thenReturn(currentPageResource);
        when(currentPageResource.getValueMap()).thenReturn(mock(ValueMap.class));
        when(currentPage.getPath()).thenReturn("/content/we-retail/us/en/products/product-page");
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(Product.class))).thenReturn(product);

        setField(productModel, "request", request);
        setField(productModel, "resource", resource);
        setField(productModel, "currentPage", currentPage);
        setField(productModel, "modelFactory", modelFactory);

        Method initMethod = ProductModel.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(productModel);

        assertNotNull(productModel.getProductItem());
        assertNotNull(productModel.getProductItem().getTitle());
    }

    private Product createProduct() {
        Product product = mock(Product.class);
        Price price = mock(Price.class);
        AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);

        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn("$64.99");

        when(product.getFound()).thenReturn(Boolean.TRUE);
        when(product.getSku()).thenReturn("fleet-cross-training-shoe");
        when(product.getName()).thenReturn("Fleet Cross-Training Shoe");
        when(product.getPriceRange()).thenReturn(price);
        when(product.getAssets()).thenReturn(Collections.emptyList());
        when(product.getVariants()).thenReturn(Collections.emptyList());
        when(product.getVariantAttributes()).thenReturn(Collections.emptyList());
        when(product.getProductRetriever()).thenReturn(retriever);

        return product;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
