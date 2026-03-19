/*
 *   Copyright 2016 Adobe Systems Incorporated
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

import we.retail.core.commerce.cif.models.CifProductViewSupport;

public class ProductItem {

    private final String path;
    private final String pagePath;
    private final String sku;
    private final String title;
    private final String description;
    private final String price;
    private final String summary;
    private final String imageUrl;
    private final String thumbnailUrl;
    private final List<ProductItem> variants;
    private final Map<String, String> variantAxesMap;
    private final Map<String, Collection<String>> variantsAxesValues;

    public ProductItem(Product product, SlingHttpServletRequest request, Page currentPage) {
        this(product, request, resolvePagePath(request, currentPage));
    }

    private ProductItem(Product product, SlingHttpServletRequest request, String resolvedPagePath) {
        ProductInterface productData = CifProductViewSupport.fetchProduct(product);
        String resolvedDescription = CifProductViewSupport.descriptionLabel(productData);
        String resolvedSummary = CifProductViewSupport.summary(productData);
        String resolvedImage = CifProductViewSupport.resolveImage(request,
            CifProductViewSupport.assetPath(product.getAssets()),
            CifProductViewSupport.imagePath(productData));

        path = resolvedPagePath;
        pagePath = resolvedPagePath;
        sku = normalizeSku(product.getSku());
        title = product.getName();
        description = resolvedDescription;
        price = CifProductViewSupport.formatPrice(product.getPriceRange());
        summary = resolvedSummary;
        imageUrl = resolvedImage;
        thumbnailUrl = resolvedImage;

        Map<String, Collection<String>> aggregatedAxesValues = new LinkedHashMap<String, Collection<String>>();
        Map<String, Map<Integer, String>> valueLookup = new LinkedHashMap<String, Map<Integer, String>>();
        for (VariantAttribute attribute : product.getVariantAttributes()) {
            Collection<String> values = new LinkedHashSet<String>();
            Map<Integer, String> idsToLabels = new LinkedHashMap<Integer, String>();
            for (VariantValue value : attribute.getValues()) {
                values.add(value.getLabel());
                idsToLabels.put(value.getId(), value.getLabel());
            }
            aggregatedAxesValues.put(attribute.getId(), values);
            valueLookup.put(attribute.getId(), idsToLabels);
        }

        variantAxesMap = Collections.emptyMap();
        variantsAxesValues = aggregatedAxesValues;
        variants = new ArrayList<ProductItem>();
        if (!product.getVariants().isEmpty()) {
            for (Variant variant : product.getVariants()) {
                variants.add(new ProductItem(variant, resolvedPagePath, resolvedDescription, resolvedSummary,
                    resolvedImage, valueLookup, request));
            }
        } else {
            variants.add(this);
        }
    }

    private ProductItem(Variant variant, String resolvedPagePath, String resolvedDescription, String resolvedSummary,
        String fallbackImage, Map<String, Map<Integer, String>> valueLookup, SlingHttpServletRequest request) {
        String resolvedSku = normalizeSku(variant.getSku());
        path = resolvedPagePath + "#" + resolvedSku;
        pagePath = path;
        sku = resolvedSku;
        title = variant.getName();
        description = resolvedDescription;
        price = CifProductViewSupport.formatPrice(variant.getPriceRange());
        summary = StringUtils.defaultIfBlank(CifProductViewSupport.stripHtml(variant.getDescription()), resolvedSummary);
        imageUrl = CifProductViewSupport.resolveImage(request,
            CifProductViewSupport.assetPath(variant.getAssets()),
            fallbackImage);
        thumbnailUrl = imageUrl;
        variants = Collections.emptyList();
        variantsAxesValues = Collections.emptyMap();
        variantAxesMap = buildVariantAxesMap(variant, valueLookup);
    }

    private static String resolvePagePath(SlingHttpServletRequest request, Page currentPage) {
        String requestUri = request != null ? request.getRequestURI() : null;
        if (StringUtils.isNotBlank(requestUri)) {
            return requestUri;
        }
        return currentPage != null ? currentPage.getPath() + ".html" : StringUtils.EMPTY;
    }

    private static String normalizeSku(String sku) {
        String normalizedSku = StringUtils.defaultString(sku);
        String resolvedSku = StringUtils.substringAfterLast(normalizedSku, "/");
        return StringUtils.isNotBlank(resolvedSku) ? resolvedSku : normalizedSku;
    }

    private static Map<String, String> buildVariantAxesMap(Variant variant, Map<String, Map<Integer, String>> valueLookup) {
        Map<String, String> axisValues = new LinkedHashMap<String, String>();
        for (Map.Entry<String, Integer> attribute : variant.getVariantAttributes().entrySet()) {
            Map<Integer, String> values = valueLookup.get(attribute.getKey());
            if (values != null) {
                String resolvedValue = values.get(attribute.getValue());
                if (StringUtils.isNotBlank(resolvedValue)) {
                    axisValues.put(attribute.getKey(), resolvedValue);
                }
            }
        }
        return axisValues;
    }

    public String getPath() {
        return path;
    }

    public String getPagePath() {
        return pagePath;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getSummary() {
        return summary;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public List<ProductItem> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public String getVariantValueForAxis(String axis) {
        return variantAxesMap.get(axis);
    }

    public String getVariantAxesMapJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        variantAxesMap.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build().toString();
    }

    public Map<String, Collection<String>> getVariantsAxesValues() {
        if (variantsAxesValues.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(variantsAxesValues);
    }
}
