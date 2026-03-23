/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package we.retail.core.commerce.cif.models;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;

public final class CommerceSiteStructureSupport {

    private static final String PN_CIF_CATEGORY_PAGE = "cq:cifCategoryPage";
    private static final String PN_CIF_PRODUCT_PAGE = "cq:cifProductPage";

    private CommerceSiteStructureSupport() {
    }

    public static boolean isCategoryRoutePage(SiteStructure siteStructure, Page currentPage) {
        return isSpecificRoutePage(siteStructure != null ? siteStructure.getCategoryPages() : null, currentPage);
    }

    public static boolean isProductRoutePage(SiteStructure siteStructure, Page currentPage) {
        return isSpecificRoutePage(siteStructure != null ? siteStructure.getProductPages() : null, currentPage);
    }

    public static String findCategoryRoute(SiteStructure siteStructure, Page currentPage) {
        return findConfiguredRoute(siteStructure, currentPage, PN_CIF_CATEGORY_PAGE);
    }

    public static String findProductRoute(SiteStructure siteStructure, Page currentPage) {
        return findConfiguredRoute(siteStructure, currentPage, PN_CIF_PRODUCT_PAGE);
    }

    private static boolean isSpecificRoutePage(List<SiteStructure.Entry> entries, Page currentPage) {
        if (entries == null || currentPage == null) {
            return false;
        }

        String currentPath = currentPage.getPath();
        for (SiteStructure.Entry entry : entries) {
            Page page = entry != null ? entry.getPage() : null;
            if (page != null && StringUtils.equals(page.getPath(), currentPath)) {
                return true;
            }
        }
        return false;
    }

    private static String findConfiguredRoute(SiteStructure siteStructure, Page currentPage, String routePropertyName) {
        if (currentPage == null) {
            return StringUtils.EMPTY;
        }

        Page configHolder = resolveConfigHolder(siteStructure, currentPage);
        String configuredRoute = readRoute(configHolder, routePropertyName);
        if (StringUtils.isNotBlank(configuredRoute)) {
            return configuredRoute;
        }

        if (siteStructure != null) {
            Page landingPage = siteStructure.getLandingPage();
            if (landingPage != null && landingPage != configHolder) {
                configuredRoute = readRoute(landingPage, routePropertyName);
                if (StringUtils.isNotBlank(configuredRoute)) {
                    return configuredRoute;
                }
            }
        }

        if (currentPage != configHolder) {
            return readRoute(currentPage, routePropertyName);
        }

        return StringUtils.EMPTY;
    }

    private static Page resolveConfigHolder(SiteStructure siteStructure, Page currentPage) {
        if (currentPage == null || siteStructure == null) {
            return currentPage;
        }

        if (siteStructure.isCatalogPage(currentPage)) {
            return currentPage;
        }

        SiteStructure.Entry entry = siteStructure.getEntry(currentPage);
        Page catalogPage = entry != null ? entry.getCatalogPage() : null;
        if (catalogPage != null) {
            return catalogPage;
        }

        Page landingPage = siteStructure.getLandingPage();
        return landingPage != null ? landingPage : currentPage;
    }

    private static String readRoute(Page page, String routePropertyName) {
        if (page == null) {
            return StringUtils.EMPTY;
        }

        Resource contentResource = page.getContentResource();
        if (contentResource == null) {
            return StringUtils.EMPTY;
        }

        return StringUtils.defaultString(contentResource.getValueMap().get(routePropertyName, String.class));
    }
}
