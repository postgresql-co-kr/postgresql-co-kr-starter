# Spring Boot 4 기반 모던 백엔드 아키텍처 및 프로젝트 구성 가이드

이 프로젝트는 **postgresql.co.kr 커뮤니티**를 위한 **Modern Spring Boot 4 & Java 25** 기반의 백엔드 스타터 킷(Starter Kit)입니다.
엔터프라이즈 환경에서 검증된 아키텍처와 최신 기술 스택을 미리 구성하여, 복잡한 설정 없이 비즈니스 로직 개발에 바로 집중할 수 있도록 설계되었습니다.

[English](README.en.md)

---

## 1. 기술 스택 분석 (Why This Stack?)

현재 프로젝트에 적용된 라이브러리들은 단순한 유행이 아닌, **생산성, 타입 안정성(Type Safety), 성능**을 극대화하기 위한 업계 표준 조합입니다.

### 1.1 Core Components

| 라이브러리 | 버전 | 선정 이유 및 업계 표준 설명 |
|:---:|:---:|:---|
| **Spring Boot** | 4.0.1 | **차세대 표준.** Jakarta EE 11 및 Java 25의 최신 기능을 완벽히 지원하며, Native Image(GraalVM) 호환성과 Virtual Threads를 기본으로 활용하여 처리량을 극대화합니다. |
| **Java** | 25 | **Latest.** Project Loom(Virtual Threads)의 성능 개선 및 최신 언어 기능(Pattern Matching 등)을 활용하여 최고의 성능과 개발자 경험을 제공합니다. |
| **Gradle** | 8.x+ | Maven 대비 2~10배 빠른 빌드 속도와 유연한 캐싱 전략으로 대규모 모노레포 빌드 시스템의 표준입니다. |

### 1.2 Data & Architecture

| 라이브러리 | 역할 | 선정 이유 |
|:---:|:---:|:---|
| **QueryDSL 5.1 (Jakarta)** | 타입 안전 쿼리 | JPA의 복잡한 동적 쿼리를 자바 코드로 작성하여 **컴파일 시점에 문법 오류를 잡는** 표준 방식입니다. Criteria API보다 가독성이 월등히 높아 실무 필수 라이브러리입니다. |
| **MapStruct 1.6** | DTO 매핑 | Reflection을 사용하는 ModelMapper와 달리 **컴파일 시점에 매핑 코드를 생성**하므로 런타임 오버헤드가 '0'입니다. 성능이 중요한 모던 아키텍처의 필수 선택입니다. |
| **UUID Creator (v7)** | ID 생성 (PK) | 기존 UUID v4(완전 랜덤)는 DB 인덱스 단편화(Fragmentation)를 유발하여 성능을 저하시킵니다. **UUID v7(시간순 정렬)**을 사용하여 DB 성능 저하 없이 UUID의 이점을 누릴 수 있습니다. |

### 1.3 AI & Cloud

| 라이브러리 | 역할 | 선정 이유 |
|:---:|:---:|:---|
| **Spring AI 2.0** | AI 통합 | OpenAI, Gemini 등 다양한 LLM을 표준화된 인터페이스(`ChatClient`)로 추상화합니다. 공급자(Vendor) 락인(Lock-in)을 방지하는 모던 스프링의 핵심 모듈입니다. |
| **AWS SDK v2** | 클라우드 | 기존 v1 대비 비동기(Non-blocking) I/O를 지원하며 메모리 사용량이 적습니다. |

---

## 2. 모노레포 명명 표준 및 아키텍처 (Naming Standards)

단순히 프로젝트를 나누는 것을 넘어, **역할과 의존성 방향**을 명확히 하기 위해 업계에서 통용되는 표준 명명 규칙(Naming Convention)을 정의합니다. 이 구조는 '의존성 지옥(Dependency Hell)'을 방지하고 MSA 전환을 용이하게 합니다.

### 2.1 표준 폴더 구조 (Standard Directory Layout)

Java/Spring 생태계와 Gradle 멀티 모듈 모범 사례(Best Practices)에 기반한 3단 계층 구조입니다.

