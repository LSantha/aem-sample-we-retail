# CIF Migration Execution Prompt

Use the following prompt as the implementation brief for performing the We.Retail to CIF migration.

```text
You are working in the local project at:

/Users/levente/devel/aem-sample-we-retail

Your source of truth for the migration is:

/Users/levente/devel/aem-sample-we-retail/cif_migration.md

Read that file first and follow it. The implementation must reflect the decisions captured there and the discussion that led to it.

Primary goal

Migrate this old AEM Commerce We.Retail project to CIF while preserving the current We.Retail site markup, styling, CSS hooks, and clientlibs as much as possible.

Do not treat this as a Venia reskin. CIF should become the commerce runtime and routing/data layer underneath the existing We.Retail presentation layer for supported storefront areas.

Non-negotiable migration rules

1. Preserve current We.Retail presentation wherever practical.
   - Keep existing `weretail` resource types stable for supported surfaces.
   - Preserve current HTL markup, DOM structure, and CSS classes for PDP, PLP/product grid, filters, page templates, and header unless a change is technically necessary.
   - Do not replace supported We.Retail storefront components with raw Venia/CIF markup.

2. Replace the legacy commerce runtime.
   - Remove the classic AEM Commerce provider/session stack from `core`.
   - Remove dependence on classic `CommerceService`, `CommerceSession`, JCR catalog blueprints, `/content/catalogs`, and `/var/commerce`.

3. Supported storefront surfaces:
   - product detail
   - product listing/product grid
   - product filter shell
   - site header/page shell

4. Unsupported features must render empty or inert:
   - cart
   - checkout
   - order history
   - order details
   - mini cart
   - nav cart behavior

5. Product recommendations are not part of this delivery.
   - Leave the current recommendation markup shell in place as much as possible.
   - Remove legacy recommendation backend coupling.
   - Add placeholders and stubs/no-op services so recommendations can be implemented later without another structural migration.

Environment and deployment rules

There are multiple local AEM instances. You must follow these rules exactly:

- Deployment and testing target: `http://localhost:4506`
- Use this AEM instance only for package deployment, verification, and functional testing.
- Sites console for the target test environment: `http://localhost:4506/sites.html/content`

Do not modify these instances:

- `http://localhost:4502`
- `http://localhost:4504`

Special rule for port 4504:

- `http://localhost:4504/content/we-retail/us/en.html` contains an installed old We.Retail site for visual and behavioral reference only.
- You may inspect it in the browser.
- You must never deploy to it, install packages to it, change content on it, or otherwise modify it.

Special rule for port 4502:

- Leave it completely untouched.
- Do not use default auto-install settings if they target 4502.
- If any Maven profile or script defaults to 4502, override it explicitly or avoid it.

Reference projects available locally

Use these local CIF references when needed:

- `/Users/levente/devel/aem-cif-guides-venia`
- `/Users/levente/devel/aem-core-cif-components`

Prefer these local references over generic assumptions.

Existing 4506 CIF catalog/client configuration

The target AEM on `4506` already has a We.Retail GraphQL client configuration available.

Reuse it.

Observed configuration on `4506`:

- OSGi PID: `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl.bcba8907-eec7-485a-812b-2abe580dd2ec`
- GraphQL client identifier: `we-retail`
- GraphQL endpoint URL: `http://localhost:4506/apps/celadon/graphql`
- This is the We.Retail CIF catalog/client setup, based on Celadon

Implementation rule:

- Prefer reusing the existing `we-retail` GraphQL client instead of creating a new catalog/client configuration.
- When adding or updating CIF cloud configuration for We.Retail, use `cq:graphqlClient=we-retail` unless there is a strong technical reason not to.
- Do not replace this with a Venia-specific client or an unrelated endpoint unless the migration work proves the existing We.Retail client is insufficient.

Implementation expectations

Follow the migration spec, but in practical terms you should do the following:

