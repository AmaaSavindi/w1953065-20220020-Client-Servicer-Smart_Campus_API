# Smart Campus API

This project is a RESTful Smart Campus backend that uses JAX-RS (Jersey) as a standard Java web application. The API is versioned under `/api/v1`, keeps all of its data in memory, and covers three main areas: rooms, sensors, and the reading histories for each sensor.

The design follows a simple resource-oriented structure:

- `GET /api/v1` is a discovery endpoint that advertises the primary resource collections.
- `/api/v1/rooms` manages room lifecycle operations.
- `/api/v1/sensors` manages sensor registration and collection filtering.
- `/api/v1/sensors/{sensorId}/readings` is implemented as a sub-resource for the nested reading history.

## Technology Stack

- Java 17 target
- JAX-RS (`javax.ws.rs`)
- Jersey 2.41
- WAR-based servlet-container deployment
- Jackson JSON provider
- In-memory storage using `ConcurrentHashMap` and synchronized lists
- JUnit 5 with the Jersey Test Framework

## Build And Run

### Windows

1. Open PowerShell in the project root.
2. Compile the project and run the tests:

   ```powershell
   .\mvnw.cmd clean test
   ```

3. Package the application:

   ```powershell
   .\mvnw.cmd package
   ```

4. Deploy the generated `target\ROOT.war` file to your local servlet container's `webapps` directory.

5. Start the servlet container and open the API root in a browser or HTTP client:

   ```text
   http://localhost:8080/api/v1
   ```

If your container is configured to use a different port, adjust the URL accordingly.

### macOS / Linux

1. Open a terminal in the project root.
2. Compile the project and run the tests:

   ```bash
   ./mvnw clean test
   ```

3. Package the application:

   ```bash
   ./mvnw package
   ```

4. Deploy the generated `target/ROOT.war` file to your local servlet container's `webapps` directory.

5. Start the servlet container and open the API root:

   ```text
   http://localhost:8080/api/v1
   ```

If your container is configured to use a different port, adjust the URL accordingly.

## API Summary

- `GET /api/v1` returns API metadata and collection links.
- `GET /api/v1/rooms` returns all room objects.
- `POST /api/v1/rooms` creates a room.
- `GET /api/v1/rooms/{roomId}` returns one room.
- `DELETE /api/v1/rooms/{roomId}` deletes a room unless active sensors are still linked to it.
- `GET /api/v1/sensors` returns all sensors, with optional `type` filtering.
- `POST /api/v1/sensors` registers a sensor against an existing room.
- `GET /api/v1/sensors/{sensorId}/readings` returns the reading history for one sensor.
- `POST /api/v1/sensors/{sensorId}/readings` appends a reading and synchronizes the parent sensor's `currentValue`.

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

The filter is case-insensitive, so `temperature` matches the stored `TEMPERATURE`.

```bash
curl "http://localhost:8080/api/v1/sensors?type=temperature"
```

### 6. Append a sensor reading

The reading `id` is optional; if omitted, the server generates a UUID. The parent sensor's `currentValue` is updated as a side effect.

```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-101/readings \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": 1713772800000,
    "value": 25.2
  }'
```

### 7. Retrieve sensor reading history

```bash
curl http://localhost:8080/api/v1/sensors/TEMP-101/readings
```

### 8. Delete a room that still has an active linked sensor

After example 4, `LAB-101` still has `TEMP-101` linked to it, so this request returns **HTTP 409 Conflict** with a structured JSON error body, enforcing the room-deletion business rule.

```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LAB-101
```

### 9. Trigger a 422 validation error with an unknown room reference

The request URI and JSON syntax are valid, but the `roomId` refers to a room that does not exist. The API returns **HTTP 422 Unprocessable Entity** with a structured JSON error body.

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

