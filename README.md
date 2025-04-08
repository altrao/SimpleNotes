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
The application can be configured using environment variables. These can be set in docker-compose.yml or passed directly to the application.

#### Database Configuration
- `DATASOURCE_URL`: Postgres connection URL
- `DATASOURCE_USERNAME`: Database username
- `DATASOURCE_PASSWORD`: Database password

#### JWT Configuration
- `JWT_SECRET`: Secret key for JWT token signing
- `JWT_ACCESS_TOKEN_EXPIRATION`: Access token expiration in milliseconds (default: 1800000 - 30 minutes)
- `JWT_REFRESH_TOKEN_EXPIRATION`: Refresh token expiration in milliseconds (default: 259200000 - 3 days)

#### Rate Limiting
- `RATE_LIMIT_CAPACITY`: Number of requests allowed per time window (default: 30)
- `RATE_LIMIT_MINUTES`: Time window duration in minutes (default: 1)
- `RATE_LIMIT_BURST_CAPACITY`: Burst capacity for short spikes (default: 5)
- `RATE_LIMIT_BURST_SECONDS`: Burst window duration in seconds (default: 1)

#### Pagination
- `PAGINATION_LIMIT`: Maximum number of items returned in paginated responses (default: 1000)

## Security and Scalability Approach

## Security Measures

The application implements security strategy with multiple layers of protection:

### Authentication & Authorization
- **JWT-based authentication** with access and refresh tokens
- Token expiration and refresh mechanisms
- Secure password storage hashing

### API Protection
- Rate limiting to prevent abuse (visible in [RateLimitConfiguration](src/main/kotlin/com/simplenotes/configuration/RateLimitConfiguration.kt))
  - Limits are set to low by default: 30 reqs/min with a burst of 5 req/s allowed
  - The rate limiting is dealt with using Redis
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
for what I could assess it's preferred to be a **btree** because it allows multi-index queries, which is used for basically all operations.

### Architectural Scalability
- Stateless design enables horizontal scaling
- Pagination implemented for all list endpoints
- Rate limiting prevents system overload

### Possible Improvements on Performance
- Connection pooling for database efficiency
- Caching strategies for frequently accessed data
- Monitoring in place to identify bottlenecks
