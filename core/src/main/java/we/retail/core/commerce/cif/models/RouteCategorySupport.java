package we.retail.core.commerce.cif.models;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.BreadcrumbQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

public final class RouteCategorySupport {

    private RouteCategorySupport() {
    }

    public static CategoryTree fetchCategoryByUrlPath(SlingHttpServletRequest request, String categoryUrlPath) {
        if (request == null || StringUtils.isBlank(categoryUrlPath)) {
            return null;
        }

        MagentoGraphqlClient client = request.adaptTo(MagentoGraphqlClient.class);
        if (client == null) {
            return null;
        }

        try {
            GraphqlResponse<Query, Error> response = client.execute(buildCategoryQuery(categoryUrlPath));
            if (response == null || response.getErrors() != null && !response.getErrors().isEmpty()) {
                return null;
            }

            Query data = response.getData();
            List<CategoryTree> categories = data != null ? data.getCategoryList() : null;
            if (categories == null || categories.isEmpty()) {
                return null;
            }

            CategoryTree firstCategory = null;
            for (CategoryTree category : categories) {
                if (category == null) {
                    continue;
                }
                if (StringUtils.equals(categoryUrlPath, category.getUrlPath())) {
                    return category;
                }
                if (firstCategory == null) {
                    firstCategory = category;
                }
            }
            return firstCategory;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String buildCategoryQuery(String categoryUrlPath) {
        CategoryFilterInput filter = new CategoryFilterInput().setUrlPath(new FilterEqualTypeInput().setEq(categoryUrlPath));
        QueryQuery.CategoryListArgumentsDefinition searchArgs = args -> args.filters(filter);
        CategoryTreeQueryDefinition queryArgs = category -> category
            .uid()
            .urlPath()
            .name()
            .breadcrumbs(breadcrumbsQuery());
        return Operations.query(query -> query.categoryList(searchArgs, queryArgs)).toString();
    }

    private static BreadcrumbQueryDefinition breadcrumbsQuery() {
        return breadcrumb -> breadcrumb
            .categoryUid()
            .categoryUrlPath()
            .categoryName();
    }
}
