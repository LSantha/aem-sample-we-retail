package we.retail.core.commerce.cif.recommendations;

import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

public interface RecommendationService {

    List<RecommendationItem> getRecommendations(SlingHttpServletRequest request, Resource resource, String relationshipType, int maxCount);
}
