# Smart Campus API

This project is a RESTful Smart Campus backend built with JAX-RS (Jersey) and an embedded Jetty server. The API is versioned under `/api/v1`, stores all data in memory, and exposes three main domain areas: rooms, sensors, and per-sensor reading histories.

The design follows a simple resource-oriented structure:
- `GET /api/v1` is a discovery endpoint that advertises the primary resource collections.
- `/api/v1/rooms` manages room lifecycle operations.
- `/api/v1/sensors` manages sensor registration and collection filtering.
- `/api/v1/sensors/{sensorId}/readings` is implemented as a sub-resource for nested reading history.

## Technology Stack

- Java 17 target
- JAX-RS (`javax.ws.rs`)
- Jersey 2.41
- Embedded Jetty 9
- Jackson JSON provider
- In-memory storage using `ConcurrentHashMap` and synchronized lists
- JUnit 5 and Jersey Test Framework

## Build And Run

### Windows

1. Open PowerShell in the project root.
2. Compile the project and run the tests:

```powershell
.\mvnw.cmd clean test
```

3. Start the embedded Jetty server:

```powershell
.\mvnw.cmd exec:java
```

4. Open the API root in a browser or HTTP client:

```text
http://localhost:8080/api/v1
```

5. Optional: run the server on a different port:

```powershell
.\mvnw.cmd -Dsmartcampus.port=9090 exec:java
```

### macOS / Linux

1. Open a terminal in the project root.
2. Compile the project and run the tests:

```bash
./mvnw clean test
```

3. Start the embedded Jetty server:

```bash
./mvnw exec:java
```

4. Open the API root:

```text
http://localhost:8080/api/v1
```

## API Summary

- `GET /api/v1`
  Returns API metadata and collection links.
- `GET /api/v1/rooms`
  Returns all room objects.
- `POST /api/v1/rooms`
  Creates a room.
- `GET /api/v1/rooms/{roomId}`
  Returns one room.
- `DELETE /api/v1/rooms/{roomId}`
  Deletes a room unless active sensors are still linked to it.
- `GET /api/v1/sensors`
  Returns all sensors, with optional `type` filtering.
- `POST /api/v1/sensors`
  Registers a sensor against an existing room.
- `GET /api/v1/sensors/{sensorId}/readings`
  Returns reading history for one sensor.
- `POST /api/v1/sensors/{sensorId}/readings`
  Appends a reading and synchronizes the parent sensor's `currentValue`.

## Working curl Examples

### 1. Discover the API

```bash
curl http://localhost:8080/api/v1
```

### 2. Create a room

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LAB-101",
    "name": "Innovation Lab",
    "capacity": 40
  }'
```

### 3. List all rooms

```bash
curl http://localhost:8080/api/v1/rooms
```

### 4. Register a sensor in an existing room

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-101",
    "type": "TEMPERATURE",
    "status": "ACTIVE",
    "currentValue": 24.5,
    "roomId": "LAB-101"
  }'
```

### 5. Filter sensors by type

```bash
curl "http://localhost:8080/api/v1/sensors?type=temperature"
```

### 6. Append a sensor reading

```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-101/readings \
  -H "Content-Type: application/json" \
  -d '{
    "id": "READ-001",
    "timestamp": 1713772800000,
    "value": 25.2
  }'
```

### 7. Retrieve sensor reading history

```bash
curl http://localhost:8080/api/v1/sensors/TEMP-101/readings
```

### 8. Attempt to delete a room with an active linked sensor

```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LAB-101
```

### 9. Trigger a 422 validation error with an unknown room reference

```bash
curl -i -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-404",
    "type": "TEMPERATURE",
    "status": "ACTIVE",
    "currentValue": 22.1,
    "roomId": "UNKNOWN-ROOM"
  }'
```

## Error Response Format

