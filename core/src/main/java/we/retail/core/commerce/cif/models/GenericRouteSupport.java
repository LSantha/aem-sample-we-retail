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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;

public final class GenericRouteSupport {

    private static final String PRODUCTS_ROOT_NAME = "products";
    private static final String ROUTE_ALIAS_EQUIPMENT = "eq";
    private static final String ROUTE_ALIAS_MEN = "me";
    private static final String ROUTE_ALIAS_WOMEN = "wo";

    private GenericRouteSupport() {
    }

    public static String extractRoutePath(SlingHttpServletRequest request) {
        if (request == null) {
            return StringUtils.EMPTY;
        }

        RequestPathInfo pathInfo = request.getRequestPathInfo();
        if (pathInfo == null) {
            return StringUtils.EMPTY;
        }

        String suffix = pathInfo.getSuffix();
        if (StringUtils.isBlank(suffix)) {
            return StringUtils.EMPTY;
        }

        return StringUtils.removeStart(StringUtils.removeEnd(suffix, ".html"), "/");
    }

    public static Page findLegacyProductsRoot(PageManager pageManager, Page currentPage) {
        if (pageManager == null || currentPage == null) {
            return null;
        }

        Page siteRoot = findSiteRoot(currentPage);
        if (siteRoot != null) {
            Page productsRoot = pageManager.getPage(siteRoot.getPath() + "/" + PRODUCTS_ROOT_NAME);
            if (productsRoot != null) {
                return productsRoot;
            }
        }

        Page page = currentPage;
        while (page != null) {
            if (StringUtils.equals(page.getName(), PRODUCTS_ROOT_NAME)) {
                return page;
            }
            page = page.getParent();
        }

        return null;
    }

    public static Page resolveLegacyCategoryPage(PageManager pageManager, Page currentPage, SlingHttpServletRequest request) {
        Page productsRoot = findLegacyProductsRoot(pageManager, currentPage);
        String routePath = extractRoutePath(request);
        if (productsRoot == null || StringUtils.isBlank(routePath)) {
            return null;
        }

        Page categoryPage = pageManager.getPage(productsRoot.getPath() + "/" + routePath);
        if (categoryPage == null) {
            categoryPage = pageManager.getPage(productsRoot.getPath() + "/" + normalizeLegacyRoutePath(routePath));
        }
        if (categoryPage == null || isProductPage(categoryPage)) {
            return null;
        }

        return categoryPage;
    }

    public static Page resolveLegacyProductPage(PageManager pageManager, Page currentPage, SlingHttpServletRequest request) {
        Page productsRoot = findLegacyProductsRoot(pageManager, currentPage);
        String routePath = extractRoutePath(request);
        if (productsRoot == null || StringUtils.isBlank(routePath)) {
            return null;
        }

        Page directMatch = pageManager.getPage(productsRoot.getPath() + "/" + routePath);
        if (!isProductPage(directMatch)) {
            directMatch = pageManager.getPage(productsRoot.getPath() + "/" + normalizeLegacyRoutePath(routePath));
        }
        if (isProductPage(directMatch)) {
            return directMatch;
        }

        List<Page> candidates = new ArrayList<Page>();
        collectProductPages(productsRoot, candidates);
        if (candidates.isEmpty()) {
            return null;
        }

        final String normalizedRoutePath = normalizeLegacyRoutePath(routePath);
        final String productSlug = StringUtils.substringAfterLast(normalizedRoutePath, "/");
        final String contextPath = StringUtils.substringBeforeLast(normalizedRoutePath, "/");
        Collections.sort(candidates, new Comparator<Page>() {
            @Override
            public int compare(Page left, Page right) {
                return Integer.valueOf(scoreProductPage(right, productSlug, contextPath))
                    .compareTo(Integer.valueOf(scoreProductPage(left, productSlug, contextPath)));
            }
        });

        return scoreProductPage(candidates.get(0), productSlug, contextPath) > 0 ? candidates.get(0) : null;
    }

    public static String findConfiguredRoute(Page currentPage, String routePropertyName) {
        Page page = currentPage;
        while (page != null) {
            Page routePage = page;
            if (routePage.getContentResource() != null) {
                String configuredPath = routePage.getContentResource().getValueMap().get(routePropertyName, String.class);
                if (StringUtils.isNotBlank(configuredPath)) {
                    return configuredPath;
                }
            }
            page = page.getParent();
        }
        return StringUtils.EMPTY;
    }

