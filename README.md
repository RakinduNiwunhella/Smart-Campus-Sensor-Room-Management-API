# Smart Campus — Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**University of Westminster — School of Computer Science and Engineering**

| | |
|---|---|
| **Name** | Rakindu Niwunhella |
| **UOW ID** | w2119870 |
| **IIT ID** | 20240426 |

---

## API Design Overview

The Smart Campus API is a **resource-oriented RESTful web service** built with JAX-RS (Jersey 2.41) that models the physical structure of a university campus as a hierarchy of HTTP resources.

**Resource Hierarchy:**
```
/api/v1                          ← Discovery & HATEOAS root
/api/v1/rooms                    ← Room collection
/api/v1/rooms/{roomId}           ← Individual room
/api/v1/sensors                  ← Sensor collection (filterable by ?type=)
/api/v1/sensors/{sensorId}       ← Individual sensor
/api/v1/sensors/{sensorId}/readings  ← Sub-resource: reading history
```

**Key design decisions:**
- **Versioned base path** (`/api/v1`) allows future non-breaking evolution of the API
- **HATEOAS discovery endpoint** at the root so clients never hardcode resource URLs
- **Sub-resource locator pattern** delegates reading history to a dedicated `SensorReadingResource` class, keeping resource classes single-responsibility
- **Centralised exception mappers** (`@Provider`) translate every business exception into a consistent `{status, error, message}` JSON body — no raw stack traces ever reach the client
- **Thread-safe singleton `DataStore`** using `ConcurrentHashMap` and double-checked locking, necessary because JAX-RS creates a new resource instance per request
- **Referential integrity** enforced at write time: a sensor cannot be created without a valid `roomId`, and a room cannot be deleted while sensors are assigned to it

