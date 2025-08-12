# Shoot - Real-time Chat Application

## Project Overview
Shoot is a Spring Boot Kotlin real-time chat application implementing hexagonal architecture with Domain-Driven Design (DDD) patterns. Features include WebSocket communication, Redis Stream messaging, Kafka persistence, MongoDB/PostgreSQL storage, and JWT authentication.

## Architecture
- **Hexagonal Architecture (Ports & Adapters)**
- **Domain-Driven Design (DDD)**
- **Event-Driven Architecture**
- **CQRS patterns for chat operations**

## Technology Stack
- **Backend**: Spring Boot 3.x, Kotlin
- **Real-time**: WebSocket with STOMP, Redis Stream
- **Persistence**: Kafka, MongoDB, PostgreSQL
- **Authentication**: JWT
- **Caching**: Redis
- **Logging**: KotlinLogging

## Key Components

### Domain Structure
- `domain/` - Core business logic and entities
- `application/` - Use cases and application services
- `adapter/` - Infrastructure adapters (in/out)
- `infrastructure/` - Cross-cutting concerns

### Real-time Features
- **WebSocket**: `/topic/chat/{roomId}` for chat messages
- **Redis Stream**: Message queuing and real-time delivery
- **Event Listeners**: Domain event processing
- **Session Management**: Active user tracking

### Database Schema
- **PostgreSQL**: Users, friend relationships, notifications
- **MongoDB**: Chat messages and reactions
- **Redis**: Sessions, caching, streams

## Common Development Tasks

### Running the Application
```bash
./gradlew bootRun
```

### Testing
```bash
./gradlew test
```

### Building
```bash
./gradlew build
```

### Code Style
- Follow Kotlin conventions
- Use single-expression functions when appropriate
- Prefer `in` operator over `contains()`
- Minimize unnecessary `this` keywords
- Remove redundant comments

### Adding New Features
1. Create domain entities/value objects in `domain/`
2. Define ports in `application/port/`
3. Implement use cases in `application/service/`
4. Create adapters in `adapter/in|out/`
5. Add event listeners if needed
6. Write tests

### WebSocket Development
- Use `WebSocketMessageBroker` for sending messages
- Implement retry logic for failed deliveries
- Use Redis fallback for offline users
- Follow STOMP topic conventions

### Event-Driven Development
- Create domain events in `domain/event/`
- Use `@TransactionalEventListener` for processing
- Implement in `application/service/event/`
- Use `SpringEventPublisher` for publishing

## Port Naming Conventions
- **LoadPort**: Query operations (findById, findAll)
- **SavePort**: Persistence operations (save, update)
- **QueryPort**: Complex queries and searches
- **CommandPort**: Command operations (create, delete)

## Testing Strategy
- Unit tests for domain logic
- Integration tests for adapters
- WebSocket tests for real-time features
- Event processing tests

## Security
- JWT authentication on all endpoints
- WebSocket session validation
- Input validation on all commands
- SQL injection prevention

## Performance Considerations
- Redis caching for frequently accessed data
- Kafka for message persistence
- Connection pooling for databases
- Async processing for non-critical operations

## Troubleshooting
- Check Redis connection for WebSocket issues
- Verify Kafka topics for message persistence
- Monitor application logs for event processing
- Validate JWT tokens for authentication errors