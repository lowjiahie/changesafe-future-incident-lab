# ChangeSafe: Future Incident Lab

**Business-aware, risk-tested development powered by IBM Bob IDE.**

ChangeSafe organizes a developer's change request into business clarification, impact analysis, risk-focused tests, approved implementation, and traceable verification evidence. This repository uses a Java e-commerce sample as the incident lab for the IBM Bob 2.0 Hackathon.

## What ChangeSafe solves

Changing checkout can affect orders, stock, payment and delivery—even when only one file is edited. Developers spend effort recovering business rules, tracing dependencies, choosing tests and preparing review evidence. Ambiguous requirements and missing adverse-condition tests can lead to incorrect implementation and rework.

ChangeSafe connects those activities into one reusable workflow. It asks about material uncertainty, follows reviewed project conventions, investigates code-backed risks, and separates verified results from remaining unknowns. It does not guarantee that all production incidents are prevented.

## Quick start: no MySQL required

Prerequisites: Git and **JDK 17**. The Maven wrapper is included; first use requires network access for dependencies. Run commands from the repository root.

Windows PowerShell:

```powershell
# Replace this example with your actual JDK 17 path if needed.
$env:JAVA_HOME = 'C:\path\to\your\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2-demo"
```

macOS/Linux, with JDK 17 configured:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2-demo
```

Open **http://localhost:8080/**. Spring Boot serves the Thymeleaf frontend; no separate npm/frontend server is needed. Stop with Ctrl+C. The H2 demo database is in memory and resets when the process stops. Use synthetic data only.

## Run with MySQL

Create a dedicated local database:

```sql
CREATE DATABASE IF NOT EXISTS changesafe
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Configure the connection in the same PowerShell terminal:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3310/changesafe?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME = 'your-local-db-user'
# Local development only: input is visible; do not screen-record passwords.
$env:DB_PASSWORD = Read-Host 'Local MySQL password'
.\mvnw.cmd spring-boot:run
```

Use your actual MySQL port; the checked-in default is **3310**, not 3306. The user must have access to this database. Set your own credentials; checked-in fallbacks are demo defaults, not production security configuration. Never point the lab at customer/production data.

| Configuration | Purpose |
| --- | --- |
| [application.properties](src/main/resources/application.properties) | Default mysql profile and SQL initialization |
| [application-mysql.properties](src/main/resources/application-mysql.properties) | DB_URL, DB_USERNAME, DB_PASSWORD and sample data |
| [application-h2-demo.properties](src/main/resources/application-h2-demo.properties) | In-memory application demo |
| [application-test.properties](src/test/resources/application-test.properties) | H2 test environment |

Startup executes the configured schema and seed SQL. CREATE TABLE IF NOT EXISTS does not upgrade existing table columns. Files under [migrations](src/main/resources/migrations) are manually reviewed/applied; they are not an automatic migration runner. Check preconditions and preserve existing data. Do not drop tables simply to upgrade them.

## Run tests

```powershell
.\mvnw.cmd test "-Dspring.profiles.active=test"
# Run one safety-contract test:
.\mvnw.cmd test "-Dspring.profiles.active=test" "-Dtest=OrderAddressRequiredTest"
```

Tests use H2. This is an incident lab, so an unresolved or intentionally seeded defect may produce an expected failing safety test. Confirm failures against the current implementation; do not weaken test expectations to obtain a green build. Historical results apply only to their recorded source state.

## Run ChangeSafe in Bob IDE

1. Open this repository in IBM Bob IDE.
2. Verify that **ChangeSafe** appears in the mode picker and **/changesafe** is discovered.
3. Review [.bob/rules/01-project-conventions.md](.bob/rules/01-project-conventions.md); approve supported conventions before dependent code generation.
4. Select ChangeSafe mode and start with a short request:

```text
/changesafe
Review the checkout flow for potential problems.
Explain findings in simple language.
Do not change application code until I approve.
```

The workflow should proactively clarify material business uncertainty. Confirm business rules and approve the proposed implementation scope. The slash command does not grant extra permissions; authorized app/test edits require an appropriate edit-capable mode. If IDE discovery fails, verify the supported configuration and explicitly ask Bob to activate the changesafe skill as a fallback.

## Architecture

ChangeSafe is a **Bob IDE workflow**, not a separate server or an LLM endpoint embedded in Spring Boot.

```text
Developer request
       |
       v
