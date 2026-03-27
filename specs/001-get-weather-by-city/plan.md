# Implementation Plan: Weather Dashboard – Current Weather by City

**Branch**: `001-get-weather-by-city` | **Date**: 2026-03-11 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-get-weather-by-city/spec.md`

## Summary

Implement a REST API endpoint that allows users to retrieve current weather information for a given city. The backend will call a third-party weather API, normalize the response into an internal DTO, and return the data with proper error handling. The implementation follows a clean layered architecture (Controller → Service → External API Client → DTO) with clear separation of concerns, dependency injection, and mocked API capability for testing.

## Technical Context

**Language/Version**: Java 17+  
**Primary Dependencies**: Spring Boot 3.5.x, Spring Web (RestTemplate or WebClient), Mockito (testing)  
**Storage**: N/A (stateless API, no persistence required)  
**Testing**: JUnit 5 (Jupiter), Mockito (mocking external API), Spring Boot Test  
**Target Platform**: Linux/macOS server (REST API service)  
**Project Type**: Web service (REST API backend)  
**Performance Goals**: < 500ms average response time (when external API responds normally)  
**Constraints**: External API dependency must be mockable for testing; API keys must be environment-based  
**Scale/Scope**: Single feature, single endpoint, supports any city in the chosen weather API

## Constitution Check

✅ **Principle I - Spec-First Development**

- Specification is comprehensive with three prioritized user stories
- All acceptance criteria are defined and testable
- No implementation started before spec approval

✅ **Principle II - Clean Layered Architecture**

- Controller layer: HTTP routing, request validation, response formatting (no business logic)
- Service layer: Validation, business rules, service exception wrapping
- API Client layer: External API communication, response mapping to internal DTOs
- DTO layer: Internal immutable data structures

✅ **Principle III - External API Isolation**

- WeatherApiClient interface abstracts external API communication
- External API response classes never visible to controller or service layers
- All mapping happens in the client layer
- Service receives only DTOs, never external API models

✅ **Principle IV - Testable by Design**

- WeatherApiClient is mockable via interface injection
- Services can be tested in isolation with mocked API client
- Controllers can be integration-tested with mocked API client
- No real external API calls in automated tests

✅ **Principle V - Simplicity & Incremental**

- Single endpoint, single responsibility
- Minimal abstractions: one service, one client, one DTO type
- No caching, no advanced features (forecasts, history, auth)
- Each user story is independently implementable

## Architecture Design

### Layered Architecture Overview

The implementation follows the constitutional requirement for clean layered architecture:

```
HTTP Request
    ↓
WeatherController (HTTP layer)
    ├─ Validates city name (empty check)
    ├─ Calls WeatherService
    └─ Maps service responses to HTTP responses
    ↓
WeatherService (Business Logic layer)
    ├─ Validates input (call to WeatherApiClient)
    ├─ Handles service-level errors
    └─ Wraps external API errors in WeatherServiceException
    ↓
WeatherApiClient (Integration layer) - interface injected
    ├─ Calls external weather API (RestTemplate or WebClient)
    ├─ Maps external API response to internal WeatherResponse DTO
    └─ Wraps API errors, timeouts, parsing errors
    ↓
WeatherResponse DTO (Data Model layer)
    └─ Immutable (record type preferred)

