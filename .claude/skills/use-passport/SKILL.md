---
name: use-passport
description: |
  econo-passport 라이브러리(com.github.JNU-econovation:econo-passport)를 현재 Spring Boot MVC 서비스에 통합한다.
  의존성 추가 → PassportConfig 설정 → @PassportAuth 사용 예시 → 테스트 헬퍼까지 한 번에 처리.

  다음 상황에서 반드시 이 스킬을 사용한다:
  - "passport 연동해줘", "econo-passport 추가해줘", "@PassportAuth 쓰고 싶어"
  - "X-User-Passport 헤더 처리", "Gateway 인증 연동", "Passport 주입 안 돼"
  - 컨트롤러에서 memberId나 roles를 Gateway 통해 받고 싶을 때
  - "/use-passport" 직접 호출
---

# use-passport

## 목표

현재 프로젝트에 econo-passport를 통합해서 `@PassportAuth Passport passport`가 컨트롤러에서 동작하도록 만든다.

인자(ARGUMENTS)가 있으면 해당 컨트롤러/서비스 이름에 맞는 예시를 생성한다.

---

## Step 1. 프로젝트 파악

다음 파일을 읽어서 현재 상태를 파악한다:

```
- build.gradle.kts (루트 또는 모듈)
- src/main/java 아래 최상위 패키지 구조
- 기존 Config 파일 유무 (glob: src/main/java/**/*Config.java)
```

파악할 것:
- JitPack 저장소가 repositories에 있는가?
- `com.github.JNU-econovation:econo-passport` 의존성이 있는가?
- `spring-boot-starter-web`이 있는가? (있으면 AutoConfiguration 자동 등록)
- 최상위 패키지명 (예: `com.blackcompany.eeos`)

---

## Step 2. build.gradle.kts 업데이트

### JitPack이 없는 경우

`repositories` 블록에 추가:
```kotlin
maven("https://jitpack.io")
```

### econo-passport 의존성이 없는 경우

`dependencies` 블록에 추가:
```kotlin
implementation("com.github.JNU-econovation:econo-passport:1.0.3")
```

> **주의**: `spring-boot-starter-web`이 없는 서비스(Reactive/WebFlux)에는 `PassportArgumentResolver`가 동작하지 않는다.
> 해당 경우 Passport 클래스 자체는 쓸 수 있지만 `@PassportAuth` 어노테이션 주입은 불가능하므로 사용자에게 알려준다.

---

## Step 3. PassportArgumentResolver 등록

`spring-boot-starter-web`이 있어도 **`@EnableWebMvc`가 있으면 AutoConfiguration이 비활성화**된다.
반드시 `WebMvcConfig.java`(또는 `@Configuration @EnableWebMvc` 클래스)를 찾아서 확인한다.

### 케이스 A — `@EnableWebMvc` 없음 (AutoConfiguration 동작)

`PassportConfig.java` 없어도 자동 등록. Skip.

### 케이스 B — `@EnableWebMvc` 있음 (수동 등록 필요)

기존 `WebMvcConfig`에 직접 등록:

```java
@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private PassportArgumentResolver passportArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(passportArgumentResolver);
    }
}
```

`@EnableWebMvc` 없이 별도 Config를 만들고 싶으면:

```java
package {최상위패키지}.config;

import com.econo.common.auth.config.AuthAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(AuthAutoConfiguration.class)
public class PassportConfig {}
```

### ⚠️ ROLE_ 접두사 주의

Spring Security의 `hasAnyRole("ADMIN")`은 내부적으로 `ROLE_ADMIN`을 찾는다.
econo-passport의 `requiredRoles = "ADMIN"`은 roles 리스트에서 `"ADMIN"` 문자열을 직접 비교한다.
JWT에서 roles 클레임이 `["USER"]`이면 `"ADMIN"` 체크는 실패한다.
Gateway의 `PassportTokenCustomizer`가 어떤 roles 값을 넣는지 확인 후 맞춰야 한다.

