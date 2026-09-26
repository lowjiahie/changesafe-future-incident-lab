# Project Conventions for IBM Bob

> Status: DRAFT · Reviewed at: NOT REVIEWED · Source state: commit 51639d030032a675b680d001b721280d9470bb87

## Authority and conflict handling

- Follow explicit task requirements and safety constraints first; then approved repository conventions below; use generic best practices only where the project is silent.
- If sources conflict or evidence is insufficient, ask before changing application code. Do not treat an observed bug as a convention.
- Insecure or broken code is not a standard. Raise it explicitly rather than reproducing it in generated code.

## Build and runtime

- **Java version:** 17 (configured as `<release>17</release>` in `pom.xml` Maven compiler plugin).
- **Build tool:** Maven 3 via wrapper. Windows command: `.\mvnw.cmd clean test`. Unix: `./mvnw clean test`.
- **Spring Boot version:** 3.2.0 (parent POM). Entry point: `ECommerceApplication.java` with `@SpringBootApplication @EnableAsync`.
- **Test profile:** `test` — activated automatically when tests run via Maven. Uses H2 in-memory database. Connection string: `jdbc:h2:mem:changesafe-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false` (sa / blank password).
- **Production database:** MySQL. Connection controlled by environment variables `DB_USERNAME`, `DB_PASSWORD`, `DB_URL` (optional override). Never commit credentials.
- **H2 demo profile:** `-Dspring-boot.run.profiles=h2-demo` for running the app locally without MySQL.
- Sources: [`pom.xml`](../pom.xml), [`README.md`](../README.md), [`src/test/resources/application-test.properties`](../src/test/resources/application-test.properties).

## Architecture and package boundaries

- **Top-level domain packages** under `com.ttulka.ecommerce`: `sales`, `billing`, `warehouse`, `shipping`, `identity`, `portal`, `common`.
- **Package pattern per aggregate:** `com.ttulka.ecommerce.<domain>.<aggregate>` (domain interfaces) with sub-packages `jdbc/` (implementation), `rest/` (REST controller), `listeners/` (event listeners / anti-corruption layer).
- **Use-case interfaces as contracts:** each bounded context exposes use-case interfaces (e.g., `PlaceOrder`, `FindPayments`, `FetchGoods`). JDBC implementations are package-private and injected by `@Configuration` classes; they are never referenced directly by other domains.
- **Cross-domain communication via domain events only:** domains must not call each other's interfaces directly. They publish events via `EventPublisher.raise(DomainEvent)` and consume them through `@EventListener` listeners in their own `listeners/` sub-package. `EventPublisher` is bridged to Spring's `ApplicationEventPublisher` in `ECommerceApplication`.
- **`common` package:** shared primitives (`Money`, `Quantity`) and event infrastructure (`EventPublisher`, `DomainEvent`). Domains may depend on `common`; they must not depend on each other's domain packages (enforced by `CleanModulesArchTest`).
- **Portal (BFF):** `com.ttulka.ecommerce.portal` orchestrates use-cases from multiple domains for the web layer. `portal.web` controllers depend only on portal use-cases, not on domain implementations directly (enforced by `CleanModulesArchTest`).
- Sources: [`src/main/java/com/ttulka/ecommerce/`](../src/main/java/com/ttulka/ecommerce/), [`CleanModulesArchTest.java`](../src/test/java/com/ttulka/ecommerce/CleanModulesArchTest.java).

## Code and naming style