ChangeSafe coordinator (mode + command + skill)
       |
       +-- Project conventions and confirmed business rules
       +-- Relevant read-only Guardian investigations
       +-- Deterministic test and output-validation scripts
       |
       v
Application source + tests + actual execution results
       |
       v
Change brief + risk report + before/after comparison
       |
       v
Human decision / optional authorized PR
```

The sample app is a modular monolith. Portal coordinates web use cases; Sales manages catalog, carts and orders; Billing, Warehouse and Shipping participate in the order event flow; Identity handles sample users. Common contains shared primitives and events.

```text
.bob/
  custom_modes.yaml
  commands/changesafe.md
  rules/01-project-conventions.md
  skills/changesafe/
    SKILL.md
    guardians/
      impact-explorer/GUARDIAN.md
      test-gap-explorer/GUARDIAN.md
      parallel-layer-explorer/GUARDIAN.md
      database-migration/GUARDIAN.md
changesafe/
  business-rules.md
  templates/
  scripts/
  evidence/<run-id>/
src/main/java/com/ttulka/ecommerce/   # Application modules
src/main/resources/                 # Views, assets, config and SQL
src/test/                           # Tests and synthetic fixtures
bob_sessions/                       # Actual Bob consumption screenshots
CHANGESAFE_BOB_BUILD_BRIEF.md
CHANGESAFE_OUTPUT_CONTRACT.md
```

Guardians investigate impact, test gaps, independent layer questions or database evolution. Their files are reusable instructions, not permanently running agents. Routing is conditional; respect the run-wide invocation limit and budget. Parallel tasks must not share conflicting writes or test state.

## Workflow

```text
1. Receive requirement
2. Understand project
3. Clarify and record business rules
4. Create lightweight PRD / plan
5. Analyze direct and downstream impact
6. Identify risks and select safety contracts
7. Generate/reuse tests and run before-change baseline
8. Obtain approval and implement focused changes
9. Replay tests and relevant regression checks
10. Review acceptance criteria
11. Generate and validate evidence pack
12. Create PR only if explicitly authorized
```

Clarification can occur at any stage. Technical failures trigger bounded investigation; missing evidence remains NOT VERIFIED. Finishing code and tests is not the end: the plan must explicitly include evidence delivery.

A PREVENTED claim requires a reproduced pre-fix failure and a passing replay of the same check after the fix. A new after-only passing test improves coverage but does not establish that red-to-green proof.

## Compare with the before-fix branch

For the deliberately vulnerable demo baseline, refer to **demo/before-fix** (local commit at documentation time: **0c44310**). Its DEMO-BRANCH.md describes the reverted checkout/cart protections. Compare it with **main** to inspect subsequent changes. Main is the comparison branch, not a claim of production readiness.

**Do not merge demo/before-fix into main.** Run it only locally with synthetic H2 data. Stop the app before changing versions and do not reuse a valuable database across schema versions.

Read-only comparison from the current checkout:

```powershell
git diff --stat demo/before-fix main
git diff demo/before-fix main -- src/main/java src/main/resources
git show demo/before-fix:DEMO-BRANCH.md
```

For a local version switch, first inspect git status and preserve any edits. Only if the checkout is clean:

```powershell
git switch demo/before-fix
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2-demo"
# Stop the process with Ctrl+C before switching back.
git switch main
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2-demo"
```

At documentation time, demo/before-fix is present locally but no origin/demo/before-fix remote-tracking branch is listed. A fresh clone may therefore not have it. The maintainer must explicitly publish the branch before asking online reviewers to use it; this README update does not push anything.

After publication, a fresh clone can obtain it with:

```powershell
git fetch origin
git switch --track origin/demo/before-fix
```

The demo baseline is a controlled reconstruction. Do not describe it as the exact historical source state of an earlier run unless source references establish that. Identical checks and settings should be used for a fair before/after comparison; record any necessary differences.

## Outputs, evidence and budget

Each run preserves its own change-brief.md, risk-report.md, comparison.md, execution summaries and original logs under changesafe/evidence/. Use [CHANGESAFE_OUTPUT_CONTRACT.md](CHANGESAFE_OUTPUT_CONTRACT.md) for exact formatting and [changesafe/README.md](changesafe/README.md) for detailed commands.

The validator checks implemented structural rules; it cannot certify business correctness, screenshot authenticity or complete coverage. Actual script output names and report links must match the current contract.

For hackathon submission, each member manually saves every used Bob task's actual consumption summary in bob_sessions/. Task JSON exports are supplementary, not screenshot replacements. Attribute human or Codex continuation work separately from Bob-assisted changes.

Budget controls use a default 5-Bobcoin reserve and an optional requester-defined run cap. Consumption is monitored manually in the IDE. Context reuse, targeted tests and conditional investigations are efficiency strategies, not guaranteed savings or hard technical limits.

## Limitations and troubleshooting

- Passing selected tests does not prove all concurrency, payment, inventory or security risks are fixed.
- A stock preflight alone is not an atomic reservation or proof against overselling.
- Migration scripts require review and disposable-database validation before application.
- Verify java -version / JAVA_HOME; this project compiles for Java 17.
- For MySQL errors, check DB_URL, port, permissions and credentials; use h2-demo for simple demos.
- For port conflicts, launch with `"-Dspring-boot.run.arguments=--server.port=8081"` and use that browser port.
- Inspect target/surefire-reports to distinguish build/environment issues from assertion failures.
- No commit, push, PR, merge or deployment is implied by running ChangeSafe.

## Original project and attribution

The sample application was created by **Tomas Tulka** in [ttulka/ddd-example-ecommerce](https://github.com/ttulka/ddd-example-ecommerce). Its original architecture and application belong to the author; the [MIT License](LICENSE) and attribution are retained. Our adaptations and ChangeSafe experiments are not an official upstream or IBM product.

Use synthetic fixtures only. Do not commit credentials, personal information, client records or confidential assets.

The following original architecture discussion is preserved as historical background, not a complete statement of this adapted application's current behavior.

---

## Original sample application: DDD Example Project in Java: eCommerce

The purpose of this project is to provide a sample implementation of an e-commerce product following **Domain-Driven Design (DDD)** and **Service-Oriented Architecture (SOA)** principles.

Programming language is Java with heavy use of Spring framework.

```sh
# build
./mvnw clean install

