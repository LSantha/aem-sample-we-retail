package we.retail.core.commerce.cif.models;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.day.cq.wcm.api.Page;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyCommercePageSupportTest {

    @Test
    public void testExtractSkuPrefersExplicitSkuOnResource() {
        Resource resource = resource(valueMap("sku", "wr-sonja-jacket"));

        assertEquals("wr-sonja-jacket", LegacyCommercePageSupport.extractSku(resource, null).orElse(null));
    }

    @Test
    public void testExtractSkuFallsBackToPageProductComponentSku() {
        Resource pageContent = resource(valueMap());
        Resource productResource = resource(valueMap("sku", "wr-sonja-jacket"));
        Page page = mock(Page.class);

        when(page.getContentResource()).thenReturn(pageContent);
        when(pageContent.getChild("root/product")).thenReturn(productResource);

        assertEquals("wr-sonja-jacket", LegacyCommercePageSupport.extractSku(null, page).orElse(null));
    }

    @Test
    public void testExtractSkuFallsBackToLegacyCommercePath() {
        Resource resource = resource(valueMap("productData", "/var/commerce/products/we-retail/wo/coats/wootwisot"));

        assertEquals("wootwisot", LegacyCommercePageSupport.extractSku(resource, null).orElse(null));
    }

    private Resource resource(ValueMapDecorator values) {
        Resource resource = mock(Resource.class);
        when(resource.getValueMap()).thenReturn(values);
        return resource;
    }

    private ValueMapDecorator valueMap() {
        return new ValueMapDecorator(new HashMap<String, Object>());
    }

    private ValueMapDecorator valueMap(String key, String value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put(key, value);
        return new ValueMapDecorator(values);
    }
}