- **No banned suffixes:** class names must not end with `Impl`, `Service`, `DTO`, `Dto`, `Repository`, `Repo`, `Entity`, or `Aggregate`. This is enforced at build time by `CleanCodeArchTest` using ArchUnit. Violations will fail the test suite.
- **JDBC implementation naming:** use the format `<InterfaceName>Jdbc` (e.g., `PlaceOrderJdbc`, `FindPaymentsJdbc`). Package-private; never public.
- **Lombok usage:** `@RequiredArgsConstructor` for constructor injection; `@NonNull` on all injected and required fields; `@Slf4j` for logging; no `@Data` (immutable value objects use explicit constructors). Lombok is a `provided` dependency.
- **Immutability:** fields are `final` wherever possible. Domain value objects (e.g., `Money`, `Quantity`) are immutable. No public setters on domain classes.
- **Null handling:** `@NonNull` annotation (Lombok) on constructor parameters and fields. Domain invariants enforced via nested checked exception classes (e.g., `Order.OrderHasNoItemsException extends IllegalStateException`).
- **Transactions:** `@Transactional` on JDBC use-case methods that write to the database (e.g., `PlaceOrderJdbc.place()`).
- **`schema.sql`:** database schema is defined in `src/main/resources/schema.sql`. Spring Boot runs it on startup.
- Sources: [`PlaceOrderJdbc.java`](../src/main/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderJdbc.java), [`CleanCodeArchTest.java`](../src/test/java/com/ttulka/ecommerce/CleanCodeArchTest.java).

## Tests and fixtures

- **Test framework:** JUnit 5 (Jupiter) + AssertJ assertions + Mockito mocking. All in `src/test/java/`.
- **JDBC unit tests:** `@JdbcTest` + `@ContextConfiguration(classes = <DomainConfig>.class)` to load only the relevant Spring JDBC slice. Use `@MockBean` for `EventPublisher`. Verify event payloads with Mockito `argThat` + `assertAll`.
- **Integration tests:** `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` + `@ActiveProfiles("test")` + `@Sql("/test-data-<fixture>.sql")` for HTTP-level workflow tests. Use REST Assured with a `CookieFilter` for session-sharing across requests.
- **Architecture tests:** plain JUnit 5 (no Spring context); use `ClassFileImporter` + ArchUnit rules. Located at the top-level test package `com.ttulka.ecommerce`.
- **Test naming convention:** snake_case method names describing observable behavior (e.g., `order_placed_raises_an_event`, `order_is_shipped`).
- **Test data:** SQL fixture files in `src/test/resources/` prefixed `test-data-<domain>-<scenario>.sql`. Use synthetic IDs and values; never use real personal data or credentials.
- **Async test timing:** integration tests that wait for async events use `Thread.sleep(ms)` (e.g., 120 ms in `OrderWorkFlowTest`). This is the current project pattern — not ideal but established.
- Sources: [`PlaceOrderTest.java`](../src/test/java/com/ttulka/ecommerce/sales/order/jdbc/PlaceOrderTest.java), [`OrderWorkFlowTest.java`](../src/test/java/com/ttulka/ecommerce/OrderWorkFlowTest.java).

## UI or other relevant conventions

- **Thymeleaf templates** in `src/main/resources/templates/`. Relevant only if portal/web controllers or views are in scope.
- **Static assets:** `src/main/resources/static/` (`cart.js`, `layout.css`). Minimal; relevant only if frontend is in scope.
- **i18n:** `src/main/resources/messages.properties`. Relevant only if UI text is changed.
- N/A for purely backend or test-only changes.

## Unconfirmed or conflicting observations

- **Retry / idempotency of checkout:** whether placing the same order twice (same `orderId`) is safe (idempotent) or creates duplicate orders/payments is not confirmed by existing tests or explicit documentation. Do not assume either behavior. This is a candidate for the ChangeSafe investigation.
- **Stock reservation timing:** when stock is reserved versus decremented (at order placement vs. goods fetch) is not explicitly documented. `GoodsFetched` event triggers the warehouse stock update listener, but whether `PlaceOrder` itself checks stock availability before publishing `OrderPlaced` is UNCONFIRMED from code inspection alone.
- **`Thread.sleep` in integration tests:** the 120 ms sleep in `OrderWorkFlowTest` is fragile under load. Observed as the established pattern but not a recommended convention for new tests.

## Approval

- Reviewed by: PENDING
- Approved at: PENDING