All custom exception mappers and the global safety-net mapper return a consistent JSON structure:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Room cannot be deleted while active sensors are still assigned."
}
```

## Conceptual Report

### 1.1 Resource Lifecycle And Synchronization

JAX-RS resource classes are request-scoped by default, so Jersey creates a new resource instance for each HTTP request unless a singleton lifecycle is explicitly configured. That default is useful because it prevents accidental sharing of mutable request state across concurrent calls. In this project the resource classes remain stateless, while the shared application state is centralized in `CampusStore`. `CampusStore` uses `ConcurrentHashMap` for the top-level room, sensor, and reading indexes, synchronized lists for nested collections such as `sensorIds` and per-sensor histories, and synchronized mutation methods for compound write operations. That separation allows request-scoped resources to stay lightweight while the in-memory store enforces consistency for shared state changes.

### 1.2 HATEOAS In The Discovery Endpoint

The discovery endpoint returns hypermedia links for the primary collections instead of forcing clients to rely entirely on out-of-band documentation. This improves client resilience because the server itself advertises the canonical entry points for `rooms` and `sensors`. A client can bootstrap from `/api/v1`, discover available collections dynamically, and reduce hard-coded URI assumptions. That makes the API more self-descriptive and lowers the maintenance cost when versions or collection paths evolve.

### 2.1 ID-Only Lists Vs Full Room Objects

Returning only room IDs minimizes payload size and reduces bandwidth consumption, which is helpful when the client only needs identifiers for follow-up calls. Returning full room objects increases response size, but it reduces additional round trips because the client receives `name`, `capacity`, and `sensorIds` immediately. This project returns full room objects because the payload is still small, the server-side processing cost is trivial in an in-memory design, and the richer representation improves usability for clients and interactive exploration of the API.

### 2.2 Idempotency Of Room Deletion

The room `DELETE` operation is idempotent because repeating the same request does not change the final server state beyond the first successful deletion. Once the room has been removed, sending the same `DELETE /rooms/{roomId}` again leaves the system in the same state: the room is still absent. The response may differ between calls, for example `204 No Content` first and `404 Not Found` later, but HTTP idempotency is defined by stable server state, not by identical response payloads.

### 3.1 Sending `text/plain` To A JSON-Only Endpoint

Adding `@Consumes(MediaType.APPLICATION_JSON)` to a resource method tells Jersey that the endpoint will only match a message body reader that can read JSON. If a client sends a payload with `Content-Type: text/plain` or `application/xml`, the runtime cannot legally bind that payload to the Java parameter under the declared consumption contract, so the request is rejected with `415 Unsupported Media Type`. This enforces an unambiguous content-negotiation contract between client and server and prevents the endpoint from receiving payloads it cannot safely parse.

### 3.2 `@QueryParam` Vs `@PathParam` For Sensor Filtering

`@QueryParam("type")` is the better choice because filtering does not identify a different resource; it refines the representation of the same sensor collection resource. `GET /sensors?type=TEMPERATURE` clearly expresses a collection query, while a path like `/sensors/type/TEMPERATURE` suggests a different hierarchical sub-resource. Query parameters are also more extensible because additional optional filters such as `status` or `roomId` can be combined naturally without inventing extra URI path patterns.

### 4.1 Benefits Of The Sub-Resource Locator Pattern

The sub-resource locator keeps the domain model layered cleanly. `SensorResource` is responsible for top-level sensor registration and collection retrieval, while `SensorReadingResource` encapsulates the nested reading history under one sensor. That improves cohesion, keeps each class focused on a single level of the URI space, and avoids overloading one resource class with unrelated concerns. It also scales more cleanly if future operations such as pagination, deletion of individual readings, or analytics endpoints need to be added under the readings branch.

### 5.1 Why HTTP 422 Is Better Than 404 For A Missing `roomId`

`422 Unprocessable Entity` is more semantically accurate because the request URI itself is valid and the JSON document is syntactically correct, but the payload contains a semantic reference to a room that does not exist. A `404 Not Found` would imply that the requested endpoint path is missing, which is not the real problem here. The client reached the correct resource, but the submitted entity could not be processed because one of its linked values failed domain validation.

### 5.2 The Dangers Of Exposing Internal Java Stack Traces

Clients that receive raw stack traces learn a great deal about the server's internal implementation. A single leaked trace usually hands the attacker five concrete pieces of reconnaissance:

- **Dependency versions** like `jersey-server 2.41` and other deployed web libraries. An attacker plugs those exact versions into the NVD, GitHub Advisory Database, or Snyk to look up known remote code execution, deserialization, or path-traversal vulnerabilities, then crafts a payload that matches the targeted endpoint.
- **Internal package and class names** like `uk.ac.westminster.smartcampus.service.CampusStore`. These map the codebase's internal module boundaries, surface domain concepts that can be probed for adjacent endpoints, and often suggest routes that are not documented publicly.
- **Filesystem paths** in frames such as `at CampusStore.addSensor(CampusStore.java:135)`. These reveal the host operating system, the build layout (for example Maven's `target/classes/...`), and sometimes absolute install directories that are useful for chaining local-file-inclusion or path-traversal attacks.
- **Logic flow**, meaning which method called which, at which line, and under which input. The attacker uses this to refine payloads that hit the same failure path deterministically, or to pivot into adjacent methods whose names hint at higher-risk operations.
- **Framework fingerprints** like `org.glassfish.jersey.server.ContainerResponse` or Jackson's `JsonMappingException`. These confirm the exact runtime stack and narrow the relevant attack surface to Jersey-, Jackson-, and servlet-container-specific vulnerabilities.

The global `ExceptionMapper<Throwable>` prevents every one of these disclosures by replacing any unexpected failure with a sanitized `500 Internal Server Error` JSON body. No stack frames, class names, file paths, dependency versions, or framework fingerprints reach the client, so an attacker gains no reconnaissance value from deliberately triggering server-side errors.

### 5.3 Why JAX-RS Filters Are Better For Logging

Request and response logging is a cross-cutting concern, so a filter is the right abstraction. A `ContainerRequestFilter` combined with a `ContainerResponseFilter` captures every call in one central location, which guarantees that the HTTP method, URI, and status code are logged consistently regardless of which resource method handled the request. If `Logger.info()` calls were instead scattered across every endpoint method, logging would become repetitive, easy to forget, and difficult to keep consistent. Filters separate observability from business logic, keeping resource classes focused on domain behaviour while the filter keeps the cross-cutting concern in one place.
