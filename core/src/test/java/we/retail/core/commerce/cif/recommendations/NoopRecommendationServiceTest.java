package we.retail.core.commerce.cif.recommendations;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class NoopRecommendationServiceTest {

    @Test
    public void testReturnsNoRecommendations() {
        NoopRecommendationService service = new NoopRecommendationService();
        assertTrue(service.getRecommendations(null, null, "related", 6).isEmpty());
    }
}