    public static List<Page> collectProductPages(Page categoryPage) {
        List<Page> productPages = new ArrayList<Page>();
        collectProductPages(categoryPage, productPages);
        Collections.sort(productPages, new Comparator<Page>() {
            @Override
            public int compare(Page left, Page right) {
                String leftTitle = StringUtils.defaultIfBlank(left.getTitle(), left.getName());
                String rightTitle = StringUtils.defaultIfBlank(right.getTitle(), right.getName());
                int titleCompare = leftTitle.compareToIgnoreCase(rightTitle);
                return titleCompare != 0 ? titleCompare : left.getPath().compareTo(right.getPath());
            }
        });
        return productPages;
    }

    public static String relativeProductPath(Page productsRoot, Page productPage) {
        if (productsRoot == null || productPage == null) {
            return StringUtils.EMPTY;
        }

        return StringUtils.removeStart(productPage.getPath(), productsRoot.getPath() + "/");
    }

    public static String toCatalogRoutePath(String legacyRoutePath) {
        if (StringUtils.isBlank(legacyRoutePath)) {
            return legacyRoutePath;
        }

        String[] segments = StringUtils.split(legacyRoutePath, '/');
        if (segments == null || segments.length == 0) {
            return legacyRoutePath;
        }

        segments[0] = toCatalogTopLevelRouteSegment(segments[0]);
        return StringUtils.join(segments, '/');
    }

    public static boolean isPlaceholderCategoryTitle(String title) {
        return StringUtils.equals(title, "Category name");
    }

    public static boolean isPlaceholderProductName(String name) {
        return StringUtils.equals(name, "Product name");
    }

    public static boolean isPlaceholderProductListItem(String name) {
        return StringUtils.startsWith(name, "Product #");
    }

    public static String normalizeLegacyRoutePath(String routePath) {
        if (StringUtils.isBlank(routePath)) {
            return routePath;
        }

        String[] segments = StringUtils.split(routePath, '/');
        if (segments == null || segments.length == 0) {
            return routePath;
        }

        segments[0] = normalizeTopLevelRouteSegment(segments[0]);
        return StringUtils.join(segments, '/');
    }

    private static Page findSiteRoot(Page currentPage) {
        Page page = currentPage;
        while (page != null) {
            if (page.getProperties().get("navRoot", false)) {
                return page;
            }
            page = page.getParent();
        }
        return null;
    }

    private static void collectProductPages(Page page, List<Page> productPages) {
        if (page == null) {
            return;
        }

        Iterator<Page> children = page.listChildren(new PageFilter());
        while (children.hasNext()) {
            Page child = children.next();
            if (isProductPage(child)) {
                productPages.add(child);
            }
            collectProductPages(child, productPages);
        }
    }

    private static boolean isProductPage(Page page) {
        return page != null && LegacyCommercePageSupport.extractSku(page.getContentResource(), page).isPresent();
    }

    private static String normalizeTopLevelRouteSegment(String segment) {
        if (StringUtils.equals(segment, ROUTE_ALIAS_EQUIPMENT)) {
            return "equipment";
        }
        if (StringUtils.equals(segment, ROUTE_ALIAS_MEN)) {
            return "men";
        }
        if (StringUtils.equals(segment, ROUTE_ALIAS_WOMEN)) {
            return "women";
        }
        return segment;
    }

    private static String toCatalogTopLevelRouteSegment(String segment) {
        if (StringUtils.equals(segment, "equipment")) {
            return ROUTE_ALIAS_EQUIPMENT;
        }
        if (StringUtils.equals(segment, "men")) {
            return ROUTE_ALIAS_MEN;
        }
        if (StringUtils.equals(segment, "women")) {
            return ROUTE_ALIAS_WOMEN;
        }
        return segment;
    }

    private static int scoreProductPage(Page page, String productSlug, String contextPath) {
        if (!isProductPage(page) || !StringUtils.equals(page.getName(), productSlug)) {
            return 0;
        }

        int score = 100;
        String candidateRelativePath = StringUtils.defaultString(page.getPath());
        if (StringUtils.isNotBlank(contextPath) && StringUtils.contains(candidateRelativePath, "/" + contextPath + "/")) {
            score += 50;
        }
        return score;
    }
}
