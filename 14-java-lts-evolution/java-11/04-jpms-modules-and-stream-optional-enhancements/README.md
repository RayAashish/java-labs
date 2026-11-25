# Java 11 — JPMS Modules, `private` Interface Methods & Stream/Optional Upgrades

## ⚡ 2-Minute Revision
- **JPMS (Java 9 Project Jigsaw):** Adds strong encapsulation via `module-info.java` (`requires`, `requires transitive`, `exports`, `opens`, `uses`/`provides ... with`), eliminating classpath "JAR Hell" split packages & shrinking custom JRE images via `jlink` (~40 MB Docker images!).
- **Interface `private` Methods (Java 9):** Extract duplicate helper code shared across multiple `default` or `static` interface methods without exposing them as `public` API!
- **Stream Upgrades (Java 9):** `takeWhile(pred)`, `dropWhile(pred)`, `Stream.ofNullable(x)`, 3-arg `Stream.iterate(seed, hasNext, next)`, `Collectors.filtering` & `Collectors.flatMapping`.
- **`Optional` Upgrades (Java 9/10):** `ifPresentOrElse(consumer, emptyAction)`, `opt.or(() -> altOptional)`, `opt.stream()`, `opt.orElseThrow()` (Java 10), `opt.isEmpty()` (Java 11).

## 📂 Contents
- [`notes/jpms-and-java9-api-enhancements.md`](notes/jpms-and-java9-api-enhancements.md)
- [`codes/lts/java11/modular/StreamOptionalAndPrivateInterfaceDemo.java`](codes/lts/java11/modular/StreamOptionalAndPrivateInterfaceDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
