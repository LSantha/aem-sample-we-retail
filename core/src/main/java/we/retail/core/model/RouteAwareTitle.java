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
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Title;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import we.retail.core.commerce.cif.models.GenericRouteSupport;
import we.retail.core.commerce.cif.models.LegacyCommercePageSupport;
import we.retail.core.util.WeRetailHelper;

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

    @ScriptVariable
    private PageManager pageManager;

    @Override
    public String getText() {
        String delegateText = delegate != null ? delegate.getText() : StringUtils.EMPTY;
        if (hasMeaningfulAuthoredTitle(delegateText)) {
            return delegateText;
        }

        Page legacyCategoryPage = GenericRouteSupport.resolveLegacyCategoryPage(pageManager, currentPage, request);
        String routeTitle = WeRetailHelper.getTitle(legacyCategoryPage);
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
}
