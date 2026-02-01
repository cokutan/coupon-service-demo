# Coupon Management System

## Architecture
This is a Spring Boot application that provides RESTful APIs for managing coupons.
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Database**: H2 (In-memory) for persistence (can be switched to PostgreSQL/MySQL).
- **Caching/Rate Limiting**: Redis is used for rate limiting MEGADEAL coupons.
- **Build Tool**: Maven

## Assumptions
- "Request New Coupon" generates a new coupon code for the user.
- "Upload" allows bulk creation of coupons.
- Rate limiting for MEGADEAL is global (10 requests/second) and per-user constraints are handled via simple checks.
- Redis is required for rate limiting features.

## Setup and Deployment

### Prerequisites
- Java 17+
- Maven
- Docker (optional, for Redis)

### Running Locally
1. Start Redis (required for rate limiting):
   ```bash
   docker run -d -p 6379:6379 redis
   ```
2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Docker Deployment
1. Build the image:
   ```bash
   docker build -t coupon-service .
   ```
2. Run with Docker Compose (if available) or link to a Redis container.

## API Endpoints
- `POST /api/coupons/upload`: Bulk upload coupons.
- `POST /api/coupons/request`: Request a new coupon.
- `POST /api/coupons/redeem`: Redeem a coupon.
- `GET /api/coupons/stats`: Get coupon statistics.
