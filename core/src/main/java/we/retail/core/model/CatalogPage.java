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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;

/**
 * The commerce data of a catalog page: the product of a product page or the product list of a category page,
 * taken from the We.Retail product and product grid components on the page or in its template.
 */
@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class CatalogPage {
    private static final String PRODUCT_RESOURCE_TYPE = "weretail/components/structure/product";
    private static final String PRODUCT_GRID_RESOURCE_TYPE = "weretail/components/content/productgrid";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @Self
    private SiteStructure siteStructure;

    @OSGiService
    private ModelFactory modelFactory;

    private Product product;
    private ProductList productList;

    @PostConstruct
    private void initModel() {
        if (modelFactory == null || request == null || siteStructure == null || currentPage == null) {
            return;
        }

        Resource pageContent = currentPage.getContentResource();
        Resource templateStructure = getTemplateStructureResource();

        // Reuse the models of the components on the page, so their GraphQL queries are the same and are only sent once.
        if (siteStructure.isProductPage(currentPage)) {
            ProductModel productModel = adaptComponent(pageContent, templateStructure, PRODUCT_RESOURCE_TYPE, ProductModel.class);
            product = productModel != null ? productModel.getCifProduct() : null;
        } else if (siteStructure.isCategoryPage(currentPage)) {
            ProductGrid productGrid = adaptComponent(pageContent, templateStructure, PRODUCT_GRID_RESOURCE_TYPE, ProductGrid.class);
            productList = productGrid != null ? productGrid.getCifProductList() : null;
        }
    }

    public Product getProduct() {
        return product;
    }

    public ProductList getProductList() {
        return productList;
    }

    /**
     * Whether the catalog does not have the product or category the URL names, whatever the WCM mode. Without one, as
     * on the template pages, CIF's rule applies: not found on publish, while author gets CIF's placeholder data.
     */
    public boolean isNotFound() {
        if (product != null) {
            AbstractProductRetriever productRetriever = product.getProductRetriever();
            return productRetriever != null ? productRetriever.fetchProduct() == null : !Boolean.TRUE.equals(product.getFound());
        }
        if (productList != null) {
            AbstractCategoryRetriever categoryRetriever = productList.getCategoryRetriever();
            return categoryRetriever != null ? categoryRetriever.fetchCategory() == null : productList.getProducts().isEmpty();
        }
        return false;
    }

    private <T> T adaptComponent(Resource contentRoot, Resource templateRoot, String resourceType, Class<T> adapterType) {
        Resource component = findDescendantByResourceType(contentRoot, resourceType);
        if (component == null) {
            component = findDescendantByResourceType(templateRoot, resourceType);
        }
        if (component == null) {
            return null;
        }

        try {
            return modelFactory.getModelFromWrappedRequest(request, component, adapterType);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Resource getTemplateStructureResource() {
        if (resourceResolver == null) {
            return null;
        }

        Template template = currentPage.getTemplate();
        if (template == null) {
            return null;
        }

        return resourceResolver.getResource(template.getPath() + "/structure/jcr:content");
    }

    private Resource findDescendantByResourceType(Resource root, String resourceType) {
        if (root == null) {
            return null;
        }

        if (root.isResourceType(resourceType)) {
            return root;
        }

        for (Resource child : root.getChildren()) {
            Resource match = findDescendantByResourceType(child, resourceType);
            if (match != null) {
                return match;
            }
        }

        return null;
    }
}
