# Modern Spring Boot 4 Backend Architecture & Project Setup Guide

This project is a **Modern Spring Boot 4 & Java 25** based Backend Starter Kit for the **postgresql.co.kr community**.
 It is designed to allow developers to focus immediately on business logic by pre-configuring an architecture and tech stack proven in enterprise environments.

[한국어 (Korean)](README.md)

---

## 1. Why This Stack?

The libraries applied in this project are not just trends but represent the **industry standard combination** for maximizing **productivity, type safety, and performance**.

### 1.1 Core Components

| Library | Version | Selection Reason & Industry Standard Description |
|:---:|:---:|:---|
| **Spring Boot** | 4.0.1 | **Next-Gen Standard.** Fully supports Jakarta EE 11 and Java 25's latest features. Maximizes throughput by natively utilizing Virtual Threads and offering Native Image (GraalVM) compatibility. |
| **Java** | 25 | **Latest.** Provides the best performance and developer experience by leveraging Project Loom (Virtual Threads) and modern language features (Pattern Matching, etc.). |
| **Gradle** | 8.x+ | **The Standard for Monorepos.** Offers 2-10x faster build speeds compared to Maven and flexible caching strategies. |

### 1.2 Data & Architecture

| Library | Role | Selection Reason |
|:---:|:---:|:---|
| **QueryDSL 5.1 (Jakarta)** | Type-Safe Query | The standard way to write complex dynamic queries in Java, catching **syntax errors at compile time**. Essential for maintainability over Criteria API. |
| **MapStruct 1.6** | DTO Mapping | Unlike ModelMapper which uses Reflection, it **generates mapping code at compile time**, resulting in zero runtime overhead. A must-have for performance-critical modern architectures. |
| **UUID Creator (v7)** | ID Generation (PK) | Random UUID v4 causes DB index fragmentation and performance degradation. **UUID v7 (Time-ordered)** allows you to enjoy the benefits of UUIDs without sacrificing DB performance. |

### 1.3 AI & Cloud

| Library | Role | Selection Reason |
|:---:|:---:|:---|
| **Spring AI 2.0** | AI Integration | Abstracts various LLMs (OpenAI, Gemini) into a standardized interface (`ChatClient`). Key module in modern Spring to prevent vendor lock-in. |
| **AWS SDK v2** | Cloud | Supports non-blocking I/O and uses less memory compared to v1. |

---

## 2. Monorepo Naming Standards & Architecture

Beyond simply dividing the project, we define standard **Naming Conventions** to clarify **roles and dependency directions**. This structure prevents 'Dependency Hell' and facilitates the transition to MSA.

### 2.1 Standard Directory Layout

A 3-tier structure based on Java/Spring ecosystem and Gradle Multi-Module Best Practices.

| Directory | Role | Description & Rationale |
|:---|:---|:---|
| **`apps`** | **Application** | **Deployment & Execution Unit.** Contains the actual server (`main` method). Its role is to assemble `modules` and run them, rather than implementing business logic directly. (e.g., `api`, `worker`, `batch`) |
| **`modules`** | **Feature/Domain** | **Business Function Unit.** Encapsulates specific domain logic (Entity, Service). Matches Gradle's 'Module' concept and is the standard term over 'domains'. (e.g., `user`, `order`) |
| **`common`** | **Shared/Infra** | **Shared Tech Support.** Provides technical utilities or infra configs without business logic. `common` or `shared` is more conventional than `libs` in the Java ecosystem. |

### 2.2 Dependency Rules

This naming convention enforces **strict one-way dependencies**.

1. **`apps`** → **`modules`** → **`common`** (Direction of reference).
2. References between **`modules`** should be cautious and kept loose to prevent circular dependencies.
3. **`common`** must never know about upper layers (`apps`, `modules`).

### 2.3 Why Monorepo?

* **Atomic Commits**: Modifications in `common` utilities and their usage in `apps/api` are managed in a single commit, preventing version mismatch issues.
* **Single Build Pipeline**: Ensures system integrity by running tests for all modules at once.

---

## 3. Step-by-Step Project Setup Guide

### Step 1: Create Project & Gradle Wrapper

The most basic way to start a project from the terminal without an IDE.

1. **Create & Move to New Folder**

    ```bash
    mkdir my-project
    cd my-project
    ```

