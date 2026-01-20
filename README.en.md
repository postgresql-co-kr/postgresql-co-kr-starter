# Between Ideal Structure and Reality

Project structures presented in documentation and conferences always look clean. Clearly separated layers like `apps / modules / common`, unidirectional dependencies, consistent build strategies. When you see them, you think, "I just need to do this."

But real-world projects are rarely like that.

For SI projects, things are somewhat better. When an architect leads and says, "We're going this way," there's resistance along the way, but schedule pressures and authority tend to sort things out.

The problem lies in maintenance work or internal projects.

- "Why does it have to be this complex?"
- "It's working fine now. Do we really need to change it?"
- "I use Eclipse, and this structure is inconvenient."
- "Gradle multi-module is hard to debug."
- "Is there anyone besides me who will maintain this?"

None of these are wrong. And the starting point of these questions is usually not technology—it's **people**.

When there's already code in production, everyone has their familiar ways, and in a busy schedule, "making it work right now" is the top priority, architecture often seems like a luxury.

So many projects start like this:

> "Let's build it as one first, and split it later."

And that "later" almost never comes.

---

This document is not meant to impose a perfect ideal. Nor is it meant to declare "this is the answer."

Instead, it aims to explain:

- Why this kind of structure becomes necessary
- At what point you hit its limits
- And where a realistic compromise lies

It's not about doing MSA from the start, nor about separating everything from day one.

Rather,

> Let's start in a direction where it's one JAR today but can be split someday

This document describes a structure for establishing that minimal agreement.

---

# Modern Backend Architecture and Project Configuration Guide Based on Spring Boot 4

This project is a **Modern Spring Boot 4 & Java 25** based backend starter kit for the **postgresql.co.kr community**.

It comes pre-configured with architectures and technology stacks proven in enterprise environments, so you can focus on business logic instead of spending time on complex configurations.

[한국어](README.md)

---

## 1. Technology Stack Analysis (Why This Stack?)

The libraries included in this project are not chosen simply because they're the latest. They are a combination designed to balance **productivity, type safety, and performance** in real production environments.

### 1.1 Core Components

| Library     | Version | Rationale                                                                      |
| ----------- | ------- | ------------------------------------------------------------------------------ |
| Spring Boot | 4.0.1   | Supports Jakarta EE 11, Java 25. Next-gen standard with Virtual Threads & GraalVM |
| Java        | 25      | Project Loom maturity stage. Latest language features and performance combined |
| Gradle      | 8.x+    | Multi-module standard with fast build speeds and caching strategies            |

### 1.2 Data & Architecture

| Library       | Role            | Rationale                                                |
| ------------- | --------------- | -------------------------------------------------------- |
| QueryDSL 5.1  | Type-safe query | Express dynamic queries in code, catch errors at compile time |
| MapStruct 1.6 | DTO mapping     | Compile-time code generation without runtime reflection  |
| UUID v7       | ID strategy     | Time-ordered UUID to prevent DB index performance degradation |

### 1.3 AI & Cloud

| Library       | Role       | Rationale                                               |
| ------------- | ---------- | ------------------------------------------------------- |
| Spring AI 2.0 | AI integration | Standard abstraction layer to avoid LLM vendor lock-in |
| AWS SDK v2    | Cloud      | Non-blocking I/O and low memory footprint               |

---

## 2. Monorepo Naming Standards and Architecture

The purpose of this structure is not simply to divide the project, but to **reveal roles and dependency directions at the code level**.

### 2.1 Standard Directory Structure

| Folder  | Role        | Description                                           |
| ------- | ----------- | ----------------------------------------------------- |
| apps    | Application | Execution/deployment unit. Assembles modules into apps |
| modules | Feature     | Where actual business domain logic resides            |
| common  | Shared      | Common technical areas unrelated to business          |

### 2.2 Dependency Rules

1. `apps → modules → common` unidirectional dependency
2. Minimize cross-module references; circular dependencies are forbidden
3. common must never know about upper layers

The moment these rules break, the structure begins to collapse quickly.

---

## 3. Project Configuration and the Meaning of Code

This section is not about explaining individual settings. It focuses on **how to read how code and configuration enforce structure and where they draw boundaries**.

---

### 3.1 Gradle Wrapper

```bash
gradle wrapper
./gradlew clean build
```

In this project, the Wrapper is not optional. All builds and executions are based on the premise that they will only use **the Gradle version defined by this repository**.

