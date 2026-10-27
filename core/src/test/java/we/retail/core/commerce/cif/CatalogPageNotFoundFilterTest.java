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

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;

import we.retail.core.model.CatalogPage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CatalogPageNotFoundFilterTest {

    private final SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    private final SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final ModelFactory modelFactory = mock(ModelFactory.class);
    private final Page page = mock(Page.class);
    private final Resource pageContent = mock(Resource.class);
    private final SiteStructure siteStructure = mock(SiteStructure.class);
    private final SlingBindings bindings = new SlingBindings();
    private final CatalogPage catalogPage = mock(CatalogPage.class);
    private CatalogPageNotFoundFilter filter;

    @Before
    public void setUp() throws Exception {
        Resource resource = mock(Resource.class);
        when(request.getResource()).thenReturn(resource);
        when(request.getAttribute(SlingBindings.class.getName())).thenReturn(bindings);
        when(resource.adaptTo(Page.class)).thenReturn(page);
        when(page.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(page.getContentResource()).thenReturn(pageContent);
        when(modelFactory.getModelFromWrappedRequest(request, pageContent, CatalogPage.class)).thenReturn(catalogPage);

        filter = new CatalogPageNotFoundFilter();
        filter.bindModelFactory(modelFactory);
        filter.activate(mock(BundleContext.class));
    }

    @Test
    public void testAnswersAMissingProductWithA404() throws Exception {
        when(siteStructure.isProductPage(page)).thenReturn(true);
        when(catalogPage.isNotFound()).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void testRendersAnExistingCategory() throws Exception {
        when(siteStructure.isCategoryPage(page)).thenReturn(true);
        when(catalogPage.isNotFound()).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendError(anyInt());
    }

    @Test
    public void testLeavesOtherPagesAlone() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(modelFactory, never()).getModelFromWrappedRequest(any(SlingHttpServletRequest.class), any(Resource.class),
            any(Class.class));
    }

    @Test
    public void testProvidesTheScriptHelperOnlyWhileBuildingTheModel() throws Exception {
        when(siteStructure.isProductPage(page)).thenReturn(true);
        final Object[] helperDuringBuild = new Object[1];
        when(modelFactory.getModelFromWrappedRequest(request, pageContent, CatalogPage.class)).thenAnswer(invocation -> {
            helperDuringBuild[0] = bindings.getSling();
            return catalogPage;
        });

        filter.doFilter(request, response, chain);

        assertNotNull(helperDuringBuild[0]);
        assertNull(bindings.getSling());
    }
}