2. **Initialize Gradle (Assuming Gradle is installed)**

    ```bash
    gradle init
    ```

    * Select type of project: `Basic`
    * Select build script DSL: `Groovy`
    * Project name: `postgresql`

3. **Check Gradle Wrapper (gradlew)**
    * A `gradlew` file is created.
    * **Important:** Use `./gradlew` instead of `gradle` for all subsequent commands to ensure complete team consistency.

### Step 2: Create Module Structure (apps/api)

```bash
# Create directories
mkdir -p apps/api/src/main/java
mkdir -p apps/api/src/main/resources
```

### Step 3: Configure settings.gradle

Define the project structure in the root `settings.gradle`.

```groovy
rootProject.name = 'postgresql-co-kr-starter'

include 'apps:api'
```

### Step 4: Configure Root build.gradle (Common Config)

Define common settings for all submodules.

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
        maven { url 'https://repo.spring.io/milestone' } // For Boot 4 / AI 2.0
    }
}
```

### Step 5: Configure App build.gradle (apps/api/build.gradle)

Write the `apps/api/build.gradle` content provided.

---

### Step 6: Standard Project Structure

Redefine the folder structure according to 'Industry Standard' naming conventions.

#### 1. Directory Structure

```text
postgresql-co-kr-starter/
├── apps/                 # [Application] Deployable Units (Boot Apps)
│   ├── api/              # API Server
│   └── worker/           # Background Worker
├── modules/              # [Feature] Business Logic Modules
│   ├── user/             # User Module
│   └── board/            # Board Module
└── common/               # [Shared] Shared Kernel (Tech Common)
    ├── core/             # Top-level Utils (Exceptions, Utils)
    └── infra/            # Infra Integration (Redis, AWS, AI)
