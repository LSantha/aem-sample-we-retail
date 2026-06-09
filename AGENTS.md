# We.Retail CIF Migration — Agent Guide

Guidance for AI agents working in this repository.

## Project Overview

This is the **We.Retail** AEM reference site (`com.adobe.cq.sample:we.retail.reactor`,
version `4.0.1-SNAPSHOT`). It is a multi-module Maven project that builds AEM content
packages and an OSGi bundle.

The codebase is mid-migration from **classic AEM Commerce** to **CIF** (Commerce
Integration Framework). The migration keeps the existing `weretail` presentation layer
(HTL, CSS, clientlibs) and replaces the legacy commerce runtime in `core` with CIF-backed
models and routing. The GraphQL backend is the local **Celadon** service
(`com.adobe.cq.commerce.celadon.aem.CeladonGraphqlServlet`), not a real Magento instance.

See `cif_migration.md` for the full migration specification, scope, principles, and
acceptance criteria. Cart, checkout, and order history are explicitly out of scope and
must render empty; product recommendations are deferred (no-op stubs only).

## Modules

| Module | Artifact | Purpose |
|--------|----------|---------|
| `parent` | `we.retail.parent` | Parent POM: dependency/plugin management, shared versions |
| `core` | `we.retail.core` | OSGi bundle: Sling Models, services, CIF adapter layer |
| `ui.apps` | `we.retail.ui.apps` | `/apps` content: components, templates, clientlibs |
| `ui.content` | `we.retail.ui.content` | Sample content under `/content` |
| `catalog` | `we.retail.catalog` | Catalog DAM assets and content |
| `config` | `we.retail.config` | OSGi configs, incl. CIF/Celadon `osgiconfig` |
| `commons.content.slim` | `we.retail.commons.content.slim` | Shared commons content |
| `all` | `we.retail.all` | Combined package embedding all subpackages |

Key versions (in `parent/pom.xml`): UberJar `6.4.4`, Core WCM Components `2.29.0`,
Core CIF Components `2.18.0`, GraphQL client `1.10.0`, Magento GraphQL `9.1.0-magento242ee`.

### CIF adapter layer (`core`)

New CIF code lives under `we.retail.core.commerce.cif`. Recommendation stubs are in
`we.retail.core.commerce.cif.recommendations` (`RecommendationService`,
`NoopRecommendationService`, `RecommendationItem`). Storefront Sling Models being
migrated to CIF are in `we.retail.core.model` (e.g. `ProductModel`, `ProductGrid`,
`ProductGridItem`, `ProductFilter`, `ProductItem`).

### CIF/Celadon OSGi config (`config`)

`config/src/content/jcr_root/apps/weretail/osgiconfig/config/` contains:
- `com.adobe.cq.commerce.celadon.aem.CeladonGraphqlServlet.cfg.json` (Celadon backend)
- `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~we-retail.cfg.json`
- `com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl.cfg.json`
- `com.adobe.cq.commerce.core.components.internal.servlets.SpecificPageFilterFactory~default.cfg.json`

## Build & Deploy

Run from the repository root with Maven 3 and Java (UberJar APIs must be available).

```bash
mvn clean install                              # build all modules
mvn clean install -PautoInstallSinglePackage   # build + deploy the 'all' package to AEM
mvn clean test                                 # unit tests (core)
```

Common single-module deploy from within a content module:

```bash
mvn clean install -PautoInstallPackage         # deploy that module's package
```

### Target AEM instance

Author instance defaults (from `parent/pom.xml`): `localhost:4502`, credentials
`admin`/`admin`. Package Manager: `http://localhost:4502/crx/packmgr/service.jsp`.

To check what is installed on the running instance:

```bash
curl -s -u admin:admin "http://localhost:4502/crx/packmgr/service.jsp?cmd=ls"
```

## Conventions

### Jira

- CIF-related tickets for this project are in the **SITES** project.
- Commit message format:

  ```
  ticket_id - ticket_title
   * description point 1
   * description point 2
  ```

- The branch name for a Jira ticket is the `ticket_id`.
- For `aem-core-cif-components` tickets, use **CIF Components** as the JIRA component.
- Do **not** put internal JIRA links in public GitHub comments — use only the JIRA ticket ID.

### Code style

- Match the commenting style and density of surrounding code. Do not add verbose or
  explanatory comments beyond existing conventions, and never include change rationale
  as code comments.
- Preserve `weretail` resource types, HTL markup, CSS class names, and DOM structure for
  supported storefront surfaces (PDP, PLP, product filter, header). Change Java and
  configuration in preference to changing HTL (see `cif_migration.md` UI Preservation Rules).
- Use package managers / Maven for dependency changes; do not hand-edit version numbers.

### Git workflow

- Do not push, create PRs, rebase, or merge without explicit permission.
- Default working branch context is `cif3` (current migration branch).

## Testing

- Unit tests run in `core` via `mvn clean test` (JUnit 4, Mockito, `io.wcm` AEM mocks).
- After changing a Sling Model or service, update the corresponding tests and run them.
- Suggested coverage per `cif_migration.md` §8.7: PDP/PLP models build the current HTL
  contract from CIF data; unsupported cart/order models removed or empty;
  `NoopRecommendationService` returns no items; feature flags disable transactional actions.

## Notes

- The upstream GitHub project (Adobe-Marketing-Cloud/aem-sample-we-retail) is archived;
  this fork carries the CIF migration work.
- `cif_migration.md` and `cif_migration_execution_prompt.md` are the source of truth for
  migration scope and sequencing — consult them before structural changes.