HTTP Response (200/400/404/503/504)
```

### Component Responsibilities

#### WeatherController

- **Responsibility**: Handle HTTP requests and responses
- **Methods**:
  - `GET /weather/city/{cityName}` → calls `WeatherService.getWeatherByCity(cityName)`
- **Error Handling**: Catches `WeatherServiceException`, maps to appropriate HTTP status
- **NO**: Business logic, direct API calls, external API model classes

#### WeatherService

- **Responsibility**: Business logic and service coordination
- **Methods**:
  - `getWeatherByCity(String cityName)` → returns `WeatherResponse`
- **Validation**: Non-empty city name check (returns error if blank)
- **Coordination**: Calls injected `WeatherApiClient.getWeather(cityName)`
- **Error Handling**: Catches client-layer exceptions, wraps in `WeatherServiceException`
- **NO**: Direct HTTP calls, external API model classes, HTTP concerns

#### WeatherApiClient (Interface)

- **Responsibility**: Abstract external API communication
- **Methods**:
  - `WeatherResponse getWeather(String cityName)`
- **Implementation** (WeatherApiClientImpl):
  - Uses RestTemplate or WebClient to call external weather API
  - Maps external API response to internal `WeatherResponse` DTO
  - Handles API errors: timeouts, 5xx errors, parsing errors
  - Returns internal DTOs only (never exposes external API models)
- **Error Handling**: Wraps in `ExternalApiException` (caught by service)

#### WeatherResponse (DTO)

- **Responsibility**: Represent internal weather data
- **Fields**:
  - `cityName` (String)
  - `temperature` (BigDecimal, Celsius)
  - `condition` (String or Enum: CLEAR, CLOUDY, RAINY, etc.)
  - `humidity` (Integer, percentage 0-100)
  - `windSpeed` (BigDecimal, km/h)
- **Type**: Use Java Record (immutable by design)
- **NO**: External API response fields or property names

### Dependency Injection

All dependencies must be injected via constructor (Spring `@Autowired` or `@RequiredArgsConstructor`):

```
WeatherController
  └─ @Autowired WeatherService

WeatherService
  └─ @Autowired WeatherApiClient (interface, not impl)

WeatherApiClient implementation bean (RestTemplate or WebClient)
```

**Why**: Enables mocking in tests without calling real implementation classes.

## API Design

### REST Endpoint

**Endpoint**: `GET /weather/city/{cityName}`

**HTTP Method**: GET  
**Path Parameter**: `cityName` (string, required, max 100 characters)  
**Query Parameters**: None  
**Request Headers**: No special headers required  
**Request Body**: None

### Request Validation

| Condition                    | Status                   | Response                                                 |
| ---------------------------- | ------------------------ | -------------------------------------------------------- |
| City name is empty or null   | 400                      | `{"error": "City name is required and cannot be empty"}` |
| City name exceeds max length | 400                      | `{"error": "City name must be 100 characters or less"}`  |
| Valid city name provided     | 200 or error codes below | See response section                                     |

### Success Response (HTTP 200)

**Content-Type**: `application/json`

```json
{
  "cityName": "London",
  "temperature": 8.5,
  "condition": "CLOUDY",
  "humidity": 72,
  "windSpeed": 12.3
}
```

**Response Fields**:

- `cityName` (string): Name of the city (as returned by weather API)
- `temperature` (number): Current temperature in Celsius
- `condition` (string): Weather condition (CLEAR, CLOUDY, RAINY, SNOWY, STORMY, etc.)
- `humidity` (integer): Humidity percentage (0-100)
- `windSpeed` (number): Wind speed in km/h

### Error Responses

#### 400 Bad Request (Validation Error)

```json
{
  "error": "City name is required and cannot be empty"
}
```

**Triggered by**: Empty city name, blank city name, city name exceeds max length

#### 404 Not Found (City Not Found)

```json
{
  "error": "City not found: 'InvalidCityName'"
}
```

**Triggered by**: External weather API returns 404 for the requested city

#### 503 Service Unavailable (External API Down)

```json
{
  "error": "Weather service is temporarily unavailable. Please try again later."
}
```

**Triggered by**: External weather API returns 5xx error or is unreachable

#### 504 Gateway Timeout (Request Timeout)

```json
{
  "error": "Request to weather service timed out. Please try again."
}
```

**Triggered by**: Request to external weather API exceeds timeout threshold (e.g., 5 seconds)

#### 500 Internal Server Error (Unexpected Error)

```json
{
  "error": "An unexpected error occurred. Please try again later."
}
```

**Triggered by**: Malformed API response, unparseable JSON, unexpected data types, other unexpected conditions

## Data Models

### WeatherResponse DTO (Internal Model)

```java
public record WeatherResponse(
    String cityName,
    BigDecimal temperature,
    String condition,
    Integer humidity,
    BigDecimal windSpeed
) {
    // Constructor validation could be added in a custom constructor if needed
}
```

**Rationale**:

- Record type ensures immutability
- BigDecimal for temperature and wind speed (floating-point precision)
- Integer for humidity (percentage, no decimal needed)
- String for condition (can be ENUM or String; see implementation notes)
- Simple and minimal, no external API types

### WeatherServiceException (Custom Exception)

```java
public class WeatherServiceException extends RuntimeException {
    private final String errorCode; // e.g., "CITY_NOT_FOUND", "API_UNAVAILABLE", "API_TIMEOUT"

