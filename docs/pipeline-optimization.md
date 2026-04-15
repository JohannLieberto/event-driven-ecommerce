# CI/CD Optimization Guide — Event-Driven E-commerce

## Objectives
- Increase meaningful code coverage
- Improve pipeline speed
- Reduce Docker image sizes
- Align with production-grade practices

---

## 1. Code Coverage Strategy

### 1.1 Exclude Non-Business Code (JaCoCo)
```xml
<configuration>
  <excludes>
    <exclude>**/config/**</exclude>
    <exclude>**/dto/**</exclude>
    <exclude>**/entity/**</exclude>
    <exclude>**/exception/**</exclude>
    <exclude>**/Application*</exclude>
  </excludes>
</configuration>
```

**Why:** Removes boilerplate from coverage → boosts % without reducing quality.

---

### 1.2 Add Targeted Unit Tests
Focus on service/business logic.

```java
@Test
void shouldFailWhenInventoryIsInsufficient() {
    when(inventoryClient.checkStock(...)).thenReturn(false);
    assertThrows(OutOfStockException.class, () -> orderService.placeOrder(...));
}
```

**Target areas:**
- Order processing
- Payment flow
- Inventory validation
- Error paths

---

### 1.3 Coverage Threshold
```xml
<rules>
  <rule>
    <element>BUNDLE</element>
    <limits>
      <limit>
        <counter>LINE</counter>
        <value>COVEREDRATIO</value>
        <minimum>0.75</minimum>
      </limit>
    </limits>
  </rule>
</rules>
```

---

## 2. Pipeline Optimization

### 2.1 Fail Fast

**Before:** Infra → E2E → Fail

**After:** Unit Tests → Infra → E2E

```groovy
stage('Unit Tests') {
  steps {
    sh 'mvn test -DskipITs'
  }
}
```

---

### 2.2 Parallel Build
```bash
mvn -T 1C clean verify
```

---

### 2.3 Cache Dependencies
- Cache `.m2` in Jenkins
- Avoid repeated downloads

---

### 2.4 Health Checks Instead of Sleep
Use:
```bash
curl http://localhost:8088/actuator/health
```

---

### 2.5 Lightweight Spring Tests
Use:
```java
@WebMvcTest
@DataJpaTest
```
instead of full `@SpringBootTest`

---

## 3. Docker Optimization

### 3.1 Multi-Stage Build
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
```

---

### 3.2 Smaller Base Images
- Replace `openjdk:17`
- With `eclipse-temurin:17-jre-alpine`

---

### 3.3 .dockerignore
```
.git
target/
node_modules/
*.log
```

---

## 4. Testing Strategy

### Test Pyramid
```
E2E (Karate)
Integration
Unit
```

| Type | Speed | Coverage |
|------|------|---------|
| Unit | Fast | High |
| Integration | Medium | Medium |
| E2E | Slow | Low |

---

## 5. Ideal Pipeline
```
1. Unit Tests
2. Build
3. Infra Start
4. Health Checks
5. E2E Tests
6. Coverage Report
```

---

## Expected Improvements

| Metric | Improvement |
|------|------------|
| Coverage | 70–85%+ |
| Pipeline Speed | 30–50% faster |
| Image Size | 60–80% smaller |
| Stability | Deterministic |

---

## Final Notes
- Coverage should come from unit tests
- Excluding boilerplate is best practice
- Optimize for speed + reliability
