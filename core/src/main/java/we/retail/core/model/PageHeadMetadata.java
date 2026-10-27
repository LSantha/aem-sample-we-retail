/*
 *   Copyright 2026 Adobe Systems Incorporated
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package we.retail.core.model;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.day.cq.wcm.api.Page;

import we.retail.core.commerce.cif.CatalogPlaceholders;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class PageHeadMetadata {

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @Self
    private CatalogPage catalogPage;

    private Product productModel;
    private ProductList productListModel;

    @PostConstruct
    private void initModel() {
        if (catalogPage != null) {
            productModel = catalogPage.getProduct();
            productListModel = catalogPage.getProductList();
        }
    }

    public String getTitle() {
        String commerceTitle = firstMeaningful(
            productModel != null ? productModel.getMetaTitle() : null,
            productModel != null ? productModel.getName() : null,
            productListModel != null ? productListModel.getMetaTitle() : null,
            productListModel != null ? productListModel.getTitle() : null);
        if (StringUtils.isNotBlank(commerceTitle)) {
            return commerceTitle;
        }

        return StringUtils.defaultIfBlank(firstMeaningful(
            currentPage != null ? currentPage.getTitle() : null,
            currentPage != null ? currentPage.getName() : null), StringUtils.EMPTY);
    }

    public String getDescription() {
        return StringUtils.defaultIfBlank(firstMeaningful(
            productModel != null ? productModel.getMetaDescription() : null,
            productListModel != null ? productListModel.getMetaDescription() : null,
            currentPage != null ? currentPage.getDescription() : null), StringUtils.EMPTY);
    }

    public String getCanonicalUrl() {
        String commerceCanonicalUrl = firstMeaningful(
            productModel != null ? productModel.getCanonicalUrl() : null,
            productListModel != null ? productListModel.getCanonicalUrl() : null);
        if (StringUtils.isNotBlank(commerceCanonicalUrl)) {
            return commerceCanonicalUrl;
        }

        if (request != null && StringUtils.isNotBlank(request.getRequestURI())) {
            return request.getRequestURI();
        }

        if (currentPage != null) {
            return currentPage.getPath() + ".html";
        }

        return StringUtils.EMPTY;
    }

    private String firstMeaningful(String... values) {
        if (values == null) {
            return StringUtils.EMPTY;
        }

        for (String value : values) {
            if (isMeaningful(value)) {
                return value;
            }
        }

        return StringUtils.EMPTY;
    }

    private boolean isMeaningful(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        return !CatalogPlaceholders.isPageTitle(value)
            && !CatalogPlaceholders.isCategoryName(value)
            && !CatalogPlaceholders.isProductName(value);
    }
}
