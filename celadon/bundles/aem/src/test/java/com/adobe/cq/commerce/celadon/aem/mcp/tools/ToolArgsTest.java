/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.celadon.aem.mcp.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolArgsTest {
    private static final JsonObject ARGS = JsonParser.parseString("""
            {
              "sku": "candace-dress-sku",
              "blank": "  ",
              "nothing": null,
              "featured": true,
              "limit": 25,
              "skus": ["a", "b"],
              "notAList": "a",
              "values": {
                "name": "Candace",
                "featured": false,
                "units": 3,
                "price": 49.99,
                "sizes": ["S", "M"],
                "nested": {"a": 1},
                "empty": null
              }
            }
            """).getAsJsonObject();

    @Test
    public void readsStrings() {
        assertEquals("candace-dress-sku", ToolArgs.str(ARGS, "sku"));
        assertNull(ToolArgs.str(ARGS, "nothing"));
        assertNull(ToolArgs.str(ARGS, "missing"));
    }

    @Test
    public void requireRejectsMissingNullAndBlankValues() {
        assertEquals("candace-dress-sku", ToolArgs.require(ARGS, "sku"));
        for (String key : List.of("missing", "nothing", "blank")) {
            IllegalArgumentException error =
                    assertThrows(IllegalArgumentException.class, () -> ToolArgs.require(ARGS, key));
            assertEquals("missing required argument: " + key, error.getMessage());
        }
    }

    @Test
    public void readsBooleansAndIntegersWithFallbacks() {
        assertTrue(ToolArgs.bool(ARGS, "featured", false));
        assertTrue(ToolArgs.bool(ARGS, "missing", true));
        assertFalse(ToolArgs.bool(ARGS, "nothing", false));
        assertEquals(25, ToolArgs.integer(ARGS, "limit", 10));
        assertEquals(10, ToolArgs.integer(ARGS, "missing", 10));
        assertEquals(10, ToolArgs.integer(ARGS, "nothing", 10));
    }

    @Test
    public void readsStringListsAndArrays() {
        assertEquals(List.of("a", "b"), ToolArgs.stringList(ARGS, "skus"));
        assertEquals(List.of(), ToolArgs.stringList(ARGS, "notAList"));
        assertEquals(List.of(), ToolArgs.stringList(ARGS, "missing"));
        assertEquals(2, ToolArgs.array(ARGS, "skus").size());
        assertEquals(0, ToolArgs.array(ARGS, "notAList").size());
        assertEquals(0, ToolArgs.array(ARGS, "missing").size());
    }

    @Test
    public void coercesValueMapEntriesToJavaTypes() {
        Map<String, Object> values = ToolArgs.valueMap(ARGS, "values");

        assertEquals("Candace", values.get("name"));
        assertEquals(Boolean.FALSE, values.get("featured"));
        assertEquals(3L, values.get("units"));
        assertEquals(49.99d, values.get("price"));
        assertEquals(List.of("S", "M"), values.get("sizes"));
        assertEquals("{\"a\":1}", values.get("nested"));
        assertEquals("null", values.get("empty"));
        assertEquals(List.of("name", "featured", "units", "price", "sizes", "nested", "empty"),
                List.copyOf(values.keySet()));
    }

    @Test
    public void valueMapIsEmptyForMissingOrNonObjectArguments() {
        assertTrue(ToolArgs.valueMap(ARGS, "missing").isEmpty());
        assertTrue(ToolArgs.valueMap(ARGS, "sku").isEmpty());
    }
}
