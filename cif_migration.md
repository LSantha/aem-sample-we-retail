<!--
  Copyright 2026 Adobe

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# CIF Migration Specification for We.Retail

Date: 2026-03-12

## 1. Purpose

This document defines the migration approach for moving the current We.Retail site from classic AEM Commerce to CIF while preserving the current site markup and styling as much as possible.

This version of the plan is intentionally biased toward:

- keeping the current `weretail` presentation layer for supported storefront areas
- replacing the old commerce runtime in `core` with CIF-backed logic
- introducing the minimum CIF configuration and routing required for product and category rendering
- rendering unsupported transactional features empty
- leaving clean placeholders and stubs for product recommendations to be implemented later

## 2. Scope and Constraints

### In scope

- CIF enablement for product and category rendering
- core bundle changes required to remove classic AEM Commerce runtime
- config and page-model changes required for CIF
- preserving current HTL, CSS classes, and clientlibs wherever practical
- placeholders and stubs for future product recommendations

### Explicitly unsupported in this migration

- cart
- checkout
- order history
- order details
- mini cart
- nav cart behavior

These surfaces must render empty or inert. They must not call the legacy commerce runtime or any CIF transactional APIs.

### Deferred to a later phase

- product recommendations
- wishlist and smartlist behavior
- full search-results redesign
- any transactional Adobe Commerce flows

## 3. Key Design Rule

The migration target is not "replace We.Retail storefront markup with Venia/CIF markup."

The migration target is:

"Keep the existing We.Retail storefront markup and styling for supported areas, and replace the legacy commerce backend under those components with CIF."

That means:

- `weretail` resource types remain the public storefront contract for supported components
- existing HTL templates remain in place unless they are tightly coupled to unsupported transactional actions
- existing CSS classes and DOM structure should be preserved
- CIF is introduced as the data and routing runtime, not as the presentation layer

## 4. Current State Summary

The current implementation is still classic AEM Commerce:

- custom commerce provider and session runtime in `core`
- classic `CommerceService` and `CommerceSession` usage in storefront models
- JCR catalog blueprint and rollout structure under `/content/catalogs`
- legacy `/var/commerce` payment, shipping, cart, and order dependencies
- component inheritance from `commerce/components/*`

Primary evidence in this codebase:

