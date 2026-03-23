package we.retail.core.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;

final class CommerceLinkSupport {

    static final String LINK_TO = "linkTo";
    static final String PRODUCT = "product";
    static final String CATEGORY = "category";
    static final String EXTERNAL_LINK = "externalLink";
    static final String URL_PATH = AbstractCategoryRetriever.CATEGORY_IDENTIFIER_URL_PATH;
    static final String DEFAULT_LINK = "#";

    private CommerceLinkSupport() {
        // Utility class
    }

    static String resolveLink(SlingHttpServletRequest request, Page currentPage, UrlProvider urlProvider,
            String linkType, String linkTo, String externalLink, String productSku, String categoryId,
            String categoryIdType) {
        String normalizedLinkType = StringUtils.defaultIfBlank(linkType, LINK_TO);

        if (EXTERNAL_LINK.equals(normalizedLinkType)) {
            return defaultLink(externalLink);
        }

        if (PRODUCT.equals(normalizedLinkType)) {
            return defaultLink(resolveProductUrl(request, currentPage, urlProvider, productSku, null, null));
        }

        if (CATEGORY.equals(normalizedLinkType)) {
            return defaultLink(resolveCategoryUrl(request, currentPage, urlProvider, categoryId, categoryIdType));
        }

        return toPageUrl(linkTo);
    }

    static String resolveProductUrl(SlingHttpServletRequest request, Page currentPage, UrlProvider urlProvider,
            String productSku, String productUrlPath, String variantSku) {
        if (request == null || currentPage == null || urlProvider == null) {
            return StringUtils.EMPTY;
        }

        if (StringUtils.isNotBlank(productUrlPath)) {
            ProductUrlFormat.Params params = new ProductUrlFormat.Params();
            params.setUrlPath(productUrlPath);
            params.setSku(productSku);
            params.setVariantSku(variantSku);

            String productUrl = urlProvider.toProductUrl(request, currentPage, params);
            if (StringUtils.isNotBlank(productUrl)) {
                return appendVariantSkuFragment(productUrl, variantSku);
            }
        }

        if (StringUtils.isBlank(productSku)) {
            return StringUtils.EMPTY;
        }

        return appendVariantSkuFragment(urlProvider.toProductUrl(request, currentPage, productSku), variantSku);
    }

    static String resolveCategoryUrl(SlingHttpServletRequest request, Page currentPage, UrlProvider urlProvider,
            String categoryId, String categoryIdType) {
        if (request == null || currentPage == null || urlProvider == null || StringUtils.isBlank(categoryId)) {
            return StringUtils.EMPTY;
        }

        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        if (URL_PATH.equals(categoryIdType)) {
            params.setUrlPath(categoryId);
        } else {
            params.setUid(categoryId);
        }

        return urlProvider.formatCategoryUrl(request, currentPage, params);
    }

    static String defaultLink(String link) {
        return StringUtils.defaultIfBlank(link, DEFAULT_LINK);
    }

    static boolean hasLink(String link) {
        return StringUtils.isNotBlank(link) && !DEFAULT_LINK.equals(link);
    }

    private static String toPageUrl(String linkTo) {
        if (StringUtils.isBlank(linkTo) || DEFAULT_LINK.equals(linkTo)) {
            return DEFAULT_LINK;
        }

        String sanitizedLink = stripQueryAndFragment(linkTo);
        String suffix = StringUtils.removeStart(linkTo, sanitizedLink);

        if (StringUtils.endsWith(sanitizedLink, ".html")) {
            return linkTo;
        }

        return sanitizedLink + ".html" + suffix;
    }

    private static String appendVariantSkuFragment(String link, String variantSku) {
        if (StringUtils.isBlank(link) || StringUtils.isBlank(variantSku)) {
            return link;
        }

        return StringUtils.substringBefore(link, "#") + "#" + variantSku;
    }

    private static String stripQueryAndFragment(String link) {
        int queryIndex = StringUtils.indexOf(link, '?');
        int fragmentIndex = StringUtils.indexOf(link, '#');
        int suffixIndex = -1;

        if (queryIndex >= 0 && fragmentIndex >= 0) {
            suffixIndex = Math.min(queryIndex, fragmentIndex);
        } else if (queryIndex >= 0) {
            suffixIndex = queryIndex;
        } else if (fragmentIndex >= 0) {
            suffixIndex = fragmentIndex;
        }

        return suffixIndex >= 0 ? StringUtils.substring(link, 0, suffixIndex) : link;
    }
}