# run 
./mvnw spring-boot:run

# open in browser http://localhost:8080
```

## Table of Contents

- [Domains](#domains)
  + [Core Domain](#core-domain)
  + [Supporting Subdomains](#supporting-subdomains)
  + [Event Workflow](#event-workflow)
  + [Services Dependencies](#services-dependencies)
- [Architectural Overview](#architectural-overview)
  + [Screaming Architecture](#screaming-architecture)
  + [Packaging](#packaging)
  + [Assembling](#assembling)
  + [Anatomy of a Service](#anatomy-of-a-service)
- [Conclusion](#conclusion)
  + [Where to Next](#where-to-next)

## Domains

Several [Business Capabilities][vcha] have been identified:

[vcha]: http://bill-poole.blogspot.com/2008/07/value-chain-analysis.html

### Core Domain

- **Sales**
  - put a product for sale
  - categorize a product
  - update a product
  - change a product price
  - validate an order
  - place an order
  
### Supporting Subdomains
  
- **Warehouse**
  - stack goods
  - fetch goods for shipping
  
- **Billing**
  - collect a payment

- **Shipping**
  - dispatch a delivery

Later, we can think about more supporting domains (not implemented in this project):

- **Marketing**
  - discount a product
  - promote a product
  
- **User Reviews**
  - add a product review
  
- **Customer Care**
  - resolve a complain
  - answer a question
  - provide help
  - loyalty program
  
The e-commerce system is a web application using a **Portal** component implementing the [Backends For Frontends (BFF)][bff] pattern.

The idea of [Microfrontends][microf] is implemented in an [alternative branch](https://github.com/ttulka/ddd-example-ecommerce/tree/microfrontend).

[bff]: https://samnewman.io/patterns/architectural/bff/
[microf]: https://martinfowler.com/articles/micro-frontends.html

### Event Workflow

The communication among domains is implemented via events:

![Event Workflow](doc/event-workflow.png)

When the customer places an order the following process starts up (the happy path):

1. Shipping prepares a new delivery.
1. Sales creates a new order and publishes the `OrderPlaced` event.
1. Shipping accepts the delivery.
1. Billing collects payment for the order and publishes the `PaymentCollected` event.
1. Warehouse fetches goods from the stock and publishes the `GoodsFetched` event.
1. Shipping dispatches the delivery and publishes the `DeliveryDispatched` event.
1. Warehouse updates the stock.

There is only the basic "happy path" workflow implemented with a big room for improvement, for example when Shipping doesn't get bot Events within a time period, the delivery process should be cancelled etc.. 

### Services Dependencies

Services cooperate together to work out the Business Capabilities: sale and deliver goods.

The actual dependencies come only from Listeners which fulfill the role of the Anti-Corruption Layer and depend only on Domain Events.

![Event and Listener](doc/event-listener.png)

Events contain no Domain Objects. 

For communication across Services an Event Publisher abstraction is used, located in the package `..ecommerce.common.events`. The interface is an Output Port (in the Hexagonal Architecture) and as a cross-cutting concern is its implementation injected by the Application.  

## Architectural Overview

While no popular architecture ([Onion][onion], [Clean][clean], [Hexagonal][hexagonal], [Trinity][trinity]) was strictly implemented, the used architectural style follows principles and good practices found over all of them.
- Low coupling, high cohesion
- Implementation hiding
- Rich domain model
- Separation of concerns
- The Dependency Rule

The below proposed architecture tries to solve one problem often common for these architectural styles: [exposing internals of objects](https://blog.ttulka.com/object-oriented-design-vs-persistence) and breaking their encapsulation. The proposed architecture employs full object encapsulation and rejects anti-patterns like Anemic Domain Model or JavaBean. An Object is a solid unit of behavior. A Service is an Object on higher level of architectural abstraction. 

[onion]: http://jeffreypalermo.com/blog/the-onion-architecture-part-1
[clean]: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
[hexagonal]: https://alistair.cockburn.us/hexagonal-architecture/
[trinity]: https://github.com/oregor-projects/trinity-demo-java

### Screaming Architecture

The architecture "screams" its intentions just by looking at the code structure:
```
..ecommerce
    billing
        payment
    sales
        category
        order
        product
    shipping
        delivery
    warehouse
