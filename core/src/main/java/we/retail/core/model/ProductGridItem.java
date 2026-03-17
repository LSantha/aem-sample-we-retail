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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.day.cq.wcm.api.Page;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

import we.retail.core.commerce.cif.models.CifProductViewSupport;
import we.retail.core.commerce.cif.models.LegacyProductPresentationSupport;
import we.retail.core.commerce.cif.models.LegacyProductPresentationSupport.Metadata;

public class ProductGridItem {

    private final boolean exists;
    private final String image;
    private final String imageResourcePath;
    private final String name;
    private final String description;
    private final String price;
    private final String path;
    private final ProductFilters filters;

    private ProductGridItem(String image, String imageResourcePath, String name, String description, String price, String path,
        ProductFilters filters) {
        this.exists = StringUtils.isNotBlank(name) && StringUtils.isNotBlank(path);
        this.image = image;
        this.imageResourcePath = imageResourcePath;
        this.name = name;
        this.description = description;
        this.price = price;
        this.path = path;
        this.filters = filters;
    }

    public static ProductGridItem fromProduct(Product product, Page page, SlingHttpServletRequest request) {
        return fromProduct(product, page, request, null);
    }

    public static ProductGridItem fromProduct(Product product, Page page, SlingHttpServletRequest request, String resolvedPath) {
        ProductInterface productData = CifProductViewSupport.fetchProduct(product);
        Optional<Metadata> metadata = LegacyProductPresentationSupport.metadata(LegacyProductPresentationSupport.productResource(page), page);
        String image = LegacyProductPresentationSupport.resolveImageReference(
            LegacyProductPresentationSupport.productResource(page),
            metadata.orElse(null),
            request,
            StringUtils.defaultIfBlank(CifProductViewSupport.assetPath(product.getAssets()), CifProductViewSupport.imagePath(productData)));
        String resolvedPrice = CifProductViewSupport.formatPrice(product.getPriceRange());
        String pagePath = page != null && request != null && request.getResourceResolver() != null
            ? request.getResourceResolver().map(request, page.getPath()) + ".html"
            : StringUtils.EMPTY;

        return new ProductGridItem(
            image,
            LegacyProductPresentationSupport.gridImageResourcePath(page),
            product.getName(),
            LegacyProductPresentationSupport.legacyDescription(metadata, CifProductViewSupport.descriptionLabel(productData)),
            resolvedPrice,
            StringUtils.defaultIfBlank(resolvedPath, pagePath),
            buildFilters(productData, resolvedPrice));
    }

    public static ProductGridItem fromProductListItem(ProductListItem productListItem, Page page, SlingHttpServletRequest request,
        String resolvedUrl) {
        ProductInterface product = productListItem.getProduct();
        String resolvedPrice = CifProductViewSupport.formatPrice(productListItem.getPriceRange());
        Optional<Metadata> metadata = LegacyProductPresentationSupport.metadata(LegacyProductPresentationSupport.productResource(page), page);

        return new ProductGridItem(
            LegacyProductPresentationSupport.resolveImageReference(
                LegacyProductPresentationSupport.productResource(page),
                metadata.orElse(null),
                request,
                productListItem.getImageURL()),
            LegacyProductPresentationSupport.gridImageResourcePath(page),
            productListItem.getTitle(),
            LegacyProductPresentationSupport.legacyDescription(metadata, CifProductViewSupport.descriptionLabel(product)),
            resolvedPrice,
            StringUtils.defaultIfBlank(resolvedUrl, StringUtils.defaultIfBlank(productListItem.getURL(), productListItem.getPath())),
            buildFilters(product, resolvedPrice));
    }

    private static ProductFilters buildFilters(ProductInterface product, String price) {
        ProductFilters productFilters = new ProductFilters();
        if (StringUtils.isNotBlank(price)) {
            productFilters.setPrice(price);
        }

        if (product instanceof ConfigurableProduct) {
            ConfigurableProduct configurableProduct = (ConfigurableProduct) product;
            for (ConfigurableProductOptions option : configurableProduct.getConfigurableOptions()) {
                if ("color".equals(option.getAttributeCode())) {
                    for (ConfigurableProductOptionsValues value : option.getValues()) {
                        productFilters.setColor(StringUtils.lowerCase(value.getLabel()));
                    }
                } else if ("size".equals(option.getAttributeCode())) {
                    for (ConfigurableProductOptionsValues value : option.getValues()) {
                        productFilters.setSize(value.getLabel());
                    }
                }
            }
        }

        return productFilters;
    }

    public boolean exists() {
        return exists;
    }

    public String getImage() {
        return image;
    }

    public String getImageResourcePath() {
        return imageResourcePath;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getPath() {
        return path;
    }

    public ProductFilters getFilters() {
        return filters;
    }

    public static class ProductFilters {

        private final Set<String> colors = new LinkedHashSet<String>();
        private final Set<String> sizes = new LinkedHashSet<String>();
        private final Set<String> prices = new LinkedHashSet<String>();

        public Set<String> getColors() {
            return Collections.unmodifiableSet(colors);
        }

        public void setColor(String color) {
            colors.add(color);
        }

        public Set<String> getSizes() {
            return Collections.unmodifiableSet(sizes);
        }

        public void setSize(String size) {
            sizes.add(size);
        }

        public Set<String> getPrices() {
            return Collections.unmodifiableSet(prices);
        }

        public void setPrice(String price) {
            prices.add(price);
        }

    }
}
