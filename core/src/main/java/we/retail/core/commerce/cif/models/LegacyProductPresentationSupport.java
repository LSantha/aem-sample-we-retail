package we.retail.core.commerce.cif.models;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.day.cq.wcm.api.Page;

public final class LegacyProductPresentationSupport {

    private static final String PN_COMMERCE_TYPE = "cq:commerceType";
    private static final String PN_FILE_REFERENCE = "fileReference";
    private static final String COMMERCE_TYPE_VARIANT = "variant";
    private static final String PN_PRODUCT_DATA = "productData";
    private static final String METADATA_RESOURCE = "/we/retail/core/commerce/cif/models/legacy-product-metadata.json";
    private static final Map<String, Metadata> METADATA_BY_IDENTIFIER = loadMetadata();

    private LegacyProductPresentationSupport() {
    }

    public static Optional<Metadata> metadata(Resource resource, Page currentPage) {
        return LegacyCommercePageSupport.extractCommercePath(resource, currentPage)
            .map(LegacyCommercePageSupport::extractProductIdentifier)
            .map(METADATA_BY_IDENTIFIER::get);
    }

    public static String baseSku(Resource resource, Page currentPage, String fallbackSku) {
        return LegacyCommercePageSupport.extractCommercePath(resource, currentPage)
            .map(LegacyCommercePageSupport::extractProductIdentifier)
            .orElseGet(() -> fallbackSku(fallbackSku));
    }

    public static Resource productResource(Page currentPage) {
        return currentPage != null ? currentPage.getContentResource("root/product") : null;
    }

    public static String gridImageResourcePath(Page currentPage) {
        Resource productResource = productResource(currentPage);
        Resource imageResource = productResource != null ? productResource.getChild("image") : null;
        return imageResource != null ? imageResource.getPath() : StringUtils.EMPTY;
    }

    public static List<Resource> variantResources(Resource productResource) {
        if (productResource == null) {
            return Collections.emptyList();
        }

        List<Resource> resources = new ArrayList<Resource>();
        for (Resource child : productResource.getChildren()) {
            if (StringUtils.equals(child.getValueMap().get(PN_COMMERCE_TYPE, String.class), COMMERCE_TYPE_VARIANT)) {
                resources.add(child);
            }
        }
        return resources;
    }

    public static Resource resolveVariantResource(List<Resource> variantResources, Variant variant, int index) {
        if (variantResources.isEmpty() || variant == null) {
            return null;
        }

        for (Resource variantResource : variantResources) {
            if (matchesVariant(variantResource, variant)) {
                return variantResource;
            }
        }

        return index >= 0 && index < variantResources.size() ? variantResources.get(index) : null;
    }

    public static String resolveVariantSku(Resource variantResource, Variant variant) {
        if (variantResource != null) {
            return StringUtils.replace(variantResource.getName(), "_", ".");
        }
        return fallbackSku(variant != null ? variant.getSku() : null);
    }

    public static String resolveImageReference(Resource resource, Metadata metadata, SlingHttpServletRequest request, String fallbackImage) {
        String imageReference = resolveImageReference(resource, request);
        if (StringUtils.isNotBlank(imageReference)) {
            return imageReference;
        }
        if (metadata != null && StringUtils.isNotBlank(metadata.getImage())) {
            return mapAssetPath(request, metadata.getImage());
        }
        return mapAssetPath(request, fallbackImage);
    }

    public static String resolveImageReference(Resource resource, SlingHttpServletRequest request) {
        if (resource == null) {
            return StringUtils.EMPTY;
        }

        Resource imageResource = resource.getChild("image");
        if (imageResource == null) {
            return StringUtils.EMPTY;
        }

        return mapAssetPath(request, imageResource.getValueMap().get(PN_FILE_REFERENCE, String.class));
    }

    public static String legacyDescription(Optional<Metadata> metadata, String fallbackDescription) {
        return metadata.map(Metadata::getDescription)
            .filter(StringUtils::isNotBlank)
            .orElse(fallbackDescription);
    }

    public static String legacySummary(Optional<Metadata> metadata, String fallbackSummary) {
        return metadata.map(Metadata::getSummary)
            .map(CifProductViewSupport::stripHtml)
            .filter(StringUtils::isNotBlank)
            .orElse(fallbackSummary);
    }

    public static String legacyFeatures(Optional<Metadata> metadata, String fallbackFeatures) {
        return metadata.map(Metadata::getFeatures)
            .filter(StringUtils::isNotBlank)
            .orElse(fallbackFeatures);
    }

    private static boolean matchesVariant(Resource variantResource, Variant variant) {
        String productData = variantResource.getValueMap().get(PN_PRODUCT_DATA, String.class);
        if (StringUtils.isBlank(productData) || variant.getVariantAttributes().isEmpty()) {
            return false;
        }

        String suffix = StringUtils.substringAfterLast(productData, "/");
        for (Map.Entry<String, Integer> attribute : variant.getVariantAttributes().entrySet()) {
            if (attribute.getValue() == null) {
                continue;
            }

            String rawValue = String.valueOf(attribute.getValue());
            if (StringUtils.equals(suffix, attribute.getKey() + "-" + rawValue)
                || StringUtils.endsWith(suffix, "-" + rawValue)
                || StringUtils.endsWith(suffix, "." + rawValue)) {
                return true;
            }
        }

        return false;
    }

    private static String mapAssetPath(SlingHttpServletRequest request, String assetPath) {
        if (StringUtils.isBlank(assetPath)) {
            return StringUtils.EMPTY;
        }

        String mappedPath = request != null && request.getResourceResolver() != null
            ? request.getResourceResolver().map(request, assetPath)
            : assetPath;
        return StringUtils.replace(mappedPath, " ", "%20");
    }

    private static String fallbackSku(String sku) {
        return StringUtils.substringAfterLast(StringUtils.defaultString(sku), "/");
    }

    private static Map<String, Metadata> loadMetadata() {
        InputStream inputStream = LegacyProductPresentationSupport.class.getResourceAsStream(METADATA_RESOURCE);
        if (inputStream == null) {
            return Collections.emptyMap();
        }

        Map<String, Metadata> metadataByIdentifier = new LinkedHashMap<String, Metadata>();
        try (JsonReader reader = Json.createReader(inputStream)) {
            JsonObject metadataObject = reader.readObject();
            for (String identifier : metadataObject.keySet()) {
                JsonObject product = metadataObject.getJsonObject(identifier);
                metadataByIdentifier.put(identifier, new Metadata(
                    product.getString("commercePath", StringUtils.EMPTY),
                    product.getString("description", StringUtils.EMPTY),
                    product.getString("summary", StringUtils.EMPTY),
                    product.getString("features", StringUtils.EMPTY),
                    product.getString("image", StringUtils.EMPTY)));
            }
        }

        return Collections.unmodifiableMap(metadataByIdentifier);
    }

    public static final class Metadata {

        private final String commercePath;
        private final String description;
        private final String summary;
        private final String features;
        private final String image;

        private Metadata(String commercePath, String description, String summary, String features, String image) {
            this.commercePath = commercePath;
            this.description = description;
            this.summary = summary;
            this.features = features;
            this.image = image;
        }

        public String getCommercePath() {
            return commercePath;
        }

        public String getDescription() {
            return description;
        }

        public String getSummary() {
            return summary;
        }

        public String getFeatures() {
            return features;
        }

        public String getImage() {
            return image;
        }
    }
}
