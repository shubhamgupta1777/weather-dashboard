<!--
═══════════════════════════════════════════════════════════════
SYNC IMPACT REPORT: Constitution v1.1.0 (Architecture Review Edition)
═══════════════════════════════════════════════════════════════

RATIFICATION: 2026-03-11
LAST AMENDED: 2026-03-11 (v1.0.0 → v1.1.0 - PATCH level improvements)

IMPROVEMENTS IN v1.1.0:
  ✓ Strengthened enforceability: MUST vs SHOULD language clarified throughout
  ✓ Resolved vague statements: "avoid framework-specific magic" → concrete guidelines
  ✓ Layered architecture: Explicit anti-patterns (what NOT to do)
  ✓ Traceability: Added spec→plan→tasks→code linkage requirements
  ✓ Testability: Clarified unit vs integration test boundaries
  ✓ Governance: Simplified (removed "core team consensus" → clearer approval model)
  ✓ Added: Testing patterns & error handling expectations
  ✓ Refined: Learning project focus (less prescriptive on "all" coverage)

MODIFIED SECTIONS:
  ✓ I. Spec-First Development   - Added explicit traceability requirement
  ✓ II. Clean Layered Architecture - Added anti-patterns section
  ✓ III. External API Isolation - Clarified "isolation" with concrete rules
  ✓ IV. Testable by Design      - Separated unit vs integration scope
  ✓ V. Simplicity & Incremental - Made guidance more actionable
  ✓ Development Workflow        - Added traceability expectation
  ✓ Governance                  - Simplified approval criteria

PRINCIPLES MAINTAINED: 5 core principles (no reordering, no deletions)
NO BREAKING CHANGES: All prior commitments preserved; only clarifications

DEPENDENT TEMPLATES:
  ✅ spec-template.md   - Align with explicit user story traceability
  ✅ plan-template.md   - Constitution Gate: verify all 5 principles explicitly
  ✅ tasks-template.md  - Link tasks to spec user stories (can map from spec.md)

═══════════════════════════════════════════════════════════════
-->

# Weather Dashboard Constitution

A project constitution for learning and experimenting with AI-led Software Development Lifecycle (AI-led SDLC) using Spec-Driven Development. This constitution establishes non-negotiable engineering principles, architectural constraints, and development workflow expectations for the Weather Dashboard application.

## Core Principles

### I. Spec-First Development

Every feature MUST begin with a specification (`spec.md`) before implementation starts. This specification MUST include:

- Prioritized user stories with independent test criteria
- Clear acceptance scenarios for each story
- API contracts (endpoints, request/response shapes) if applicable

Implementation plans (`plan.md`) and task lists (`tasks.md`) MUST be derived from and remain traceable to the specification. Each task MUST reference the user story it satisfies with a direct link.

**Traceability Requirement**: Every line of implementation code MUST satisfy at least one acceptance criterion from spec.md. Code reviews MUST verify this linkage.

**Anti-Pattern**: Coding to internal assumptions or undocumented requirements. ❌ Do not start implementation until spec.md is reviewed and approved.

### II. Clean Layered Architecture

The backend MUST follow a strict four-layer architecture with clear separation of concerns:

```
HTTP Layer (Controllers):     Handles requests/responses, JSON serialization, status codes
Business Logic Layer (Services): Implements features, validation, business rules
Integration Layer (API Clients): Communicates with external APIs, error handling, retries
Data Model Layer (DTOs/POJOs):  Internal domain models, never expose external API schemas
```

**Layer Responsibilities** (MUST NOT violate):

- **Controllers**: Only HTTP routing, request validation (basic), response formatting. No business logic.
- **Services**: Business rules, validation logic, coordinate between data and integration layers. No direct HTTP concerns, no external API calls.
- **API Clients**: Encapsulate all external weather API communication. Methods return internal DTOs (never external API models).
- **DTOs**: Immutable data structures representing internal domain concepts.

**Anti-Patterns**:

- ❌ Service constructor calling `new WeatherApiClient()` (violates dependency injection)
- ❌ Controller containing validation loops or business rules
- ❌ Service directly using external API response classes (e.g., returning `T extends WeatherApiResponse`)
- ❌ No public fields or getters in DTOs (prefer records)

### III. External API Isolation (Non-Negotiable)

All weather API communication MUST be abstracted behind a client layer. This principle ensures:

**Requirements**:

- API clients MUST implement an interface (e.g., `WeatherClient`), never used directly
- Services MUST receive clients via constructor injection, never instantiate them
- API responses MUST be mapped to internal DTOs within the client layer
- Services MUST never reference external API model classes
- All API errors MUST be caught at the client layer and wrapped in service-level exceptions

**Mapping Rule**: Each public method in the API client MUST return only internal DTO types. External API JSON shapes are implementation details of that layer.

**Anti-Patterns**:

- ❌ Injecting HTTP client directly into service (e.g., `RestTemplate`, `WebClient`)
- ❌ Services importing `com.external.weather.api.*` packages
- ❌ Passing external API response objects as service method parameters or return values
- ❌ Catching external API exceptions in controller layer

### IV. Testable by Design

The architecture MUST support testable implementation at all levels.

**Unit Tests** (services in isolation):

- MUST mock the `WeatherClient` interface
- MUST not touch real external APIs
- MUST validate business logic independent of HTTP or API concerns

**Integration Tests** (controller + service, mocked API client):

- MUST test complete HTTP request/response cycles
- MUST mock external API client to verify service integration
- MUST verify request handling and response formatting

**Test Isolation Rule**: Each test MUST be runnable independently. Tests MUST not depend on:

