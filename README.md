# Multi-Threaded Java HTTP Web Server

A high-performance, lightweight HTTP Web Server built from scratch in Java using raw TCP Sockets, the **Producer-Consumer** concurrency pattern, and a custom **Thread Pool** with bounded backpressure management.

---

## 🚀 Key Architectural Features

- **Producer-Consumer Pattern**:
  - **Producer (Acceptor Loop)**: Main thread listens on `ServerSocket.accept()`, wraps each client socket in a `ClientHandler` (`Runnable`) task, and enqueues it.
  - **Consumers (Worker Pool)**: Fixed pool of worker threads continuously dequeues and processes incoming HTTP connection tasks.
- **Backpressure Management**:
  - Utilizes a bounded `ArrayBlockingQueue<Runnable>`. When the queue reaches maximum capacity, `queue.put()` blocks the acceptor thread, delegating connection buffering to the operating system's TCP listen backlog.
- **Dynamic Thread Pool Sizing**:
  - Automatically initializes worker thread count based on hardware CPU cores (`Runtime.getRuntime().availableProcessors()`).
- **Full HTTP Method Handling**:
  - `GET`: Serves static web assets (`.html`, `.css`, `.js`, `.png`, `.json`, etc.) with security path-traversal protection.
  - `POST`: Processes incoming request payloads and creates file resources in `uploads/`.
  - `PUT`: Creates or overwrites specified file resources.
  - `DELETE`: Removes specified files from server storage.
  - `HEAD`: Returns HTTP headers without body payload.
- **Interactive Web Dashboard**:
  - Includes a modern glassmorphic web dashboard with a live HTTP API tester for testing `GET`, `POST`, `PUT`, and `DELETE` requests directly from your browser.

---

## 📁 Project Structure

```
Multi-Threaded-HTTP-WebServer/
├── src/
│   └── com/
│       └── webserver/
│           ├── HttpServer.java          # Main server entry point & socket listener loop (Producer)
│           ├── ThreadPool.java          # Worker Thread Pool managing BlockingQueue<Runnable>
│           ├── ClientHandler.java       # Connection task implementing java.lang.Runnable
│           ├── HttpRequest.java         # Stream parser (Request Line, Headers, Body)
│           ├── HttpResponse.java        # Response serializer (Status Line, Headers, Stream Writer)
│           ├── HttpMethod.java          # Enum for supported HTTP verbs
│           ├── HttpStatus.java          # Enum for standard HTTP status codes
│           └── RequestHandler.java      # Dispatcher logic for GET, POST, PUT, DELETE
├── public/
│   ├── index.html                       # Web dashboard & interactive API tester
│   ├── 404.html                         # Custom 404 error page
│   └── style.css                        # Modern glassmorphism CSS styling
├── uploads/                             # Target directory for POST/PUT file storage
└── README.md                            # Project documentation
```

---

## 🛠️ Getting Started

### Prerequisites
- **Java Development Kit (JDK 8 or higher)** installed on your system.

### Compilation

Compile all Java source files into the `bin/` directory:

#### **Windows (PowerShell)**:
```powershell
if (-not (Test-Path bin)) { New-Item -ItemType Directory -Path bin }
$javac = "javac" # Or absolute path to your javac.exe
$files = Get-ChildItem -Recurse -Filter *.java src | Select-Object -ExpandProperty FullName
& $javac -d bin $files
```

#### **Linux / macOS**:
```bash
mkdir -p bin
javac -d bin $(find src -name "*.java")
```

---

## 🏃 Running the Server

Start the web server on default port `8080`:

```bash
java -cp bin com.webserver.HttpServer 8080
```

Once started, open your web browser and navigate to:
👉 **`http://localhost:8080`**

---

## 🧪 Testing HTTP Endpoints

You can test the server using `curl`, PowerShell `Invoke-RestMethod`, or the built-in interactive dashboard:

| Method | Endpoint | Description | Sample Command |
| :--- | :--- | :--- | :--- |
| **GET** | `/` or `/index.html` | Serves main web dashboard | `curl http://localhost:8080/` |
| **POST** | `/api/upload` | Creates payload file in `uploads/` | `curl -X POST -d "Hello Java" http://localhost:8080/api/upload` |
| **PUT** | `/test.txt` | Creates or overwrites `uploads/test.txt` | `curl -X PUT -d "Updated content" http://localhost:8080/test.txt` |
| **DELETE**| `/test.txt` | Removes `uploads/test.txt` | `curl -X DELETE http://localhost:8080/test.txt` |

---

## 🧱 Architecture Diagram

```
[ Incoming Browser / Client Connections ]
                    │
                    ▼
     ┌─────────────────────────────┐
     │   ServerSocket.accept()     │  <-- Producer Thread (HttpServer.java)
     └──────────────┬──────────────┘
                    │
                    ▼  Enqueues ClientHandler (Runnable)
     ┌─────────────────────────────┐
     │ ArrayBlockingQueue<Runnable>│  <-- Bounded Queue (Backpressure Management)
     └──────────────┬──────────────┘
                    │
       ┌────────────┼────────────┐
       ▼            ▼            ▼
 ┌──────────┐ ┌──────────┐ ┌──────────┐
 │ Worker 1 │ │ Worker 2 │ │ Worker N │ <-- Consumers (ThreadPool.java)
 └────┬─────┘ └────┬─────┘ └────┬─────┘
      └────────────┼────────────┘
                   ▼
     ┌───────────────────────────┐
     │   RequestHandler.java     │ <-- Executes GET / POST / PUT / DELETE
     └───────────────────────────┘
```

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
