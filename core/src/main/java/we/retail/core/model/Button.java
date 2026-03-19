/*******************************************************************************
 * Copyright 2018 Adobe Systems Incorporated
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package we.retail.core.model;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class Button {

    private static final String CSS_CLASS_DEFAULT = "";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private UrlProvider urlProvider;

    @ValueMapValue
    private String linkTo;

    @ValueMapValue
    private String cssClass;

    @ValueMapValue
    private String linkType;

    @ValueMapValue
    private String productSku;

    @ValueMapValue
    private String categoryId;

    @ValueMapValue
    private String categoryIdType;

    @ValueMapValue
    private String externalLink;

    private String link;

    @PostConstruct
    private void initModel() {
        link = CommerceLinkSupport.resolveLink(request, currentPage, urlProvider, linkType, linkTo, externalLink, productSku,
            categoryId, categoryIdType);
    }

    public String getLinkTo() {
        return linkTo;
    }

    public String getLink() {
        return CommerceLinkSupport.defaultLink(link);
    }

    public String getCssClass() {
        return StringUtils.defaultString(cssClass, CSS_CLASS_DEFAULT);
    }

    public boolean isVisible() {
        return !CommerceLinkSupport.isCheckoutFlowLink(getLink());
    }
}