Without a Wrapper, as multi-module structures grow, these problems inevitably occur:

- Gradle version differences between local and CI
- Plugin DSL behavior inconsistencies
- Build errors reproducible only on specific versions

The Wrapper eliminates all these variables. Build problems that occur afterward are no longer environment issues—they converge to **design or code issues**.

In other words, the purpose of the Wrapper is not convenience—it's **clarifying build responsibility**.

---

### 3.2 settings.gradle

```groovy
rootProject.name = 'postgresql-co-kr-starter'

include 'apps:api'
include 'modules:user'
include 'common:core'
```

`settings.gradle` is the structure declaration. Modules included here all carry the meaning of **code that will be maintained long-term**.

What's important in this file is not *what was included* but **what was not included yet**.

- Experimental code
- Features with unclear direction
- Temporary response logic

When these elements start being `include`d indiscriminately, the boundaries of the monorepo quickly blur.

Managing `settings.gradle` conservatively means:

- The structure serves as documentation
- Adding a module becomes a deliberate decision
- The project's growth direction is recorded in code

---

### 3.3 Root build.gradle

```groovy
allprojects {
    group = 'kr.co.postgresql'
    version = '0.0.1-SNAPSHOT'
}
```

The root `build.gradle` is not a functionality definition file. It's an area where **only minimal common rules for the entire project should exist**.

If the following code starts appearing here, it's a danger sign:

- Plugin settings for specific modules
- Business dependency declarations
- Environment-specific branching logic

The more common rules there are, the cost of change increases not linearly but exponentially.

In this structure:

- The root rarely changes, like a constitution
- Each module's `build.gradle` handles actual policies

---

### 3.4 common/core

```groovy
dependencies {
    api 'com.github.f4b6a3:uuid-creator:6.0.0'
}
```

`core` is the starting point for all dependency directions. The most important characteristic of this module is that **it depends on nothing**.

The moment a framework enters, this module is no longer a safe foundation.

Code that can go into `core` is clear:

- Pure utilities
- Type definitions
- Identifiers, rules, common value objects

The more this boundary is maintained, the longer the upper structure remains stable.

---

### 3.5 common/infra

```groovy
api 'org.springframework.boot:spring-boot-starter-data-redis'
api 'software.amazon.awssdk:s3'
```

`infra` is the area that interfaces with the external world. DB, cache, message brokers, and cloud SDKs gather here.

The important point is to **ensure business logic doesn't directly know about these dependencies**.

The thicker `infra` becomes, the lighter business code should become.

#### Where should Java Config be placed?

In this structure, the location of Java Config is determined by **the direction of dependencies**.

- Settings for initializing/connecting external technologies → `common/infra`
- Combining domain rules or determining policies → `modules/*`
- Choosing execution methods, profiles, bean combinations → `apps/*`

A typical example of Java Config located in `infra`:

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }
}
```

The characteristics of this configuration are clear:

- It **knows about Redis** as an external technology
- It carries no business meaning
- Other modules don't need to know implementation details

Therefore, such Config should be owned by `infra`.

Conversely, these configurations should not be placed in `infra`:

- Specific domain service combinations
- Conditional business policy bean configurations
- Settings that determine use case flows

Java Config is not simply a "configuration file"—it's **a means to fix dependency and responsibility boundaries in code**.

When Config is placed in the wrong location, dependency rules collapse instantly.

---

### 3.6 modules/user

```groovy
api 'org.springframework.boot:spring-boot-starter-data-jpa'
api 'com.querydsl:querydsl-jpa:5.1.0:jakarta'
```

All business rules exist in this area. If this module shakes, the entire project shakes.

The reason for using QueryDSL is simple: **to pull failure points from runtime to compile time**.

---

### 3.7 apps/api

```groovy
implementation project(':modules:user')
implementation project(':common:infra')
```

`apps` is where decisions are made, not where logic is written.

- Which modules to bundle and run
- Which profile to deploy with

It handles only these choices.

The emptier `apps` is, the longer the structure survives.

---

## 4. Conclusion

This structure does not impose an ideal picture. It's simply a choice to preemptively reduce problems that naturally emerge over time.

Starting in a direction where it's one application now but can be split anytime.

That's the core message this document aims to convey.

---

## GitHub

[https://github.com/postgresql-co-kr/postgresql-co-kr-starter/tree/main](https://github.com/postgresql-co-kr/postgresql-co-kr-starter/tree/main)
