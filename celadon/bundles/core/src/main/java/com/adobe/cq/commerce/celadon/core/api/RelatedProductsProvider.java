/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.celadon.core.api;

import java.util.List;

/**
 * Supplies the products returned in a product's {@code related_products} field.
 *
 * <p>Products are identified by their path as listed by the {@link CatalogGateway}: the
 * repository path of the product's content fragment when the gateway provides one, its path
 * relative to the catalog root otherwise.</p>
 */
@FunctionalInterface
public interface RelatedProductsProvider {

    /**
     * Returns the products related to {@code product}, best match first.
     *
     * @param product    the path of the product
     * @param candidates the paths of the catalog's products, in catalog order
     * @return paths taken from {@code candidates}
     */
    List<String> relatedProducts(String product, List<String> candidates);
}