    public WeatherServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

**Rationale**:

- Wraps external API exceptions at service layer
- Allows controller to map error codes to HTTP status
- Provides clean separation between external and internal error handling

### External API Response Model (Example: OpenWeatherMap)

**Example**: If using OpenWeatherMap, the external API might return:

```json
{
  "main": {
    "temp": 8.5,
    "humidity": 72
  },
  "weather": [
    {
      "main": "Clouds"
    }
  ],
  "wind": {
    "speed": 12.3
  },
  "name": "London"
}
```

**Note**: This external model is ONLY visible in the WeatherApiClient implementation. It is never imported or referenced by the service or controller.

## External API Integration

### HTTP Client Technology

**Option A: RestTemplate** (Traditional, stable)

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

**Option B: WebClient** (Modern, reactive-friendly)

```java
@Bean
public WebClient webClient() {
    return WebClient.create();
}
```

**Decision**: Use RestTemplate for simplicity (learning project); switch to WebClient if scaling is needed later.

### API Configuration

**Environment Variables** (set in `.env` or system environment):

```
WEATHER_API_KEY=your_api_key_here
WEATHER_API_BASE_URL=https://api.openweathermap.org/data/2.5
WEATHER_API_TIMEOUT_MS=5000
```

**Spring Configuration Class**:

```java
@Configuration
public class WeatherApiConfig {
    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.baseUrl}")
    private String baseUrl;

    @Value("${weather.api.timeoutMs:5000}")
    private long timeoutMs;

    @Bean
    public WeatherApiClient weatherApiClient(RestTemplate restTemplate) {
        return new WeatherApiClientImpl(restTemplate, apiKey, baseUrl, timeoutMs);
    }
}
```

**application.yaml/properties**:

```yaml
weather:
  api:
    key: ${WEATHER_API_KEY}
    baseUrl: ${WEATHER_API_BASE_URL}
    timeoutMs: ${WEATHER_API_TIMEOUT_MS:5000}
```

### Response Mapping Strategy

**Flow**:

1. WeatherApiClient calls external API: `/weather?q=London&appid=KEY`
2. Receives external API response (OpenWeatherMap format or similar)
3. **Immediately map to internal DTO**:
   - Extract relevant fields from external response
   - Convert external condition name (e.g., "Clouds") to internal enum/string (e.g., "CLOUDY")
   - Return only `WeatherResponse` type
4. Service receives `WeatherResponse` (never sees external API model)

**Mapping Implementation** (in client layer):

```java
private WeatherResponse mapToInternal(ExternalWeatherApiResponse external) {
    return new WeatherResponse(
        external.getName(),                    // cityName
        new BigDecimal(external.getMain().getTemp()),  // temperature
        mapCondition(external.getWeather()[0].getMain()),  // condition
        external.getMain().getHumidity(),     // humidity
        new BigDecimal(external.getWind().getSpeed())  // windSpeed
    );
}

private String mapCondition(String externalCondition) {
    return switch (externalCondition.toUpperCase()) {
        case "CLEAR" -> "CLEAR";
        case "CLOUDS" -> "CLOUDY";
        case "RAIN" -> "RAINY";
        case "SNOW" -> "SNOWY";
        case "THUNDERSTORM" -> "STORMY";
        case "MIST", "SMOKE", "DUST", "FOG" -> "FOGGY";
        default -> "UNKNOWN";
    };
}
```

## Error Handling

### By Layer

#### Service Layer (WeatherService)

```java
public WeatherResponse getWeatherByCity(String cityName) {
    // Input validation
    if (cityName == null || cityName.isBlank()) {
        throw new WeatherServiceException(
            "City name is required and cannot be empty",
            "INVALID_INPUT",
            null
        );
    }

    try {
        return weatherApiClient.getWeather(cityName);
    } catch (ExternalApiException e) {
        if (e.getStatusCode() == 404) {
            throw new WeatherServiceException(
                "City not found: '" + cityName + "'",
                "CITY_NOT_FOUND",
                e
            );
        } else if (e.getStatusCode() >= 500) {
            throw new WeatherServiceException(
                "Weather service is temporarily unavailable",
                "API_UNAVAILABLE",
                e
            );
        } else if (e.getCause() instanceof SocketTimeoutException) {
            throw new WeatherServiceException(
                "Request to weather service timed out",
                "API_TIMEOUT",
                e
            );
        } else {
            throw new WeatherServiceException(
                "An unexpected error occurred",
                "UNEXPECTED_ERROR",
                e
            );
        }
    }
}
```

#### Controller Layer (WeatherController)

```java
@GetMapping("/weather/city/{cityName}")
public ResponseEntity<WeatherResponse> getWeatherByCity(@PathVariable String cityName) {
    try {
        WeatherResponse response = weatherService.getWeatherByCity(cityName);
        return ResponseEntity.ok(response);
    } catch (WeatherServiceException e) {
        HttpStatus status = mapErrorCodeToStatus(e.getErrorCode());
        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(e.getMessage()));
    }
}

private HttpStatus mapErrorCodeToStatus(String errorCode) {
    return switch (errorCode) {
        case "INVALID_INPUT" -> HttpStatus.BAD_REQUEST;
        case "CITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
        case "API_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
        case "API_TIMEOUT" -> HttpStatus.GATEWAY_TIMEOUT;
        default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
}
```

#### Client Layer (WeatherApiClient)

```java
public WeatherResponse getWeather(String cityName) {
    try {
        String url = String.format("%s/weather?q=%s&appid=%s", baseUrl, cityName, apiKey);
        ResponseEntity<ExternalWeatherApiResponse> response = restTemplate.getForEntity(
            url,
            ExternalWeatherApiResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new ExternalApiException("Invalid response", response.getStatusCodeValue());
        }

        return mapToInternal(response.getBody());

    } catch (HttpClientErrorException | HttpServerErrorException e) {
        logger.warn("Weather API error: {} - {}", e.getStatusCode(), e.getMessage());
        throw new ExternalApiException(e.getMessage(), e.getStatusCode().value(), e);

    } catch (ResourceAccessException e) {
        // Network error, timeout, etc.
        Throwable cause = e.getCause();
        if (cause instanceof SocketTimeoutException) {
            logger.warn("Weather API timeout");
            throw new ExternalApiException("API request timeout", 0, e);
        }
        logger.warn("Weather API network error: {}", e.getMessage());
        throw new ExternalApiException(e.getMessage(), 0, e);

    } catch (Exception e) {
        logger.error("Unexpected error calling weather API", e);
        throw new ExternalApiException("Unexpected error: " + e.getMessage(), 0, e);
    }
}
```

### Error Mapping Summary

| External Condition               | Exception Type          | Service Error Code | HTTP Status | Message                       |
| -------------------------------- | ----------------------- | ------------------ | ----------- | ----------------------------- |
| Empty city name                  | WeatherServiceException | INVALID_INPUT      | 400         | "City name is required"       |
| Invalid city name format         | WeatherServiceException | INVALID_INPUT      | 400         | "Invalid city name format"    |
| City not found (API returns 404) | WeatherServiceException | CITY_NOT_FOUND     | 404         | "City not found: 'name'"      |
| API returns 5xx                  | WeatherServiceException | API_UNAVAILABLE    | 503         | "Weather service unavailable" |
| Network timeout                  | WeatherServiceException | API_TIMEOUT        | 504         | "Request timed out"           |
| Malformed API response           | WeatherServiceException | UNEXPECTED_ERROR   | 500         | "Unexpected error occurred"   |

## Testing Strategy

### Unit Tests (WeatherService)

**Class**: `WeatherServiceTest`

```java
class WeatherServiceTest {
    private WeatherService service;
    private WeatherApiClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(WeatherApiClient.class);
        service = new WeatherService(mockClient);
    }

    // P1: Retrieve Current Weather by City Name
    @Test
    void testGetWeatherByCity_validCity_returnsWeatherResponse() {
        // Given
        String cityName = "London";
        WeatherResponse mockResponse = new WeatherResponse("London",
            new BigDecimal("8.5"), "CLOUDY", 72, new BigDecimal("12.3"));
        when(mockClient.getWeather(cityName)).thenReturn(mockResponse);

        // When
        WeatherResponse result = service.getWeatherByCity(cityName);

        // Then
        assertThat(result).isEqualTo(mockResponse);
        verify(mockClient).getWeather(cityName);
    }

    @Test
    void testGetWeatherByCity_repeatedQueries_fetchesFreshData() {
        // Given
        when(mockClient.getWeather("London"))
            .thenReturn(new WeatherResponse("London", new BigDecimal("8.5"),
                "CLOUDY", 72, new BigDecimal("12.3")))
            .thenReturn(new WeatherResponse("London", new BigDecimal("9.0"),
                "CLOUDY", 71, new BigDecimal("12.5")));

        // When
        WeatherResponse first = service.getWeatherByCity("London");
        WeatherResponse second = service.getWeatherByCity("London");

        // Then
        assertThat(first.temperature()).isEqualTo(new BigDecimal("8.5"));
        assertThat(second.temperature()).isEqualTo(new BigDecimal("9.0"));
        verify(mockClient, times(2)).getWeather("London");
    }

    // P2: Handle Invalid/Not Found Cities
    @Test
    void testGetWeatherByCity_emptyCity_throwsException() {
        // When/Then
        assertThrows(WeatherServiceException.class, () -> service.getWeatherByCity(""));
    }

    @Test
    void testGetWeatherByCity_cityNotFound_returns404() {
        // Given
        when(mockClient.getWeather("InvalidCity"))
            .thenThrow(new ExternalApiException("Not found", 404));

        // When/Then
        WeatherServiceException ex = assertThrows(WeatherServiceException.class,
            () -> service.getWeatherByCity("InvalidCity"));
        assertThat(ex.getErrorCode()).isEqualTo("CITY_NOT_FOUND");
    }

    // P3: Handle Weather API Failures
    @Test
    void testGetWeatherByCity_apiUnavailable_returns503() {
        // Given
        when(mockClient.getWeather("London"))
            .thenThrow(new ExternalApiException("Internal error", 500));

        // When/Then
        WeatherServiceException ex = assertThrows(WeatherServiceException.class,
            () -> service.getWeatherByCity("London"));
        assertThat(ex.getErrorCode()).isEqualTo("API_UNAVAILABLE");
    }

    @Test
    void testGetWeatherByCity_timeout_returns504() {
        // Given
        when(mockClient.getWeather("London"))
            .thenThrow(new ExternalApiException("Timeout", 0, new SocketTimeoutException()));

        // When/Then
        WeatherServiceException ex = assertThrows(WeatherServiceException.class,
            () -> service.getWeatherByCity("London"));
        assertThat(ex.getErrorCode()).isEqualTo("API_TIMEOUT");
    }
}
```

### Integration Tests (WeatherController)

**Class**: `WeatherControllerTest`

```java
@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherApiClient mockApiClient;

    // P1: Valid City Query
    @Test
    void testGetWeatherByCity_validCity_returns200() throws Exception {
        // Given
        WeatherResponse response = new WeatherResponse("London",
            new BigDecimal("8.5"), "CLOUDY", 72, new BigDecimal("12.3"));
        when(mockApiClient.getWeather("London")).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/weather/city/London"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cityName").value("London"))
            .andExpect(jsonPath("$.temperature").value(8.5))
            .andExpect(jsonPath("$.condition").value("CLOUDY"))
            .andExpect(jsonPath("$.humidity").value(72))
            .andExpect(jsonPath("$.windSpeed").value(12.3));
    }

    // P2: Invalid/Not Found Cities
    @Test
    void testGetWeatherByCity_emptyCity_returns400() throws Exception {
        mockMvc.perform(get("/weather/city/"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetWeatherByCity_cityNotFound_returns404() throws Exception {
        // Given
        when(mockApiClient.getWeather("InvalidCity"))
            .thenThrow(new ExternalApiException("Not found", 404));

        // When/Then
        mockMvc.perform(get("/weather/city/InvalidCity"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }

    // P3: API Failures
    @Test
    void testGetWeatherByCity_apiUnavailable_returns503() throws Exception {
        // Given
        when(mockApiClient.getWeather("London"))
            .thenThrow(new ExternalApiException("Service error", 500));

        // When/Then
        mockMvc.perform(get("/weather/city/London"))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void testGetWeatherByCity_timeout_returns504() throws Exception {
        // Given
        when(mockApiClient.getWeather("London"))
            .thenThrow(new ExternalApiException("Timeout", 0,
                new SocketTimeoutException()));

        // When/Then
        mockMvc.perform(get("/weather/city/London"))
            .andExpect(status().isGatewayTimeout());
    }
}
```

### Mocking Strategy

- **WeatherApiClient** is injected as an interface (never use the implementation directly in tests)
- Use `@MockBean` in integration tests to replace the real client
- Unit tests create a plain mock via `mock(WeatherApiClient.class)`
- **No real HTTP calls** to external weather API in any automated test
- Use Mockito's `when().thenReturn()` and `when().thenThrow()` for all scenarios

## Configuration Management

### Environment Variables

Create `.env` file (not committed to git) in project root or set in CI/CD:

```bash
WEATHER_API_KEY=your_openweathermap_api_key
WEATHER_API_BASE_URL=https://api.openweathermap.org/data/2.5
WEATHER_API_TIMEOUT_MS=5000
```

### Spring Boot Properties File

**`application.yaml`** (or `application.properties`):

```yaml
spring:
  application:
    name: weather-dashboard

weather:
  api:
    key: ${WEATHER_API_KEY:} # Default empty if not set
    baseUrl: ${WEATHER_API_BASE_URL:https://api.openweathermap.org/data/2.5}
    timeoutMs: ${WEATHER_API_TIMEOUT_MS:5000}
```

### Configuration Class

```java
@Configuration
@ConfigurationProperties(prefix = "weather.api")
public class WeatherApiProperties {
    private String key;
    private String baseUrl;
    private long timeoutMs = 5000;

    // Getters and setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
}
```

### Runtime Configuration

Spring will automatically load environment variables and `.env` files if using a library like `spring-dotenv` or manually configured.

## Key Implementation Notes

### DTO Mapping: Do It in the Client Layer Only

✅ **Correct**:

```java
// In WeatherApiClientImpl
private WeatherResponse mapToInternal(ExternalWeatherResponse external) {
    return new WeatherResponse(...);
}
```

❌ **Incorrect**:

```java
// In WeatherService (business logic should not map)
WeatherResponse response = externalResponse; // Never!
```

### Dependency Injection: Always Use Interfaces

✅ **Correct**:

```java
@Service
public class WeatherService {
    private final WeatherApiClient client; // Interface

    public WeatherService(WeatherApiClient client) {
        this.client = client;
    }
}
```

❌ **Incorrect**:

```java
@Service
public class WeatherService {
    private final WeatherApiClientImpl client; // Implementation class

    public WeatherService(WeatherApiClientImpl client) {
        this.client = client;
    }
}
```

### Exception Handling: Wrap at Each Boundary

✅ **Correct**:

```java
// Client layer wraps external exceptions
try {
    restTemplate.get(...);
} catch (HttpStatusCodeException e) {
    throw new ExternalApiException(..., e);
}

// Service layer further wraps client exceptions
try {
    client.getWeather(...);
} catch (ExternalApiException e) {
    throw new WeatherServiceException(..., e);
}

// Controller layer maps to HTTP status
catch (WeatherServiceException e) {
    return ResponseEntity.status(mapToHttpStatus(e.getErrorCode()))...
}
```

### Testing: No Real API Calls

✅ **Correct**:

```java
@MockBean
private WeatherApiClient mockClient;

when(mockClient.getWeather("London")).thenReturn(mockResponse);
```

❌ **Incorrect**:

```java
@Autowired
private WeatherApiClient realClient; // Will call real API!
```

## Summary of Changes

### New Files to Create

1. **Controller**: `src/main/java/com/weatherdashboard/api/controller/WeatherController.java`
2. **Service**: `src/main/java/com/weatherdashboard/api/service/WeatherService.java`
3. **API Client (Interface)**: `src/main/java/com/weatherdashboard/api/client/WeatherApiClient.java`
4. **API Client (Implementation)**: `src/main/java/com/weatherdashboard/api/client/WeatherApiClientImpl.java`
5. **DTOs**: `src/main/java/com/weatherdashboard/api/dto/WeatherResponse.java`
6. **Exceptions**: `src/main/java/com/weatherdashboard/api/exception/WeatherServiceException.java`, `ExternalApiException.java`
7. **Configuration**: `src/main/java/com/weatherdashboard/api/config/WeatherApiConfig.java`
8. **Tests**: `src/test/java/.../WeatherServiceTest.java`, `WeatherControllerTest.java`
9. **Config File**: `application.yaml` (update with weather API properties)

### New Directories

```
src/main/java/com/weatherdashboard/
├── api/
│   ├── controller/
│   ├── service/
│   ├── client/
│   ├── dto/
│   ├── exception/
│   └── config/
└── (other existing structure)

src/test/java/com/weatherdashboard/
├── api/
│   ├── controller/
│   └── service/
```

## Traceability to Specification

All functional requirements and user stories map to implementation components:

| Spec Element                       | Implementation Component                                             |
| ---------------------------------- | -------------------------------------------------------------------- |
| FR-001: REST endpoint              | WeatherController.getWeatherByCity()                                 |
| FR-002: Call third-party API       | WeatherApiClientImpl.getWeather()                                    |
| FR-003: Return normalized DTO      | WeatherResponse record                                               |
| FR-004: Map third-party response   | WeatherApiClientImpl.mapToInternal()                                 |
| FR-005: Validate city name         | WeatherService.getWeatherByCity() + WeatherController                |
| FR-006: Handle not found           | ExternalApiException catch, map to 404                               |
| FR-007: Handle API errors          | ExternalApiException → WeatherServiceException → HTTP status mapping |
| FR-008: Logging                    | Logger in WeatherApiClientImpl and WeatherService                    |
| FR-009: Dependency injection       | All layers use constructor injection                                 |
| P1 Story: Valid city query         | All components working together                                      |
| P2 Story: Invalid/not found cities | Exception handling in service + controller                           |
| P3 Story: API failures             | Exception wrapping, timeout handling, error status codes             |

---

**Plan Status**: Ready for task breakdown  
**Next Phase**: Generate `tasks.md` with specific, actionable implementation tasks grouped by user story
