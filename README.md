# Smart Campus API

This project is a RESTful backend for the Smart Campus coursework. It uses JAX-RS with Jersey on an embedded Jetty server and keeps all data in memory using Java collections only. The API manages rooms, sensors, and nested sensor readings under the versioned base path `/api/v1`.

## API Overview

- `GET /api/v1` returns discovery metadata and collection links.
- `GET /api/v1/rooms` lists all rooms.
- `POST /api/v1/rooms` creates a room.
- `GET /api/v1/rooms/{roomId}` returns one room.
- `DELETE /api/v1/rooms/{roomId}` deletes a room if it has no linked sensors.
- `GET /api/v1/sensors` lists sensors, with optional `type` filtering.
- `POST /api/v1/sensors` registers a sensor against an existing room.
- `GET /api/v1/sensors/{sensorId}/readings` returns reading history for a sensor.
- `POST /api/v1/sensors/{sensorId}/readings` appends a new reading and updates the parent sensor's `currentValue`.

## Tech Stack

- Java 17 target
- JAX-RS (`javax.ws.rs`)
- Jersey 2
- Embedded Jetty 9
- Jackson JSON
- In-memory storage using `ConcurrentHashMap` and synchronized lists
- JUnit 5 with Jersey Test Framework

## How To Build And Run

### Windows

1. Run the tests:
   ```powershell
   .\mvnw.cmd test
   ```
2. Start the API:
   ```powershell
   .\mvnw.cmd exec:java
   ```
3. Open the API at:
   ```text
   http://localhost:8080/api/v1
   ```

### macOS / Linux

1. Run the tests:
   ```bash
   ./mvnw test
   ```
2. Start the API:
   ```bash
   ./mvnw exec:java
   ```

## Sample curl Commands

### 1. Discover the API root

```bash
curl http://localhost:8080/api/v1
```

### 2. Create a room

```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LIB-301",
    "name": "Library Quiet Study",
    "capacity": 30
  }'
```

### 3. List all rooms

```bash
curl http://localhost:8080/api/v1/rooms
```

### 4. Register a sensor for an existing room

```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "CO2-001",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 415.3,
    "roomId": "LIB-301"
  }'
```

### 5. Filter sensors by type

```bash
curl "http://localhost:8080/api/v1/sensors?type=co2"
```

### 6. Add a sensor reading

```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": 1713772800000,
    "value": 420.8
  }'
```

### 7. Read sensor history

```bash
curl http://localhost:8080/api/v1/sensors/CO2-001/readings
```

### 8. Attempt to delete a room that still has sensors

```bash
curl -i -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

## Example Error Response

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "The requested roomId does not exist."
}
```

## Report Answers

### 1.1 Architecture and Config

By default, JAX-RS resource classes are request-scoped, so the runtime creates a new resource instance for each incoming request unless a different lifecycle is configured. That means instance fields inside resource classes are not a safe place for shared application data because they would be recreated per request. For this coursework I kept the shared state in a separate singleton-style `CampusStore`, using `ConcurrentHashMap` for the main collections and synchronized lists for nested lists. That design reduces race conditions and avoids losing in-memory state between requests.

### 1.2 Discovery Endpoint

Hypermedia is useful because the response itself tells the client where the important collections are instead of forcing the client to hard-code every path from external documentation. This makes the API more self-descriptive, helps new client developers navigate the service faster, and reduces the chance of clients breaking when links or versions change in the future.

### 2.1 Room Implementation

Returning only IDs gives a smaller payload, so it saves bandwidth when the client only needs references and plans to fetch details later. Returning full room objects is more convenient because the client can render useful data immediately without extra round trips. In this implementation I returned full room objects because the payload is still small and it gives a better developer experience for a coursework API.

### 2.2 Deletion and Logic

The `DELETE` operation is idempotent because repeating the same delete request does not recreate the room or change the final server state after the first successful deletion. After the first successful call the room is gone, and later identical calls still leave the server in the same state: the room remains absent. The response can change from `204 No Content` to `404 Not Found`, but idempotency is about the resulting state, not identical response bodies.

### 3.1 Sensor Integrity

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells Jersey to accept JSON for that endpoint. If a client sends a different format such as `text/plain` or `application/xml`, Jersey cannot find a matching message body reader for the declared media type and the request is rejected with `415 Unsupported Media Type`. This protects the endpoint from ambiguous or invalid payload handling.

### 3.2 Filtered Retrieval

Query parameters are better for filtering because the client is still requesting the same collection resource, just with an extra search condition applied to it. A path like `/sensors/type/CO2` makes the filter look like a different resource hierarchy, while `GET /sensors?type=CO2` clearly communicates that it is the normal sensors collection with a filter. Query parameters also scale better when multiple filters are added later.

### 4.1 Sub-Resource Locator

The sub-resource locator pattern keeps the API modular by letting `SensorResource` handle sensor-level concerns while `SensorReadingResource` handles the nested reading history. This is easier to maintain than putting every nested sensor-reading route into one large class. It improves readability, keeps each class focused on one level of the domain, and makes future extensions like deleting one reading or paging the history easier to add.

### 4.2 Historical Management

When a new reading is posted, the API stores the reading in that sensor's history and also updates the parent sensor's `currentValue`. This keeps the aggregate sensor record consistent with the detailed history so that clients can either fetch the latest summary value from `/sensors` or inspect the full timeline under `/readings`.

### 5.1 Specific Exceptions

I used dedicated exception classes and exception mappers for the three business-rule failures required in the brief. This keeps the resource methods readable and makes the HTTP response generation consistent across the API. Each mapper returns a structured JSON error body instead of a container-generated HTML page or raw stack trace.

### 5.2 Why 422 Is Better Than 404 Here

`422 Unprocessable Entity` is more accurate because the request URI is valid and the JSON structure itself is valid, but one field inside the payload refers to a linked resource that does not exist. A `404 Not Found` usually describes the requested URL, not a broken foreign-key-style reference inside an otherwise valid request body. In this case the client reached the correct endpoint, but the submitted data could not be processed.

### 5.3 Sensor State Constraint

I treated the `MAINTENANCE` status as a hard availability rule for posting new readings. Returning `403 Forbidden` makes it clear that the client is authenticated enough to reach the endpoint but is not allowed to perform that action while the sensor is in a restricted state.

### 5.4 Global Safety Net and Security

Exposing raw Java stack traces is risky because they can reveal internal package names, class names, file paths, framework versions, and hints about how the code is structured. An attacker can use that information to target known library vulnerabilities, guess hidden endpoints, or understand where error handling is weak. The global `ExceptionMapper<Throwable>` prevents that by returning a clean generic `500` response instead.

### 5.5 Why Filters Are Better For Logging

Filters are better for cross-cutting concerns because they run for every request and response in one central place. If logging were written manually inside every resource method, it would be repetitive, easy to forget, and harder to keep consistent. A filter keeps the resource classes focused on business logic while still giving complete observability of HTTP method, URI, and status code.
