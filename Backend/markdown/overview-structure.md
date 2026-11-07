Overview: Modular Monolith Architecture
Concepts: A monolith system but with design for future microservices migration. Focus on isolate business logic, configurations and dependencies.

Overview rules: 
- Each module should be independent and can be developed, tested, and deployed separately.
- Modules should have clear boundaries and should not have direct dependencies on each other. They can depend on shared libraries.
- Common logic should be extracted into shared libraries.
- Modules should be able to communicate with each other through contracts, implementations can be either business services or api client services.
- Modules should be able to be scaled independently.
- Each module should have its own database.
- Entities between different bounded contexts must not have direct relationships or foreign keys.
- Repositories and services use JPA entities directly; do not introduce a parallel Models layer.
- Use `Shared.Utils.QueryBuilder.QueryBuilder` for dynamic read queries and pagination instead of Criteria or QueryDSL. Use JPA for entity loading and persistence.

Authentication and Authorization rules:
- Authentication concept should be stateless. 
- User state change will be fired by events. Kafka, RabbitMQ or similar message broker is recommended.
- Revoked tokens must be stored in a database or cache until their expiration time, should not be stored permanently.
- User infomation should be extracted from JWT payload. Not from issuer service.
- Authorization should be done by checking user roles and permissions from JWT payload. If any changes happen to user roles or permissions, all old access tokens should be invalidated. user will grant new tokens if refresh token is still valid.
- Only one type of token is JWT, no api keys or other types of tokens. to ensure auditability and security.
