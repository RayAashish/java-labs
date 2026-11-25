# 03. Maven & Gradle Build Lifecycles

---

## 🟢 Layer 1 — Noob Track: Why We Use Build Tools

Running `javac` by hand works for 3 files, but a real project has 500 classes and 40 third-party JARs (Spring, Jackson, MySQL driver, JUnit). **Maven** (`pom.xml`) and **Gradle** (`build.gradle`) automatically download dependencies, compile, run tests, and package `.jar` files.

---

## 🟡 Layer 2 — Practitioner Track: Maven Lifecycle & Dependency Scopes

### Standard Maven Phases (Running any phase executes all prior phases!)
1. `validate` -> Checks `pom.xml` correctness.
2. `compile` -> Compiles `src/main/java` into `target/classes`.
3. `test` -> Compiles `src/test/java` & runs unit tests (`mvn test`).
4. `package` -> Bundles compiled classes into a `.jar` / `.war`.
5. `verify` -> Runs integration tests & checks.
6. `install` -> Copies JAR into local `~/.m2/repository` cache.
7. `deploy` -> Publishes JAR to remote artifact repo (Nexus/Artifactory).
*(Tip: `mvn clean` runs the separate `clean` lifecycle to delete `target/` first -> `mvn clean test` or `mvn clean package`.)*

### Dependency Scopes
| Scope | Compile Classpath? | Test Classpath? | Bundled in Final Package? | Example |
|---|---|---|---|---|
| `compile` (default) | Yes | Yes | **Yes** | `spring-web`, `guava` |
| `provided` | Yes | Yes | **No** (supplied by runtime container) | `jakarta.servlet-api`, `lombok` |
| `runtime` | No | Yes | **Yes** | `mysql-connector-j`, `logback-classic` |
| `test` | No | Yes | **No** | `junit-jupiter`, `mockito-core` |

---

## 🔴 Layer 3 — Senior Track: Fat JARs, BOMs & Dependency Conflicts

- **BOM (`<dependencyManagement>`):** Aligns compatible versions across dozens of libraries (`spring-boot-dependencies` BOM prevents Jackson/Netty version mismatches).
- **Transitive Dependency Conflicts:** Maven resolves conflicts via **nearest definition in the dependency tree** (tie broken by first declaration in `pom.xml`), whereas Gradle selects the **highest version** by default! Debug with `mvn dependency:tree`.
- **Fat / Executable JAR (`spring-boot-maven-plugin` or `maven-shade-plugin`):** Standard `jar` doesn't embed dependency JARs inside `java -jar app.jar`; Spring Boot repackages nested JARs under `BOOT-INF/lib` with a custom `JarLauncher` classloader.
