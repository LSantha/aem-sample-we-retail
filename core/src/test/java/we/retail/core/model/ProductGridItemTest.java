package we.retail.core.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

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
        Page page = mock(Page.class);
        Resource productResource = mock(Resource.class);
        Resource imageResource = mock(Resource.class);
        Price price = createPrice("$64.99");
        ConfigurableProduct product = createConfigurableProduct();

        when(listItem.getImageURL()).thenReturn("https://cdn.example.com/current-cif-image.jpg");
        when(listItem.getTitle()).thenReturn("Sussex Rain Boots");
        when(listItem.getURL()).thenReturn("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots.html");
        when(listItem.getPriceRange()).thenReturn(price);
        when(listItem.getProduct()).thenReturn(product);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(page.getPath()).thenReturn("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots");
        when(page.getContentResource("root/product")).thenReturn(productResource);
        when(productResource.getValueMap()).thenReturn(valueMap("productData",
            "/var/commerce/products/we-retail/me/footwear/meotwisus"));
        when(productResource.getChild("image")).thenReturn(imageResource);
        when(imageResource.getPath())
            .thenReturn("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots/jcr:content/root/product/image");
        when(imageResource.getValueMap()).thenReturn(valueMap("fileReference",
            "/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg"));
        when(resourceResolver.map(request, "/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg"))
            .thenReturn("/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg");

        ProductGridItem item = ProductGridItem.fromProductListItem(listItem, page, request, listItem.getURL());

        assertTrue(item.exists());
        assertEquals("footwear", item.getDescription());
        assertEquals("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots/jcr:content/root/product/image",
            item.getImageResourcePath());
        assertTrue(item.getFilters().getColors().contains("red"));
        assertTrue(item.getFilters().getSizes().contains("9"));
        assertTrue(item.getFilters().getPrices().contains("$64.99"));
    }

    private ConfigurableProduct createConfigurableProduct() {
        ConfigurableProduct product = mock(ConfigurableProduct.class);
        CategoryInterface category = createCategory("Men");
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

    private ValueMapDecorator valueMap(String key, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key, value);
        return new ValueMapDecorator(values);
    }
}
