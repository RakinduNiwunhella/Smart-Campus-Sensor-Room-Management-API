# Smart Campus - Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**University of Westminster - - School of Computer Science and Engineering**

| | |
|---|---|
| **Name** | Rakindu Niwunhella |
| **UOW ID** | w2119870 |
| **IIT ID** | 20240426 |

---

## Video Demonstration & Conceptual Report


[**View Video Demonstration**](https://drive.google.com/file/d/1D9lHClrjjM8IJwgYh9VrNGUKvXt6EXL6/view?usp=share_link)

[**Conceptual Report**](https://drive.google.com/file/d/1l40ZdRyZr9W1JNd5I4ELmF-0fGoGYn5Q/view?usp=share_link)

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
- **Versioned base path** (**/api/v1**) allows future non-breaking evolution of the API
- **HATEOAS discovery endpoint** at the root so clients never hardcode resource URLs
- **Sub-resource locator pattern** delegates reading history to a dedicated **SensorReadingResource** class, keeping resource classes single-responsibility
- **Centralised exception mappers** (**@Provider**) translate every business exception into a consistent **{status, error, message}** JSON body - no raw stack traces ever reach the client
- **Thread-safe singleton **DataStore**** using **ConcurrentHashMap** and double-checked locking, necessary because JAX-RS creates a new resource instance per request
- **Referential integrity** enforced at write time: a sensor cannot be created without a valid **roomId**, and a room cannot be deleted while sensors are assigned to it

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
8. [Coursework Q&A](#8-coursework-qa)
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
| JSON Serialisation | Jackson (via **jersey-media-json-jackson**) |
| Data Persistence | In-memory **ConcurrentHashMap** (Singleton) |
| API Base Path | **/api/v1** |

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
2. Navigate to the **smart-campus-api** folder and select it
3. NetBeans detects it as a Maven project - click **Open Project**
4. Wait for NetBeans to resolve dependencies (progress shown in the bottom bar)

### 3. Set the Server

1. Right-click the project in the **Projects** panel → **Properties**
2. Go to **Run** category
3. Set **Server** to the Tomcat 9 instance registered in step 1
4. Leave **Context Path** as **/smart-campus-api**
5. Click **OK**

### 4. Clean and Build

1. Right-click the project in the **Projects** panel → **Clean and Build**
2. Wait for the **BUILD SUCCESS** message in the Output window
3. This produces the deployable WAR file under **target/**

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
  "version": "1.0.0",
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

All commands assume the server is running at **http://localhost:8080/Smart-Campus-API**. The API ships with seed data so every GET command works immediately after startup.

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
  -d '{"id": "Library-02", "name": "Library Room 10", "capacity": 200}'
```

### 4. Register a new sensor in an existing room

```bash
curl -X POST http://localhost:8080/Smart-Campus-API/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-102", "type": "Temperature", "status": "ACTIVE", "currentValue": 32.0, "roomId": "HALL-01"}'
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

All endpoints consume and produce **application/json**. Base URL: **http://localhost:8080/Smart-Campus-API/api/v1**

### 5.1 Discovery

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| **GET** | **/api/v1** | Returns API metadata and HATEOAS navigation links | **200 OK** |

---

### 5.2 Room Management

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| **GET** | **/api/v1/rooms** | List all rooms | **200 OK** |
| **POST** | **/api/v1/rooms** | Create a new room | **201 Created** |
| **GET** | **/api/v1/rooms/{roomId}** | Get a specific room by ID | **200 OK** |
| **DELETE** | **/api/v1/rooms/{roomId}** | Delete a room (blocked if sensors are assigned) | **204 No Content** |

**POST /rooms - Request Body**

```json
{
  "id": "HALL-02",
  "name": "Seminar Room B",
  "capacity": 40
}
```

**Constraints**

- **id** is required → **400 Bad Request** if missing
- Duplicate **id** → **409 Conflict**
- DELETE on a room with sensors → **409 Conflict**
- DELETE on a non-existent room → **404 Not Found**

---

### 5.3 Sensor Operations

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| **GET** | **/api/v1/sensors** | List all sensors | **200 OK** |
| **GET** | **/api/v1/sensors?type={type}** | Filter sensors by type (e.g. **CO2**, **Temperature**) | **200 OK** |
| **POST** | **/api/v1/sensors** | Register a new sensor | **201 Created** |
| **GET** | **/api/v1/sensors/{sensorId}** | Get a specific sensor by ID | **200 OK** |

**POST /sensors - Request Body**

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

- **id** is required → **400 Bad Request** if missing
- **roomId** must reference an existing room → **422 Unprocessable Entity**
- Duplicate **id** → **409 Conflict**
- **status** defaults to **"ACTIVE"** if omitted
- Valid statuses: **ACTIVE**, **MAINTENANCE**, **OFFLINE**

---

### 5.4 Sensor Readings (Sub-Resource)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| **GET** | **/api/v1/sensors/{sensorId}/readings** | Retrieve full reading history | **200 OK** |
| **POST** | **/api/v1/sensors/{sensorId}/readings** | Append a new reading | **201 Created** |

**POST /readings - Request Body**

```json
{
  "value": 23.4
}
```

> **id** (UUID) and **timestamp** (epoch ms) are auto-generated by the server if omitted.

**Side-effects on POST**

- Updates the parent **Sensor.currentValue** to the new reading's **value**.

**Constraints**

- Sensor must be in **ACTIVE** status → **403 Forbidden** if **MAINTENANCE** or **OFFLINE**
- Sensor must exist → **404 Not Found**

---

## 6. Data Models

### Room

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| **id** | **String** | Yes | Unique identifier, e.g. **"LIB-301"** |
| **name** | **String** | No | Human-readable label |
| **capacity** | **int** | No | Maximum occupancy |
| **sensorIds** | **List<String>** | Auto | IDs of sensors deployed in this room |

### Sensor

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| **id** | **String** | Yes | Unique identifier, e.g. **"TEMP-001"** |
| **type** | **String** | No | **Temperature**, **CO2**, **Occupancy**, etc. |
| **status** | **String** | No | **ACTIVE** / **MAINTENANCE** / **OFFLINE** (default: **ACTIVE**) |
| **currentValue** | **double** | No | Most recent measurement |
| **roomId** | **String** | Yes | Foreign key - must reference an existing Room |

### SensorReading

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| **id** | **String** | No | UUID; auto-generated if omitted |
| **timestamp** | **long** | No | Epoch ms; auto-generated if omitted |
| **value** | **double** | Yes | Measured metric value |

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
| Delete room with active sensors | **RoomNotEmptyException** | **409 Conflict** |
| Sensor references non-existent room | **LinkedResourceNotFoundException** | **422 Unprocessable Entity** |
| POST reading to unavailable sensor | **SensorUnavailableException** | **403 Forbidden** |
| Any unhandled runtime exception | **Throwable** (catch-all) | **500 Internal Server Error** |

Raw Java stack traces are **never** returned in HTTP responses. All exceptions are intercepted by **@Provider** ExceptionMappers; full stack traces are written to the server log only.

---

## 8. Coursework Q&A

### Part 1 – Service Architecture & Setup

#### Part 1.1 JAX-RS Resource Lifecycle & In-Memory Data Management

For every HTTP request, JAX-RS creates a new instance of every resource class (given a per-request lifecycle). This standard default means request resource classes are not retained as they are created, utilized, and subsequently discarded for each request.

Each request gets a new resource instance, so instance variables do not persist between requests. Therefore, shared data (like rooms and sensors) must be stored in a singleton. A thread-safe singleton **DataStore** with **ConcurrentHashMap** is used to safely handle concurrent access. Using a **HashMap** could lead to data corruption, data loss, runtime errors, and other unexpected outcomes.

#### Part 1.2 HATEOAS and Hypermedia in RESTful Design

HATEOAS (Hypermedia as the Engine of Application State) is one indicator of a mature REST API, and at runtime it is self-describing. Instead of a client needing to consult external documentation to know which URLs to call next, each response carries embedded links that describe the available transitions from the current state.

Compared to static documentation, this approach benefits client developers in several ways:

- **Decoupling:** Clients do not rely on fixed URLs. Even if the server changes its endpoint structure, clients that use provided links can keep working without needing updates.
- **Discoverability:** A client can navigate the entire API starting from a single known entry point (**GET /smart-campus-api/api/v1**) by following links, reducing the learning curve.
- **Self-consistency:** Responses include links that match the current system state, so clients get accurate, context-aware directions instead of depending on possibly outdated documentation.

### Part 2 – Room Management

#### Part 2.1 Returning IDs vs. Full Room Objects

Returning only IDs reduces the initial payload size, which benefits scenarios where the client only needs to know which rooms exist and will selectively fetch individual rooms. However, this design increases client and server latency because it requires more requests to retrieve additional information.

Returning full room objects provides all available information in one round trip, reducing latency for clients that need to display or process room details immediately. The disadvantage is increased network bandwidth for large collections.

In short, full objects work best for small to medium datasets since fewer requests improve performance. For very large datasets, pagination with partial (summary) data is the more balanced approach.

#### Part 2.2 Idempotency of DELETE

The DELETE operation is idempotent in this implementation, in line with the HTTP specification. Idempotency means that making the same request multiple times produces the same server-side state as making it once.

In this implementation:

- **First DELETE on an existing, empty room:** The room is removed from the store and **204 No Content** is returned.
- **Second DELETE on the same room ID:** The room no longer exists, so **404 Not Found** is returned.

Strictly speaking, the HTTP response code differs between the first and second call, but the resource state is identical after both calls: the room is absent. RFC 9110 defines idempotency in terms of side effects on the server state, not in terms of response status code. Therefore, DELETE is correctly considered idempotent here.

The only case that does not meet idempotency is when a DELETE operation is performed on a room that still contains sensors. In that case, **RoomNotEmptyException** is thrown, and a consistent **409 Conflict** response is returned until the sensors are removed.

### Part 3 – Sensor Operations & Linking

#### Part 3.1 @Consumes(MediaType.APPLICATION_JSON) and Format Mismatches

The **@Consumes(MediaType.APPLICATION_JSON)** annotation states that the POST method can only process a request body with **Content-Type: application/json**.

If a client sends a request with an unsupported content type such as **text/plain** or **application/xml**, the JAX-RS runtime first examines the **Content-Type** header before dispatching the request to any resource method. If it cannot find a method whose **@Consumes** annotation matches the incoming content type, it immediately returns HTTP **415 Unsupported Media Type** without invoking any application code.

This means the resource method body is never executed. No partial parsing, no null entity objects, and no risk of data corruption. The annotation acts as a contract at framework level, making the API self-protecting against malformed or unexpected input formats.

#### Part 3.2 @QueryParam vs. Path Segment for Filtering

Using a query parameter (**GET /api/v1/sensors?type=CO2**) is superior to embedding the filter in the path (**/api/v1/sensors/type/CO2**) for the following reasons:

- **Semantic correctness:** Path segments identify a specific resource. **sensors/CO2** means CO2 is a distinct resource, not a filter criterion applied to the sensors collection. Query parameters semantically represent optional modifiers on a collection.
- **Multiple filters:** Query parameters compose naturally. **?type=CO2&status=ACTIVE** is intuitive, while path-based filtering with multiple criteria results in combinatorial URL designs that are difficult to maintain.
- **Cacheability and bookmarking:** Query parameter URLs are understood by HTTP caches and browsers as referring to the same base resource with optional refinements.
- **Optional by nature:** Query parameters are inherently optional. Omitting them returns the full unfiltered collection, while a path-based approach would require a separate route for the unfiltered case.

### Part 4 – Deep Nesting with Sub-Resources

#### Part 4.1 Architectural Benefits of the Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates responsibility for a nested URL hierarchy to a separate, dedicated class. In this case, **SensorResource** handles **/sensors/{sensorId}** and returns an instance of **SensorReadingResource** to handle everything under **/sensors/{sensorId}/readings**.

Key benefits:

- **Single Responsibility:** Every class has one concern. **SensorReadingResource** only handles reading history, not sensor registration or room linkage.
- **Reduced class size and cognitive complexity:** A single monolithic resource class handling all nested paths becomes very large and difficult to navigate, test, or modify. Splitting by concern keeps each class focused and shorter.
- **Reusability:** The sub-resource class can, in principle, be reused from multiple parent locators.
- **Testability:** **SensorReadingResource** can be unit-tested in isolation by constructing it with a known **sensorId**, without needing the full JAX-RS container.
- **Contextual injection:** When the parent locator passes constructor state (the **sensorId**), context is available to all methods, reducing repeated **@PathParam** declarations in each method.

### Part 5 – Advanced Error Handling & Logging

#### Part 5.2 HTTP 422 vs. HTTP 404 for Missing Referenced Resources

When a client POSTs a new sensor with a **roomId** of **"room-101"** and that room does not exist:

- **404 Not Found** is semantically wrong in this context. 404 means the requested URL itself does not exist. The URL **/api/v1/sensors** exists and is valid; the request was received and understood.
- **422 Unprocessable Entity** is more accurate because the HTTP request was syntactically valid (well-formed JSON, correct Content-Type), reached the correct endpoint, but the server cannot process it because the semantic content is invalid, specifically it contains a reference to a resource that does not exist in the system.

422 communicates to the client: "I understood your request perfectly, but the data inside it violates a business rule." This helps client developers distinguish between "wrong URL" (404) and "valid request with bad data" (422), enabling more precise error-handling logic.

#### Part 5.4 Security Risks of Exposing Java Stack Traces

Exposing raw Java stack traces in APIs is an information disclosure vulnerability (OWASP A05: Security Misconfiguration) because it reveals sensitive internal details that attackers can exploit.

Raw stack traces can reveal framework and library internals, including package paths and external dependencies such as **org.glassfish.jersey.server**, which may help attackers identify known vulnerabilities. They also disclose internal architecture through package names such as **com.smartcampus.store.DataStore**, making it easier to plan targeted attacks. In addition, errors such as **NullPointerException** can reveal exact methods and line numbers, effectively exposing execution paths that attackers can probe for deeper weaknesses.

A **GlobalExceptionMapper** mitigates these risks by intercepting all **Throwable** instances and returning only a generic **500 Internal Server Error** JSON response to clients, without exposing stack traces or internal details, while logging full error information securely on the server for authorized developers.

#### Part 5.5 JAX-RS Filters for Cross-Cutting Concerns vs. Inline Logging

Using a JAX-RS filter that implements **ContainerRequestFilter** and **ContainerResponseFilter** is superior to inserting **Logger.info()** calls in every resource method for several reasons:

- **DRY principle:** Logging logic is centralized and automatically applied to all requests and responses, so new resource methods do not need additional logging code.
- **Consistency:** A filter ensures every request is captured, even those that fail before reaching resource methods (such as 415 errors), whereas inline logging can miss these cases.
- **Separation of concerns:** Resource methods remain focused on core business logic, avoiding mixing logging, authentication, or other cross-cutting concerns that reduce clarity and testability.
- **Maintainability:** Updates to logging format or logging frameworks can be made in one location (the filter), instead of modifying many resource methods.
- **Auditability:** A filter guarantees a complete and reliable audit trail, as logging cannot be accidentally skipped when new endpoints are introduced.

