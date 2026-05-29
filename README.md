# econo-passport

[![JitPack](https://jitpack.io/v/JNU-econovation/econo-passport.svg)](https://jitpack.io/#JNU-econovation/econo-passport)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)

ECONO 마이크로서비스에서 `@PassportAuth` 어노테이션으로 인증된 사용자 정보를 컨트롤러에 주입하는 라이브러리.

api-gateway(`BearerToPassportFilter`)가 JWT를 검증하고 `X-User-Passport` 헤더를 주입하면, 이 라이브러리가 `Passport` 객체를 자동으로 파라미터에 바인딩합니다.

---

## 설치

### Step 1. JitPack 저장소 추가

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
```

### Step 2. 의존성 추가

```kotlin
dependencies {
    implementation("com.github.JNU-econovation:econo-passport:1.0.0")
}
```

> **⚠️ 주의 (Spring MVC 사용 서비스)**
> 이 라이브러리는 `spring-boot-starter-web`을 `compileOnly`로 선언합니다.
> MVC 서비스는 자체적으로 `spring-boot-starter-web`을 추가해야 합니다 (이미 있으면 그대로).

> **Reactive(WebFlux) 서비스**에서는 `PassportArgumentResolver`가 동작하지 않습니다.
> Gateway 자체가 Reactive라면 `Passport` 클래스만 직접 사용하세요.

### Step 3. 자동 설정 확인

`spring-boot-starter-web`이 있는 서비스는 **별도 설정 없이** `PassportArgumentResolver`가 자동 등록됩니다.

자동 설정이 안 되는 환경이라면:

```java
@Configuration
@Import(AuthAutoConfiguration.class)
public class PassportConfig {}
```

---

## 사용법

### 기본

```java
@RestController
public class ProgramController {

    @GetMapping("/api/programs")
    public ResponseEntity<?> getPrograms(@PassportAuth Passport passport) {
        Long memberId = passport.getMemberId();
        return ResponseEntity.ok(programService.getPrograms(memberId));
    }
}
```

### 권한 체크

```java
// ADMIN만
@GetMapping("/api/admin/users")
public ResponseEntity<?> getUsers(
        @PassportAuth(requiredRoles = "ADMIN") Passport passport) { ... }

// ADMIN 또는 MANAGER (OR 조건)
@GetMapping("/api/programs")
public ResponseEntity<?> getPrograms(
        @PassportAuth(requiredRoles = {"ADMIN", "MANAGER"}) Passport passport) { ... }

// 선택적 인증 (비로그인 허용)
@GetMapping("/api/public")
public ResponseEntity<?> getPublic(
        @PassportAuth(required = false) Passport passport) {
    if (passport != null) { /* 인증된 사용자 */ }
}
```

### @PassportAuth 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `required` | boolean | `true` | Passport 필수 여부 |
| `validateExpiry` | boolean | `true` | 만료 시간 검증 |
| `requiredRoles` | String[] | `{}` | 필요 권한 (OR 조건) |
| `requireAllRoles` | boolean | `false` | AND 조건으로 전환 |
| `includeHigherRoles` | boolean | `false` | 권한 계층 포함 |

---

## Passport 필드

```java
Long memberId          // 회원 ID
String loginId         // 로그인 아이디
String name            // 이름
Integer generation     // 기수
String status          // 활동 상태 (AM / RM / CM / OB)
List<String> roles     // 권한 목록
LocalDateTime issuedAt
LocalDateTime expiresAt

// 유틸 메서드
boolean isAdmin()
boolean isExpired()
boolean isActive()
boolean isMember(Long memberId)
boolean canAccessMember(Long targetMemberId)
boolean hasRole(String role)
boolean hasAnyRole(String... roles)
```

---

## 전체 인증 흐름

```
클라이언트
  Authorization: Bearer <RS256 JWT>
        ↓
api-gateway (BearerToPassportFilter, order=-1)
  JWT 검증(JWKS) → Passport 구성 → X-User-Passport: Base64(JSON) 주입
  removeRequestHeader("Authorization")  ← 내부 서비스로 Bearer 미전달
        ↓
내부 서비스 (이 라이브러리)
  PassportArgumentResolver → X-User-Passport 디코딩
  → @PassportAuth Passport passport 주입
```

---

## 테스트에서 사용

MockMvc 테스트에서 `X-User-Passport` 헤더를 직접 주입합니다.

```java
@Test
void testWithPassport() throws Exception {
    Passport mockPassport = new Passport(
        1L, "user01", "홍길동", 30, "AM",
        List.of("USER"),
        LocalDateTime.now(), LocalDateTime.now().plusHours(1)
    );
    String encoded = Base64.getEncoder().encodeToString(
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .writeValueAsString(mockPassport)
            .getBytes(StandardCharsets.UTF_8)
    );

    mockMvc.perform(get("/api/programs")
            .header("X-User-Passport", encoded))
        .andExpect(status().isOk());
}
```

---

## 개발 환경

- Java 21
- Spring Boot 3.2.2
- Gradle Kotlin DSL

## 빌드

```bash
./gradlew build          # 빌드 + 테스트 + Spotless 검사
./gradlew test           # 테스트만
./gradlew spotlessApply  # 코드 포맷팅
```