- `core/src/main/java/we/retail/core/WeRetailCommerceServiceFactory.java`
- `core/src/main/java/we/retail/core/WeRetailCommerceServiceImpl.java`
- `core/src/main/java/we/retail/core/WeRetailCommerceSessionImpl.java`
- `core/src/main/java/we/retail/core/model/ProductModel.java`
- `core/src/main/java/we/retail/core/model/ProductGrid.java`
- `core/src/main/java/we/retail/core/model/ProductGridItem.java`
- `core/src/main/java/we/retail/core/components/impl/CartEntryServlet.java`
- `ui.content/src/main/content/jcr_root/content/catalogs/we-retail/en/website-catalog/.content.xml`
- `ui.content/src/main/content/jcr_root/content/we-retail/language-masters/en/products/.content.xml`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/product/.content.xml`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/navcart/.content.xml`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productrecommendation/.content.xml`

## 5. Local CIF Reference Shape

The local CIF projects show the target architecture:

- `aem-cif-guides-venia` uses CIF dependencies in `core` and `ui.apps`
- CIF config lives under `/conf/.../settings/cloudconfigs/commerce`
- OSGi config lives under `/apps/.../osgiconfig`
- site roots define `cq:cifCategoryPage`, `cq:cifProductPage`, and `cq:cifSearchResultsPage`
- CIF route pages are normal AEM pages
- custom Java is thin and extends CIF models rather than replacing the commerce runtime

Important reference files used for this plan:

- `/Users/levente/devel/aem-cif-guides-venia/core/pom.xml`
- `/Users/levente/devel/aem-cif-guides-venia/ui.apps/pom.xml`
- `/Users/levente/devel/aem-cif-guides-venia/ui.config/pom.xml`
- `/Users/levente/devel/aem-cif-guides-venia/ui.content/src/main/content/jcr_root/conf/venia/settings/cloudconfigs/commerce/.content.xml`
- `/Users/levente/devel/aem-cif-guides-venia/ui.content/src/main/content/jcr_root/conf/venia/_sling_configs/.content.xml`
- `/Users/levente/devel/aem-cif-guides-venia/ui.config/src/main/content/jcr_root/apps/venia/osgiconfig/config/com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl.cfg.json`
- `/Users/levente/devel/aem-cif-guides-venia/ui.config/src/main/content/jcr_root/apps/venia/osgiconfig/config/com.adobe.cq.commerce.core.components.internal.servlets.SpecificPageFilterFactory~default.cfg.json`

## 6. Target End State

### Supported storefront surfaces

Supported storefront surfaces continue to use We.Retail markup:

- product detail shell keeps `weretail/components/structure/product`
- product listing shell keeps `weretail/components/content/productgrid`
- product filter shell keeps `weretail/components/structure/productfilter`
- site header and page templates remain We.Retail

The data inside those surfaces comes from CIF-backed services and models.

### Unsupported surfaces

These components render empty and contain no active commerce behavior:

- `weretail/components/content/shoppingcart`
- `weretail/components/content/shoppingcartprices`
- `weretail/components/content/minicart`
- `weretail/components/content/checkoutform`
- `weretail/components/content/orderhistory`
- `weretail/components/content/orderdetails`
- `weretail/components/structure/navcart`

### Recommendations

Recommendations stay present as placeholders, but no real recommendation engine is delivered in this migration:

- the current recommendation component markup shell is preserved
- new Java stubs define the future integration point
- the default implementation returns no recommendations
- no ContextHub or legacy product-relationship dependency remains

## 7. Migration Principles

### Principle 1: Preserve presentation first

For supported surfaces, the current HTL and client-side DOM should remain the first choice.

Examples to preserve:

- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/product/product.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/content/productgrid/productgrid.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/content/productgrid/item/item.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productfilter/productfilter.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/header/include.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productrecommendation/productrecommendation.html`

### Principle 2: Replace the runtime, not just the adapter

The current custom commerce provider stack must be removed, not wrapped.

### Principle 3: Keep resource types stable

Where possible, keep the `weretail` resource types instead of changing pages to Venia resource types. This reduces markup drift and CSS regression risk.

### Principle 4: CIF provides routing and data

CIF should own:

- GraphQL connectivity
- category/product route resolution
- external catalog context
- store configuration

We.Retail should own:

- page layout
- HTML structure
- CSS hooks
- current client-side visual behavior

### Principle 5: Unsupported features must fail safe

No dead add-to-cart, checkout, or order-history flows should remain visible as working features.

If preserving layout requires keeping an action container, the controls inside it must be hidden or disabled by feature flag.

## 8. Core Bundle Plan

## 8.1 Remove the legacy commerce runtime

The following classes should be retired and then deleted once replacement code is in place:

- `core/src/main/java/we/retail/core/WeRetailCommerceServiceFactory.java`
- `core/src/main/java/we/retail/core/WeRetailCommerceServiceImpl.java`
- `core/src/main/java/we/retail/core/WeRetailCommerceSessionImpl.java`
- `core/src/main/java/we/retail/core/WeRetailProductImpl.java`

Reason:

- they depend on classic `CommerceService` and `CommerceSession`
- they assume JCR catalog content and `/var/commerce`
- they implement fake shipping, payment, cart, and order behavior that does not map to CIF

## 8.2 Introduce a new CIF adapter layer in `core`

Add a dedicated package structure for CIF-backed We.Retail presentation models. Suggested package layout:

```text
core/src/main/java/we/retail/core/commerce/cif/
  config/
  models/
  services/
  dto/
  recommendations/
```

Suggested responsibilities:

