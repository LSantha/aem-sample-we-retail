package we.retail.core.commerce.cif.models;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.wcm.core.components.models.Breadcrumb;

public final class CifModelAdapter {

    public static final String PRODUCT_RESOURCE_TYPE = "core/cif/components/commerce/product/v2/product";
    public static final String PRODUCT_LIST_RESOURCE_TYPE = "core/cif/components/commerce/productlist/v2/productlist";
    public static final String BREADCRUMB_RESOURCE_TYPE = "core/cif/components/structure/breadcrumb/v1/breadcrumb";

    private static final String PN_SELECTION = "selection";

    private CifModelAdapter() {
    }

    public static Product adaptToProduct(ModelFactory modelFactory, SlingHttpServletRequest request, Resource resource, String sku) {
        Map<String, Object> overrides = new HashMap<String, Object>();
        if (StringUtils.isNotBlank(sku)) {
            overrides.put(PN_SELECTION, sku);
        }
        Resource wrappedResource = new ResourceTypeOverrideResource(resource, PRODUCT_RESOURCE_TYPE, overrides);
        return modelFactory.getModelFromWrappedRequest(request, wrappedResource, Product.class);
    }

    public static ProductList adaptToProductList(ModelFactory modelFactory, SlingHttpServletRequest request, Resource resource) {
        Resource wrappedResource = new ResourceTypeOverrideResource(resource, PRODUCT_LIST_RESOURCE_TYPE, new HashMap<String, Object>());
        return modelFactory.getModelFromWrappedRequest(request, wrappedResource, ProductList.class);
    }

    public static Breadcrumb adaptToBreadcrumb(ModelFactory modelFactory, SlingHttpServletRequest request, Resource resource) {
        Resource wrappedResource = new ResourceTypeOverrideResource(resource, BREADCRUMB_RESOURCE_TYPE, new HashMap<String, Object>());
        return modelFactory.getModelFromWrappedRequest(request, wrappedResource, Breadcrumb.class);
    }

    private static final class ResourceTypeOverrideResource extends ResourceWrapper {

        private final String resourceType;
        private final ValueMap valueMap;

        ResourceTypeOverrideResource(Resource resource, String resourceType, Map<String, Object> overrides) {
            super(resource);
            this.resourceType = resourceType;
            Map<String, Object> mergedValues = new HashMap<String, Object>();
            mergedValues.putAll(resource.getValueMap());
            mergedValues.putAll(overrides);
            this.valueMap = new ValueMapDecorator(mergedValues);
        }

        @Override
        public String getResourceType() {
            return resourceType;
        }

        @Override
        public ValueMap getValueMap() {
            return valueMap;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == ValueMap.class || type == Map.class) {
                return (AdapterType) valueMap;
            }
            return super.adaptTo(type);
        }
    }
}
