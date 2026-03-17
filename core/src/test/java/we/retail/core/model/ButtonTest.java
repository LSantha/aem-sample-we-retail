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

import org.junit.Assert;
import org.junit.Test;

public class ButtonTest {

    /**
     * Test the button link
     */
    @Test
    public void testGetLinkTo() throws Exception {
        Button button = new Button();

        setField(button, "linkTo", "/content/we-retail/us/en/products/men");

        Assert.assertEquals("/content/we-retail/us/en/products/men", button.getLinkTo());
    }

    /**
     * Test the button CSS class
     */
    @Test
    public void testGetCssClass() throws Exception {
        Button button = new Button();

        setField(button, "cssClass", "myClass");

        Assert.assertEquals("myClass", button.getCssClass());
    }

    @Test
    public void testKeepsNonTransactionalLinksVisible() throws Exception {
        Button button = new Button();

        setField(button, "linkTo", "/content/we-retail/us/en/products/men");

        Assert.assertEquals(true, button.isVisible());
    }

    @Test
    public void testHidesCheckoutLinks() throws Exception {
        Button checkoutButton = new Button();

        setField(checkoutButton, "linkTo", "/content/we-retail/us/en/user/checkout");

        Assert.assertFalse(checkoutButton.isVisible());
    }

    @Test
    public void testHidesCartLinks() throws Exception {
        Button cartButton = new Button();

        setField(cartButton, "linkTo", "/content/we-retail/us/en/user/cart");

        Assert.assertFalse(cartButton.isVisible());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}
