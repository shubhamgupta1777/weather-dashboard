# Tasks: Weather Dashboard – Current Weather by City

**Input**: Design documents from `/specs/001-get-weather-by-city/`  
**Specification**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)  
**Prerequisites**: spec.md (APPROVED), plan.md (REVIEWED)

**Organization**: Tasks are grouped by architectural layer and user story to enable independent implementation and testing of each story. Each task is sized for completion within 1-2 hours.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Implementation Strategy

**Order of Execution**:

1. Setup phase: Maven dependencies, project structure
2. Foundational phase: Exceptions, DTOs, configuration properties (blocking for all stories)
3. User Story 1 (US1): API client interface + implementation, service, controller for happy path
4. User Story 2 (US2): Add validation layers, error handling for invalid/not found
5. User Story 3 (US3): Enhance error handling for API failures, resilience
6. Testing phase: Unit tests for each layer, integration tests
7. Polish: Code review, documentation updates, final validation

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure  
**Deliverable**: Maven POM updated, directory structure created, base packages ready

- [x] T001 Add Maven dependencies for Spring Boot Web and testing libraries to `pom.xml`
  - Add `spring-boot-starter-web`, `spring-boot-starter-test`, `mockito-core`, `mockito-junit-jupiter`
  - Verify Java 17+ compatibility
  - Expected outcome: `pom.xml` updated with all required dependencies

- [x] T002 [P] Create package structure for the weather feature in `src/main/java/com/weatherdashboard/api/`
  - Create subdirectories: `controller/`, `service/`, `client/`, `dto/`, `exception/`, `config/`
  - Create test directories: `src/test/java/com/weatherdashboard/api/` with same structure
  - Expected outcome: Directory structure ready for implementation files

- [x] T003 [P] Update Spring Boot application class to be ready for testing in `src/main/java/com/weatherdashboard/WeatherDashboardApplication.java`
  - Ensure it exists and has `@SpringBootApplication` annotation
  - Set up component scanning to include the new api package
  - Expected outcome: Application class ready to run, scanning configured

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create exception classes and DTOs that all layers depend on  
**Deliverable**: Exception hierarchy, immutable DTOs, configuration properties

- [x] T004 Create custom exception for external API errors in `src/main/java/com/weatherdashboard/api/exception/ExternalApiException.java`
  - Fields: `statusCode` (int), message, cause (Throwable)
  - Constructor: `ExternalApiException(String message, int statusCode, Throwable cause)`
  - Getters for all fields
  - Expected outcome: Exception class ready to be thrown by API client

- [x] T005 [P] Create custom exception for service layer in `src/main/java/com/weatherdashboard/api/exception/WeatherServiceException.java`
  - Fields: `errorCode` (String), message, cause (Throwable)
  - Constructor: `WeatherServiceException(String message, String errorCode, Throwable cause)`
  - Getters for all fields
  - Expected outcome: Exception class ready for business logic errors

- [x] T006 [P] Create WeatherResponse DTO in `src/main/java/com/weatherdashboard/api/dto/WeatherResponse.java`
  - Use Java Record: `record WeatherResponse(String cityName, BigDecimal temperature, String condition, Integer humidity, BigDecimal windSpeed)`
  - No methods needed; records provide getters automatically
  - Expected outcome: Immutable DTO ready for all layers to use

- [x] T007 [P] Create ErrorResponse DTO in `src/main/java/com/weatherdashboard/api/dto/ErrorResponse.java`
  - Use Java Record: `record ErrorResponse(String error)`
  - Used for error HTTP responses
  - Expected outcome: Error response DTO ready for controller error handling

- [x] T008 [P] Create Weather API configuration properties class in `src/main/java/com/weatherdashboard/api/config/WeatherApiProperties.java`
  - Annotate with `@Configuration` and `@ConfigurationProperties(prefix = "weather.api")`
  - Fields: `key`, `baseUrl`, `timeoutMs` (default 5000)
  - Add getters and setters
  - Expected outcome: Configuration properties class ready to bind from application.yaml