- `services`: read CIF context, resolve current product/category, build route URLs, expose feature flags
- `models`: adapt CIF data to the fields expected by current HTL
- `dto`: small immutable view objects for current templates
- `recommendations`: placeholder interfaces and no-op implementations for later delivery

## 8.3 Preserve existing component contracts for supported UI

The current We.Retail HTL templates expect model contracts shaped around the old runtime. Instead of rewriting markup to CIF contracts, rewrite the Java to satisfy the current templates.

### Product detail

Retain:

- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/product/product.html`

Rewrite:

- `core/src/main/java/we/retail/core/model/ProductModel.java`
- `core/src/main/java/we/retail/core/model/ProductItem.java`
- `core/src/main/java/we/retail/core/model/handler/CommerceHandler.java`

Target behavior:

- resolve the current CIF product from route context instead of `CommerceService`
- populate the existing HTL fields already used by `product.html`
- keep the current outer DOM, CSS classes, and `we-product-variant` contract
- remove hard dependency on classic product page proxies and `cq.commerce.product`

Notes:

- `CommerceHandler` should stop generating legacy `.add.html`, `.commerce.smartlist.management.html`, and `addcartentry` URLs
- if transactional controls remain out of scope at go-live, `ProductModel` should expose a feature flag that lets HTL keep the action area layout while hiding or disabling the actual controls

### Product list and product filter

Retain:

- `ui.apps/src/main/content/jcr_root/apps/weretail/components/content/productgrid/productgrid.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/content/productgrid/item/item.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productfilter/productfilter.html`

Rewrite:

- `core/src/main/java/we/retail/core/model/ProductGrid.java`
- `core/src/main/java/we/retail/core/model/ProductGridItem.java`
- `core/src/main/java/we/retail/core/model/ProductFilter.java`

Target behavior:

- stop traversing child pages and legacy product pages
- populate the current grid item template from CIF category/search results
- preserve current `<li>`, `.we-ProductsGrid-item`, and filter-related attribute structure
- preserve current color/size/price data attributes where practical so the front end can continue to style and filter the same way

Implementation note:

The current product filter is a local UI contract, not a CIF contract. It should be treated as a We.Retail presentation shell whose values come from CIF layered navigation data or CIF search results.

### Header and navigation commerce hooks

Retain:

- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/header/include.html`

Change:

- `weretail/components/structure/navcart` becomes empty or inert
- do not let header rendering reintroduce legacy cart state

If fully empty output breaks header spacing, render a non-interactive placeholder using the same outer CSS hooks, but no links, no counts, and no behavior.

## 8.4 Remove unsupported transactional backend code

The following Java should be retired and then removed:

- `core/src/main/java/we/retail/core/components/impl/CartEntryServlet.java`
- `core/src/main/java/we/retail/core/model/ShoppingCartModel.java`
- `core/src/main/java/we/retail/core/model/ShoppingCartPricesModel.java`
- `core/src/main/java/we/retail/core/model/OrderHistoryModel.java`
- `core/src/main/java/we/retail/core/model/OrderModel.java`
- `core/src/main/java/we/retail/core/model/VendorOrderModel.java`

Rationale:

- cart, checkout, and order history are out of scope
- keeping these classes encourages accidental runtime coupling to old selectors, old forms, and old `/var/commerce` content

## 8.5 Introduce explicit feature flags

Add a small configuration-backed feature model so We.Retail templates can remain stable while unsupported features are switched off cleanly.

Suggested flags:

- `enableTransactionalFlows = false`
- `enableRecommendations = false`
- `enableWishlist = false`

Recommended use:

- product page action area can keep layout but disable or hide submit controls
- empty cart, checkout, and order components can be controlled without hardcoding logic in HTL
- future recommendation rollout becomes a config toggle, not another structural change

## 8.6 Product recommendations placeholders and stubs

Recommendations are explicitly deferred. The migration must leave a stable placeholder/stub layer.

### Preserve the current markup shell

Retain:

- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productrecommendation/.content.xml`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productrecommendation/productrecommendation.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productrecommendation/defaultcontent.html`
- `ui.apps/src/main/content/jcr_root/apps/weretail/components/structure/productrecommendation/producttemplate.html`

Keep these front-end contracts stable:

- `.recommendations-viewer`
- `.recommendations-content`
- `.products-grid`
- `data-relationship-type`
- `data-max-count`

Implementation note:

- `productrecommendation/.content.xml` must stop inheriting from `commerce/components/recommendation`
- once that supertype is removed, add a local `defaultcontent.html` placeholder so the component does not depend on legacy inherited templates

### Remove the legacy recommendation backend dependency

Retire and remove the classic relationship provider chain:

- `core/src/main/java/we/retail/core/productrelationships/AbstractRelationshipsProvider.java`
- all concrete relationship provider implementations under `core/src/main/java/we/retail/core/productrelationships`

### Add future-proof stubs in `core`

Introduce interfaces and empty default implementations like:

```text
we.retail.core.commerce.cif.recommendations.RecommendationItem
we.retail.core.commerce.cif.recommendations.RecommendationService
we.retail.core.commerce.cif.recommendations.NoopRecommendationService
```

Required behavior for the first cut:

- `RecommendationService#getRecommendations(...)` returns an empty list
- no legacy ContextHub or product-relationship provider is required
- the component remains safe to render with no data

Optional future hook:

- add a JSON exporter or Sling Model exporter later, but do not introduce it in this migration unless needed

### Recommendation component behavior in this migration

Recommended first-cut behavior:

- keep the existing component markup
- keep the current clientlib category wiring only if it safely no-ops with empty data
- leave a visible empty-state or default-state placeholder if desired by business
- do not fetch real recommendation data
- ensure the current recommendation clientlib can safely run even when the result set is empty

## 8.7 Tests for the core bundle

Add or update tests around the new CIF-backed presentation layer:

- product page model builds the current HTL contract from CIF data
- product grid model builds the current grid item contract from CIF category/search data
- unsupported cart and order models are gone or return empty rendering paths
- recommendation service default implementation returns no items
- feature flags correctly disable transactional actions

## 9. Config and CIF Enablement Plan

## 9.1 Build dependency changes

### `core/pom.xml`

Add CIF provided dependencies similar to the local Venia project:

- `com.adobe.commerce.cif:core-cif-components-core`
- `com.adobe.commerce.cif:graphql-client`
- `com.adobe.commerce.cif:magento-graphql`

### `ui.apps/pom.xml`

Add CIF package and provided dependencies:

- `core-cif-components-apps`
- `core-cif-components-config`
- optionally `core-cif-components-extensions-product-recs-content` for later use, even if the feature stays disabled

Remove the classic dependency:

- `day/cq60/product:cq-commerce-content`

### `all/pom.xml`

Remove:

- `day/cq60/product:cq-commerce-content`

Keep packaging aligned with the new config package structure.

## 9.2 Expand the config module or introduce `ui.config`

Current config packaging is minimal and only ships `/apps/weretail/config` and `/apps/weretail/config.publish`.

Recommended target:

- create a dedicated `ui.config` package, matching the local CIF reference project

Acceptable pragmatic alternative:

- extend the existing `config` module to package `/apps/weretail/osgiconfig/config`, `/apps/weretail/osgiconfig/config.author`, and `/apps/weretail/osgiconfig/config.publish`

Either option is valid. The key point is to stop treating CIF OSGi configuration as ad hoc legacy `/apps/weretail/config` content.

## 9.3 Required OSGi configs

Add CIF-oriented OSGi configs modeled on the local reference:

### GraphQL client

Add an environment-specific config equivalent to:

- `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~default.cfg.json`

Required fields:

- `identifier`
- `url`
- `httpMethod`
- timeouts
- cache settings

### CIF URL provider

Add:

- `com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl.cfg.json`

Purpose:

- context-aware product URLs
- context-aware category URLs
- stable route generation

