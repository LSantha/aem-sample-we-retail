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
package we.retail.core.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Title;
import com.day.cq.wcm.api.Page;

import we.retail.core.commerce.cif.models.CommerceSiteStructureSupport;
import we.retail.core.commerce.cif.models.RouteCategorySupport;
import we.retail.core.commerce.cif.models.RoutePathSupport;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Title.class,
    resourceType = "weretail/components/content/title")
public class RouteAwareTitle implements Title {

    @Self
    private SlingHttpServletRequest request;

    @Self
    @Via(type = ResourceSuperType.class)
    private Title delegate;

    @SlingObject
    private Resource resource;

    @ScriptVariable
    private Page currentPage;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private SiteStructure siteStructure;

    @Override
    public String getText() {
        String delegateText = delegate != null ? delegate.getText() : StringUtils.EMPTY;
        if (hasMeaningfulAuthoredTitle(delegateText)) {
            return delegateText;
        }

        String routeTitle = resolveCategoryRouteTitle();
        if (StringUtils.isNotBlank(routeTitle)) {
            return routeTitle;
        }

        return delegateText;
    }

    @Override
    public String getType() {
        return delegate != null ? delegate.getType() : null;
    }

    @Override
    public Link getLink() {
        return delegate != null ? delegate.getLink() : null;
    }

    @Override
    public String getLinkURL() {
        return delegate != null ? delegate.getLinkURL() : null;
    }

    @Override
    public boolean isLinkDisabled() {
        return delegate != null && delegate.isLinkDisabled();
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId() : null;
    }

    @Override
    public String getAppliedCssClasses() {
        return delegate != null ? delegate.getAppliedCssClasses() : null;
    }

    @Override
    public String getExportedType() {
        return resource != null ? resource.getResourceType() : null;
    }

    private String resolveCategoryRouteTitle() {
        if (!CommerceSiteStructureSupport.isCategoryRoutePage(siteStructure, currentPage)) {
            return StringUtils.EMPTY;
        }

        String routePath = RoutePathSupport.extractRoutePath(request);
        if (StringUtils.isBlank(routePath)) {
            return StringUtils.EMPTY;
        }

        CategoryTree category = RouteCategorySupport.fetchCategoryByUrlPath(request, routePath);
        if (category != null && StringUtils.isNotBlank(category.getName())) {
            return category.getName();
        }

        return humanizeRoutePath(routePath);
    }

    private boolean hasMeaningfulAuthoredTitle(String delegateText) {
        if (resource == null) {
            return false;
        }

        String authoredTitle = resource.getValueMap().get("jcr:title", String.class);
        if (StringUtils.isBlank(authoredTitle)) {
            return false;
        }

        return !isPlaceholderTitle(delegateText) && !isPlaceholderTitle(authoredTitle);
    }

    private boolean isPlaceholderTitle(String text) {
        return StringUtils.equals(text, "Category Page") || StringUtils.equals(text, "Product Page");
    }

    private String humanizeRoutePath(String routePath) {
        String segment = StringUtils.substringAfterLast(routePath, "/");
        String normalized = StringUtils.defaultIfBlank(segment, routePath);
        if (StringUtils.equals(normalized, "me")) {
            return "Men";
        }
        if (StringUtils.equals(normalized, "wo")) {
            return "Women";
        }
        if (StringUtils.equals(normalized, "eq")) {
            return "Equipment";
        }

        String[] words = StringUtils.split(StringUtils.replaceChars(normalized, "-_", "  "));
        if (words == null || words.length == 0) {
            return normalized;
        }

        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(StringUtils.capitalize(StringUtils.lowerCase(word)));
        }
        return title.toString();
    }
}
