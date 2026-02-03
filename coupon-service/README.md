# Coupon Management System

## Architecture
This is a Spring Boot application that provides RESTful APIs for managing coupons.
- **Language**: Java 21
- **Framework**: Spring Boot 4.0
- **Database**: H2 (In-memory) for persistence.
- **Concurrency/Rate Limiting**: Redis is used for concurrency & rate limiting MEGADEAL coupons.
- **Build Tool**: Maven

## Assumptions

### Docker Deployment
1. Build the image:
   ```bash
   docker compose up --build
   ```

## API Endpoints
- `POST /api/coupons/upload`: Bulk upload coupons.
- `POST /api/coupons/request`: Request a new coupon.
- `POST /api/coupons/redeem`: Redeem a coupon.
- `GET /api/coupons/stats`: Get coupon statistics.
- `GET /actuator/metrics/http.server.requests`: Get requests statistics. (`tag=uri:/api/coupons/request` add parameters if needed)