---

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Project Structure](#2-project-structure)
3. [Getting Started](#3-getting-started)
4. [Sample curl Commands](#4-sample-curl-commands)
5. [API Reference](#5-api-reference)
   - [Discovery](#51-discovery)
   - [Rooms](#52-room-management)
   - [Sensors](#53-sensor-operations)
   - [Sensor Readings](#54-sensor-readings-sub-resource)
6. [Data Models](#6-data-models)
7. [Error Handling](#7-error-handling)
8. [Seed Data](#8-seed-data)
9. [Coursework Q&A](#9-coursework-qa)
   - [Part 1 – Service Architecture & Setup](#part-1--service-architecture--setup)
   - [Part 2 – Room Management](#part-2--room-management)
   - [Part 3 – Sensor Operations & Linking](#part-3--sensor-operations--linking)
   - [Part 4 – Deep Nesting with Sub-Resources](#part-4--deep-nesting-with-sub-resources)
   - [Part 5 – Advanced Error Handling & Logging](#part-5--advanced-error-handling--logging)

---

## 1. Technology Stack

| Component | Details |
|-----------|---------|
| Language | Java 11 |
| Framework | Jersey 2.41 (JAX-RS 2.1) |
| Servlet Container | Apache Tomcat 9 |
| Build Tool | Maven |
| JSON Serialisation | Jackson (via `jersey-media-json-jackson`) |
| Dependency Injection | HK2 (Jersey built-in) |
| Data Persistence | In-memory `ConcurrentHashMap` (Singleton) |
| Packaging | WAR |
| API Base Path | `/api/v1` |

---

## 2. Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── SmartCampusApplication.java       # JAX-RS entry point (@ApplicationPath)
    ├── filter/
    │   └── LoggingFilter.java            # Request + response logging
    ├── exception/
    │   ├── ErrorResponse.java            # Uniform error DTO
    │   ├── GlobalExceptionMapper.java    # 500 catch-all
    │   ├── LinkedResourceNotFoundException.java        # 422
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── RoomNotEmptyException.java                  # 409
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── SensorUnavailableException.java             # 403
    │   └── SensorUnavailableExceptionMapper.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── resource/
    │   ├── DiscoveryResource.java        # GET /api/v1
    │   ├── RoomResource.java             # /api/v1/rooms
    │   ├── SensorResource.java           # /api/v1/sensors
    │   └── SensorReadingResource.java    # /api/v1/sensors/{id}/readings
    └── store/
        └── DataStore.java                # Thread-safe singleton data store
```

---

## 3. Getting Started

### Prerequisites

- [NetBeans IDE 17+](https://netbeans.apache.org/) (with Java SE and Web bundles)
- Java JDK 11 (set as the platform in NetBeans)
- Apache Tomcat 9 registered in NetBeans as a Server

### 1. Register Tomcat in NetBeans

1. Go to **Tools → Servers → Add Server**
2. Select **Apache Tomcat or TomEE** and click **Next**
3. Set the **Server Location** to your Tomcat 9 installation directory
4. Complete the wizard and click **Finish**

### 2. Open the Project

1. Go to **File → Open Project**
2. Navigate to the `smart-campus-api` folder and select it
3. NetBeans detects it as a Maven project — click **Open Project**
4. Wait for NetBeans to resolve dependencies (progress shown in the bottom bar)

### 3. Set the Server

1. Right-click the project in the **Projects** panel → **Properties**
2. Go to **Run** category
3. Set **Server** to the Tomcat 9 instance registered in step 1
4. Leave **Context Path** as `/smart-campus-api`
5. Click **OK**

### 4. Clean and Build

1. Right-click the project in the **Projects** panel → **Clean and Build**
2. Wait for the `BUILD SUCCESS` message in the Output window
3. This produces the deployable WAR file under `target/`

### 5. Run the Project

- Right-click the project → **Run**, or click the green **Run** button (▶), or press **F6**
- NetBeans deploys the WAR to Tomcat and opens a browser tab automatically

### 6. Verify

Open your browser or an API client (e.g. Postman) and navigate to:

```
http://localhost:8080/Smart-Campus-API/api/v1
```

Expected response:

```json
{
  "name": "Smart Campus Sensor & Room Management API",
  "version": "1.0",
  "description": "RESTful API for managing campus rooms and IoT sensors.",
  "contact": "admin@smartcampus.ac.uk",
  "resources": {
    "self": "/api/v1",
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---

## 4. Sample curl Commands

All commands assume the server is running at `http://localhost:8080/Smart-Campus-API`. The API ships with seed data so every GET command works immediately after startup.

### 1. Discover the API root

```bash
curl -X GET http://localhost:8080/Smart-Campus-API/api/v1
```

### 2. List all rooms

```bash
curl -X GET http://localhost:8080/Smart-Campus-API/api/v1/rooms
```

### 3. Create a new room

```bash
curl -X POST http://localhost:8080/Smart-Campus-API/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "HALL-02", "name": "Seminar Room B", "capacity": 40}'
```

### 4. Register a new sensor in an existing room

```bash
curl -X POST http://localhost:8080/Smart-Campus-API/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-002", "type": "Temperature", "status": "ACTIVE", "currentValue": 21.0, "roomId": "HALL-01"}'
```

### 5. Filter sensors by type

```bash
curl -X GET "http://localhost:8080/Smart-Campus-API/api/v1/sensors?type=Temperature"
```

### 6. Post a new reading for a sensor

```bash
curl -X POST http://localhost:8080/Smart-Campus-API/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 24.7}'
```

### 7. Retrieve reading history for a sensor

```bash
curl -X GET http://localhost:8080/Smart-Campus-API/api/v1/sensors/TEMP-001/readings
```

### 8. Delete a room with no sensors

```bash
curl -X DELETE http://localhost:8080/Smart-Campus-API/api/v1/rooms/HALL-01
```

### 9. Attempt to delete a room that has sensors (expect 409 Conflict)

```bash
curl -X DELETE http://localhost:8080/Smart-Campus-API/api/v1/rooms/LIB-301
```

---

## 5. API Reference

All endpoints consume and produce `application/json`. Base URL: `http://localhost:8080/Smart-Campus-API/api/v1`

### 5.1 Discovery

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/v1` | Returns API metadata and HATEOAS navigation links | `200 OK` |

---

### 5.2 Room Management

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/v1/rooms` | List all rooms | `200 OK` |
| `POST` | `/api/v1/rooms` | Create a new room | `201 Created` |
| `GET` | `/api/v1/rooms/{roomId}` | Get a specific room by ID | `200 OK` |
| `DELETE` | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors are assigned) | `204 No Content` |

**POST /rooms — Request Body**

```json
{
  "id": "HALL-02",
  "name": "Seminar Room B",
  "capacity": 40
}
```

**Constraints**

- `id` is required → `400 Bad Request` if missing
- Duplicate `id` → `409 Conflict`
- DELETE on a room with sensors → `409 Conflict`
- DELETE on a non-existent room → `404 Not Found`

---

### 5.3 Sensor Operations

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/v1/sensors` | List all sensors | `200 OK` |
| `GET` | `/api/v1/sensors?type={type}` | Filter sensors by type (e.g. `CO2`, `Temperature`) | `200 OK` |
| `POST` | `/api/v1/sensors` | Register a new sensor | `201 Created` |
| `GET` | `/api/v1/sensors/{sensorId}` | Get a specific sensor by ID | `200 OK` |

**POST /sensors — Request Body**

```json
{
  "id": "TEMP-002",
  "type": "Temperature",
  "status": "ACTIVE",
  "currentValue": 21.0,
  "roomId": "HALL-01"
}
```

**Constraints**

- `id` is required → `400 Bad Request` if missing
- `roomId` must reference an existing room → `422 Unprocessable Entity`
- Duplicate `id` → `409 Conflict`
- `status` defaults to `"ACTIVE"` if omitted
- Valid statuses: `ACTIVE`, `MAINTENANCE`, `OFFLINE`

---

### 5.4 Sensor Readings (Sub-Resource)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `GET` | `/api/v1/sensors/{sensorId}/readings` | Retrieve full reading history | `200 OK` |
| `POST` | `/api/v1/sensors/{sensorId}/readings` | Append a new reading | `201 Created` |

**POST /readings — Request Body**

```json
{
  "value": 23.4
}
```

> `id` (UUID) and `timestamp` (epoch ms) are auto-generated by the server if omitted.

**Side-effects on POST**

- Updates the parent `Sensor.currentValue` to the new reading's `value`.

**Constraints**

- Sensor must be in `ACTIVE` status → `403 Forbidden` if `MAINTENANCE` or `OFFLINE`
- Sensor must exist → `404 Not Found`

---

## 6. Data Models

### Room

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | `String` | Yes | Unique identifier, e.g. `"LIB-301"` |
| `name` | `String` | No | Human-readable label |
| `capacity` | `int` | No | Maximum occupancy |
| `sensorIds` | `List<String>` | Auto | IDs of sensors deployed in this room |

### Sensor

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | `String` | Yes | Unique identifier, e.g. `"TEMP-001"` |
| `type` | `String` | No | `Temperature`, `CO2`, `Occupancy`, etc. |
| `status` | `String` | No | `ACTIVE` / `MAINTENANCE` / `OFFLINE` (default: `ACTIVE`) |
| `currentValue` | `double` | No | Most recent measurement |
| `roomId` | `String` | Yes | Foreign key — must reference an existing Room |

### SensorReading

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | `String` | No | UUID; auto-generated if omitted |
| `timestamp` | `long` | No | Epoch ms; auto-generated if omitted |
| `value` | `double` | Yes | Measured metric value |

---

## 7. Error Handling

All error responses share a consistent JSON body:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room 'LIB-301' cannot be deleted because it still has active sensors assigned to it."
}
```

| Scenario | Exception | HTTP Status |
|----------|-----------|-------------|
| Delete room with active sensors | `RoomNotEmptyException` | `409 Conflict` |
| Sensor references non-existent room | `LinkedResourceNotFoundException` | `422 Unprocessable Entity` |
| POST reading to unavailable sensor | `SensorUnavailableException` | `403 Forbidden` |
| Any unhandled runtime exception | `Throwable` (catch-all) | `500 Internal Server Error` |

Raw Java stack traces are **never** returned in HTTP responses. All exceptions are intercepted by `@Provider` ExceptionMappers; full stack traces are written to the server log only.

---

## 8. Seed Data

The `DataStore` pre-loads the following data so the API is immediately testable after deployment:

### Rooms

| ID | Name | Capacity |
|----|------|----------|
| `LIB-301` | Library Quiet Study | 50 |
| `LAB-101` | Computer Lab 1 | 30 |
| `HALL-01` | Main Hall | 200 (no sensors — can be safely deleted in demos) |

### Sensors

| ID | Type | Status | Current Value | Room |
|----|------|--------|---------------|------|
| `TEMP-001` | Temperature | `ACTIVE` | 22.5 °C | `LIB-301` |
| `CO2-001` | CO2 | `ACTIVE` | 412 ppm | `LIB-301` |
| `OCC-001` | Occupancy | `MAINTENANCE` | 0.0 | `LAB-101` |

> Data is held in-memory and resets on every server restart.

---

## 9. Coursework Q&A

### Part 1 – Service Architecture & Setup

#### Q1.1 – JAX-RS Resource Lifecycle & Impact on In-Memory State

> *Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? How does this impact managing in-memory data structures?*

By default, JAX-RS creates a **new resource class instance for every incoming HTTP request** (per-request scope). This is the JAX-RS specification default and Jersey follows it. The rationale is thread safety by isolation: each request gets its own object, so instance fields of a resource class are never shared between concurrent requests.

However, this means that any state stored as an instance field of a resource class (e.g., a `HashMap<>` field on `RoomResource`) would be **created fresh on every request and discarded immediately after** — making it impossible to persist data between calls.

**How this project addresses it:**  
All shared state lives in `DataStore`, a **Singleton** class outside the resource hierarchy. `DataStore.getInstance()` uses the double-checked locking pattern with a `volatile` field to ensure exactly one instance is created safely across threads:

```java
private static volatile DataStore instance;

public static DataStore getInstance() {
    if (instance == null) {
        synchronized (DataStore.class) {
            if (instance == null) {
                instance = new DataStore();
            }
        }
    }
    return instance;
}
```

Because multiple resource instances (from concurrent requests) all call `DataStore.getInstance()` and receive the same object, we need the underlying collections to be **thread-safe**. The store uses `ConcurrentHashMap` for rooms, sensors, and readings — a data structure that allows concurrent reads and fine-grained locking on writes, preventing race conditions and data corruption without blocking the entire map on every operation.

#### Q1.2 – HATEOAS and Its Benefits Over Static Documentation

> *Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers compared to static documentation?*

**HATEOAS** (Hypermedia as the Engine of Application State) is one of Roy Fielding's original REST constraints. It requires that API responses include navigational links so that clients can discover and traverse resources dynamically rather than relying on externally documented, hardcoded URLs.

In this project, `GET /api/v1` returns:

```json
{
  "resources": {
    "self":    "/api/v1",
    "rooms":   "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

**Why it is considered advanced:**

A truly RESTful API (Level 3 on the Richardson Maturity Model) is self-describing. The server advertises what is possible and where, rather than forcing the client to know the URL structure in advance.

**Benefits over static documentation:**

| Static Documentation | HATEOAS |
|----------------------|---------|
| Client hardcodes every URL | Client discovers URLs from responses |
| Breaks when the server restructures a path | Client follows new links transparently |
| Out-of-date docs cause integration failures | Server is always the single source of truth |
| Clients must read external docs to know valid next actions | Server communicates valid transitions inline |

In practice, HATEOAS reduces client-server coupling: when the API team changes a path from `/api/v1/rooms` to `/api/v2/spaces`, clients that follow links from the discovery endpoint adapt without code changes, whereas clients with hardcoded URLs break immediately.

### Part 2 – Room Management

#### Q2.1 – Returning Full Room Objects vs. IDs Only

> *When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client-side processing.*

**Returning IDs only** produces a compact response (e.g., `["LIB-301","LAB-101","HALL-01"]`) that consumes very little bandwidth. However, it forces the client to make **N additional requests** — one `GET /api/v1/rooms/{id}` per room — to retrieve any useful detail. This is the classic *N+1 request problem*: with 1,000 rooms, the client issues 1,001 HTTP requests, adding latency, load on the server, and complexity in the client code (parallel fetching or pagination logic).

**Returning full room objects** transfers more data per response but allows the client to render or process the entire collection in a **single round-trip**. The client does no extra work; data is ready to display immediately.

**This project returns full objects** (`GET /api/v1/rooms` → `List<Room>`), which is the right trade-off for a campus management dashboard where operators need name, capacity, and sensor assignments together. For very large datasets, the idiomatic solution is **pagination** (e.g., `?page=0&size=20`) rather than switching to ID-only responses.

#### Q2.2 – Idempotency of DELETE

> *Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client sends the same DELETE request multiple times.*

**Yes, DELETE is idempotent in this implementation.**

REST idempotency means: executing the same request **N times produces the same server state** as executing it once. It does **not** require the HTTP response status code to be identical on every call.

Trace of repeated `DELETE /api/v1/rooms/HALL-01`:

| Call | Room exists? | Action | Response |
|------|-------------|--------|----------|
| 1st | Yes, no sensors | Room removed from `DataStore` | `204 No Content` |
| 2nd | No | Nothing changes; room is already absent | `404 Not Found` |
| 3rd+ | No | Same as 2nd | `404 Not Found` |

After the first call, the server state is "HALL-01 does not exist." Every subsequent call finds that state already true and does not alter it further — the outcome is identical. This satisfies the idempotency contract.

The status code difference (`204` vs `404`) is acceptable and specified by RFC 7231: a `404` on a repeated DELETE communicates that the resource is already gone, which is consistent information for the client.

### Part 3 – Sensor Operations & Linking

#### Q3.1 – Effect of `@Consumes(APPLICATION_JSON)` on Mismatched Content-Type

> *Explain the technical consequences if a client sends data in a different format when `@Consumes(MediaType.APPLICATION_JSON)` is used.*

`@Consumes(MediaType.APPLICATION_JSON)` declares that the resource method **only accepts** requests whose `Content-Type` header is `application/json`.

When a client sends a request with a different content type (e.g., `Content-Type: text/plain` or `Content-Type: application/xml`):

1. **Jersey's content-negotiation layer** inspects the `Content-Type` header before the request reaches the resource method.
2. Finding no matching `@Consumes` mapping, Jersey returns **`415 Unsupported Media Type`** automatically — no application code executes.
3. The response is generated by the JAX-RS runtime, not by any custom exception mapper.

This is a deliberate design feature: it enforces a strict API contract at the framework level, prevents poorly formed requests from reaching business logic, and gives clients an unambiguous, standards-compliant error code. The `GlobalExceptionMapper` is not involved because the rejection happens before dispatch.

#### Q3.2 – `@QueryParam` Filtering vs. Path-Based Filtering

> *Contrast `@QueryParam` filtering (`?type=CO2`) with a path-based alternative (`/sensors/type/CO2`). Why is the query parameter approach generally superior for filtering?*

**Path-based filtering** (`/api/v1/sensors/type/CO2`) makes the filter value part of the **resource identifier (URI)**. This implies that `CO2` sensors are a distinct resource with a fixed address, which is semantically incorrect — `CO2` is a constraint on a collection, not a collection itself. It also creates ambiguity: if a sensor's `id` happened to be `"type"`, the route would conflict.

**Query parameter filtering** (`/api/v1/sensors?type=CO2`) keeps the canonical resource path clean (`/api/v1/sensors` identifies the collection) while expressing filtering, sorting, and pagination as optional, composable modifiers.

| Concern | Path-based | Query Parameter |
|---------|-----------|-----------------|
| Semantic clarity | Filter looks like a sub-resource | Filter is clearly a modifier on the collection |
| Composability | Combining filters is awkward (`/type/CO2/status/ACTIVE`) | Natural: `?type=CO2&status=ACTIVE` |
| Optional parameters | Hard — empty segments break routing | Easy — omit the parameter entirely |
| Cacheability | Creates many cache keys for the same data | Standard query strings are handled by CDNs and caches |
| REST convention | Violates resource-orientation | Consistent with REST and HTTP specifications |

In this project, omitting `?type` returns all sensors; providing it narrows the result — exactly the semantics a query parameter communicates.

### Part 4 – Deep Nesting with Sub-Resources

#### Q4.1 – Architectural Benefits of the Sub-Resource Locator Pattern

> *Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs?*

A **Sub-Resource Locator** is a resource method that carries no HTTP verb annotation (`@GET`, `@POST`, etc.). Instead, it returns an object instance, and JAX-RS continues routing the remaining path segments against that object's annotations. In this project:

```java
// SensorResource.java
@Path("/{sensorId}/readings")
public SensorReadingResource getReadingsResource(@PathParam("sensorId") String sensorId) {
    // validate sensor exists, then delegate
    return new SensorReadingResource(sensorId);
}
```

**Benefits:**

1. **Separation of concerns** — `SensorResource` owns the lifecycle of sensors; `SensorReadingResource` owns the lifecycle of readings. Neither bleeds into the other's domain. Each class has a single reason to change.

2. **Manageable class size** — Without the locator pattern, every nested path (`/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`, hypothetical `/sensors/{id}/alerts`, etc.) would add more methods to an already large `SensorResource`. Controllers become impossible to navigate as APIs grow.

3. **Contextual injection** — The locator passes the resolved `sensorId` (already validated against the store) into the sub-resource constructor. `SensorReadingResource` never needs to re-query or re-validate the parent context; the locator acts as a gateway.

4. **Independent testability** — `SensorReadingResource` can be unit-tested by constructing it directly with a known `sensorId` and a mock `DataStore`, without needing to stand up a full Jersey container or involve routing logic.

5. **Scalability** — In a production API with dozens of nested resource types, each sub-resource class can be assigned to a different developer or team. The locator acts as the contract between the parent and child resource without coupling their implementations.

### Part 5 – Advanced Error Handling & Logging

#### Q5.2 – Why HTTP 422 Is More Semantically Accurate Than 404 for a Missing Reference

> *Why is HTTP 422 Unprocessable Entity often considered more semantically accurate than 404 Not Found when the issue is a missing reference inside a valid JSON payload?*

**404 Not Found** means the **requested resource** (the URL being accessed) does not exist. When a client `POST`s to `/api/v1/sensors`, that endpoint absolutely exists and is reachable — a `404` would incorrectly suggest the URL itself is wrong.

**422 Unprocessable Entity** means the server understood the request syntax and located the endpoint, but **the semantic content of the request body is invalid** — in this case, the `roomId` field references a room that does not exist in the system.

The distinction matters for client developers:

| Code | Client interprets as | Correct here? |
|------|---------------------|---------------|
| `404` | "The endpoint I called does not exist — check your URL" | No — the URL is correct |
| `400` | "My JSON is malformed or a required field is missing" | Partially — but the JSON is well-formed |
| `422` | "My JSON is valid and the endpoint exists, but a referenced entity is missing" | Yes |

`422` gives the client precise, actionable information: the request structure is fine, but a **foreign key constraint** in the payload failed. This guides the developer to fix the `roomId` value rather than waste time debugging their HTTP client or URL construction.

#### Q5.4 – Cybersecurity Risks of Exposing Java Stack Traces

> *From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers.*

Returning raw stack traces in HTTP responses violates the **OWASP principle of minimising information disclosure** and introduces several concrete attack vectors:

1. **Framework and library fingerprinting** — A stack trace reveals exact library names and versions (e.g., `jersey-server-2.41`, `jackson-databind-2.14.0`). Attackers cross-reference these against public CVE databases to find known exploits for the exact versions in use.

2. **Internal path disclosure** — Stack frames include fully qualified class names and file paths (e.g., `com.smartcampus.store.DataStore.seedData(DataStore.java:63)`), revealing the application's package structure, class hierarchy, and deployment directory — information that aids code injection and directory traversal attacks.

3. **Business logic inference** — Method call sequences in a trace expose the internal flow of the application. An attacker can deduce how validation is ordered, which methods handle sensitive operations, and where to target crafted inputs to bypass checks.

4. **Exception message leakage** — Exception messages often include raw SQL queries, configuration values, internal IDs, or error conditions that reveal how the system works internally.

**How this project mitigates it:**  
`GlobalExceptionMapper` intercepts all unhandled `Throwable` instances and returns only a generic `500` JSON body (`"An unexpected error occurred"`), while the full stack trace is written exclusively to the server-side log (`java.util.logging.Logger` at `SEVERE` level). Developers retain full diagnostic information; attackers receive nothing useful.

---

*Report included in this README.md as required by the submission specification.*
