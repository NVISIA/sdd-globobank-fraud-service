# Quick Start Guide: Initial Fraud Service Processing Implementation

**Feature**: 001-implement-initial-fraud-service-processing  
**Date**: 2025-10-21  
**Purpose**: Developer setup and testing guide for fraud detection service

## Prerequisites

### Required Software

- **Java 17**: OpenJDK 17 LTS or later
- **Maven 3.8+**: Build automation and dependency management
- **Docker Desktop**: Container runtime for local PostgreSQL and TestContainers
- **PostgreSQL 15**: Database for fraudulent cards storage (via Docker)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code with Java extensions

### Installation Commands (Windows)

```powershell
# Install Java 17 via Chocolatey
choco install openjdk17

# Install Maven
choco install maven

# Install Docker Desktop
choco install docker-desktop

# Verify installations
java -version
mvn -version
docker --version
```

### Installation Commands (macOS)

```bash
# Install Java 17 via Homebrew
brew install openjdk@17

# Install Maven
brew install maven

# Install Docker Desktop
brew install --cask docker

# Verify installations
java -version
mvn -version
docker --version
```

## Project Setup

### 1. Clone and Navigate to Project

```bash
git clone https://github.com/NVISIA/sdd-globobank-fraud-service.git
cd sdd-globobank-fraud-service
git checkout 001-implement-initial-fraud-service-processing
```

### 2. Start Local Database

```bash
# Start PostgreSQL container for development
docker run -d \
  --name fraud-postgres \
  -e POSTGRES_DB=frauddb \
  -e POSTGRES_USER=frauduser \
  -e POSTGRES_PASSWORD=fraudpass \
  -p 5432:5432 \
  postgres:15-alpine

# Verify database is running
docker ps | grep fraud-postgres
```

### 3. Build and Test Project

```bash
# Build project and run tests
mvn clean verify

# Run only unit tests
mvn test

# Run only integration tests
mvn failsafe:integration-test

# Generate test coverage report
mvn jacoco:report
```

## Development Workflow

### Running the Service Locally

```bash
# Start the fraud detection service
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Service will be available at http://localhost:8080
```

### Environment Profiles

- **default**: In-memory H2 database for quick testing
- **dev**: Local PostgreSQL with detailed logging
- **test**: TestContainers-managed PostgreSQL for integration tests
- **prod**: Production configuration with AWS RDS

### Configuration Files

- `application.yml`: Base configuration
- `application-dev.yml`: Development environment overrides
- `application-test.yml`: Test environment configuration
- `application-prod.yml`: Production environment settings

## API Testing

### Health Check Endpoint

```bash
# Test service health
curl -X GET http://localhost:8080/fraud/v1/health

# Expected response
{
  "status": "UP",
  "timestamp": "2025-10-21T14:30:00.000Z",
  "components": {
    "database": "UP",
    "fraud-rules": "UP"
  }
}
```

### Fraud Assessment API

```bash
# Test clean transaction (0 risk score)
curl -X POST http://localhost:8080/fraud/v1/risk-assessments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <oauth-token>" \
  -d '{
    "transactionId": "550e8400-e29b-41d4-a716-446655440000",
    "creditCardNumber": "4532015112830366"
  }'

# Expected response
{
  "riskScore": 0,
  "fraudulent": false,
  "timestamp": "2025-10-21T14:30:00.000Z"
}
```

### Adding Test Data

```sql
-- Connect to local PostgreSQL
docker exec -it fraud-postgres psql -U frauduser -d frauddb

-- Insert known fraudulent card
INSERT INTO fraudulent_cards (card_number, created_date, active) 
VALUES ('4000000000000002', CURRENT_TIMESTAMP, true);

# Test fraudulent transaction
curl -X POST http://localhost:8080/fraud/v1/risk-assessments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <oauth-token>" \
  -d '{
    "transactionId": "123e4567-e89b-12d3-a456-426614174000",
    "creditCardNumber": "4000000000000002"
  }'

# Expected response
{
  "riskScore": 1000,
  "fraudulent": true,
  "timestamp": "2025-10-21T14:30:00.000Z"
}
```

## Testing Strategy

### Unit Tests

```bash
# Run specific test class
mvn test -Dtest=FraudDetectionServiceTest

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Integration Tests

```bash
# Run integration tests with TestContainers
mvn verify -DskipUnitTests

# Run specific integration test
mvn verify -Dit.test=FraudServiceIntegrationTest
```

### Performance Testing

```bash
# Load test with Apache Bench (install separately)
ab -n 1000 -c 10 -H "Content-Type: application/json" \
   -H "Authorization: Bearer <token>" \
   -p transaction.json \
   http://localhost:8080/fraud/v1/risk-assessments

# Monitor response times (should be <200ms for 95% of requests)
```

## Database Management

### Schema Migration

```bash
# Flyway migrations run automatically on startup
# Manual migration (if needed)
mvn flyway:migrate -Dflyway.configFiles=src/main/resources/flyway.conf
```

### Database Reset

```bash
# Reset local database
docker stop fraud-postgres
docker rm fraud-postgres

# Restart fresh database
docker run -d \
  --name fraud-postgres \
  -e POSTGRES_DB=frauddb \
  -e POSTGRES_USER=frauduser \
  -e POSTGRES_PASSWORD=fraudpass \
  -p 5432:5432 \
  postgres:15-alpine
```

## Debugging and Troubleshooting

### Enable Debug Logging

```bash
# Run with debug logging
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dlogging.level.com.globobank.fraud=DEBUG
```

### Common Issues

#### Database Connection Failed

```bash
# Check if PostgreSQL is running
docker ps | grep fraud-postgres

# Check connection
docker exec -it fraud-postgres psql -U frauduser -d frauddb -c "SELECT 1;"
```

#### OAuth Authentication Issues

```bash
# For development, use test profile with disabled security
mvn spring-boot:run -Dspring-boot.run.profiles=test,no-security
```

#### Port Already in Use

```bash
# Run on different port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Logs and Monitoring

```bash
# View application logs
tail -f logs/fraud-service.log

# View database queries (debug mode)
grep "org.hibernate.SQL" logs/fraud-service.log
```

## Development Tools

### IDE Configuration

#### IntelliJ IDEA

1. Import as Maven project
2. Set Project SDK to Java 17
3. Enable annotation processing for Spring Boot
4. Install Spring Boot and Docker plugins

#### VS Code

```bash
# Install Java extensions
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-spring-boot
```

### Code Quality

```bash
# Run static analysis
mvn spotbugs:check

# Format code
mvn spotless:apply

# Check style compliance
mvn checkstyle:check
```

## Next Steps

1. **Implement Basic CRUD**: Set up Spring Boot project structure
2. **Add Security**: Configure OAuth 2.0 with Spring Security
3. **Database Integration**: Set up JPA repositories and entities
4. **API Development**: Implement REST controllers and validation
5. **Testing**: Write comprehensive unit and integration tests
6. **Deployment**: Configure Docker and Kubernetes manifests

## Support

- **Documentation**: `/docs/` directory for detailed architecture docs
- **API Specs**: `/specs/001-implement-initial-fraud-service-processing/contracts/`
- **Issue Tracking**: GitHub Issues for bug reports and feature requests
- **Team Contact**: <fraud-dev@globobank.com> for development questions

---

**Quick Start Complete**: Environment setup, API testing, and development workflow documented for immediate development start.