```

**Naming Conventions:**

* **`apps` (Application)**: Deployable execution units. They assemble `modules` to run.
* **`modules` (Business Feature)**: Where business logic lives. Divided by domain (User, Board).
* **`common` (Shared Support)**:
  * **`core`**: Pure utilities and domain primitives used commonly. Minimal framework dependencies.
  * **`infra`**: Concrete implementations of frameworks like Spring Security, Redis. Focuses on system infra config.

#### Core vs Infra Role Distinction (Why?)

* **Core (Pure Domain)**
  * **Meaning**: The innermost, unchanging core value of the system.
  * **Content**: Language Level Utils, Entities, Value Objects.
  * **Principle**: Minimize external framework dependencies (POJO) to remain lightweight and reusable anywhere.
  * **Conclusion**: Adding heavy configs like Spring Security creates pollution.

* **Infra (Tech Implementation)**
  * **Meaning**: Technical foundation that makes the system run.
  * **Content**: Security Config, JPA Repositories, Redis Config, External APIs.
  * **Principle**: Where specific **framework technologies** are used to implement actual behavior.
  * **Conclusion**: `SecurityConfig` belongs in `infra`.

**💡 Tip: AutoConfiguration Exclusion**
Use `autoconfigure.exclude` in `application.yml` to flexibly control features not needed in your current environment (e.g., exclude Redis if Docker isn't running).

#### Deployment Strategy

Strategy for building and deploying modules in a monorepo.

**1. Library Dependency (Default)**
`modules/*` are built as **Libraries (Plain JAR)**. `apps/*` include them as dependencies, packaging everything into a single **Monolithic BootJAR**.

* **Use Case**: Initial development, simple deployment, Modular Monolith.
* **Config**: `bootJar { enabled = false }`, `jar { enabled = true }` in module's build.gradle.

**2. Executable BootJAR (MSA Transition)**
If a module (e.g., `modules/user`) needs to scale independently, it can be converted to an executable BootJAR.

* **Use Case**: Separating independent microservices.
* **Method**: Apply `org.springframework.boot` plugin and set `bootJar { enabled = true }`.

#### 2. `settings.gradle` (Root)

Reflects the changed structure.

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

(Same as before, note the paths)

```groovy
// ... (Similar structure to Step 4 but with subprojects block defined)
// See complete file for details on mapstruct, querydsl dependencies.
```

#### 4. `common/core/build.gradle` (Base Common)

```groovy
dependencies {
    // Utilities only (Minimal usage)
    api 'com.fasterxml.jackson.core:jackson-databind'
    api 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    api 'com.github.f4b6a3:uuid-creator:6.0.0'
    api 'org.springframework:spring-context'
}
```

#### 5. `common/infra/build.gradle` (Infrastructure Common)

```groovy
dependencies {
    api project(':common:core')
    // Redis, S3, AI, FCM, etc.
    api 'org.springframework.boot:spring-boot-starter-data-redis'
    // ...
}
```

#### 6. `modules/user/build.gradle` (User Module)

```groovy
dependencies {
    api project(':common:core') // Only Core (Infra only if needed)
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    // QueryDSL, Security...
}
```

#### 7. `modules/board/build.gradle` (Board Module)

```groovy
dependencies {
    api project(':common:core')
    implementation project(':modules:user') // Depends on User module
    // ...
}
```

#### 8. `apps/api/build.gradle` (Application)

```groovy
plugins {
    id 'org.springframework.boot'
}

dependencies {
    // 1. Feature Modules
    implementation project(':modules:user')
    implementation project(':modules:board')

    // 2. Tech Support
    implementation project(':common:infra')

    // 3. Web Layer
    implementation 'org.springframework.boot:spring-boot-starter-web'
    // Test H2
    runtimeOnly 'com.h2database:h2'
}

bootJar { enabled = true }
jar { enabled = false }

// Java 25 Preview
tasks.withType(JavaExec) {
    jvmArgs = ['--enable-preview']
}
```

---

## 4. Example Source Code

#### A. Common Module Service (`HelloCore.java`)

(Simple Service returning "Hello from Core Module!")

#### B. API Application (`ApiApplication.java`)

(Main class with `@SpringBootApplication`)

#### C. Test Controller (`TestController.java`)

Simple endpoint for verification.

```java
@RestController
public class TestController {
    @GetMapping("/")
    public Map<String, String> hello() {
        return Map.of("message", "Hello World! && Welcome. ... https://postgresql.co.kr");
    }
}
```

#### D. App Config (`application.yml`)

Configured to use H2 database for local run without external infra.

---

## 5. IDE Setup & Debugging Guide

### 4.1 IntelliJ IDEA (Recommended)

1. **Open**: Open `postgresql` folder.
2. **Import**: Click 'Load Gradle Project'.
3. **Run**: Run `ApiApplication.java`.
4. **Debug**: Set breakpoints and run in debug mode.

### 4.2 VS Code

1. **Extensions**: Install 'Extension Pack for Java' & 'Spring Boot Extension Pack'.
2. **Open**: Open folder and wait for Java Projects initialization.
3. **Run**: Run `ApiApplication` main method.

---

## 4. Gradle Wrapper & Build (CLI)

If you need to initialize environment or rebuild:

```bash
# 1. Refresh Wrapper
gradle wrapper

# 2. Clean Build
./gradlew clean build
```

**Tip:** Always use `./gradlew` to ensure consistent build environment across the team.

---

## 5. QueryDSL Setup Tips

For Spring Boot 3/4 (Jakarta EE), proper QueryDSL setup is crucial.

```bash
# Generate Q-Class
./gradlew clean compileJava
```

---

## 6. Conclusion

The current `apps/api/build.gradle` configuration is **"Well-engineered"**, not "Over-engineered".

* **Spring Boot 4 + Java 25**: Future-proof performance.
* **JPA + QueryDSL**: Balance of productivity and complexity control.
* **MapStruct**: No runtime penalty mapping.
* **UUID v7**: DB-optimized IDs.
* **Spring AI**: Modern tech stack.
* **Modularization**: Scalable architecture.

This is the **modern Java backend standard** adopted by unicorn startups and leading enterprises for new projects.

## 7. References

* [Spring Boot 4.0.1](https://spring.io/blog/2025/10/01/spring-boot-4-0-1-is-available)
* [Spring AI 2.0](https://spring.io/blog/2025/10/01/spring-ai-2-0-is-available)
* [QueryDSL 5.1](https://querydsl.com/)
* [MapStruct 1.6](https://mapstruct.org/)
* [UUID v7](https://www.rfc-editor.org/rfc/rfc9562)
* [AWS SDK v2](https://docs.aws.amazon.com/ko_kr/sdk-for-java/v2/developer-guide/welcome.html)
* [postgresql.co.kr](https://postgresql.co.kr/)