- [x] T009 [P] Update `src/main/resources/application.yaml` with weather API properties
  - Add weather.api.key (from environment: ${WEATHER_API_KEY})
  - Add weather.api.baseUrl (from environment: ${WEATHER_API_BASE_URL})
  - Add weather.api.timeoutMs (default 5000)
  - Expected outcome: Properties configured and ready to bind

---

## Phase 3: User Story 1 - Retrieve Current Weather by City Name (MVP)

**Acceptance Criteria** (from spec.md):

- Endpoint returns HTTP 200 with city, temperature, condition, humidity, wind speed
- Response is mapped to internal DTO (not raw API response)
- Each request fetches latest data independently

**Deliverable**: Working endpoint for valid city names, API client integration, service coordination

### 3.1 External API Client Layer

- [x] T010 Create external weather API response DTO in `src/main/java/com/weatherdashboard/api/client/dto/OpenWeatherMapResponse.java`
  - Package name: `client.dto` (internal to client layer only)
  - Fields: `main` (nested object with temp, humidity), `weather` (array with main type), `wind` (nested object with speed), `name`
  - Note: This is the EXTERNAL API response shape; never used above client layer
  - Expected outcome: External API response model ready for deserialization

- [x] T011 [P] Create WeatherApiClient interface in `src/main/java/com/weatherdashboard/api/client/WeatherApiClient.java`
  - Method: `WeatherResponse getWeather(String cityName) throws ExternalApiException`
  - Purpose: Contract for external API communication
  - Expected outcome: Interface ready for implementation and mocking

- [x] T012 Create WeatherApiClientImpl in `src/main/java/com/weatherdashboard/api/client/WeatherApiClientImpl.java`
  - Inject `RestTemplate`, `WeatherApiProperties`
  - Implement `getWeather(String cityName)` method
  - Call external API: `https://{baseUrl}/weather?q={cityName}&appid={key}`
  - Catch exceptions: HttpClientErrorException, HttpServerErrorException, ResourceAccessException (timeout), generic Exception
  - Map each exception to `ExternalApiException` with appropriate status code
  - Expected outcome: API calls successful, exceptions wrapped correctly

- [x] T013 [P] Implement mapping logic in WeatherApiClientImpl: `mapToInternal(OpenWeatherMapResponse external) → WeatherResponse`
  - Map cityName from `external.getName()`
  - Map temperature from `external.getMain().getTemp()` to BigDecimal
  - Map condition from `external.getWeather()[0].getMain()` using mapCondition() helper
  - Map humidity from `external.getMain().getHumidity()`
  - Map windSpeed from `external.getWind().getSpeed()` to BigDecimal
  - Expected outcome: Mapping logic correctly converts external → internal

- [x] T014 [P] Implement condition mapping helper in WeatherApiClientImpl: `mapCondition(String externalCondition) → String`
  - Handle: "CLEAR" → "CLEAR", "CLOUDS" → "CLOUDY", "RAIN" → "RAINY", "SNOW" → "SNOWY", "THUNDERSTORM" → "STORMY", etc.
  - Default: "UNKNOWN" for unmapped conditions
  - Expected outcome: Weather conditions correctly normalized

- [x] T015 [P] Create WeatherApiConfig bean class in `src/main/java/com/weatherdashboard/api/config/WeatherApiConfig.java`
  - Annotate with `@Configuration`
  - Create `@Bean RestTemplate restTemplate()` returning new RestTemplate()
  - Create `@Bean WeatherApiClient weatherApiClient(RestTemplate, WeatherApiProperties)` returning WeatherApiClientImpl instance
  - Expected outcome: Spring beans registered and ready for injection

### 3.2 Service Layer

