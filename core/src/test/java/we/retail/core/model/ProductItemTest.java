package we.retail.core.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

import org.apache.sling.api.SlingHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductItemTest {

    @Test
    public void testBuildsLegacyWeRetailVariantContractFromPageContent() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource productResource = createProductResource();
        when(request.getRequestURI()).thenReturn("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots.html");
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.map(request, "/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg"))
            .thenReturn("/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg");

        ProductItem item = new ProductItem(createProduct(), request, null, productResource);

        assertEquals("meotwisus", item.getSku());
        assertEquals("Sussex Rain Boots", item.getTitle());
        assertEquals("footwear", item.getDescription());
        assertEquals("$65.00", item.getPrice());
        assertEquals("Classic in style, the Sussex Rain Boots provide excellent waterproof protection and great traction.",
            item.getSummary());
        assertTrue(item.getFeatures().contains("Mid-height natural rubber uppers"));
        assertEquals("/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg", item.getImageUrl());
        assertEquals("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots/jcr:content/root/product", item.getPath());
        assertEquals(1, item.getVariants().size());
        assertEquals("meotwisus-9", item.getVariants().get(0).getSku());
        assertEquals("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots/jcr:content/root/product/meotwisus-9",
            item.getVariants().get(0).getPath());
        assertEquals("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots.html#meotwisus-9",
            item.getVariants().get(0).getPagePath());
        assertEquals("9", item.getVariants().get(0).getVariantValueForAxis("size"));
        assertTrue(item.getVariantsAxesValues().get("size").contains("9"));
    }

    @Test
    public void testPrefersCifSkusWhenAvailable() {
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource productResource = createProductResource();
        when(request.getRequestURI()).thenReturn("/content/we-retail/us/en/cif-products/product-page.html/wo/coats/sonja-insulated-jacket.html");
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.map(request, "/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg"))
            .thenReturn("/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg");

        ProductItem item = new ProductItem(createProduct("wr-sonja-jacket", "wr-sonja-jacket-green-xs"), request, null, productResource);

        assertEquals("wr-sonja-jacket", item.getSku());
        assertEquals("wr-sonja-jacket-green-xs", item.getVariants().get(0).getSku());
        assertEquals("/content/we-retail/us/en/cif-products/product-page.html/wo/coats/sonja-insulated-jacket.html#wr-sonja-jacket-green-xs",
            item.getVariants().get(0).getPagePath());
    }

    private Product createProduct() {
        return createProduct("me/footwear/meotwisus", "meotwisus-9");
    }

    private Product createProduct(String productSku, String variantSku) {
        Product product = mock(Product.class);
        Price price = createPrice("$65.00");
        Asset asset = createAsset("https://cdn.example.com/current-cif-image.jpg");
        Variant variant = createVariant(variantSku);
        VariantAttribute attribute = createVariantAttribute();
        ProductInterface productData = createProductData();
        AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);

        when(product.getFound()).thenReturn(Boolean.TRUE);
        when(product.getSku()).thenReturn(productSku);
        when(product.getName()).thenReturn("Sussex Rain Boots");
        when(product.getPriceRange()).thenReturn(price);
        when(product.getAssets()).thenReturn(Collections.singletonList(asset));
        when(product.getVariants()).thenReturn(Collections.singletonList(variant));
        when(product.getVariantAttributes()).thenReturn(Collections.singletonList(attribute));
        when(retriever.fetchProduct()).thenReturn(productData);
        when(product.getProductRetriever()).thenReturn(retriever);

        return product;
    }

    private Variant createVariant() {
        return createVariant("meotwisus-9");
    }

    private Variant createVariant(String sku) {
        Variant variant = mock(Variant.class);
        Price price = createPrice("$65.00");
        when(variant.getSku()).thenReturn(sku);
        when(variant.getName()).thenReturn("Sussex Rain Boots");
        when(variant.getDescription()).thenReturn("");
        when(variant.getPriceRange()).thenReturn(price);
        when(variant.getAssets()).thenReturn(Collections.<Asset>emptyList());
        LinkedHashMap<String, Integer> attributes = new LinkedHashMap<String, Integer>();
        attributes.put("size", Integer.valueOf(9));
        when(variant.getVariantAttributes()).thenReturn(attributes);
        return variant;
    }

    private VariantAttribute createVariantAttribute() {
        VariantAttribute attribute = mock(VariantAttribute.class);
        VariantValue value = createVariantValue();
        when(attribute.getId()).thenReturn("size");
        when(attribute.getValues()).thenReturn(Collections.singletonList(value));
        return attribute;
    }

    private VariantValue createVariantValue() {
        VariantValue value = mock(VariantValue.class);
        when(value.getId()).thenReturn(Integer.valueOf(9));
        when(value.getLabel()).thenReturn("9");
        return value;
    }

    private ProductInterface createProductData() {
        ProductInterface productData = mock(ProductInterface.class);
        CategoryInterface category = createCategory("Men");
        ComplexTextValue shortDescription = createTextValue("<p>Current CIF summary</p>");
        ComplexTextValue description = createTextValue("<p>Current CIF description</p>");
        ProductImage image = createImage("https://cdn.example.com/current-cif-image.jpg");

        when(productData.getCategories()).thenReturn(Collections.singletonList(category));
        when(productData.getShortDescription()).thenReturn(shortDescription);
        when(productData.getDescription()).thenReturn(description);
        when(productData.getSmallImage()).thenReturn(image);
        return productData;
    }

    private Resource createProductResource() {
        Resource productResource = mock(Resource.class);
        Resource productImage = mock(Resource.class);
        Resource variantResource = mock(Resource.class);
        Resource variantImage = mock(Resource.class);

        when(productResource.getPath()).thenReturn("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots/jcr:content/root/product");
        when(productResource.getValueMap()).thenReturn(valueMap("productData",
            "/var/commerce/products/we-retail/me/footwear/meotwisus"));
        when(productResource.getChild("image")).thenReturn(productImage);
        when(productResource.getChildren()).thenReturn(Arrays.asList(variantResource));

        when(productImage.getValueMap()).thenReturn(valueMap("fileReference",
            "/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg"));

        when(variantResource.getName()).thenReturn("meotwisus-9");
        when(variantResource.getPath())
            .thenReturn("/content/we-retail/us/en/products/men/footwear/sussex-rain-boots/jcr:content/root/product/meotwisus-9");
        when(variantResource.getValueMap()).thenReturn(valueMap(
            "cq:commerceType", "variant",
            "productData", "/var/commerce/products/we-retail/me/footwear/meotwisus/size-9"));
        when(variantResource.getChild("image")).thenReturn(variantImage);

        when(variantImage.getValueMap()).thenReturn(valueMap("fileReference",
            "/content/dam/we-retail/en/products/apparel/footwear/source/Sussex.jpg"));

        return productResource;
    }

    private ValueMapDecorator valueMap(String key, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key, value);
        return new ValueMapDecorator(values);
    }

    private ValueMapDecorator valueMap(String key1, String value1, String key2, String value2) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key1, value1);
        values.put(key2, value2);
        return new ValueMapDecorator(values);
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

    private ProductImage createImage(String url) {
        ProductImage image = mock(ProductImage.class);
        when(image.getUrl()).thenReturn(url);
        return image;
    }

    private Asset createAsset(String path) {
        Asset asset = mock(Asset.class);
        when(asset.getPath()).thenReturn(path);
        return asset;
    }

    private Price createPrice(String formattedFinalPrice) {
        Price price = mock(Price.class);
        when(price.isEmpty()).thenReturn(false);
        when(price.isRange()).thenReturn(Boolean.FALSE);
        when(price.getFormattedFinalPrice()).thenReturn(formattedFinalPrice);
        return price;
    }
}