- Other tests' execution order
- Shared state or test fixtures
- Real external API calls

**Minimum Coverage**: All non-trivial business logic (services) requires unit tests. All REST controllers have at least one integration test per HTTP endpoint.

**Anti-Patterns**:

- ❌ Integration tests that make real HTTP calls to external weather API
- ❌ Unit tests that import Spring context or HTTP libraries
- ❌ Test data kept in mutable static fields
- ❌ Skipping tests during development ("@Disabled", "@Ignore")

### V. Simplicity and Incremental Development

Features MUST be delivered incrementally, with each task producing a deployable increment. Prefer simple implementations over premature abstraction.

**YAGNI Principle**: Do not implement features, abstractions, or infrastructure "just in case." Implement only what is needed to satisfy current acceptance criteria.

**Guidance**:

- Start with the minimum viable implementation that passes tests
- Refactor only when complexity becomes demonstrable (duplicated code, hard to test)
- Avoid framework magic; prefer explicit, readable code
- Each task SHOULD be completable in 1-2 days

**Anti-Patterns**:

- ❌ Creating generic abstract base classes for a single implementation
- ❌ Over-parameterized methods or heavy use of reflection
- ❌ Premature optimization without profiling data
- ❌ Adding technologies (caching, queues, databases) before needed

## Technical Stack and Constraints

The project operates under the following immutable technical constraints:

- **Java Version**: 17 or higher (LTS preferred)
- **Spring Boot Version**: 3.5.x (<4.0)
- **Build Tool**: Maven (single-module structure)
- **API Design**: RESTful HTTP JSON API
- **External Dependencies**: Weather API integration (configurable via environment)
- **Configuration**: API keys and sensitive data stored in environment variables (never hardcoded)
- **Testing Framework**: JUnit 5 (Jupiter) + Mockito for unit/integration tests
- **Code Style**: Follow Spring Boot conventions and Google Java Style Guide

**Error Handling & Logging Expectations** (enforces principles IV & II):

- Services MUST catch external API errors and wrap them in domain-specific exceptions (e.g., `WeatherServiceException`)
- API clients MUST log API errors with context (request details, response status) at WARN or ERROR level
- Controllers MUST translate service exceptions to HTTP status codes; errors SHOULD include a user-friendly message in response
- No debug-level logging of sensitive data (API keys, user credentials)

## Development Workflow

All features follow this strict, gate-controlled workflow cycle:

```
Feature Idea
    ↓
spec.md (user stories with priorities, acceptance scenarios, API contracts)
    ↓
plan.md (technical approach, layer allocation, test strategy)
    ↓
tasks.md (actionable, ordered, dependency-mapped, spec-linked tasks)
    ↓
Code Implementation (per tasks, maintaining spec traceability)
    ↓
Unit & Integration Tests (verifying spec.md acceptance criteria)
    ↓
Code Review (verify constitution compliance + spec traceability)
    ↓
Merge & Deploy
```

**Gate Requirements** (MUST enforce in pull requests):

1. **Before `plan.md`**: spec.md MUST be reviewed and approved. No exceptions.
2. **Before `tasks.md`**: plan.md MUST be reviewed and aligned with actual code structure.
3. **Before coding**: tasks.md MUST exist and be linked from branch/PR description.
4. **Every pull request MUST**:
   - Reference at least one task from tasks.md (e.g., "Closes #42: Implement weather endpoint")
   - Link affected test(s) that verify spec.md acceptance criteria
   - Include architecture review comment confirming no layer violations

**Traceability Format** (required in all code commits):

- Commit message MUST reference the task ID: `git commit -m "feat: fetch weather data (task 3.1 from tasks.md)"`
- Pull request MUST map code changes to acceptance scenarios from spec.md
- Code comments SHOULD indicate which spec.md user story is being satisfied (use story ID in comments)

## Governance

### Constitution Authority

This constitution supersedes all conflicting project conventions, coding styles, or informal practices. When a conflict arises, this document takes precedence. Team members MUST justify any deviation from these principles in writing (PR comment or task description).

### Amendment Process

Amendments to this constitution require:

1. **Rationale**: Document the reason for change and which principle(s) are affected
2. **Impact Analysis**: Identify existing code/features that require retrofit
3. **Change Description**: Clearly state what is being added/removed/modified
4. **Version Bump**: Determine semantic version change (MAJOR: principle removal, MINOR: principle expansion, PATCH: clarifications)
5. **Review**: At least one team member approval required before merge

**Process**: Amendments are made via pull request to `.specify/memory/constitution.md` with the above details in the PR description.

### Compliance & Code Review

Every pull request MUST verify:

- ✅ All code follows the layered architecture (Principle II)
- ✅ External API communication only through abstracted client (Principle III)
- ✅ Tests are independent and don't call real APIs (Principle IV)
- ✅ Implementation is traceable to spec.md (Principle I)
- ✅ No unnecessary abstractions or premature optimization (Principle V)

**Review Checklist** (copy into PR template):

```
- [ ] Code changes linked to task(s) from tasks.md
- [ ] Test(s) verify spec.md acceptance criteria
- [ ] No service imports external API model classes
- [ ] No controller contains business logic
- [ ] No direct API client instantiation (dependency injection only)
- [ ] All external API errors caught and wrapped at client layer
```

**Non-Negotiable Violations** (require fix before merge):

- Service layer code instantiating or directly using HTTP clients
- Controller logic beyond HTTP mapping (validation loops, calculations)
- External API response classes leaking into service layer
- Tests making real calls to external APIs

**Version**: 1.1.0 | **Ratified**: 2026-03-11 | **Last Amended**: 2026-03-11