```

Going deeper the technical concepts are visible too:
```
..ecommerce
    billing
        payment
            jdbc
        listeners
        rest
```

### Packaging

As shown in the previous section, the code is structured by the domain together with packages for technical concerns (`jdbc`, `rest`, `web`, etc.).

Such a packaging style is the first step for a further modularization. 

The semantic of a package is following: `company.product.domain.service.[entity|impl]`, where `entity` and `impl` are optional. Full example: `com.ttulka.ecommerce.billing.payment.jdbc`. 

### Assembling

While a physically monolithic deployment is okay for most cases, a logically monolithic design, where everything is coupled with everything, is evil.

To show that the Monolith architectural pattern is not equal to the Big Ball Of Mud, a modular monolithic architecture was chosen as the start point.

The services can be further cut into separate modules (eg. Maven artifacts) by feature:
```
com.ttulka.ecommerce:ecommerce-application
com.ttulka.ecommerce.sales:catalog-service
com.ttulka.ecommerce.sales:cart-service
com.ttulka.ecommerce.sales:order-service
com.ttulka.ecommerce.billing:payment-service
com.ttulka.ecommerce.shipping:delivery-service
com.ttulka.ecommerce.warehouse:warehouse-service
```

Or by [component](https://blog.ttulka.com/package-by-component-with-clean-modules-in-java):
```
com.ttulka.ecommerce.billing:payment-domain
com.ttulka.ecommerce.billing:payment-jdbc
com.ttulka.ecommerce.billing:payment-rest
com.ttulka.ecommerce.billing:payment-events
com.ttulka.ecommerce.billing:payment-listeners
```

In detail:
```
com.ttulka.ecommerce.billing:payment-domain
    ..billing
        payment
            Payment
            PaymentId
            CollectPayment
            FindPayments
com.ttulka.ecommerce.billing:payment-jdbc
    ..billing.payment.jdbc
        PaymentJdbc
        CollectPaymentJdbc   
        FindPaymentsJdbc     
com.ttulka.ecommerce.billing:payment-rest
    ..billing.payment.rest
        PaymentController
com.ttulka.ecommerce.billing:payment-events
    ..billing.payment
        PaymentCollected
com.ttulka.ecommerce.billing:payment-listeners
    ..billing.payment.listeners
        OrderPlacedListener