### Specific page filter

Add:

- `com.adobe.cq.commerce.core.components.internal.servlets.SpecificPageFilterFactory~default.cfg.json`

Purpose:

- ensure category/product route pages resolve correctly without leaking the technical route-page names into public behavior

### Optional but recommended

- cache invalidation support for CIF route URLs
- associated-content config if the project needs CIF-associated content behavior later

## 9.4 Add CIF cloud config under `/conf`

Create:

- `/conf/we-retail/settings/cloudconfigs/commerce`

This node should carry the CIF storefront config and should be modeled on the Venia reference page.

Expected properties:

- `cq:graphqlClient`
- `magentoStore`
- `magentoRootCategoryId`
- `enableUIDSupport`
- `enableClientSidePriceLoading`

## 9.5 Add `_sling_configs` for context-aware config

Create:

- `/conf/we-retail/_sling_configs/.content.xml`

Include at minimum:

- `CommerceStorefrontContextConfig enabled=true`

Recommended additions:

- custom We.Retail commerce feature flags described in section 8.5

## 9.6 Site root and store root page changes

Update `/content/we-retail` and the relevant store/language roots.

### On `/content/we-retail`

Keep:

- `cq:conf=/conf/we-retail`

Add:

- `sling:configRef=/conf/we-retail`

### On each active storefront root

Example:

- `/content/we-retail/language-masters/en`
- possibly regional roots such as `/content/we-retail/us/en`

Add:

- `cq:cifCategoryPage`
- `cq:cifProductPage`
- `cq:cifSearchResultsPage`

These should point to dedicated route pages implemented with We.Retail presentation components, not Venia components.

## 9.7 Page-model changes for CIF routing

The old page model based on:

- `cq:catalogBlueprint`
- `cq:commerceProvider`
- `cq:commerceType`
- `cq:CatalogSyncConfig`
- `/content/catalogs/we-retail`

must be removed.

### Visible catalog landing page

Keep the existing `/products` section page if the business wants to preserve the current visible URL and layout.

Recommended behavior:

- convert it from legacy catalog-blueprint behavior into a normal AEM page
- preserve its current We.Retail layout where possible

### Dedicated CIF route pages

Add hidden or non-navigation route pages under the site tree such as:

- `/content/we-retail/.../products/category-page`
- `/content/we-retail/.../products/product-page`
- `/content/we-retail/.../search`

Important:

- these pages should use We.Retail page templates and We.Retail commerce presentation components
- they should not switch to Venia component markup

Example strategy:

- `category-page` renders `weretail/components/content/productgrid` and `weretail/components/structure/productfilter`
- `product-page` renders `weretail/components/structure/product`

This keeps CIF routing compatible while preserving current visual output.

## 9.8 Remove legacy package filters and content

Remove after CIF route pages and config are in place:

- `/content/catalogs/we-retail/...` from `ui.content` package filters
- `/var/commerce/payment-methods/we-retail` from `ui.apps` package filters
- `/var/commerce/shipping-methods/we-retail` from `ui.apps` package filters

These old content roots are part of the classic AEM Commerce model and should not survive the migration.

## 9.9 Remove obsolete service-user mapping

Remove the `orders` mapping from:

- `config/src/content/jcr_root/apps/weretail/config/org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-we-retail.xml`

It only exists because the current stub commerce session writes order state into JCR.

## 10. UI Preservation Rules

These rules are mandatory for implementation unless blocked by a specific technical issue:

### Rule 1

Do not replace We.Retail PDP and PLP HTL with raw CIF core component HTL.

### Rule 2

Do not change CSS class names in current We.Retail product and grid markup unless the existing name is directly tied to removed legacy behavior.

### Rule 3

If a supported component can keep the same markup by changing only Java or configuration, prefer that over HTL changes.

### Rule 4

If a transactional action is unsupported, preserve the layout around it where practical, but do not leave an active dead-end link or submit action.

### Rule 5