1. Build and config foundation
   - Add CIF Maven dependencies where required.
   - Introduce the necessary CIF package/config dependencies.
   - Add or restructure the config packaging so CIF OSGi configs can be managed cleanly.
   - Reuse the existing `we-retail` GraphQL client on `4506` where possible, and only add or adjust client config if the migration requires it.
   - Add URL provider config, specific page filter config, and related CIF configs required by the spec.
   - Add `/conf/we-retail/settings/cloudconfigs/commerce`.
   - Add `/conf/we-retail/_sling_configs`.
   - Add `sling:configRef` and CIF route page properties where required.
   - Wire We.Retail CIF config to the existing `cq:graphqlClient=we-retail` catalog/client setup unless blocked and root category = '/' for celadon catalogs .

2. Route/page model migration
   - Replace classic catalog blueprint assumptions with CIF route-page behavior.
   - Stop depending on `/content/catalogs/we-retail`.
   - Add CIF category/product/search route pages using We.Retail page/resource types where possible so visual output stays aligned with the current site.

3. Core bundle migration
   - Remove the legacy provider/session runtime.
   - Replace supported storefront Java with CIF-backed adapter models/services that feed the existing We.Retail HTL contracts.
   - Preserve current view-model outputs expected by existing product/product-grid/filter templates.
   - Add feature flags where needed to disable unsupported transactional controls without breaking layout.

4. Unsupported transactional flows
   - Remove or neutralize cart/checkout/order backend code.
   - Make unsupported components render empty or inert.
   - Ensure no legacy cart POST endpoint or old selector-based flow remains accidentally active.

5. Recommendations placeholder layer
   - Preserve the recommendation shell/markup as much as possible.
   - Remove legacy `commerce/components/recommendation` and classic product-relationship dependencies.
   - Add no-op recommendation service/stub interfaces and placeholder rendering behavior for later implementation.

Important implementation style

- Prefer changing Java and config over rewriting stable HTL.
- Keep the current We.Retail HTML and CSS contract intact when possible.
- If a supported component can keep its current markup by introducing a CIF-backed adapter model, do that.
- If an unsupported feature currently renders action controls, either remove the actions or make them inert behind an explicit feature flag.
- Do not leave broken links, dead submits, or UI that appears functional but is not.

Expected file areas of change

- `/Users/levente/devel/aem-sample-we-retail/core`
- `/Users/levente/devel/aem-sample-we-retail/config` and/or a new config package if introduced
- `/Users/levente/devel/aem-sample-we-retail/ui.apps`
- `/Users/levente/devel/aem-sample-we-retail/ui.content`
- `/Users/levente/devel/aem-sample-we-retail/all`
- any related Maven build files needed to support CIF

Verification requirements

Deploy and test only against `http://localhost:4506`.

Use the AEM on `4504` only as a visual reference point. Compare against it if needed, but never modify it.

At minimum, verify these outcomes on `4506`:

1. Product detail pages render with We.Retail styling and CIF-backed data.
2. Category/product list pages render with current We.Retail product-grid styling and CIF-backed data.
3. The current page/header presentation remains visually close to the old We.Retail site.
4. Unsupported cart/checkout/order pages render empty or inert without errors.
5. Recommendation components render safely as placeholders/stubs with no live recommendation integration.
6. No deployment or content change occurs on `4502` or `4504`.

Working method

- Start by reading `/Users/levente/devel/aem-sample-we-retail/cif_migration.md`.
- Inspect the current code before editing.
- Use the local CIF reference projects to ground implementation decisions.
- Make incremental changes and verify them on `4506`.
- Be especially careful with any install/deploy command so it cannot hit `4502` or `4504`.
- If you need to inspect the old installed We.Retail site for reference, use `4504` read-only.

Definition of done

The task is done when:

- the code reflects the migration spec
- deployment/testing has been performed only on `4506`
- `4502` and `4504` remain untouched
- supported storefront areas use CIF under the current We.Retail presentation
- unsupported areas are empty/inert
- recommendations are left as safe placeholders/stubs for a later phase
- the resulting changes are summarized with the key technical decisions, files changed, and verification performed
```
