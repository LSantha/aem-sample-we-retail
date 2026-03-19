/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
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

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class HeroImage {

    private static final String PN_FULL_WIDTH = "useFullWidth";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private UrlProvider urlProvider;

    @ValueMapValue
    private String buttonLabel;

    @ValueMapValue
    private String buttonLinkType;

    @ValueMapValue
    private String buttonLinkTo;

    @ValueMapValue
    private String buttonProductSku;

    @ValueMapValue
    private String buttonCategoryId;

    @ValueMapValue
    private String buttonCategoryIdType;

    @ValueMapValue
    private String buttonExternalLink;

    private String classList;
    private Image image;
    private String buttonLink;

    @PostConstruct
    private void initModel() {
        classList = getClassList();
        image = getImage();
        buttonLink = CommerceLinkSupport.resolveLink(request, currentPage, urlProvider, buttonLinkType, buttonLinkTo,
            buttonExternalLink, buttonProductSku, buttonCategoryId, buttonCategoryIdType);
    }

    public String getClassList() {
        if (classList != null) {
            return classList;
        }
        classList = "we-HeroImage";
        if ("true".equals(properties.get(PN_FULL_WIDTH, ""))) {
            classList += " width-full";
        }
        return classList;
    }

    public Image getImage() {
        if (image != null) {
            return image;
        }
        com.adobe.cq.wcm.core.components.models.Image image = request.adaptTo(com.adobe.cq.wcm.core.components.models.Image.class);
        if(image != null) {
            this.image = new Image(image.getSrc());
        }
        return this.image;
    }

    public String getButtonLink() {
        return CommerceLinkSupport.defaultLink(buttonLink);
    }

    public boolean isButtonCallToAction() {
        return StringUtils.isNotBlank(buttonLabel) && CommerceLinkSupport.hasLink(getButtonLink());
    }

    public class Image {
        private String src;

        public Image(String src) {
            this.src = src;
        }

        public String getSrc() {
            return src;
        }
    }

}
