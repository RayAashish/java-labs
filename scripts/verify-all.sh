#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$(mktemp -d)"
trap 'rm -rf "$OUT_DIR"' EXIT

echo "=== Compiling all pure-JDK modules across java-labs (JDK 25) ==="
PURE_JAVA_FILES=()
while IFS= read -r -d '' file; do
  PURE_JAVA_FILES+=("$file")
done < <(find "$ROOT_DIR" \
  -path "$ROOT_DIR/11-networking-and-sockets/spring-websocket-reference" -prune -o \
  -path "$ROOT_DIR/12-jdbc-and-database-connectivity" -prune -o \
  -name "*.java" -print0)

javac --enable-preview --release 25 -d "$OUT_DIR" "${PURE_JAVA_FILES[@]}"
echo "Pure-JDK modules compiled cleanly (${#PURE_JAVA_FILES[@]} files)!"

echo "=== Compiling JDBC module with MySQL Connector JAR ==="
JDBC_FILES=()
while IFS= read -r -d '' file; do
  JDBC_FILES+=("$file")
done < <(find "$ROOT_DIR/12-jdbc-and-database-connectivity/codes" -name "*.java" -print0)

javac -cp "$ROOT_DIR/12-jdbc-and-database-connectivity/lib/mysql-connector-j-9.6.0.jar" \
  -d "$OUT_DIR" "${JDBC_FILES[@]}"
echo "JDBC module compiled cleanly (${#JDBC_FILES[@]} files)!"

echo "=== Smoke-running representative labs across modules ==="
java -cp "$OUT_DIR" foundations.wrappers.WrapperCachingAndEqualityDemo >/dev/null
java -cp "$OUT_DIR" strings.StringPoolAndCompactStringsDemo >/dev/null
java -cp "$OUT_DIR" math.BigDecimalFinancialPrecisionDemo >/dev/null
java -cp "$OUT_DIR" datetime.JavaTimeApiMasteryDemo >/dev/null
java -cp "$OUT_DIR" oops.c12_mini_project.SmartHomeMiniProject >/dev/null
java -cp "$OUT_DIR" exceptions.phase0_beginner.ExceptionBasicsForBeginnersDemo >/dev/null
java -cp "$OUT_DIR" generics.GenericsAndPecsDemo >/dev/null
java -cp "$OUT_DIR" reflection.ReflectionAndDynamicProxyDemo >/dev/null
java -cp "$OUT_DIR" annotations.CustomAnnotationProcessorDemo >/dev/null
java -cp "$OUT_DIR" functional.streams.StreamsCollectorsAndGatherersDemo >/dev/null
java -cp "$OUT_DIR" collections.hashmap_deep_dive.custom.MyHashMapTest >/dev/null
java -cp "$OUT_DIR" ionio.phase0_beginner.BeginnerFileReadWriteDemo >/dev/null
java -cp "$OUT_DIR" concurrency.phase0_beginner.BeginnerThreadAndRaceConditionDemo >/dev/null
java -cp "$OUT_DIR" concurrency.phase5_virtual_threads_project_loom.ScopedValueDemo >/dev/null
java --enable-preview -cp "$OUT_DIR" concurrency.phase5_virtual_threads_project_loom.StructuredTaskScopeShutdownOnFailureDemo >/dev/null
java -cp "$OUT_DIR" internals.beginner.BeginnerStackVsHeapDemo >/dev/null
java -cp "$OUT_DIR" patterns.creational.CreationalPatternsDemo >/dev/null
java -cp "$OUT_DIR" lts.java25.ModernJdk25ErgonomicsAndFeaturesDemo >/dev/null
java -ea -cp "$OUT_DIR" tooling.testing.UnitTestingAndTestDoublesDemo >/dev/null
java -cp "$OUT_DIR" tooling.diagnostics.RuntimeDiagnosticsDemo >/dev/null

echo "ALL_MODULES_VERIFIED_OK"
