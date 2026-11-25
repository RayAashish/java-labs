# 03. Object-Oriented Programming (OOP) — Master Laboratory

> **Dual-Mode Design:** Need a 5-minute rapid revision before an interview? Open [`oops-revisit.md`](oops-revisit.md). Want deep 3-layer explanations and 44+ runnable Java code labs? Follow the progression below!

---

## ⚡ Rapid Revision Entry Points

- 👉 **[`oops-revisit.md`](oops-revisit.md)** — High-signal Senior Revision Engine (Memory layout, Overload ladder, `vtable` dispatch, 25 interview output puzzles, 5-min rapid recap).
- 👉 **[`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)** — Exhaustive Noob-to-Architect OOP Interview Q&A Bank.

---

## 🧭 3-Layer Spiral Curriculum

| Module | Topic | Notes | Runnable Code Labs |
|---|---|---|---|
| **01** | **Foundations of OOP** (Stack vs Heap, Object Headers, Pass-by-Value) | [01-foundations-of-oops.md](notes/01-foundations-of-oops.md) | `oops.c01_foundations.*` |
| **02** | **Constructors & Lifecycle** (`this()`, `super()`, Shallow vs Deep Copy, Init Order) | [02-constructors-and-lifecycle.md](notes/02-constructors-and-lifecycle.md) | `oops.c02_constructors.*` |
| **03** | **Encapsulation & Immutability** (4 Access Modifiers, Cross-Package `protected`, Immutable Recipe) | [03-encapsulation-and-immutability.md](notes/03-encapsulation-and-immutability.md) | `oops.c03_encapsulation.*` |
| **04** | **Inheritance & Hierarchies** (`extends`, `super`, Diamond Problem, Memory Payload) | [04-inheritance-and-hierarchies.md](notes/04-inheritance-and-hierarchies.md) | `oops.c04_inheritance.*` |
| **05** | **Polymorphism & Dispatch** (4-Tier Overload Resolution, `vtable` Dispatch, Covariant Returns) | [05-polymorphism-and-dispatch.md](notes/05-polymorphism-and-dispatch.md) | `oops.c05_polymorphism.*` |
| **06** | **Abstraction & Modern Interfaces** (Abstract Class vs Java 8/9+ Interfaces, Conflict Resolution) | [06-abstraction-and-interfaces.md](notes/06-abstraction-and-interfaces.md) | `oops.c06_abstraction.*` |
| **07** | **`static` & `final` Mechanics** (Metaspace State, Method Hiding, Reference Mutability) | [07-static-and-final.md](notes/07-static-and-final.md) | [StaticAndFinalDemo.java](codes/oops/c07_static_final/StaticAndFinalDemo.java) |
| **08** | **`java.lang.Object` Essentials** (`equals()` 5 Axioms, `hashCode()` Contract, Copy Constructors) | [08-object-class-essentials.md](notes/08-object-class-essentials.md) | [ObjectContractsDemo.java](codes/oops/c08_object_essentials/ObjectContractsDemo.java) |
| **09** | **Composition vs Aggregation vs Association** (GoF Principle, Fragile Base Class) | [09-composition-and-association.md](notes/09-composition-and-association.md) | [CompositionVsAggregationDemo.java](codes/oops/c09_composition/CompositionVsAggregationDemo.java) |
| **10** | **Nested, Inner, Local & Anonymous Classes** (`Outer.this`, Effectively Final Capture) | [10-nested-and-inner-classes.md](notes/10-nested-and-inner-classes.md) | [NestedAndInnerClassesDemo.java](codes/oops/c10_nested_classes/NestedAndInnerClassesDemo.java) |
| **11** | **Enums as First-Class Types** (Private Constructors, Constant Bodies, `EnumSet` / `EnumMap`) | [11-enums-first-class-types.md](notes/11-enums-first-class-types.md) | [EnumsMasteryDemo.java](codes/oops/c11_enums/EnumsMasteryDemo.java) |
| **12** | **Capstone Mini-Project** (Smart Home Controller tying all OOP pillars together) | [away-from-old-school-examples.md](notes/away-from-old-school-examples.md) | [SmartHomeMiniProject.java](codes/oops/c12_mini_project/SmartHomeMiniProject.java) |

---

## 🚀 Compile & Run

```bash
# Compile every OOP module cleanly
javac -d out $(find 03-object-oriented-programming/codes -name "*.java")

# Run selected labs
java -cp out oops.c01_foundations.beginner.ObjectsAndClass
java -cp out oops.c02_constructors.DeepAndShallowCopyConstructorProblem
java -cp out oops.c03_encapsulation.ImmutableClass
java -cp out oops.c05_polymorphism.dynamic_polymorphism.MethodOverriding
java -cp out oops.c07_static_final.StaticAndFinalDemo
java -cp out oops.c08_object_essentials.ObjectContractsDemo
java -cp out oops.c09_composition.CompositionVsAggregationDemo
java -cp out oops.c10_nested_classes.NestedAndInnerClassesDemo
java -cp out oops.c11_enums.EnumsMasteryDemo
java -cp out oops.c12_mini_project.SmartHomeMiniProject
```