- [x] T016 Create WeatherService in `src/main/java/com/weatherdashboard/api/service/WeatherService.java`
  - Annotate with `@Service`
  - Inject `WeatherApiClient` (interface) via constructor
  - Implement: `getWeatherByCity(String cityName) → WeatherResponse throws WeatherServiceException`
  - Call injected client: `weatherApiClient.getWeather(cityName)`
  - Return response directly (no exception caught yet; that's in T017)
  - Expected outcome: Service method ready to coordinate with client

### 3.3 Controller Layer

- [x] T017 Create WeatherController in `src/main/java/com/weatherdashboard/api/controller/WeatherController.java`
  - Annotate with `@RestController` and `@RequestMapping("/weather")`
  - Inject `WeatherService` via constructor
  - Implement: `@GetMapping("/city/{cityName}") getWeatherByCity(@PathVariable String cityName) → ResponseEntity<WeatherResponse>`
  - Call service: `weatherService.getWeatherByCity(cityName)`
  - Return: `ResponseEntity.ok(response)`
  - Expected outcome: GET /weather/city/{cityName} endpoint works for valid cities

### 3.4 Testing for US1

- [x] T018 Create unit test for WeatherService (Happy Path) in `src/test/java/com/weatherdashboard/api/service/WeatherServiceTest.java`
  - Test method: `testGetWeatherByCity_validCity_returnsWeatherResponse()`
  - Mock `WeatherApiClient` with a valid response
  - Assert returned WeatherResponse matches expected values (city, temp, condition, humidity, wind)
  - Verify mock was called once
  - Expected outcome: Service unit test passing for valid city

- [x] T019 [P] Create integration test for WeatherController (Happy Path) in `src/test/java/com/weatherdashboard/api/controller/WeatherControllerTest.java`
  - Use `@SpringBootTest` and `@AutoConfigureMockMvc`
  - Mock `WeatherApiClient` bean with `@MockBean`
  - Test method: `testGetWeatherByCity_validCity_returns200()`
  - Perform GET `/weather/city/London`
  - Assert HTTP 200
  - Assert response JSON has all 5 fields with correct values
  - Expected outcome: Integration test passing, endpoint works end-to-end

---

## Phase 4: User Story 2 - Handle Invalid/Not Found Cities Gracefully

**Acceptance Criteria** (from spec.md):

- Empty/blank city name returns HTTP 400 with validation error
- Non-existent city returns HTTP 404 with "City not found" message
- Invalid format (special chars) returns HTTP 400 with descriptive message

**Deliverable**: Input validation, error handling for not-found cases, proper HTTP status mapping

### 4.1 Service Layer - Input Validation

- [ ] T020 Add input validation to WeatherService.getWeatherByCity()
  - Check if cityName is null or blank
  - If invalid: throw `WeatherServiceException("City name is required and cannot be empty", "INVALID_INPUT", null)`
  - Check if cityName exceeds max length (100 characters)
  - If too long: throw `WeatherServiceException("City name must be 100 characters or less", "INVALID_INPUT", null)`
  - Expected outcome: Service validates input before calling client

### 4.2 Service Layer - Error Handling

- [ ] T021 Add error handling to WeatherService.getWeatherByCity()
  - Wrap API client call in try-catch for `ExternalApiException`
  - If status 404: throw `WeatherServiceException("City not found: '" + cityName + "'", "CITY_NOT_FOUND", cause)`
  - Other exceptions will be caught in US3; for now, rethrow as generic
  - Expected outcome: Service catches and wraps 404 errors

### 4.3 Controller Layer - Error Mapping

- [ ] T022 Add exception handling to WeatherController
  - Add method: `@ExceptionHandler(WeatherServiceException.class)`
  - Method: `handleWeatherServiceException(WeatherServiceException ex) → ResponseEntity<ErrorResponse>`
  - Map error codes to HTTP status:
    - "INVALID_INPUT" → HttpStatus.BAD_REQUEST (400)
    - "CITY_NOT_FOUND" → HttpStatus.NOT_FOUND (404)
    - Others → HttpStatus.INTERNAL_SERVER_ERROR (500)
  - Return ResponseEntity with ErrorResponse (message from exception)
  - Expected outcome: Exceptions converted to HTTP responses with correct status codes

### 4.4 Testing for US2

- [ ] T023 Add unit tests to WeatherServiceTest for invalid/not found scenarios
  - Test method: `testGetWeatherByCity_emptyCity_throwsException()`
  - Assert throws WeatherServiceException with errorCode = "INVALID_INPUT"
  - Test method: `testGetWeatherByCity_cityNotFound_throws404Exception()`
  - Mock client to throw ExternalApiException(status=404)
  - Assert throws WeatherServiceException with errorCode = "CITY_NOT_FOUND"
  - Expected outcome: Service unit tests passing for error cases

- [ ] T024 [P] Add integration tests to WeatherControllerTest for invalid/not found scenarios
  - Test method: `testGetWeatherByCity_emptyCity_returns400()`
  - Perform GET `/weather/city/` (no city name)
  - Assert HTTP 400
  - Test method: `testGetWeatherByCity_cityNotFound_returns404()`
  - Mock client to throw ExternalApiException(status=404)
  - Perform GET `/weather/city/InvalidCity`
  - Assert HTTP 404 and error message in response
  - Expected outcome: Integration tests passing for error cases

---

## Phase 5: User Story 3 - Handle Weather API Failures Gracefully

**Acceptance Criteria** (from spec.md):

- API 5xx error returns HTTP 503 (Service Unavailable)
- Network timeout returns HTTP 504 (Gateway Timeout)
- Malformed API response returns HTTP 500 with generic message (no implementation details)
- Proper logging of errors

**Deliverable**: Comprehensive error handling for external API failures, timeout management, logging

### 5.1 Client Layer - Enhanced Error Handling

- [ ] T025 Add timeout handling to WeatherApiClientImpl
  - Configure RestTemplate with timeout settings (use Timeout from WeatherApiProperties)
  - In exception handling: Detect SocketTimeoutException in ResourceAccessException cause chain
  - For timeout: throw `ExternalApiException("API request timeout", 0, cause)` (statusCode=0 indicates timeout)
  - Expected outcome: Timeouts caught and wrapped

- [ ] T026 [P] Enhance logging in WeatherApiClientImpl
  - Import SLF4J logger
  - Before API call: log.debug("Fetching weather for city: {}", cityName)
  - On HTTP error: log.warn("Weather API error: {} - {}", statusCode, message)
  - On timeout: log.warn("Weather API timeout for city: {}", cityName)
  - On unexpected error: log.error("Unexpected error calling weather API", exception)
  - Expected outcome: Debugging information captured in logs

- [ ] T027 [P] Add error details extraction from ExternalApiException in client
  - Extract HTTP status code (200-599)
  - Extract error message from response body (if available)
  - Log all relevant context
  - Expected outcome: Rich error information for debugging

### 5.2 Service Layer - Enhanced Error Mapping

- [ ] T028 Enhance error handling in WeatherService.getWeatherByCity()
  - Catch ExternalApiException with status >= 500: throw `WeatherServiceException("Weather service is temporarily unavailable", "API_UNAVAILABLE", cause)`
  - Catch ExternalApiException with status = 0 (timeout): throw `WeatherServiceException("Request to weather service timed out", "API_TIMEOUT", cause)`
  - Catch other exceptions (parse errors, unexpected): throw `WeatherServiceException("An unexpected error occurred", "UNEXPECTED_ERROR", cause)`
  - Add logging for each error type
  - Expected outcome: All error types mapped to appropriate messages and codes

### 5.3 Controller Layer - Enhanced Error Mapping

- [ ] T029 Update exception mapping in WeatherController
  - Add cases to error code → HTTP status mapping:
    - "API_UNAVAILABLE" → HttpStatus.SERVICE_UNAVAILABLE (503)
    - "API_TIMEOUT" → HttpStatus.GATEWAY_TIMEOUT (504)
    - "UNEXPECTED_ERROR" → HttpStatus.INTERNAL_SERVER_ERROR (500)
  - For 500 errors: return generic message (no implementation details to client)
  - Expected outcome: API failures mapped to correct HTTP status codes

- [ ] T030 [P] Add global exception handler for unmapped exceptions in WeatherController
  - Method: `@ExceptionHandler(Exception.class)`
  - Catches any unexpected exceptions not handled specifically
  - Log error: log.error("Unexpected error", exception)
  - Return HTTP 500 with generic message
  - Expected outcome: No exception stack traces leak to clients

### 5.4 Testing for US3

- [ ] T031 Add unit tests to WeatherServiceTest for API failures
  - Test method: `testGetWeatherByCity_apiUnavailable_returns503()`
  - Mock client to throw ExternalApiException(status=500)
  - Assert throws WeatherServiceException with errorCode = "API_UNAVAILABLE"
  - Test method: `testGetWeatherByCity_timeout_returns504()`
  - Mock client to throw ExternalApiException with SocketTimeoutException cause
  - Assert throws WeatherServiceException with errorCode = "API_TIMEOUT"
  - Test method: `testGetWeatherByCity_malformedData_returnsUnexpectedError()`
  - Mock client to throw ExternalApiException with parsing error
  - Assert throws WeatherServiceException with errorCode = "UNEXPECTED_ERROR"
  - Expected outcome: Service tests passing for all API failure scenarios

- [ ] T032 [P] Add integration tests to WeatherControllerTest for API failures
  - Test method: `testGetWeatherByCity_apiUnavailable_returns503()`
  - Mock client to throw ExternalApiException(status=500)
  - Perform GET `/weather/city/London`
  - Assert HTTP 503
  - Test method: `testGetWeatherByCity_timeout_returns504()`
  - Mock client to throw timeout exception
  - Perform GET `/weather/city/London`
  - Assert HTTP 504
  - Expected outcome: Integration tests passing for all failure scenarios

---

## Phase 6: Testing & Validation

**Purpose**: Comprehensive test coverage, validation against spec, code quality  
**Deliverable**: All user stories tested and passing, specification acceptance verified

- [ ] T033 Add test for repeated city queries (P1 AS2) in WeatherServiceTest
  - Test method: `testGetWeatherByCity_repeatedQueries_fetchesFreshData()`
  - Mock client to return different temperature values on successive calls
  - Call service twice for same city
  - Assert both calls to service return different values (fresh data each time)
  - Verify mock called twice
  - Expected outcome: Confirms each request fetches fresh data

- [ ] T034 [P] Add test for DTO mapping in WeatherServiceTest
  - Test method: `testGetWeatherByCity_dtoMapping_convertsExternalToInternal()`
  - Mock client to return specific WeatherResponse with all fields populated
  - Call service
  - Assert all fields present and correctly mapped
  - Expected outcome: DTO mapping verified throughout system

- [ ] T035 [P] Run all unit and integration tests and verify passing
  - $ mvn clean test
  - Assert all 15+ tests pass: service tests + controller tests
  - Check test coverage (goal: 80%+ for service and controller)
  - Expected outcome: All tests passing, coverage acceptable

- [ ] T036 [P] Validate all spec acceptance scenarios
  - Map each acceptance scenario from spec.md to test case
  - Verify each test passes:
    - P1 AS1: ✓ Valid city returns 200 with all fields
    - P1 AS2: ✓ Repeated queries fetch fresh data
    - P1 AS3: ✓ Response is internal DTO (not raw API)
    - P2 AS1: ✓ Not found city returns 404
    - P2 AS2: ✓ Empty city returns 400
    - P2 AS3: ✓ Invalid format returns 400
    - P3 AS1: ✓ API 5xx returns 503
    - P3 AS2: ✓ Timeout returns 504
    - P3 AS3: ✓ Malformed data returns 500
  - Expected outcome: All acceptance criteria satisfied

---

## Phase 7: Documentation & Polish

**Purpose**: Code quality, documentation, final review  
**Deliverable**: Production-ready code, updated documentation

- [ ] T037 Add JavaDoc comments to public classes and methods
  - WeatherController: Document endpoint purpose, parameters, response
  - WeatherService: Document business logic for getWeatherByCity()
  - WeatherApiClient interface: Document contract
  - WeatherResponse: Document DTO fields
  - Expected outcome: Code documented for future maintainers

- [ ] T038 [P] Verify clean layered architecture (no violations)
  - Audit WeatherController: No API imports, no service logic
  - Audit WeatherService: No HTTP imports, no client instantiation
  - Audit WeatherApiClient: Allowed to import external API DTOs (internal only)
  - Expected outcome: Architecture verified, no principle violations

- [ ] T039 [P] Update README.md with feature documentation
  - Add: Feature name, purpose, endpoint, example request/response
  - Add: Environment variables needed (WEATHER_API_KEY, etc.)
  - Add: How to run tests
  - Expected outcome: Developers can quickly understand the feature

- [ ] T040 [P] Code review checklist
  - Verify all tasks completed
  - Check each task against spec acceptance criteria
  - Verify no TODO/FIXME comments left
  - Verify all classes follow Java conventions
  - Verify tests are comprehensive
  - Expected outcome: Code ready for merge

- [ ] T041 Merge feature branch to main
  - Commit message references all task IDs: "feat: implement current weather by city (tasks 001-041)"
  - Commit links to spec.md and plan.md
  - Expected outcome: Feature merged, ready for deployment

---

## Implementation Dependencies & Parallel Execution

### Critical Path (Must Complete Sequentially)

1. Phase 2: Exceptions and DTOs (all others depend on these)
2. Phase 3.1: API Client (service depends on this)
3. Phase 3.2: Service (controller depends on this)
4. Phase 3.3: Controller (integration tests need this)

### Parallel Opportunities

- **T002, T003**: Directory structure and app config (parallel)
- **T005-T009**: All DTOs and config (parallel, after T004)
- **T010-T015**: API client implementation (mostly parallel)
- **T018-T019**: Service and controller tests for US1 (parallel)
- **T023-T024**: Tests for US2 (parallel)
- **T031-T032**: Tests for US3 (parallel)
- **T037-T040**: Documentation and review (parallel)

### Recommended Execution Groups

```
Group 1 (Setup):        T001, T002, T003
Group 2 (Foundation):   T004, +parallel(T005-T009)
Group 3 (API Layer):    T010, T011, T012, +parallel(T013-T015)
Group 4 (Config Bean):  T015, T016
Group 5 (Service):      T016, T017
Group 6 (Tests US1):    +parallel(T018, T019)
Group 7 (Validation US2): T020, T021, T022, +parallel(T023, T024)
Group 8 (Resilience US3): T025, +parallel(T026, T027), T028, T029, T030
Group 9 (Tests US3):    +parallel(T031, T032)
Group 10 (Final):       T033, T034, T035, T036, +parallel(T037-T041)
```

---

## Success Criteria

✅ **All Tasks Complete When**:

1. All 41 tasks marked as [x] (completed)
2. All tests passing: `mvn clean test` → 100% pass rate
3. All spec acceptance scenarios verified
4. No architecture principle violations
5. Code reviewed and approved
6. README documentation updated
7. Feature branch merged to main

✅ **Code Quality Gates**:

- No TODOs or debug code left
- Test coverage > 80% for service and controller layers
- All exceptions properly wrapped and logged
- No external API models visible above client layer
- All user inputs validated
- All API errors return appropriate HTTP status codes

✅ **Specification Compliance**:

- SC-001: Response time < 500ms (mocked tests)
- SC-002: 100% of fields returned (verified in T036)
- SC-003: Invalid city returns HTTP 404 (verified in T024)
- SC-004: API failures handled gracefully (verified in T032)
- SC-005: Layered architecture verified (verified in T038)
- SC-006: All tests independent, no real API calls (verified in T035)
- SC-007: All code traceable to spec (verified in T036)

---

**Grand Total**: 41 tasks organized in 7 phases
**Estimated Duration**: 3-4 days for one developer (or 1-2 days for two developers with parallel execution)
**Next Action**: Begin Phase 1 (Setup) - Start with T001
