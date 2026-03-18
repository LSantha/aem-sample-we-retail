package we.retail.core.commerce.cif.models;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.day.cq.wcm.api.Page;

public final class LegacyCommercePageSupport {

    private static final String PN_PRODUCT_DATA = "productData";
    private static final String PN_PRODUCT_MASTER = "cq:productMaster";
    private static final String PN_SELECTION = "selection";
    private static final String PN_SKU = "sku";

    private LegacyCommercePageSupport() {
    }

    public static Optional<String> extractSku(Resource resource, Page currentPage) {
        Optional<String> explicitSku = extractExplicitSku(resource);
        if (explicitSku.isPresent()) {
            return explicitSku;
        }

        if (currentPage != null) {
            Resource pageContent = currentPage.getContentResource();
            explicitSku = extractExplicitSku(pageContent);
            if (explicitSku.isPresent()) {
                return explicitSku;
            }
        }

        return extractCommercePath(resource, currentPage).map(LegacyCommercePageSupport::extractProductIdentifier);
    }

    public static Optional<String> extractExplicitSku(Resource resource) {
        if (resource == null) {
            return Optional.empty();
        }

        String sku = resource.getValueMap().get(PN_SKU, String.class);
        if (StringUtils.isNotBlank(sku)) {
            return Optional.of(sku);
        }

        Resource productComponent = resource.getChild("root/product");
        if (productComponent != null) {
            sku = productComponent.getValueMap().get(PN_SKU, String.class);
            if (StringUtils.isNotBlank(sku)) {
                return Optional.of(sku);
            }
        }

        return Optional.empty();
    }

    public static Optional<String> extractCommercePath(Resource resource, Page currentPage) {
        if (resource != null) {
            String selection = resource.getValueMap().get(PN_SELECTION, String.class);
            if (StringUtils.isNotBlank(selection)) {
                return Optional.of(selection);
            }

            String productData = resource.getValueMap().get(PN_PRODUCT_DATA, String.class);
            if (StringUtils.isNotBlank(productData)) {
                return Optional.of(productData);
            }
        }

        if (currentPage == null) {
            return Optional.empty();
        }

        Resource pageContent = currentPage.getContentResource();
        if (pageContent == null) {
            return Optional.empty();
        }

        ValueMap pageProperties = pageContent.getValueMap();
        String productMaster = pageProperties.get(PN_PRODUCT_MASTER, String.class);
        if (StringUtils.isNotBlank(productMaster)) {
            return Optional.of(productMaster);
        }

        Resource productComponent = pageContent.getChild("root/product");
        if (productComponent != null) {
            productMaster = productComponent.getValueMap().get(PN_PRODUCT_DATA, String.class);
            if (StringUtils.isNotBlank(productMaster)) {
                return Optional.of(productMaster);
            }
        }

        return Optional.empty();
    }

    public static boolean isReferencedRoutePage(Page currentPage, String routePropertyName) {
        if (currentPage == null) {
            return false;
        }
        Page page = currentPage;
        while (page != null) {
            Resource contentResource = page.getContentResource();
            if (contentResource != null) {
                String configuredPath = contentResource.getValueMap().get(routePropertyName, String.class);
                if (StringUtils.equals(configuredPath, currentPage.getPath())) {
                    return true;
                }
            }
            page = page.getParent();
        }
        return false;
    }

    public static String extractProductIdentifier(String commercePath) {
        String lastSegment = StringUtils.substringAfterLast(commercePath, "/");
        if (StringUtils.startsWith(lastSegment, "size-")) {
            return StringUtils.substringAfterLast(StringUtils.substringBeforeLast(commercePath, "/"), "/");
        }
        return lastSegment;
    }
}
