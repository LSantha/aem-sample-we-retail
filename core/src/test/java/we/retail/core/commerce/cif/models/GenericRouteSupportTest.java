package we.retail.core.commerce.cif.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GenericRouteSupportTest {

    @Test
    public void testNormalizeLegacyRoutePathExpandsCeladonAliases() {
        assertEquals("women/coats/sonja-insulated-jacket",
            GenericRouteSupport.normalizeLegacyRoutePath("wo/coats/sonja-insulated-jacket"));
        assertEquals("men", GenericRouteSupport.normalizeLegacyRoutePath("me"));
        assertEquals("equipment/hiking", GenericRouteSupport.normalizeLegacyRoutePath("eq/hiking"));
    }

    @Test
    public void testToCatalogRoutePathCompressesLegacyTopLevelSegments() {
        assertEquals("wo/coats/sonja-insulated-jacket",
            GenericRouteSupport.toCatalogRoutePath("women/coats/sonja-insulated-jacket"));
        assertEquals("me", GenericRouteSupport.toCatalogRoutePath("men"));
        assertEquals("eq/hiking", GenericRouteSupport.toCatalogRoutePath("equipment/hiking"));
    }
}
