package we.retail.core.model;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductGridItemTest {

    @Test
    public void testBuildsFiltersFromConfigurableProduct() {
        ProductListItem listItem = mock(ProductListItem.class);
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Price price = createPrice("$64.99");
        ConfigurableProduct product = createConfigurableProduct();

        when(listItem.getImageURL()).thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg");
        when(listItem.getTitle()).thenReturn("Sussex Rain Boots");
        when(listItem.getURL()).thenReturn("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html");
        when(listItem.getPriceRange()).thenReturn(price);
        when(listItem.getProduct()).thenReturn(product);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.map(request, "/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg"))
            .thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg");

        ProductGridItem item = ProductGridItem.fromProductListItem(listItem, request, listItem.getURL());

        assertTrue(item.exists());
        assertEquals("Footwear", item.getDescription());
        assertEquals("/content/dam/celadon/we-retail/me/footwear/meotwisus_img_0.jpeg", item.getImage());
        assertTrue(item.getFilters().getColors().contains("red"));
        assertTrue(item.getFilters().getSizes().contains("9"));
        assertTrue(item.getFilters().getPrices().contains("$64.99"));
    }

    @Test
    public void testUsesSelectedVariantForImageAndPrice() {
        Product product = mock(Product.class);
        Variant variant = mock(Variant.class);
        AbstractProductRetriever productRetriever = mock(AbstractProductRetriever.class);
        ProductInterface productData = mock(ProductInterface.class);
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Asset variantAsset = mock(Asset.class);
        Price basePrice = createPrice("$64.99");
        Price variantPrice = createPrice("$74.99");

        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(product.getName()).thenReturn("Sussex Rain Boots");
        when(product.getPriceRange()).thenReturn(basePrice);
        when(product.getVariants()).thenReturn(Collections.singletonList(variant));
        when(product.getProductRetriever()).thenReturn(productRetriever);
        when(productRetriever.fetchProduct()).thenReturn(productData);
        when(variant.getSku()).thenReturn("meotwisus-red-9");
        when(variant.getName()).thenReturn("Sussex Rain Boots");
        when(variant.getPriceRange()).thenReturn(variantPrice);
        when(variant.getAssets()).thenReturn(Collections.singletonList(variantAsset));
        when(variantAsset.getPath()).thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg");
        when(resourceResolver.map(request, "/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg"))
            .thenReturn("/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg");

        ProductGridItem item = ProductGridItem.fromProduct(product, null, request,
            "/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html#meotwisus-red-9",
            "meotwisus-red-9");

        assertTrue(item.exists());
        assertEquals("$74.99", item.getPrice());
        assertEquals("/content/dam/celadon/we-retail/me/footwear/meotwisus-red-9.jpeg", item.getImage());
        assertEquals("/content/we-retail/us/en/products/product-page.html/me/footwear/meotwisus.html#meotwisus-red-9",
            item.getPath());
    }

    private ConfigurableProduct createConfigurableProduct() {
        ConfigurableProduct product = mock(ConfigurableProduct.class);
        CategoryInterface category = createCategory("Footwear");
        ComplexTextValue shortDescription = createTextValue("<p>Lightweight training shoe</p>");
        ConfigurableProductOptions colorOption = createOption("color", "Red");
        ConfigurableProductOptions sizeOption = createOption("size", "9");

        when(product.getCategories()).thenReturn(Collections.singletonList(category));
        when(product.getShortDescription()).thenReturn(shortDescription);
        when(product.getConfigurableOptions()).thenReturn(Arrays.asList(colorOption, sizeOption));
        return product;
    }

    private ConfigurableProductOptions createOption(String attributeCode, String label) {
        ConfigurableProductOptions option = mock(ConfigurableProductOptions.class);
        ConfigurableProductOptionsValues value = mock(ConfigurableProductOptionsValues.class);
        when(value.getLabel()).thenReturn(label);
        when(option.getAttributeCode()).thenReturn(attributeCode);
        when(option.getValues()).thenReturn(Collections.singletonList(value));
        return option;
    }

    private CategoryInterface createCategory(String name) {
        CategoryInterface category = mock(CategoryInterface.class);
        when(category.getName()).thenReturn(name);
        return category;
    }

    private ComplexTextValue createTextValue(String html) {
        ComplexTextValue textValue = mock(ComplexTextValue.class);
        when(textValue.getHtml()).thenReturn(html);
        return textValue;
    }

    private Price createPrice(String formattedFinalPrice) {
        Price price = mock(Price.class);
        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn(formattedFinalPrice);
        return price;
    }
}
