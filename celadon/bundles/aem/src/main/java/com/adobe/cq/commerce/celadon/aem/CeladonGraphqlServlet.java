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
package com.adobe.cq.commerce.celadon.aem;

import com.adobe.cq.commerce.celadon.aem.attribute.manifest.ManifestReaderImpl;
import com.adobe.cq.commerce.celadon.aem.catalog.AemCatalogGateway;
import com.adobe.cq.commerce.celadon.core.api.CeladonGraphqlEngine;
import com.adobe.cq.commerce.celadon.core.api.FetcherContext;
import com.adobe.cq.commerce.celadon.core.api.JsonSupport;
import com.adobe.cq.commerce.celadon.core.api.attribute.AttributeManifest;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.osgi.service.metatype.annotations.Designate;

@Component(
        service = {Servlet.class, ResourceChangeListener.class},
        property = {
                "sling.auth.requirements=-/apps/celadon/graphql",
                // Invalidate the cached catalog snapshot whenever catalog content
                // changes (author edits in the Assets console, importer writes,
                // replication) so live edits are reflected without a bundle restart.
                ResourceChangeListener.PATHS + "=/content/dam/celadon",
                ResourceChangeListener.CHANGES + "=ADDED",
                ResourceChangeListener.CHANGES + "=CHANGED",
                ResourceChangeListener.CHANGES + "=REMOVED"
        }
)
@Designate(ocd = CeladonGraphqlServletConfiguration.class)
@HttpWhiteboardServletPattern("/apps/celadon/graphql")
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=org.osgi.service.http)")
public class CeladonGraphqlServlet extends HttpServlet implements ResourceChangeListener {
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private volatile CeladonGraphqlEngine engine;
    private volatile FetcherContext fetcherContext;

    @Activate
    protected void activate(CeladonGraphqlServletConfiguration configuration) {
        this.fetcherContext = new FetcherContext(
                configuration.basePath(),
                configuration.authorizationHeader()
        );
        AttributeManifest manifest = loadManifest(configuration.basePath());
        this.engine = new CeladonGraphqlEngine(fetcherContext, manifest);
    }

    private AttributeManifest loadManifest(String basePath) {
        String normalized = FetcherContext.normalizeBasePath(basePath);
        if (normalized == null || normalized.isBlank()) {
            return AttributeManifest.empty(basePath);
        }
        // basePath looks like "celadon/<catalog>"; ManifestReaderImpl expects just "<catalog>"
        int slash = normalized.lastIndexOf('/');
        String catalog = slash < 0 ? normalized : normalized.substring(slash + 1);
        try (ResourceResolver resolver = resourceResolverFactory.getResourceResolver(resourceResolverAuthInfo())) {
            return new ManifestReaderImpl().read(resolver, catalog)
                    .orElse(AttributeManifest.empty(catalog));
        } catch (LoginException e) {
            return AttributeManifest.empty(catalog);
        }
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        // Any change under /content/dam/celadon (author edit, import, replication)
        // drops the cached snapshot; the next query rebuilds it from current JCR.
        CeladonGraphqlEngine current = engine;
        if (current != null && !changes.isEmpty()) {
            current.invalidateSnapshots();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        execute(request, response);
    }

    private void execute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RequestPayload payload = resolvePayload(request);
        Map<String, Object> variables = new LinkedHashMap<>(payload.variables());
        Map<String, Object> result = engine.execute(
                payload.query(),
                payload.operationName().isBlank() ? null : payload.operationName(),
                variables,
                new AemCatalogGateway(openCatalogResolver(), fetcherContext, true)
        );

        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        response.getWriter().write(JsonSupport.toJson(result));
    }

    private RequestPayload resolvePayload(HttpServletRequest request) throws IOException {
        String parameterQuery = trimToEmpty(request.getParameter("query"));
        if (!parameterQuery.isBlank()) {
            return new RequestPayload(
                    parameterQuery,
                    trimToEmpty(request.getParameter("operationName")),
                    parseVariables(request.getParameter("variables"))
            );
        }

        if ("GET".equalsIgnoreCase(request.getMethod()) || isFormRequest(request)) {
            return RequestPayload.empty();
        }

        String body = readBody(request);
        if (body.isBlank()) {
            return RequestPayload.empty();
        }

        Map<String, Object> json = parseJsonBody(body);
        if (!json.isEmpty()) {
            return new RequestPayload(
                    trimToEmpty(json.get("query")),
                    trimToEmpty(json.get("operationName")),
                    parseVariables(json.get("variables"))
            );
        }

        return new RequestPayload(body, "", Map.of());
    }

    private boolean isFormRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("application/x-www-form-urlencoded")
                || contentType.startsWith("multipart/form-data");
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> parseJsonBody(String body) {
        return JsonSupport.parseMap(body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseVariables(Object rawVariables) {
        if (rawVariables instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (rawVariables instanceof String string && !string.isBlank()) {
            return JsonSupport.parseMap(string);
        }
        return Map.of();
    }

    private String trimToEmpty(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private ResourceResolver openCatalogResolver() {
        try {
            return resourceResolverFactory.getResourceResolver(resourceResolverAuthInfo());
        } catch (LoginException e) {
            throw new IllegalStateException("Failed to open catalog resource resolver for direct AEM backend", e);
        }
    }

    private Map<String, Object> resourceResolverAuthInfo() {
        String authorizationHeader = fetcherContext.authorizationHeader();
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Map.of();
        }
        if (!authorizationHeader.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Map.of();
        }
        String token = authorizationHeader.substring(6).trim();
        String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
        int separator = decoded.indexOf(':');
        if (separator < 0) {
            throw new IllegalStateException("Authorization header must contain user and password");
        }
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.USER, decoded.substring(0, separator));
        authInfo.put(ResourceResolverFactory.PASSWORD, decoded.substring(separator + 1).toCharArray());
        return authInfo;
    }

    private record RequestPayload(String query, String operationName, Map<String, Object> variables) {
        private static RequestPayload empty() {
            return new RequestPayload("", "", Map.of());
        }
    }
}
