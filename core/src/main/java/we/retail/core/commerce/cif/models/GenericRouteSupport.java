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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;

import com.day.cq.wcm.api.Page;

public final class GenericRouteSupport {

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

    public static boolean isReferencedRoutePage(Page currentPage, String routePropertyName) {
        if (currentPage == null) {
            return false;
        }

        String currentPath = currentPage.getPath();
        Page page = currentPage;
        while (page != null) {
            if (page.getContentResource() != null) {
                String configuredPath = page.getContentResource().getValueMap().get(routePropertyName, String.class);
                if (StringUtils.equals(configuredPath, currentPath)) {
                    return true;
                }
            }
            page = page.getParent();
        }
        return false;
    }
}
