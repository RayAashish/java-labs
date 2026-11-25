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

echo "=== Smoke-running representative labs across all 15 modules ==="
RUN_CLASSES=(
  foundations.variables.Variables
  foundations.operators.ArithmeticOperators
  foundations.control.decision.ModernSwitchExpressionDemo
  foundations.wrappers.WrapperCachingAndEqualityDemo
  strings.StringPoolAndCompactStringsDemo
  strings.StringBuilderAndConcatenationDemo
  strings.ModernStringMethodsDemo
  math.BigDecimalFinancialPrecisionDemo
  datetime.JavaTimeApiMasteryDemo
  oops.c06_abstraction.AbstractionAndInterfacesDemo
  oops.c12_mini_project.SmartHomeMiniProject
  exceptions.phase0_beginner.ExceptionBasicsForBeginnersDemo
  generics.GenericsBasicsAndBoundsDemo
  generics.WildcardsAndPecsDemo
  generics.TypeErasureAndBridgeMethodsDemo
  reflection.ReflectionIntrospectionDemo
  reflection.DynamicProxyAopDemo
  annotations.CustomAnnotationProcessorDemo
  functional.lambdas.LambdasAndInvokedynamicDemo
  functional.lambdas.FunctionalInterfacesCatalogDemo
  functional.lambdas.MethodReferencesFourFormsDemo
  functional.optional.OptionalCompleteApiAndTrapsDemo
  functional.streams.StreamCreationAllWaysDemo
  functional.streams.StreamIntermediateOpsMasteryDemo
  functional.streams.StreamTerminalAndReduceDemo
  functional.streams.CollectorsCompleteMasteryDemo
  functional.streams.PrimitiveStreamsAndGatherersDemo
  functional.streams.ParallelStreamsPitfallsAndBenchmarkDemo
  collections.hashmap_deep_dive.custom.MyHashMapTest
  collections.beginner.SequencedCollectionsDemo
  ionio.phase0_beginner.BeginnerFileReadWriteDemo
  concurrency.phase0_beginner.BeginnerThreadAndRaceConditionDemo
  concurrency.phase5_virtual_threads_project_loom.ScopedValueDemo
  internals.beginner.BeginnerStackVsHeapDemo
  patterns.solid.SingleResponsibilityPrincipleDemo
  patterns.solid.OpenClosedPrincipleDemo
  patterns.solid.LiskovSubstitutionPrincipleDemo
  patterns.solid.InterfaceSegregationPrincipleDemo
  patterns.solid.DependencyInversionPrincipleDemo
  patterns.creational.SingletonPatternDemo
  patterns.creational.FactoryMethodPatternDemo
  patterns.creational.AbstractFactoryPatternDemo
  patterns.creational.BuilderPatternDemo
  patterns.creational.PrototypePatternDemo
  patterns.structural.AdapterPatternDemo
  patterns.structural.DecoratorPatternDemo
  patterns.structural.ProxyPatternDemo
  patterns.structural.FacadePatternDemo
  patterns.structural.CompositePatternDemo
  patterns.behavioral.StrategyPatternDemo
  patterns.behavioral.ObserverPatternDemo
  patterns.behavioral.TemplateMethodPatternDemo
  patterns.behavioral.ChainOfResponsibilityPatternDemo
  patterns.behavioral.CommandPatternDemo
  patterns.behavioral.StatePatternDemo
  lts.java08.lambdas.LambdaAndFunctionalInterfacesDemo
  lts.java08.streams.StreamCreationAndLifecycleDemo
  lts.java08.streams.StreamIntermediateAndTerminalOpsDemo
  lts.java08.streams.StreamCollectorsAndPrimitivesDemo
  lts.java08.streams.ParallelStreamsPitfallsDemo
  lts.java08.optional.OptionalCompleteGuideDemo
  lts.java08.datetime.DateAndTimeJsr310Demo
  lts.java08.interfaces.DefaultAndStaticMethodsDemo
  lts.java08.async.CompletableFuturePipelinesDemo
  lts.java08.runtime.Java8Base64ConcurrencyAndAnnotationsDemo
  lts.java11.varinference.VarTypeInferenceAndLambdaDemo
  lts.java11.httpclient.StandardHttpClientLocalDemo
  lts.java11.stringsfiles.StringFilesAndCollectionsDemo
  lts.java11.modular.StreamOptionalAndPrivateInterfaceDemo
  lts.java11.runtime.RuntimeApisAndNestmatesDemo
  lts.java17.records.RecordsDeepDiveDemo
  lts.java17.sealedtypes.SealedClassesHierarchyDemo
  lts.java17.flow.SwitchExprAndInstanceofPatternDemo
  lts.java17.text.TextBlocksAndStreamAdditionsDemo
  lts.java17.security.HelpfulNpePrngAndFilterDemo
  lts.java21.loom.VirtualThreadsDeepDiveDemo
  lts.java21.sequenced.SequencedCollectionsDeepDiveDemo
  lts.java21.switchpatterns.SwitchPatternMatchingDeepDiveDemo
  lts.java21.recordpatterns.NestedRecordPatternsDemo
  lts.java21.runtime.KemUtf8AndCharsetDemo
  lts.java25.ffm.FfmMemoryAndDowncallDemo
  lts.java25.unnamed.UnnamedVariablesAndPatternsDemo
  lts.java25.gatherers.StreamGatherersMasteryDemo
  lts.java25.concurrency.ScopedValuesDeepDiveDemo
  lts.java25.ergonomics.FlexibleConstructorBodiesDemo
  tooling.modules.JpmsAndServiceLoaderDemo
  tooling.diagnostics.RuntimeDiagnosticsDemo
)

for cls in "${RUN_CLASSES[@]}"; do
  java -cp "$OUT_DIR" "$cls" >/dev/null
done

java -ea -cp "$OUT_DIR" tooling.testing.UnitTestingAndTestDoublesDemo >/dev/null
java --enable-preview -cp "$OUT_DIR" concurrency.phase5_virtual_threads_project_loom.StructuredTaskScopeShutdownOnFailureDemo >/dev/null
java --enable-preview -cp "$OUT_DIR" lts.java25.concurrency.StructuredTaskScopeJoinerDemo >/dev/null

echo "ALL_MODULES_VERIFIED_OK (${#RUN_CLASSES[@]} + 3 smoke runs passed)"
