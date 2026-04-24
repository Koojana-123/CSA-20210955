# Smart Campus — Sensor & Room Management API

A JAX-RS (Jersey 2) RESTful API for managing campus rooms and sensors.  
Built with **Jersey 2.41**, **Grizzly HTTP Server**, and **Java 11+**. No database — pure in-memory `ConcurrentHashMap` storage.



## Build & Run Instructions



### Step 1 — download and copy the project

```bash
copy the project into your desktop 
```

### Step 2 — open with netbeans

```bash
open project with netbeans

```


### Step 3 — Run the server

```bash
java -jar target/smartcampus-api-1.0.0.jar
```

The server starts on **http://localhost:8080**.  


### Step 4 — Verify it's running

```bash
curl http://localhost:8080/api/v1
```

Expected output:
```json
{
  "api": "Smart Campus Sensor & Room Management API",
  "version": "1.0.0",
  "contact": "admin@smartcampus.ac.uk",
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

---


## Sample curl Commands

> Make sure the server is running on `localhost:8080` before executing these.

### 1 — Create a Room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}' | jq .
```

Expected response (201 Created):
```json
{
  "id": "LIB-301",
  "name": "Library Quiet Study",
  "capacity": 50,
  "sensorIds": []
}
```

---

### 2 — Register a Sensor in that Room

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001",
  "type":"CO2",
  "status":"ACTIVE",
  "currentValue":0.0,
  "roomId":"LIB-301"}' | jq .
```

Expected response (201 Created):
```json
{
  "id": "CO2-001",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "LIB-301"
}
```

---

### 3 — Post a Sensor Reading

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}' | jq .
```

Expected response (201 Created):
```json
{
  "id": "f3a1...",
  "timestamp": 1714000000000,
  "value": 412.5
}
```

---

### 4 — Filter Sensors by Type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=CO2" | jq .
```

---

### 5 — Attempt to Delete a Room With Sensors (409 Conflict)

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | jq .
```

Expected response (409 Conflict):
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Cannot delete room 'LIB-301'. It still has 1 sensor(s) assigned: [CO2-001]",
  "hint": "Remove or reassign all sensors in this room before deleting it."
}
```


---

## Report — Question Answers

Part 1 — Service Architecture & Setup 

Question 1
Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

 The normal behavior of JAX-RS results in the creation of a new resource instance per every HTTP request coming in. Since every incoming HTTP request will create a new resource instance, any field that may be added to a resource class will disappear after the completion of each request. Therefore, a Hashmap placed as an instance field of a resource class will always start as an empty map with every HTTP request – it will never have any values stored in it. The solution is to hold all shared mutable state in the DataStore singleton object), to which every resource class can refer through DataStore.getInstance(). Since there is a risk of simultaneous execution of requests, ConcurrentHashMap is used everywhere.

Question 2
 Why is the provision of ”Hypermedia” (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

HATEOAS stands for hypermedia as the engine of application state and it refers to the concept that API responses should include links that tell the client what actions the client may take. Advantages of HATEOAS compared to documentation


Clients starting their journey from the /api/v1 endpoint will find all the collections of resources in the API response without having to refer to documentation anywhere else.


 If there happens to be any change in the URL of a particular resource, the only thing that requires an update is the server. Clients navigating through links will have the new URL automatically. 
By exploring the API using tools like postman and curl, developers can navigate through the entire API just based on links received.









Part 2 — Room Management 

Question 1
When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.


Sending back just the IDs (like ["LIB-301", "ENG-101"]) reduces bandwidth usage but requires making N extra GET requests per room using the path /rooms/{id}, leading to increased latency and server load. Sending back full room objects (which is what we have done here) is less efficient in terms of bandwidth usage initially, but eliminates extra requests and allows for rendering the entire room listing in one go. This is ideal for use cases where the user interface displays a table of rooms with room name and capacity. In most campus management applications, full object return is better. When bandwidth is seriously limited, we can always use an extra endpoint sending only IDs.

Question 2
 Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times. 

 Yes, the DELETE method is idempotent in this case, according to RFC 9110 standards. By definition, idempotency is when the result of making multiple requests to the server is the same as if you only made one request. In this API: First request to delete the empty room deletes the room and sends 204 No Content response. Second request to delete the room with the same ID results in the same 204 No Content response (even though 404 Not Found makes more sense). In any case, no extra bad side effects happen from repeating DELETE request.


Part 3 — Sensor Operations & Filtering 

Question 1
We explicitly use the @Consumes (MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch? 


"@Consumes(MediaType.APPLICATION_JSON)" indicates that the POST operation can be invoked only if there is a Content-Type:application/json header present. 
Any other content-type such as text/plain or application/xml will lead to the following behavior by JAX-RS implementation: Before the method execution, it verifies the incoming Content-Type header. If it finds no match, it immediately returns HTTP 415 unsupported media type response and does not execute the method body at all.

Question 2
You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?




Part 4 — Deep Nesting with Sub-Resources 

Question1
Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class? 

Sub-Resource Locator allows nesting paths to be handled by a separate class.
Advantages of this design:
1. Separation of responsibilities: SensorResource handles CRUD operations for the sensor. ReadingResource handles the history of readings. 
2. Simplicity: Having only one "god" class that handles all nesting paths will lead to unreadable code very quickly.
3. Testability: ReadingResource can be tested in isolation without involving the routing mechanism. It is enough to pass a valid sensor id to its constructor. 
4. Scalability: With growing complexity of the API, each concern gets its own class, not additional methods in the same file.


Part 5 — Advanced Error Handling & Logging

Question 1
 Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?  

In case the client posts a sensor whose roomId doesn’t exist: 404 Not Found means the URL for the resource (/api/v1/sensors) couldn’t be found, whereas it definitely was found. The endpoint exists and works fine. 422 Unprocessable Entity means the server understood the request, managed to parse the JSON, yet it found semantic meaning in it incorrect: the room doesn’t exist. It’s crucial to understand that 404 is an error on the side of routing, whereas 422 is a semantic one. 422 will make the client clearly understand what went wrong.


Question2
From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

The leaking of a Java stack trace to an outside user represents a potential security threat
1. Identification of technologies used: A stack trace contains the names and versions of specific software packages . An attacker can search for any known vulnerabilities corresponding to that version. 
2.Discovery of the internal structure of the system: Package and class names give insight into the internal architecture of the software, without having the actual source code. 
3.Exposure of file paths: Stack traces contain file paths in an absolute manner, thereby leaking information about the directory layout, usernames, etc.
 The proper way to handle such exceptions is what this project does in the GlobalExceptionMapper: logging the trace on the server side while returning a plain "Internal Server Error" response to the client.

Question3
 Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?

Don't Repeat Yourself (DRY): Using a filter means logging happens once and automatically applies to all endpoints, both current and future. Without using a filter, we need to remember to include logging code in all future resource methods.
 Logging using filters ensures a consistent logging format for all endpoints. Using manual logging might result in inconsistencies in logging format or omissions. 
Resource methods are meant to encapsulate business logic, not the infrastructure used for observability. Filters neatly achieve this separation.
 Logging in a filter will ensure that all requests are logged, including those where the call to the resource method does not take place due to error (such as in case of 415 rejections). Manual logging would have failed in such cases.