| 폴더명 | 역할 (Role) | 설명 및 표준 근거 |
|:---|:---|:---|
| **`apps`** | **Application** | **배포 및 실행 단위.** 실제 서버(`main` 메서드 포함)가 위치하며, 비즈니스 로직을 직접 구현하기보다 `modules`를 조립(Assembly)하여 실행하는 역할입니다. (e.g., `api`, `worker`, `batch`) |
| **`modules`** | **Feature/Domain** | **비즈니스 기능 단위.** 특정 도메인 로직(Entity, Service)을 캡슐화합니다. Gradle의 'Module' 개념과 일치하며, 과거의 `domains`보다 물리적 구성 단위를 지칭하는 데 더 적합한 표준 용어입니다. (e.g., `user`, `order`) |
| **`common`** | **Shared/Infra** | **공통 기술 지원.** 비즈니스 로직 없이, 순수하게 기술적인 유틸리티나 인프라 설정을 제공합니다. Java 진영에서는 `libs`보다 `common` 또는 `shared`라는 명칭이 '공통 라이브러리'로서 훨씬 관습적입니다. |

### 2.2 의존성 규칙 (Dependency Rules)

이 명명 규칙은 **엄격한 단방향 의존성**을 강제하기 위함입니다.

1. **`apps`** → **`modules`** → **`common`** 순서로만 참조가 가능합니다.
2. **`modules`** 간의 참조는 신중해야 하며, 순환 참조를 막기 위해 가능한 느슨하게 유지해야 합니다.
3. **`common`**은 절대 상위 계층(`apps`, `modules`)을 알지 못해야 합니다.

### 2.3 모노레포의 장점 (Why Monorepo?)

* **원자적 커밋(Atomic Commits)**: `common`의 유틸리티 수정과 이를 사용하는 `apps/api`의 수정이 하나의 커밋으로 관리되어 버전 불일치 문제를 원천 차단합니다.
* **단일 빌드 파이프라인**: 모든 모듈의 테스트를 한 번에 수행하여 전체 시스템의 정합성을 보장합니다.

---

## 3. 초보자를 위한 프로젝트 구성 가이드 (Step-by-Step)

### Step 1: 프로젝트 및 Gradle Wrapper 생성

IDE(IntelliJ) 없이 터미널에서 프로젝트를 시작하는 가장 기초적인 방법입니다.

1. **새 폴더 생성 및 이동**

    ```bash
    mkdir my-project
    cd my-project
    ```

2. **Gradle 초기화 (Gradle이 설치되어 있다고 가정)**

    ```bash
    gradle init
    ```

    * Select type of project to generate: `Basic` (멀티 모듈 구성을 위해 기본 선택)
    * Select build script DSL: `Groovy` (현재 가장 많이 쓰임)
    * Project name: `postgresql`

3. **Gradle Wrapper (gradlew) 확인**
    * `gradlew` 파일이 생성됩니다.
    * **중요:** 이후 모든 명령어는 `gradle` 대신 `./gradlew`를 사용합니다. 이는 팀원 모두가 **동일한 Gradle 버전**을 사용하도록 보장합니다.

### Step 2: 모듈 구조 생성 (apps/api)

```bash
# 디렉토리 생성
mkdir -p apps/api/src/main/java
mkdir -p apps/api/src/main/resources
```

### Step 3: settings.gradle 설정

루트 디렉토리의 `settings.gradle` 파일을 열어 프로젝트 구조를 정의합니다.

```groovy
rootProject.name = 'postgresql-co-kr-starter'

include 'apps:api'
```

### Step 4: 루트 build.gradle 설정 (공통 설정)

모든 하위 모듈에 적용될 공통 설정을 정의합니다.

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1' apply false
    id 'io.spring.dependency-management' version '1.1.7' apply false
}

allprojects {
    group = 'kr.co.postgresql'
    version = '0.0.1-SNAPSHOT'

    repositories {
        mavenCentral()
        maven { url 'https://repo.spring.io/milestone' } // Boot 4 / AI 2.0용
    }
}
```

### Step 5: 앱 build.gradle 설정 (apps/api/build.gradle)

귀하가 제공해준 `apps/api/build.gradle` 내용을 해당 위치에 작성합니다.

---

### Step 6: 표준 모듈 구성 (Standard Project Structure)

‘업계 표준’ 명명 규칙에 따라 폴더 구조를 재정의합니다. Java 진영에서는 의미가 모호한 `libs`나 `domains` 대신, **역할(Role)**에 기반한 직관적인 이름을 선호합니다.

#### 1. 폴더 구조 및 명명 규칙 (Directory Structure)

```text
postgresql-co-kr-starter/
├── apps/                 # [Application] 배포 가능한 실행 단위 (Boot Apps)
│   ├── api/              # API Server
│   └── worker/           # Background Worker
├── modules/              # [Feature] 비즈니스 기능을 담은 모듈 (Business Logic)
│   ├── user/             # 회원 모듈 (User Module)
│   └── board/            # 게시판 모듈 (Board Module)
└── common/               # [Shared] 비즈니스와 무관한 기술 공통 모듈 (Shared Kernel)
    ├── core/             # 최상위 공통 유틸리티 (Exceptions, Utils)
    └── infra/            # 외부 인프라 연동 (Redis, AWS, AI)
