# econo-passport

[![JitPack](https://jitpack.io/v/JNU-econovation/econo-passport.svg)](https://jitpack.io/#JNU-econovation/econo-passport)
[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)

ECONO 마이크로서비스에서 `@PassportAuth` 어노테이션을 통해 인증/인가를 간편하게 처리할 수 있는 라이브러리입니다.

api-gateway(`BearerToPassportFilter`)가 JWT를 검증하고 `X-User-Passport` 헤더를 주입하면, 이 라이브러리가 자동으로 `Passport` 객체를 컨트롤러 파라미터에 주입합니다.

---

## 설치

### 1. JitPack 저장소 추가

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
```

### 2. 의존성 추가

```kotlin
dependencies {
    implementation("com.github.JNU-econovation:econo-passport:1.0.0")
}
```

> **주의:** 이 라이브러리는 `spring-boot-starter-web`을 `compileOnly`로 선언합니다.
> 소비 서비스가 Spring MVC를 사용한다면 자체적으로 `spring-boot-starter-web`을 추가해야 합니다.
> Reactive(WebFlux) 서비스에서는 `PassportArgumentResolver`가 동작하지 않습니다.

---

## 사용법

### 기본 사용

```java
@RestController
public class ProgramController {

    @GetMapping("/api/programs")
    public ResponseEntity<List<Program>> getPrograms(@PassportAuth Passport passport) {
        Long memberId = passport.getMemberId();
        return ResponseEntity.ok(programService.getUserPrograms(memberId));
    }
}
```

### 권한 체크

```java
// ADMIN만 접근 가능
@GetMapping("/api/admin/users")
public ResponseEntity<?> getAllUsers(
        @PassportAuth(requiredRoles = "ADMIN") Passport passport) {
    return ResponseEntity.ok(userService.getAllUsers());
}

// 선택적 인증
@GetMapping("/api/public/programs")
public ResponseEntity<?> getPublicPrograms(
        @PassportAuth(required = false) Passport passport) {
    if (passport != null) {
        return ResponseEntity.ok(programService.getUserPrograms(passport.getMemberId()));
    }
    return ResponseEntity.ok(programService.getPublicPrograms());
}
```

### @PassportAuth 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `required` | boolean | true | Passport 필수 여부 |
| `validateExpiry` | boolean | true | 만료 검증 여부 |
| `requiredRoles` | String[] | {} | 필요한 권한 |
| `requireAllRoles` | boolean | false | 모든 권한 필요 여부 |
| `includeHigherRoles` | boolean | false | 권한 계층 포함 |

---

## Passport 필드

```java
Long memberId        // 회원 ID
String loginId       // 로그인 아이디
String name          // 이름
Integer generation   // 기수
String status        // 활동 상태 (AM / RM / CM / OB)
List<String> roles   // 권한 목록
LocalDateTime issuedAt
LocalDateTime expiresAt
```

---

## 자동 설정

`spring-boot-starter-web` 의존성이 있는 서비스에서 자동으로 `PassportArgumentResolver`가 등록됩니다. 별도 설정 불필요.

수동 등록이 필요한 경우:

```java
@Configuration
@Import(AuthAutoConfiguration.class)
public class PassportConfig {}
```

---

## 전체 인증 흐름

```
클라이언트
  Authorization: Bearer <JWT>
        ↓
api-gateway (BearerToPassportFilter)
  JWT 검증 → Passport 구성 → X-User-Passport: Base64(JSON)
        ↓
내부 서비스 (이 라이브러리)
  PassportArgumentResolver → @PassportAuth Passport passport
```

---

## 개발 환경

- Java 21
- Spring Boot 3.2.2
- Gradle Kotlin DSL

## 빌드

```bash
./gradlew build
./gradlew test
./gradlew spotlessApply   # 포맷팅
```
