# Note Manager

A Java Spring application enhanced with a RESTful API and designed to help with **notes management**. This project provides a secure and user-friendly functionalities for creating, reading, updating, and deleting notes.
The purpose of this application is to provide a backend service for managing notes, which handles user authentication, session management, account lockout mechanisms, and input validation.
The application has two independent parts:
 - **MVC Module**: Handles traditional web-based interactions.  
 - **REST API Module**: Offers standardised endpoints for integration with external systems.

Both modules share a centralised architecture for core functionalities, including JPA repositories, services, database entities, and caching, ensuring consistency and reusability.
The addition of the REST API makes the system more flexible and keeps the MVC and API components separate, allowing them to operate independently.

[Getting started](#getting-started)

---

## Technologies Used

- **Java 21**: Core programming language for the application.
- **Spring Boot 3.4.0**: Simplifies application development with embedded server and configuration support.
- **Spring Data JPA**: Provides powerful instruments for database access.
- **Spring Security**: For user authentication and authorization.
- **PostgreSQL**: Database for storing note data.
- **Flyway**: Manages database schema migrations and populates initial data.
- **Jakarta Bean Validation**: Ensures data integrity with annotations `@NotNull` and `@NotEmpty`.
- **Lombok**: Reduces boilerplate code with annotations like `@Builder`, `@Getter`, and `@RequiredArgsConstructor`.
- **JUnit 5 & Mockito**: Provides a robust framework for writing unit and integration tests.
- **JWT (JSON Web Tokens)**: Secures authentication and session management with stateless, token-based mechanisms.
- **Springdoc OpenAPI**: Generates API documentation automatically and integrates with Swagger UI.

---

## Main Features

### 1. **[RESTful Endpoints](#existing-endpoints)**:
  - Standardized CRUD operations (`GET`, `POST`, `PUT`, `DELETE`) for managing notes.
  - Query-based searching to locate notes by a keyword.

### 2. Database Configuration
- **Database-Backed Note Management**: Notes are stored in a database managed by Flyway migrations.
- **Optimisation**: Custom queries in repositories reduce database calls and enhance performance.
  - Example: `findByUserAndKeyword` combines user filtering and keyword matching in one query.

### 2. Security
- **Separation of Notes Between Users**: Each user's notes are isolated and private.
- **User Registration**: New users can sign up via `/api/v1/signup`
- Endpoints `/api/v1/signup` and `/api/v1/login` are publicly accessible, while all other endpoints require authentication.
- **Session Management**: Stateless architecture is implemented with JWT authentication for secure token-based sessions.
- **Token Generation**:
  - JWT tokens are generated and validated using `JwtUtil` with a secret key and expiration configuration.
  - Tokens include username claims and are signed using HMAC-SHA.
- **Access Control**:
  - Role-based access control (currently, all users default to the `ROLE_USER` role).
  - JWT validation and user caching to ensure secure and efficient access to authenticated resources.
- **Account/Password Management**:
  - Passwords are hashed using a secure `PasswordEncoder` implementation.
  - Failed login attempts are tracked, with accounts locked temporarily after 3 unsuccessful login attempts.
- **Input Validation**:
  - Strict validation using annotations like `@NotNull`, `@NotEmpty`, and `@Positive`.
  - Custom exception handling to provide meaningful feedback and HTTP status codes on invalid inputs.

### 3. Business Logic
- **CRUD Operations**:
  - **List All Notes**: Retrieve all existing notes with pagination support.
  - **Get Note by ID**: Fetch a specific note using its unique ID.
  - **Create Note**: Add a new note with an auto-generated ID.
  - **Update Note**: Modify an existing note.
  - **Delete Note**: Remove a note by ID.
- **Search Notes**: Allows users to search for notes containing a specific keyword. This feature scans note titles and content for matches.
- **User Input Validation**:
  - Validates note data such as title and content using Jakarta Bean Validation.
  - Additional checks for logical consistency (e.g., non-duplicate usernames) are performed within the service layer.
- **Caching**: Data frequently reused within a request (e.g., fetched user details) is temporarily cached.

### 4. Error Handling
- **Global Exception Handling**: Provides error messages via a global exception handler.

---

## Getting Started

### Prerequisites

- **Java 21**: Ensure Java 21 is installed on your system.
- **Gradle**: This project uses Gradle for dependency management and build tasks.

### Installation

1. Clone the repository:
```shell
git clone git@github.com:ruslanaprus/goit-academy-dev-hw18.git
cd goit-academy-dev-hw18
```
2. Database Configuration and JWT Secret Setup:
 -  Copy the `.env.example` file into `.env`:
```shell
cp .env.example .env
```
 - Populate the `.env` file with the required details:
   - **Your DB details**: Set values: [`GOIT_DB2_URL, GOIT_DB_USER, GOIT_DB_PASS`] for DB connection for the Flyway plugin in `build.gradle` and for your application.
   - **JWT Secret**: Add a secure value to the `SECRET` key. This will be used for generating and validating JWT tokens.

3. Run Flyway Migration: To apply database migrations, run:
```shell
gradle flywayMigrate
```
4. Build the project:
```shell
./gradlew clean build
```
5. Run the application:
```shell
./gradlew bootRun
```
6. Visit the website at http://localhost:8080/swagger-ui/index.html to check OpenAPI documentation for exploring and testing API endpoints.

## Existing Endpoints
### Authentication Endpoints
- `POST /api/v1/signup`: Create a new user account.
- `POST /api/v1/login`: Authenticate a user and generate a JWT.

### Notes Endpoints
- `GET /api/v1/notes`: List all notes (paginated).
- `GET /api/v1/notes/{id}`: Retrieve a specific note by ID.
- `POST /api/v1/notes`: Create a new note.
- `PUT /api/v1/notes/{id}`: Update an existing note.
- `DELETE /api/v1/notes/{id}`: Delete a note by ID.
- `GET /api/v1/notes/search?keyword={keyword}`: Search notes by keyword.

---

## Future Enhancements

- **Expand Validation Rules**: Add constraints like maximum length for note content and title.
- **Optimise Caching**: Explore distributed caching solutions (e.g., Redis).
- **Implement Soft Deletes**: Instead of permanently deleting notes, mark them as archived for potential recovery.
- **Enhance Role-Based Access Control**: Introduce more roles such as `ROLE_ADMIN` to manage administrative actions.
- **Implement Sharing Notes with other users**
- **Performance Monitoring**: Integrate tools like Prometheus for tracking API performance and identifying bottlenecks.