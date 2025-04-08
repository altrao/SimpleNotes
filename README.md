# Simple Notes

## Getting Started

### Prerequisites
- Docker installed

### Running the Application
1. Clone this repository: 
```bash
git clone https://github.com/altrao/SimpleNotes.git
```
3. Enter the folder 
```bash
cd SimpleNotes
```
2. Run the application with Docker Compose:
```bash
docker compose up -d
```
3. The application will be available at: http://localhost:8080

### Configuration
Key environment variables (set in docker-compose.yml):
- `JWT_SECRET`: Secret key for JWT token signing
- `JWT_ACCESS_TOKEN_EXPIRATION`: Access token expiration (ms)
- `JWT_REFRESH_TOKEN_EXPIRATION`: Refresh token expiration (ms)
- Database credentials and other settings

## Security and Scalability Approach

## Security Measures

The application implements security strategy with multiple layers of protection:

### Authentication & Authorization
- **JWT-based authentication** with access and refresh tokens
- Token expiration and refresh mechanisms
- Secure password storage hashing

### API Protection
- Rate limiting to prevent abuse (visible in [RateLimitConfiguration](src/main/kotlin/com/simplenotes/configuration/RateLimitConfiguration.kt))
  - The rate limiting is dealt with in-memory (though it could be using Redis for distributed limiting, but for this case I kept it in-memory for simplicity)
- Input validation for all API requests
- Only authenticated requests can go through

## Scalability Strategy

### Database Indexing (Postgres)
The indexing strategy focuses on the most critical query paths:

1. **Primary Keys**: All tables have proper primary keys with automatic indexing
2. **Foreign Keys**: Indexed to optimize join operations
3. **Query Patterns**:
   - Composite indexes for common filter combinations
   - Partial indexes for frequently queried subsets of data
4. **Indexing strategies**:
   - Though it would make sense for username and user id to be **hash** indexes as there is no range queries and only exact matches,
for what I could assess it's preferred to be a **btree** because they allow multi-index queries, which is used for basically all operations.

### Architectural Scalability
- Stateless design enables horizontal scaling
- Pagination implemented for all list endpoints
- Rate limiting prevents system overload
- Asynchronous processing for background tasks

### Performance Considerations
- Connection pooling for database efficiency
- Caching strategies for frequently accessed data
- Monitoring in place to identify bottlenecks
