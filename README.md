# Payment API - Sample Service for Self-Healing Monitoring

A simple payment processing service with intentional bugs for testing self-healing system.

## Features

- REST API for payment processing
- Prometheus metrics endpoint
- Structured JSON logging
- Intentional bugs:
  - NullPointerException when customer is null
  - Division by zero error
  - Error rate tracking

## Running Locally

```bash
# Build
mvn clean package

# Run
java -jar target/payment-api-1.0.0.jar

# Or with Spring Boot Maven plugin
mvn spring-boot:run
```

## API Endpoints

- `POST /api/payments` - Process payment
- `GET /api/payments/health` - Health check
- `GET /actuator/metrics` - Metrics
- `GET /actuator/prometheus` - Prometheus metrics

## Self-Healing Configuration

```yaml
system:
  name: payment-api

metrics:
  endpoint: http://localhost:8081/actuator/prometheus

logs:
  location: logs/payment-api.log
  format: json

git:
  repository: https://github.com/afsinb/payment-api.git

thresholds:
  error_rate: 0.05
  response_time_ms: 2000
```

## Example Requests

```bash
# Trigger NullPointerException
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{"customer": null, "amount": 100, "currency": "USD", "exchangeRate": 1.0}'

# Valid payment
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{"customer": "john", "amount": 100, "currency": "USD", "exchangeRate": 1.0}'

# Health check
curl http://localhost:8081/api/payments/health

# Metrics
curl http://localhost:8081/actuator/prometheus
```