If preserving exact markup would force retention of the old commerce runtime, the runtime must still be removed. In that case, preserve the closest possible DOM shape and CSS hooks.

## 11. Suggested Feature Behavior in the First CIF Cut

### Product page

- product visuals, title, description, price, SKU, variants: supported
- add-to-cart: disabled or hidden behind feature flag
- wishlist: disabled or hidden
- reviews block: unchanged only if it does not depend on legacy commerce tracking; otherwise render as-is but do not depend on old commerce runtime

### Product list page

- product tiles and tile styling: supported
- product filter shell: supported
- CIF-backed filtering/faceting: supported only as far as needed to preserve the current filter experience

### Recommendations

- placeholder only
- no real recommendation data

### Cart, checkout, orders

- empty

## 12. Migration Sequence

### Phase 1: CIF foundation

- add CIF Maven dependencies
- add GraphQL client config
- add URL provider and specific page filter configs
- add `/conf/we-retail/settings/cloudconfigs/commerce`
- add `/conf/we-retail/_sling_configs`
- add `sling:configRef` and `cq:cif*Page` properties

Exit criteria:

- CIF configuration resolves correctly on one target site root
- route-page infrastructure is in place

### Phase 2: Preserve We.Retail presentation on top of CIF

- rewrite product page model layer to CIF-backed models
- rewrite product grid and filter model layer to CIF-backed models
- keep existing HTL and clientlibs as stable as possible

Exit criteria:

- one product page renders with current We.Retail look using CIF data
- one category page renders with current We.Retail grid look using CIF data

### Phase 3: Disable unsupported transactional flows

- remove cart servlet and legacy cart/order Java
- empty unsupported cart, checkout, and order components
- ensure no old form actions or selectors remain active

Exit criteria:

- cart, checkout, and order-history pages render empty without server errors
- no legacy cart POST endpoints remain in use

### Phase 4: Recommendation placeholders and cleanup

- introduce no-op recommendation service and stubs
- keep current recommendation markup shell
- remove old relationship provider code
- remove `/content/catalogs` and `/var/commerce` packaging

Exit criteria:

- recommendations render safely with no data
- no classic AEM Commerce runtime remains in the codebase

## 13. Acceptance Criteria

The migration is complete when all of the following are true:

- product detail pages render with We.Retail styling and CIF data
- category pages render with We.Retail grid styling and CIF data
- We.Retail page templates and header remain visually consistent
- classic AEM Commerce provider/session classes are removed
- `/content/catalogs/we-retail` is no longer required
- `/var/commerce` is no longer required
- cart, checkout, and order history surfaces render empty
- recommendation components render safely with placeholder behavior only
- no legacy add-to-cart servlet or legacy smartlist endpoint remains active by accident

## 14. Implementation Notes and Risks

### Highest-risk area

The biggest implementation risk is not CIF configuration itself. It is reproducing the current We.Retail PDP and PLP view contracts while changing the data source from classic commerce pages to CIF route data.

### Practical mitigation

- keep HTL stable
- introduce small DTOs tailored to current templates
- adapt CIF data into those DTOs
- add regression tests around rendered model values

### Search and filter risk

The current filter UX is tightly tied to current grid markup and current item attributes. It should be treated as a presentation contract that CIF data must satisfy, not as a cue to adopt CIF default markup.

### Transactional control risk

If product pages still show active add-to-cart controls while cart is unsupported, the site will feel broken. The default assumption in this spec is to disable or hide those controls until a transactional phase exists.

## 15. Recommended Deliverables

Minimum deliverables for this migration:

- revised `core` bundle with CIF-backed We.Retail presentation models
- CIF OSGi configs and `/conf` storefront config
- route pages and page property changes
- empty rendering for unsupported cart and order surfaces
- recommendation placeholder/stub layer
- removal plan for legacy catalog and `/var/commerce` content

This spec intentionally keeps the presentation layer stable and moves the migration burden into `core` and configuration, which is the right tradeoff for this site.
