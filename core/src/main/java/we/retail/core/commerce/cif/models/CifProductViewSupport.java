package we.retail.core.commerce.cif.models;

import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

public final class CifProductViewSupport {

    private CifProductViewSupport() {
    }

    public static ProductInterface fetchProduct(Product product) {
        return product.getProductRetriever() != null ? product.getProductRetriever().fetchProduct() : null;
    }

    public static String formatPrice(Price priceRange) {
        if (priceRange == null || priceRange.isEmpty()) {
            return StringUtils.EMPTY;
        }
        if (Boolean.TRUE.equals(priceRange.isRange()) && StringUtils.isNotBlank(priceRange.getFormattedFinalPriceMax())) {
            return priceRange.getFormattedFinalPrice() + " - " + priceRange.getFormattedFinalPriceMax();
        }
        if (StringUtils.isNotBlank(priceRange.getFormattedFinalPrice())) {
            return priceRange.getFormattedFinalPrice();
        }
        return StringUtils.defaultString(priceRange.getFormattedRegularPrice());
    }

    public static String firstCategoryName(ProductInterface productData) {
        if (productData == null || productData.getCategories() == null || productData.getCategories().isEmpty()) {
            return StringUtils.EMPTY;
        }

        CategoryInterface selectedCategory = null;
        for (CategoryInterface category : productData.getCategories()) {
            if (category == null || StringUtils.isBlank(category.getName())) {
                continue;
            }

            if (selectedCategory == null) {
                selectedCategory = category;
                continue;
            }

            int currentDepth = StringUtils.countMatches(StringUtils.defaultString(category.getUrlPath()), "/");
            int selectedDepth = StringUtils.countMatches(StringUtils.defaultString(selectedCategory.getUrlPath()), "/");
            if (currentDepth >= selectedDepth) {
                selectedCategory = category;
            }
        }

        return selectedCategory != null ? StringUtils.defaultString(selectedCategory.getName()) : StringUtils.EMPTY;
    }

    public static String descriptionLabel(ProductInterface productData) {
        String category = firstCategoryName(productData);
        if (StringUtils.isNotBlank(category)) {
            return category;
        }
        return shortDescription(productData);
    }

    public static String shortDescription(ProductInterface productData) {
        return stripHtml(productData != null ? productData.getShortDescription() : null);
    }

    public static String longDescription(ProductInterface productData) {
        return stripHtml(productData != null ? productData.getDescription() : null);
    }

    public static String summary(ProductInterface productData) {
        String summary = shortDescription(productData);
        if (StringUtils.isNotBlank(summary)) {
            return summary;
        }
        return longDescription(productData);
    }

    public static String assetPath(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return StringUtils.EMPTY;
        }
        Asset asset = assets.get(0);
        return asset != null ? StringUtils.defaultString(asset.getPath()) : StringUtils.EMPTY;
    }

    public static String imagePath(ProductInterface productData) {
        if (productData == null) {
            return StringUtils.EMPTY;
        }
        ProductImage smallImage = productData.getSmallImage();
        if (smallImage != null && StringUtils.isNotBlank(smallImage.getUrl())) {
            return smallImage.getUrl();
        }
        ProductImage thumbnail = productData.getThumbnail();
        if (thumbnail != null && StringUtils.isNotBlank(thumbnail.getUrl())) {
            return thumbnail.getUrl();
        }
        ProductImage image = productData.getImage();
        if (image != null) {
            return StringUtils.defaultString(image.getUrl());
        }
        return StringUtils.EMPTY;
    }

    public static String resolveImage(SlingHttpServletRequest request, String preferredImage, String fallbackImage) {
        return mapAssetPath(request, StringUtils.defaultIfBlank(preferredImage, fallbackImage));
    }

    public static String mapAssetPath(SlingHttpServletRequest request, String assetPath) {
        if (StringUtils.isBlank(assetPath)) {
            return StringUtils.EMPTY;
        }

        String mappedPath = assetPath;
        if (request != null && request.getResourceResolver() != null && StringUtils.startsWith(assetPath, "/")) {
            mappedPath = request.getResourceResolver().map(request, assetPath);
        }

        return StringUtils.replace(mappedPath, " ", "%20");
    }

    public static String stripHtml(ComplexTextValue textValue) {
        if (textValue == null) {
            return StringUtils.EMPTY;
        }
        return stripHtml(textValue.getHtml());
    }

    public static String stripHtml(String value) {
        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }
        String withoutTags = value.replaceAll("<[^>]+>", " ");
        return StringEscapeUtils.unescapeHtml4(withoutTags).replaceAll("\\s+", " ").trim();
    }
}