```

Which can be brought together with a Spring Boot Starter, containing only Configuration classes and dependencies on other modules:
```
com.ttulka.ecommerce.billing:payment-spring-boot-starter
    ..billing.payment
        jdbc
            PaymentJdbcConfig
        listeners
            PaymentListenersConfig
    META-INF
        spring.factories
```

Note: Events are actually part of the domain, that's why they are in the package `..ecommerce.billing.payment` and not in `..ecommerce.billing.payment.events`. They are in a separate module to break the build cyclic dependencies: a dependent module (Listener) needs to know only Events and not the entire Domain. 

See this approach in an alternative branch: [modulith](https://github.com/ttulka/ddd-example-ecommerce/tree/modulith).

### Anatomy of a Service 

**[Service](http://udidahan.com/2010/11/15/the-known-unknowns-of-soa/)** is the technical authority for a specific business capability.
- There is a one-to-one mapping between a Bounded Context and a Subdomain (ideal case).
- A Bounded Context defines the boundaries of the biggest services possible.
- A Bounded Context can be decomposed into multiple service boundaries.
    - For example, Sales domain contains Catalog, Cart and Order services.
- A service boundaries are based on service responsibilities and behavior.
- A service is defined by its logical boundaries, not a physical deployment unit.

**Application** is a deployment unit. A monolithic Application can have more Services.
- Bootstrap (application container etc.). 
- Cross-cutting concerns (security, transactions, messaging, logging, etc.).

![Application and Services](doc/application-services.png)

**Configuration** assemblies the Service as a single component.
- Has dependencies to all inner layers.
- Can be implemented by Spring's context `@Configuration` or simply by object composition and Dependency Injection.
- Implements the Dependency Inversion Principle.  

**Gateways** create the published API of the Service.
 - Driving Adapters in the Hexagonal Architecture.
 - REST, SOAP, or web Controllers,
 - Event Listeners,
 - CLI.
 
**Use-Cases** are entry points to the service capabilities and together with **Entities** form the _Domain API_.
- Ports in the Hexagonal Architecture.
- No implementation details.
- None or minimal dependencies.
 
_Domain Implementation_ fulfills the Business Capabilities with particular technologies.
- Driven Adapters in the Hexagonal Architecture.
- Tools and libraries,
- persistence,
- external interfaces access.

Source code dependencies point always inwards and, except Configuration, are strict: allows coupling only to the one layer below it (for example, Gateways mustn't call Entities directly, etc.).
 
![Service Anatomy](doc/service-anatomy.png)

#### Example of a Service Anatomy 

As a concrete example consider the Business Capability to find payments in Billing service:

- Application is implemented via Spring Boot Application.
- `PaymentJdbcConfig` configures the JDBC implementations for the Domain. 
- Gateway is implemented as a REST Controller.
- Use-Case interface  `FindPayments` is implemented with `PaymentsJdbc` in Use-Cases Implementation.  
- Entity `Payment` is implemented with `PaymentJdbc` in Entities Implementation.

![Service Anatomy](doc/service-anatomy-example.png)

There is no arrow from Configuration to Gateways because `PaymentController` is annotated with Spring's `@Component` which makes it available for component scanning the application is based on. This is only one possible approach. Another option would be to put the Controller as a Bean into the Configuration, etc..

## Conclusion

The goal of this project is to demonstrate basic principles of Domain-Driven Design in a simple but non-trivial example.

For the sake of simplicity a very well-known domain (e-commerce) was chosen. As every domain differs in context of business, several assumption must have been made.

Although all fundamental use-case were implemented, there is still a room for improvement. Cross-cutting concerns like authentication, authorization or monitoring are not implemented.

### Where to Next

Check out the alternative branches and repos to see additional concepts and technologies in action:

- [Modulith](https://github.com/ttulka/ddd-example-ecommerce/tree/modulith): A separate Maven module per service.
- [Microfrontends](https://github.com/ttulka/ddd-example-ecommerce/tree/microfrontend): Service Web Components as part of the service codebase.
- [Microservices](https://github.com/ttulka/ddd-example-ecommerce-microservices): Deployments with Docker and Kubernetes.
- [Kotlin](https://github.com/ttulka/ddd-example-ecommerce-kotlin): The same project again, this time in Kotlin.