All custom exception mappers return a consistent JSON structure:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room cannot be deleted while active sensors are still assigned."
}
```

## Conceptual Report

### Part 1: Resource Lifecycle And Synchronization

JAX-RS resource classes are request-scoped by default, so Jersey creates a new resource instance for each HTTP request unless a singleton lifecycle is explicitly configured. That default is useful because it prevents accidental sharing of mutable request state across concurrent calls. In this project the resource classes remain stateless, while the shared application state is centralized in `CampusStore`. `CampusStore` uses `ConcurrentHashMap` for the top-level room, sensor, and reading indexes, synchronized lists for nested collections such as `sensorIds` and per-sensor histories, and synchronized mutation methods for compound write operations. That separation allows request-scoped resources to stay lightweight while the in-memory store enforces consistency for shared state changes.

### Part 1: HATEOAS In The Discovery Endpoint

The discovery endpoint returns hypermedia links for the primary collections instead of forcing clients to rely entirely on out-of-band documentation. This improves client resilience because the server itself advertises the canonical entry points for `rooms` and `sensors`. A client can bootstrap from `/api/v1`, discover available collections dynamically, and reduce hard-coded URI assumptions. That makes the API more self-descriptive and lowers the maintenance cost when versions or collection paths evolve.

### Part 2: ID-Only Lists Vs Full Room Objects

Returning only room IDs minimizes payload size and reduces bandwidth consumption, which is helpful when the client only needs identifiers for follow-up calls. Returning full room objects increases response size, but it reduces additional round trips because the client receives `name`, `capacity`, and `sensorIds` immediately. In this coursework the room collection returns full objects because the payload is still small, the server-side processing cost is trivial in an in-memory design, and the richer representation improves usability for clients and demonstrations.

### Part 2: Idempotency Of Room Deletion

The room `DELETE` operation is idempotent because repeating the same request does not change the final server state beyond the first successful deletion. Once the room has been removed, sending the same `DELETE /rooms/{roomId}` again leaves the system in the same state: the room is still absent. The response may differ between calls, for example `204 No Content` first and `404 Not Found` later, but HTTP idempotency is defined by stable server state, not by identical response payloads.

### Part 3: Sending `text/plain` To A JSON-Only Endpoint

When a resource method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, Jersey selects a message body reader only for JSON-compatible requests. If a client sends `text/plain`, the runtime cannot legally bind that payload to the Java parameter using the declared consumption contract, so the request is rejected with `415 Unsupported Media Type`. This protects the endpoint from ambiguous parsing behavior and enforces a precise content negotiation contract between client and server.

### Part 3: `@QueryParam` Vs `@PathParam` For Sensor Filtering

`@QueryParam("type")` is the better choice because filtering does not identify a different resource; it refines the representation of the same sensor collection resource. `GET /sensors?type=TEMPERATURE` clearly expresses a collection query, while a path like `/sensors/type/TEMPERATURE` suggests a different hierarchical sub-resource. Query parameters are also more extensible because additional optional filters such as `status` or `roomId` can be combined naturally without inventing extra URI path patterns.

### Part 4: Benefits Of The Sub-Resource Locator Pattern

The sub-resource locator keeps the domain model layered cleanly. `SensorResource` is responsible for top-level sensor registration and collection retrieval, while `SensorReadingResource` encapsulates the nested reading history under one sensor. That improves cohesion, keeps each class focused on a single level of the URI space, and avoids overloading one resource class with unrelated concerns. It also scales more cleanly if future operations such as pagination, deletion of individual readings, or analytics endpoints need to be added under the readings branch.

### Part 5: Why HTTP 422 Is Better Than 404 For A Missing `roomId`

`422 Unprocessable Entity` is more semantically accurate because the request URI itself is valid and the JSON document is syntactically correct, but the payload contains a semantic reference to a room that does not exist. A `404 Not Found` would imply that the requested endpoint path is missing, which is not the real problem here. The client reached the correct resource, but the submitted entity could not be processed because one of its linked values failed domain validation.

### Part 5: Risks Of Exposing Internal Java Stack Traces

Returning raw stack traces to API consumers leaks implementation details such as package names, class names, file paths, dependency behavior, and call-stack structure. From a cybersecurity perspective, that information helps attackers fingerprint the technology stack, identify potential vulnerable libraries, and map internal execution paths. It also reveals failure surfaces that should remain private. The global `ExceptionMapper<Throwable>` prevents this leakage by returning a generic `500` response body with no internal stack-trace data.

### Part 5: Why JAX-RS Filters Are Better For Logging

Request and response logging is a cross-cutting concern, so a filter is the correct abstraction. A `ContainerRequestFilter` and `ContainerResponseFilter` can intercept every call in one centralized place, guaranteeing consistent logging of HTTP method, URI, and status code regardless of which resource method executes. If `Logger.info()` calls were copied into every endpoint method, logging would become repetitive, easy to forget, and harder to keep consistent across the codebase. Filters preserve separation of concerns by keeping observability outside the business logic.
