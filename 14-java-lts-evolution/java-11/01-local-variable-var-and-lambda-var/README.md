# Java 11 — Local-Variable Type Inference (`var`) & Lambda `var`

## ⚡ 2-Minute Revision
- **What:** `var` lets `javac` infer the local variable's static type from the right-hand initializer (`var map = new HashMap<String, List<User>>();`).
- **Java 10 vs Java 11:** Java 10 introduced `var` for local variables and `for`-loop counters. **Java 11 (JEP 323)** added `var` inside **lambda parameters** so you can attach annotations (`(@NonNull var id) -> ...`) without abandoning type inference!
- **Still 100% Statically Typed:** Once inferred, you cannot assign an `int` to a `String` `var` variable. Identical `.class` bytecode is emitted as explicit typing!
- **Top Pitfalls:**
  - Cannot use without an initializer (`var x;` fails), nor with `null` (`var x = null;` fails), nor for class fields, method parameters, or method return types!
  - Diamond trap: `var list = new ArrayList<>();` infers `ArrayList<Object>`, NOT `ArrayList<String>`!

## 📂 Contents
- [`notes/var-type-inference-deep-dive.md`](notes/var-type-inference-deep-dive.md)
- [`codes/lts/java11/varinference/VarTypeInferenceAndLambdaDemo.java`](codes/lts/java11/varinference/VarTypeInferenceAndLambdaDemo.java)
- [`interview/INTERVIEW_QUESTIONS.md`](interview/INTERVIEW_QUESTIONS.md)