---

## Step 4. 예시 코드 출력

ARGUMENTS로 컨트롤러 이름이 주어졌으면 그 이름을 사용하고, 없으면 `ExampleController`로 출력한다.

### 기본 주입

```java
@GetMapping("/api/something")
public ResponseEntity<?> getSomething(@PassportAuth Passport passport) {
    Long memberId = passport.getMemberId();
    String name = passport.getName();
    List<String> roles = passport.getRoles();
    return ResponseEntity.ok(service.get(memberId));
}
```

### 권한 체크

```java
// ADMIN만 접근 가능
@GetMapping("/api/admin/something")
public ResponseEntity<?> adminOnly(
        @PassportAuth(requiredRoles = "ADMIN") Passport passport) {
    return ResponseEntity.ok(service.adminGet(passport.getMemberId()));
}

// 선택적 인증 (비로그인도 허용)
@GetMapping("/api/public/something")
public ResponseEntity<?> publicEndpoint(
        @PassportAuth(required = false) Passport passport) {
    if (passport != null) {
        return ResponseEntity.ok(service.getForMember(passport.getMemberId()));
    }
    return ResponseEntity.ok(service.getPublic());
}
```

### Passport 주요 메서드

```java
passport.getMemberId()          // Long — 회원 ID
passport.getLoginId()           // String — 로그인 아이디
passport.getName()              // String — 이름
passport.getGeneration()        // Integer — 기수
passport.getStatus()            // String — AM / RM / CM / OB
passport.getRoles()             // List<String> — 권한 목록
passport.isAdmin()              // boolean
passport.isActive()             // boolean — 유효 + 미만료
passport.isMember(Long id)      // boolean — 본인 확인
passport.canAccessMember(Long id) // boolean — 본인 or Admin
```

---

## Step 5. 테스트 헬퍼

MockMvc 테스트에서 Passport를 직접 주입하는 헬퍼를 안내한다.

```java
// 테스트 베이스 클래스 또는 유틸에 추가
import com.econo.common.auth.core.passport.Passport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

public static String mockPassportHeader(Long memberId, String... roles) throws Exception {
    Passport passport = new Passport(
        memberId,
        "testuser",
        "테스트유저",
        30,
        "AM",
        List.of(roles),
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(1)
    );
    String json = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .writeValueAsString(passport);
    return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
}

// MockMvc 테스트에서 사용
mockMvc.perform(get("/api/programs")
        .header("X-User-Passport", mockPassportHeader(1L, "USER")))
    .andExpect(status().isOk());

// ADMIN 테스트
mockMvc.perform(get("/api/admin/users")
        .header("X-User-Passport", mockPassportHeader(1L, "ADMIN")))
    .andExpect(status().isOk());
```

---

## Step 6. 전체 흐름 안내

작업이 끝나면 아래 흐름을 출력한다:

```
클라이언트
  Authorization: Bearer <JWT>
        ↓
api-gateway (BearerToPassportFilter)
  JWT 검증 → Passport 구성 → X-User-Passport: Base64(JSON) 주입
  removeRequestHeader("Authorization")
        ↓
이 서비스 (econo-passport)
  PassportArgumentResolver → X-User-Passport 파싱
  → @PassportAuth Passport passport 컨트롤러에 주입
```

**중요**: 이 서비스는 반드시 api-gateway 뒤에서만 실행해야 한다.
직접 공개 노출 시 `X-User-Passport` 헤더를 위조할 수 있다.

---

## 완료 체크리스트

- [ ] `build.gradle.kts`에 JitPack 저장소 추가됨
- [ ] `econo-passport:1.0.3` 의존성 추가됨
- [ ] PassportConfig 생성됨 (필요한 경우)
- [ ] 컨트롤러 예시 코드 확인
- [ ] 테스트 헬퍼 안내 완료
