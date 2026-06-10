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
package com.adobe.cq.commerce.celadon.core.api;

import java.util.Objects;

public final class FetcherContext {
    private final String baseUrl;
    private final String basePath;
    private final String authorizationHeader;

    public FetcherContext(String baseUrl, String basePath, String authorizationHeader) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.basePath = normalizeBasePath(basePath);
        this.authorizationHeader = authorizationHeader == null ? "" : authorizationHeader.trim();
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String basePath() {
        return basePath;
    }

    public String authorizationHeader() {
        return authorizationHeader;
    }

    public static String normalizeBaseUrl(String value) {
        Objects.requireNonNull(value, "baseUrl");
        String normalized = value.trim();
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized;
    }

    public static String normalizeBasePath(String value) {
        Objects.requireNonNull(value, "basePath");
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
