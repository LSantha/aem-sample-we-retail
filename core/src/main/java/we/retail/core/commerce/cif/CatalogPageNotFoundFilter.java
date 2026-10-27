/*
 *   Copyright 2026 Adobe Systems Incorporated
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

package we.retail.core.commerce.cif;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.engine.EngineConstants;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.scripting.core.ScriptHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;

import we.retail.core.model.CatalogPage;

/**
 * Answers a product or category page whose product or category the catalog does not have with a 404, like classic
 * We.Retail did for missing product and category pages. CIF's own filter does the same for its product and product
 * list components, but does not find the We.Retail product and product grid components.
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        EngineConstants.SLING_FILTER_RESOURCETYPES + "=" + NameConstants.NT_PAGE,
        EngineConstants.SLING_FILTER_EXTENSIONS + "=html",
        "sling.filter.resource.pattern=/content(/.+)?",
        Constants.SERVICE_RANKING + ":Integer=-6000"
    })
public class CatalogPageNotFoundFilter implements Filter {

    private ModelFactory modelFactory;

    private BundleContext bundleContext;

    @Reference
    void bindModelFactory(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof SlingHttpServletRequest && response instanceof SlingHttpServletResponse
            && isNotFound((SlingHttpServletRequest) request, (SlingHttpServletResponse) response)) {
            ((SlingHttpServletResponse) response).sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}

    private boolean isNotFound(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        Page page = request.getResource().adaptTo(Page.class);
        SiteStructure siteStructure = page != null ? page.adaptTo(SiteStructure.class) : null;
        if (siteStructure == null || page.getContentResource() == null
            || !siteStructure.isProductPage(page) && !siteStructure.isCategoryPage(page)) {
            return false;
        }

        // The bindings of a model built outside a script need the SlingScriptHelper, as in CIF's own filter.
        SlingBindings bindings = getSlingBindings(request);
        boolean addScriptHelper = bindings.getSling() == null;
        if (addScriptHelper) {
            bindings.put(SlingBindings.SLING, new ScriptHelper(bundleContext, null, request, response));
        }
        try {
            CatalogPage catalogPage = modelFactory.getModelFromWrappedRequest(request, page.getContentResource(), CatalogPage.class);
            return catalogPage != null && catalogPage.isNotFound();
        } finally {
            if (addScriptHelper) {
                bindings.remove(SlingBindings.SLING);
            }
        }
    }

    private static SlingBindings getSlingBindings(SlingHttpServletRequest request) {
        Object bindings = request.getAttribute(SlingBindings.class.getName());
        if (bindings instanceof SlingBindings) {
            return (SlingBindings) bindings;
        }
        SlingBindings slingBindings = new SlingBindings();
        request.setAttribute(SlingBindings.class.getName(), slingBindings);
        return slingBindings;
    }
}
