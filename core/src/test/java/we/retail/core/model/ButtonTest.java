/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package we.retail.core.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.day.cq.wcm.api.Page;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ButtonTest {

    @Test
    public void testGetLinkTo() throws Exception {
        Button button = new Button();

        setField(button, "linkTo", "/content/we-retail/us/en/men");
        invokeInit(button);

        Assert.assertEquals("/content/we-retail/us/en/men", button.getLinkTo());
    }

    @Test
    public void testGetCssClass() throws Exception {
        Button button = new Button();

        setField(button, "cssClass", "myClass");
        invokeInit(button);

        Assert.assertEquals("myClass", button.getCssClass());
    }

    @Test
    public void testResolvesPageLink() throws Exception {
        Button button = new Button();

        setField(button, "linkTo", "/content/we-retail/us/en/men");
        invokeInit(button);

        Assert.assertEquals("/content/we-retail/us/en/men.html", button.getLink());
    }

    @Test
    public void testResolvesCategoryLinkFromUrlPathSelection() throws Exception {
        Button button = new Button();
        org.apache.sling.api.SlingHttpServletRequest request = mock(org.apache.sling.api.SlingHttpServletRequest.class);
        Page currentPage = mock(Page.class);
        UrlProvider urlProvider = mock(UrlProvider.class);

        when(urlProvider.formatCategoryUrl(eq(request), eq(currentPage), any()))
            .thenReturn("/content/we-retail/us/en/products/category-page.html/eq/hiking.html");

        setField(button, "request", request);
        setField(button, "currentPage", currentPage);
        setField(button, "urlProvider", urlProvider);
        setField(button, "linkType", "category");
        setField(button, "categoryId", "eq/hiking");
        setField(button, "categoryIdType", "urlPath");
        invokeInit(button);

        Assert.assertEquals("/content/we-retail/us/en/products/category-page.html/eq/hiking.html", button.getLink());
    }

    @Test
    public void testResolvesProductLink() throws Exception {
        Button button = new Button();
        org.apache.sling.api.SlingHttpServletRequest request = mock(org.apache.sling.api.SlingHttpServletRequest.class);
        Page currentPage = mock(Page.class);
        UrlProvider urlProvider = mock(UrlProvider.class);

        when(urlProvider.toProductUrl(request, currentPage, "meskwielt"))
            .thenReturn("/content/we-retail/us/en/products/product-page.html/me/coats/meskwielt.html");

        setField(button, "request", request);
        setField(button, "currentPage", currentPage);
        setField(button, "urlProvider", urlProvider);
        setField(button, "linkType", "product");
        setField(button, "productSku", "meskwielt");
        invokeInit(button);

        Assert.assertEquals("/content/we-retail/us/en/products/product-page.html/me/coats/meskwielt.html", button.getLink());
    }

    private void invokeInit(Button button) throws Exception {
        Method initMethod = Button.class.getDeclaredMethod("initModel");
        initMethod.setAccessible(true);
        initMethod.invoke(button);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
