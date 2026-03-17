package we.retail.core.commerce.cif.recommendations;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;

@Component(service = RecommendationService.class)
public class NoopRecommendationService implements RecommendationService {

    @Override
    public List<RecommendationItem> getRecommendations(SlingHttpServletRequest request, Resource resource, String relationshipType,
        int maxCount) {
        return Collections.emptyList();
    }
}
