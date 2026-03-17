package we.retail.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;

import we.retail.core.commerce.cif.models.CifModelAdapter;
import we.retail.core.commerce.cif.models.LegacyCommercePageSupport;

@Model(adaptables = SlingHttpServletRequest.class)
public class ProductRecommendationFallbackModel {

    private static final int DEFAULT_MAX = 6;
    private static final int SAME_CATEGORY_SCORE = 100;
    private static final int SAME_SECTION_SCORE = 40;
    private static final int TAG_SCORE = 10;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private ModelFactory modelFactory;

    private List<ProductGridItem> items = Collections.emptyList();

    @PostConstruct
    private void initModel() {
        if (currentPage == null || modelFactory == null || request == null) {
            return;
        }

        Page productsRoot = findProductsRoot(currentPage);
        if (productsRoot == null) {
            return;
        }

        Set<String> currentTags = getTags(currentPage);
        List<Candidate> candidates = new ArrayList<Candidate>();
        collectCandidates(productsRoot, productsRoot, currentTags, candidates);

        Collections.sort(candidates, new Comparator<Candidate>() {
            @Override
            public int compare(Candidate left, Candidate right) {
                int scoreCompare = Integer.valueOf(right.score).compareTo(Integer.valueOf(left.score));
                if (scoreCompare != 0) {
                    return scoreCompare;
                }
                return StringUtils.defaultString(left.page.getPath()).compareTo(StringUtils.defaultString(right.page.getPath()));
            }
        });

        List<ProductGridItem> resolvedItems = new ArrayList<ProductGridItem>();
        int maxItems = DEFAULT_MAX;
        Resource resource = request.getResource();
        if (resource != null) {
            maxItems = Math.max(1, resource.getValueMap().get("max", DEFAULT_MAX));
        }

        for (Candidate candidate : candidates) {
            try {
                Product product = CifModelAdapter.adaptToProduct(modelFactory, request, candidate.page.getContentResource(), candidate.sku);
                if (product == null || !Boolean.TRUE.equals(product.getFound())) {
                    continue;
                }

                ProductGridItem item = ProductGridItem.fromProduct(product, candidate.page, request);
                if (item.exists()) {
                    resolvedItems.add(item);
                }
            } catch (RuntimeException e) {
                // Ignore individual product failures so the fallback list still renders.
            }

            if (resolvedItems.size() >= maxItems) {
                break;
            }
        }

        items = Collections.unmodifiableList(resolvedItems);
    }

    public List<ProductGridItem> getItems() {
        return items;
    }

    private void collectCandidates(Page searchRoot, Page page, Set<String> currentTags, List<Candidate> candidates) {
        Iterator<Page> children = page.listChildren(new PageFilter());
        while (children.hasNext()) {
            Page child = children.next();
            if (!StringUtils.equals(child.getPath(), currentPage.getPath())) {
                String sku = LegacyCommercePageSupport.extractSku(child.getContentResource(), child).orElse(null);
                if (StringUtils.isNotBlank(sku)) {
                    candidates.add(new Candidate(child, sku, scoreCandidate(searchRoot, child, currentTags)));
                }
            }
            collectCandidates(searchRoot, child, currentTags, candidates);
        }
    }

    private int scoreCandidate(Page searchRoot, Page candidate, Set<String> currentTags) {
        int score = 0;

        Page currentCategory = currentPage.getParent();
        Page candidateCategory = candidate.getParent();
        if (currentCategory != null && candidateCategory != null && StringUtils.equals(currentCategory.getPath(), candidateCategory.getPath())) {
            score += SAME_CATEGORY_SCORE;
        }

        if (StringUtils.equals(sectionKey(searchRoot, currentPage), sectionKey(searchRoot, candidate))) {
            score += SAME_SECTION_SCORE;
        }

        Set<String> candidateTags = getTags(candidate);
        for (String tag : candidateTags) {
            if (currentTags.contains(tag)) {
                score += TAG_SCORE;
            }
        }

        return score;
    }

    private String sectionKey(Page searchRoot, Page page) {
        if (searchRoot == null || page == null) {
            return StringUtils.EMPTY;
        }

        String relativePath = StringUtils.removeStart(page.getPath(), searchRoot.getPath() + "/");
        return StringUtils.substringBefore(relativePath, "/");
    }

    private Set<String> getTags(Page page) {
        String[] pageTags = page != null ? page.getProperties().get("cq:tags", String[].class) : null;
        Set<String> tags = new LinkedHashSet<String>();
        if (pageTags == null) {
            return tags;
        }

        for (String tag : pageTags) {
            if (StringUtils.isNotBlank(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private Page findProductsRoot(Page page) {
        Page cursor = page;
        while (cursor != null) {
            if ("products".equals(cursor.getName())) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static final class Candidate {

        private final Page page;
        private final String sku;
        private final int score;

        private Candidate(Page page, String sku, int score) {
            this.page = page;
            this.sku = sku;
            this.score = score;
        }
    }
}
