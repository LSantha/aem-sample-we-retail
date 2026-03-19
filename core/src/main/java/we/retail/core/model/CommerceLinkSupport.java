package we.retail.core.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
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

        if (PRODUCT.equals(normalizedLinkType)
                && StringUtils.isNotBlank(productSku)
                && request != null
                && currentPage != null
                && urlProvider != null) {
            return defaultLink(urlProvider.toProductUrl(request, currentPage, productSku));
        }

        if (CATEGORY.equals(normalizedLinkType)
                && StringUtils.isNotBlank(categoryId)
                && request != null
                && currentPage != null
                && urlProvider != null) {
            CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
            if (URL_PATH.equals(categoryIdType)) {
                params.setUrlPath(categoryId);
            } else {
                params.setUid(categoryId);
            }
            return defaultLink(urlProvider.formatCategoryUrl(request, currentPage, params));
        }

        return toPageUrl(linkTo);
    }

    static String defaultLink(String link) {
        return StringUtils.defaultIfBlank(link, DEFAULT_LINK);
    }

    static boolean hasLink(String link) {
        return StringUtils.isNotBlank(link) && !DEFAULT_LINK.equals(link);
    }

    static boolean isCheckoutFlowLink(String link) {
        if (!hasLink(link)) {
            return false;
        }

        String sanitizedLink = stripQueryAndFragment(link);
        String normalizedLink = StringUtils.removeEnd(sanitizedLink, ".html");
        return StringUtils.contains(normalizedLink, "/user/cart")
                || StringUtils.contains(normalizedLink, "/user/checkout");
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