```

**명명 규칙의 근거 (Naming Coventions):**

* **`apps` (Application)**: 배포 가능한 실행 단위입니다. 비즈니스 로직을 직접 구현하지 않고, 하위 `modules`를 조립하여 실행하는 역할을 합니다. (e.g., API 서버, 배치 서버)
* **`modules` (Business Feature)**: 실제 업무(Business Logic)가 구현되는 곳입니다. 도메인별(User, Board)로 나뉘며, 각 모듈은 독립적인 업무 단위를 형성합니다.
* **`common` (Shared Support)**:
  * **`core`**: 비즈니스 로직에서 공통으로 사용되는 순수 유틸리티나 도메인 기초 객체를 포함합니다. 프레임워크 의존성을 최소화합니다.
  * **`infra`**: Spring Security, Redis 등 구체적인 기술/프레임워크 구현체가 위치합니다. 업무 로직보다는 시스템 인프라 설정에 집중합니다.

#### Core vs Infra 역할 구분 상세 (Why?)

* **Core (순수 영역)**
  * **의미**: 시스템의 가장 안쪽, 변하지 않는 핵심 가치.
  * **내용**: Language Level Utils(StringUtil, DateUtil) 또는 순수 비즈니스 도메인(Entity, Value Object).
  * **원칙**: 가능한 한 외부 프레임워크(Spring, Hibernate 등)에 대한 의존성을 배제하여(POJO), 어디서든 재사용 가능하겠끔 가벼운 상태를 유지해야 합니다.
  * **결론**: Spring Security라는 무거운 프레임워크 설정이 들어오면 Core의 순수성이 오염됩니다.

* **Infra (기술 구현 영역)**
  * **의미**: 시스템을 돌아가게 만드는 기술적/환경적 기반.
  * **내용**: Security Config, JPA Repository 구현체, Redis 설정, 외부 API 연동(S3, FCM).
  * **원칙**: 구체적인 **프레임워크 기술(Spring Security)**을 사용하여 실제 동작을 구현하는 곳입니다.
  * **결론**: `SecurityConfig`는 "Spring Security를 어떻게 쓸 것인가"에 대한 기술적 구현이므로 Infra 모듈이 제격입니다.

**💡 Tip: AutoConfiguration Exclusion (자동 구성 제외)**
`application.yml`에서 `autoconfigure.exclude` 옵션을 사용하여, 현재 프로젝트 환경에서 필요하지 않거나 직접 설정해야 하는 기능(예: Docker 없이 실행 시 Redis 제외, 특정 DB Driver 충돌 방지 등)을 유연하게 제어할 수 있습니다. 각자의 로컬 환경에 맞춰 적절히 제외/포함 하세요.

#### 모듈의 배포 및 실행 전략 (Deployment Strategy)

모노레포 환경에서 각 모듈(`apps`, `modules`)을 어떻게 빌드하고 배포하는지에 대한 전략입니다. 기본적으로 **모듈러 모놀리스(Modular Monolith)** 구조를 따르지만, 언제든 **MSA(Microservices)**로 전환 가능한 유연함을 가집니다.

**1. 라이브러리 의존성 방식 (Library Dependency - Default)**
기본적으로 `modules/*`는 독립적으로 실행되지 않는 **라이브러리(Plain JAR)** 형태로 빌드됩니다.
`apps/*` (예: `apps:api`)가 이 모듈들을 의존성(`implementation project(':modules:user')`)으로 포함하여, 최종적으로는 **하나의 실행 가능한 애플리케이션(Monolithic BootJAR)** 안에 모두 패키징됩니다.

* **용도**: 초기 개발 단계, 배포 복잡도 최소화, 모듈러 모놀리스 아키텍처.
* **설정**: `modules/*`의 `build.gradle`에서 `bootJar { enabled = false }`, `jar { enabled = true }`로 설정하여 실행 불가능한 일반 JAR로 만듭니다.

**2. 실행 가능한 형태 배포 (Executable BootJAR) - MSA 전환 시**
만약 `modules/user`의 트래픽이 급증하여 독립적인 **User 마이크로서비스**로 분리 배포하고 싶다면, 해당 모듈을 감싸는 앱(`apps/user-api`)을 만들거나 모듈 자체를 실행 가능하게 변경할 수 있습니다.

* **용도**: `modules/user`만 따로 떼어내어 독립적인 서버 프로세스로 배포 및 스케일 아웃(Scale-out).
* **방법**: 분리하려는 모듈의 `build.gradle`에 `id 'org.springframework.boot'` 플러그인을 적용하고, `bootJar { enabled = true }`로 설정 변경. 이렇게 하면 해당 모듈은 더 이상 라이브러리가 아닌 독립 실행 가능한 JAR 파일이 됩니다.

#### 2. `settings.gradle` (Root)

변경된 폴더 구조를 반영합니다.

```groovy
pluginManagement {
    repositories {
        mavenCentral()
        maven { url 'https://repo.spring.io/milestone' }
        maven { url 'https://repo.spring.io/snapshot' }
        gradlePluginPortal()
    }
}

rootProject.name = 'postgresql-co-kr-starter'

// 1. Applications (Deployables)
include 'apps:api'
include 'apps:worker'

// 2. Modules (Business Features)
include 'modules:user'
include 'modules:board'

// 3. Common (Shared Support)
include 'common:core'
include 'common:infra'
```

#### 3. `build.gradle` (Root)

(이전과 동일, 경로만 변경됨을 인지하세요)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1' apply false
    id 'io.spring.dependency-management' version '1.1.7' apply false
}

allprojects {
    group = 'kr.co.postgresql'
    version = '0.0.1-SNAPSHOT'

    repositories {
        mavenCentral()
        maven { url 'https://repo.spring.io/milestone' }
    }
}

subprojects {
    apply plugin: 'java-library'
    apply plugin: 'io.spring.dependency-management'

    dependencyManagement {
        imports {
            mavenBom "org.springframework.boot:spring-boot-dependencies:4.0.1"
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    dependencies {
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'

        // MapStruct (Common)
        implementation 'org.mapstruct:mapstruct:1.6.3'
        annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
        annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'

        testImplementation 'org.junit.jupiter:junit-jupiter'
        testImplementation 'org.assertj:assertj-core'
    }

    tasks.named('test') {
        useJUnitPlatform()
        failOnNoDiscoveredTests = false
    }
}
```

#### 4. `common/core/build.gradle` (Base Common)

모든 모듈의 근간이 되는 공통 모듈입니다.

```groovy
dependencies {
    // 가장 기초적인 유틸리티만 포함 (의존성 최소화 원칙)
    api 'com.fasterxml.jackson.core:jackson-databind'
    api 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    api 'com.github.f4b6a3:uuid-creator:6.0.0'
    api 'org.springframework:spring-context'
}
```

#### 5. `common/infra/build.gradle` (Infrastructure Common)

외부 시스템 연동을 캡슐화합니다.

```groovy
dependencies {
    api project(':common:core')

    // Redis, S3, AI, FCM 등 기술 의존성 집약
    api 'org.springframework.boot:spring-boot-starter-data-redis'
    api platform('software.amazon.awssdk:bom:2.29.35')
    api 'software.amazon.awssdk:s3'
    api 'com.google.firebase:firebase-admin:9.2.0'
    api 'org.springframework.ai:spring-ai-vertex-ai-gemini-spring-boot-starter'

    // Auth Utils
    api 'io.jsonwebtoken:jjwt-api:0.13.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.13.0'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.13.0'
}
```

#### 6. `modules/user/build.gradle` (User Module)

```groovy
dependencies {
    api project(':common:core') // Core만 의존 (Infra는 필요 시에만)

    // 비즈니스 로직에 필요한 DB 기술
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'

    // QueryDSL
    api 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
    annotationProcessor "com.querydsl:querydsl-apt:5.1.0:jakarta"
    annotationProcessor "jakarta.annotation:jakarta.annotation-api"
    annotationProcessor "jakarta.persistence:jakarta.persistence-api"

    // Security
    api 'org.springframework.boot:spring-boot-starter-security'
}
// Q-Class 설정 생략 (동일)
```

#### 7. `modules/board/build.gradle` (Board Module)

```groovy
dependencies {
    api project(':common:core')
    implementation project(':modules:user') // User 모듈 참조

    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    // ... QueryDSL 설정
}
```

#### 8. `apps/api/build.gradle` (Application)

```groovy
plugins {
    id 'org.springframework.boot'
}

dependencies {
    // 1. Feature Modules (Business)
    implementation project(':modules:user')
    implementation project(':modules:board')

    // 2. Technical Support (Infra)
    implementation project(':common:infra')

    // 3. Web Layer Config
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // 로컬 테스트용 인메모리 DB
    runtimeOnly 'com.h2database:h2'
}

bootJar { enabled = true }
jar { enabled = false }

// Java 25 Preview 기능 활성화 (필요 시)
tasks.withType(JavaExec) {
    jvmArgs = ['--enable-preview']
}
```

---

## 4. 빠른 실행을 위한 예제 소스 코드 (Example Source)

프로젝트 구조를 잡은 후, 실제로 잘 동작하는지 확인하기 위한 "Hello World" 코드입니다.

#### A. 공통 모듈 서비스 (`libs/core/src/main/java/kr/co/postgresql/core/HelloCore.java`)

```java
package kr.co.postgresql.core;

import org.springframework.stereotype.Service;

@Service
public class HelloCore {
    public String getGreeting() {
        return "Hello from Core Module! (Shared Logic)";
    }
}
```

#### B. API 실행 클래스 (`apps/api/src/main/java/kr/co/postgresql/api/ApiApplication.java`)

```java
package kr.co.postgresql.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "kr.co.postgresql")
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
```

#### C. 테스트 컨트롤러 (`apps/api/src/main/java/kr/co/postgresql/api/TestController.java`)

서버가 정상적으로 실행되는지 확인하기 위한 간단한 엔드포인트입니다.

```java
package kr.co.postgresql.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of("message", "Hello World! && 환영합니다. 여기까지 오시느라 수많은 에러와 싸우며 삽질(?) 좀 하셨죠? 대단합니다! 👏👏 그 끈기에 박수를 보내며, 당신의 멋진 개발 여정을 응원합니다. https://postgresql.co.kr");
    }
}
```

#### D. 앱 설정 (`apps/api/src/main/resources/application.yml`)

로컬 실행 시 H2 데이터베이스를 사용하여 별도 인프라 없이도 서버가 실행되도록 설정합니다.

```yaml
spring:
  application:
    name: postgresql-api

  # 로컬 개발 시 외부 인프라 자동 구성 제외 (Redis, AI 등)
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.ai.autoconfigure.vertexai.gemini.VertexAiGeminiAutoConfiguration

  # 로컬 테스트용 H2 DB 설정
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    open-in-view: false

logging:
  level:
    root: INFO
    kr.co.postgresql: DEBUG
```

---

## 5. IDE 실행 및 디버깅 가이드 (IntelliJ & VS Code)

이미 프로젝트에는 `HelloCore`(공통 모듈)와 `ApiApplication`(API 서버)이 미리 생성되어 있습니다. IDE에서 이를 열고 바로 실행하는 방법을 안내합니다.

### 4.1 IntelliJ IDEA (권장)

Spring Boot 개발에 가장 강력한 도구입니다.

1. **Open**: `postgresql` 폴더를 엽니다 (`File > Open`).
2. **Import**: 'Load Gradle Project' 팝업이 뜨면 클릭합니다.
    * 팝업이 안 뜨는 경우: 파일을 열었을 때 우측 상단에 나타나는 **코끼리 아이콘(Load Gradle Changes)**을 클릭하거나, 우측 사이드바의 **Gradle 탭**을 열고 **새로고침(Reload)** 아이콘을 클릭합니다.
3. **Run**:
    * `apps/api/src/main/java/kr/co/postgresql/api/ApiApplication.java` 파일을 엽니다.
    * 왼쪽의 초록색 화살표(▶)를 클릭하고 `Run 'ApiApplication'`을 선택합니다.
4. **Debug**:
    * 코드 라인 번호 옆을 클릭하여 **Breakpoint**(빨간 원)를 찍습니다.
    * 초록색 화살표(▶) 대신 벌레 모양 아이콘(🐞 Debug)을 클릭하여 실행합니다.
5. **확인**:
    * 브라우저에서 `http://localhost:8080` 접속
    * 결과: `API Server: Hello from Core Module! (Shared Logic)`

### 4.2 VS Code

가볍고 무료이며, Extension을 통해 훌륭한 Java 개발 환경이 됩니다.

1. **Extensions 설치**: 'Extension Pack for Java' (Microsoft) 및 'Spring Boot Extension Pack' (VMware) 설치 필수.
2. **Open**: `postgresql` 폴더를 엽니다.
3. **Wait**: 하단 상태바에 'Opening Java Projects'가 완료될 때까지 기다립니다.
4. **Run**:
    * `ApiApplication.java` 파일을 엽니다.
    * `main` 메소드 위에 나타나는 `Run | Debug` 글자 중 `Run` 클릭.
5. **Troubleshooting**:
    * Java 25 인식이 안 될 경우 `cmd + ,` (설정) -> `java.configuration.runtimes` 에서 JDK 25 경로를 추가해야 할 수 있습니다.

---

## 4. Gradle Wrapper 설정 및 프로젝트 빌드 (CLI)

소스 코드를 새로 내려받았거나(`docs/java/postgresql-co-kr-starter` 복사 등) 환경을 초기화해야 할 경우, 터미널에서 다음 명령어를 실행하여 Gradle Wrapper를 갱신하고 빌드를 검증합니다.

```bash
# 1. Gradle Wrapper 갱신 (설치된 Gradle 버전에 맞춰 스크립트 생성)
gradle wrapper

# 2. 클린 빌드 실행 (테스트 포함, 의존성 다운로드)
./gradlew clean build
```

이 과정은 프로젝트의 `gradlew` 스크립트가 실행 권한을 가지고 올바르게 동작하도록 보장합니다.

> **Tip: .gradle 폴더에 여러 버전(예: 8.13, 9.2.1)이 생성되는 이유?**
>
> 프로젝트 루트의 `.gradle` 폴더 안에 여러 버전 폴더가 공존하는 것은, **로컬에 설치된 글로벌 `gradle`**(예: 9.x)과 **프로젝트 래퍼 `./gradlew`**(예: 8.x)가 서로 다른 버전을 사용해 빌드를 수행했기 때문입니다.
> Gradle은 버전별로 독립적인 캐시와 데몬을 관리하므로, 버전이 다르면 각각의 폴더가 생성됩니다.
>
> * **`gradle wrapper`**: 로컬에 설치된 최신 Gradle을 기준으로 프로젝트의 래퍼(Wrapper) 설정을 갱신합니다.
> * **`./gradlew`**: `gradle-wrapper.properties`에 명시된 특정 버전을 사용하여 빌드를 수행합니다.
>
> **따라서, 일관된 빌드 환경을 위해 초기 설정 이후에는 반드시 `./gradlew` 명령어를 사용하는 것이 원칙입니다.**

---

## 5. QueryDSL 및 Annotation Processor 설정 팁

Spring Boot 3/4 환경에서는 `jakarta.*` 패키지를 사용해야 하므로 QueryDSL 설정 시 주의가 필요합니다. 제공해주신 설정은 완벽합니다.

**QueryDSL 빌드 방법:**
Q-Class(쿼리 타입 클래스)는 컴파일 시점에 생성됩니다.

```bash
# Q-Class 생성 및 컴파일
./gradlew clean compileJava
```

성공 시 `apps/api/src/main/generated` 경로에 `QUser.java` 등의 파일이 생성됩니다.

---

## 6. 결론

지금 구성하신 `apps/api/build.gradle`은 **"Over-engineering"이 아닌 "Well-engineered"** 상태입니다.

* **Spring Boot 4 + Java 25**: 미래 지향적 성능 확보
* **JPA + QueryDSL**: 생산성과 복잡도 해결의 완벽한 조화
* **MapStruct**: 런타임 성능 저하 없는 매핑
* **UUID v7**: DB 성능을 고려한 식별자 전략
* **Spring AI**: 최신 트렌드 반영
* **모듈화**: `apps`와 `libs` 분리로 확장성 확보

이 구성은 유니콘 스타트업이나 대기업의 신규 프로젝트에서 채택하는 **가장 모던한 자바 백엔드 표준**입니다.
