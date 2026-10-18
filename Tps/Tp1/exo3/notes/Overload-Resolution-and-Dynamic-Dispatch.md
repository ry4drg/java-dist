# Overload Resolution and Dynamic Dispatch

## 1. Problem Statement

Consider the following class hierarchy and program. We wish to determine the exact output and, more importantly, to understand *why* each line is printed. The example is instructive because it combines two independent mechanisms of Java's method-call semantics — **compile-time overload resolution** and **runtime dynamic dispatch** — whose interaction is frequently misunderstood.

```java
class A {
    public String f(D obj) { return " A and D "; }
    public String f(A obj) { return " A and A "; }
}

class B extends A {
    public String f(B obj) { return " B and B "; }
    public String f(A obj) { return " B and A "; }
}

class C extends B { }
class D extends B { }

class Exo3 {
    public static void main(String[] args) {
        A a1 = new A();
        A a2 = new B();
        B b  = new B();
        C c  = new C();
        D d  = new D();

        System.out.println(a1.f(b));
        System.out.println(a1.f(c));
        System.out.println(a1.f(d));
        System.out.println(a2.f(b));
        System.out.println(a2.f(c));
        System.out.println(a2.f(d));
        System.out.println(b.f(b));
        System.out.println(b.f(c));
        System.out.println(b.f(d));
    }
}
```

## 2. The Two Governing Rules

Every call of the form `receiver.f(arg)` in Java is resolved in **two distinct phases**, governed by two independent rules.

> **Rule 1 — Overload Resolution (compile time).**
> The compiler selects the *signature* of the method to invoke. It does so using the **static (declared) type of the receiver** and the **static (declared) type of the argument**. It never inspects the runtime types of either. If no exact match exists on the receiver's static type, the compiler searches upward in the receiver's superclass hierarchy for the most specific applicable signature.

> **Rule 2 — Dynamic Dispatch (runtime).**
> Once a signature has been selected, the JVM chooses the *implementation* to execute using the **dynamic (actual) type of the receiver**. If the receiver's runtime class overrides the selected signature, the override is executed; otherwise, the inherited implementation is used.

These two rules are **independent**. Rule 1 decides *which method is named* in the bytecode; Rule 2 decides *which body actually runs*. Confusing them is the principal source of error in this problem.

## 3. Analysis of the Class Hierarchy

We first enumerate which methods each class declares and which it inherits.

| Class | Declared methods | Inherited methods |
|-------|------------------|-------------------|
| `A` | `f(D)`, `f(A)` | — |
| `B` | `f(B)`, `f(A)` (overrides `A.f(A)`) | `f(D)` from `A` |
| `C` | — | all of `B` |
| `D` | — | all of `B` |

Two observations are essential.

1. **`B.f(B)` is an overload, not an override.** It introduces a new signature `f(B)` that does not exist in `A`. Therefore it is only visible to the compiler when the receiver's static type is `B` or a subtype of `B`.
2. **`B.f(A)` is an override.** It has the same signature as `A.f(A)`. When the JVM dispatches on an object whose runtime type is `B`, the override replaces the parent implementation.
3. **`B` does not override `f(D)`.** The signature `f(D)` remains inherited from `A`. Calls resolving to `f(D)` on a `B` receiver therefore execute `A.f(D)`.

Next, the static and dynamic types of each variable:

| Variable | Static type (declared) | Dynamic type (actual) |
|----------|------------------------|------------------------|
| `a1` | `A` | `A` |
| `a2` | `A` | **`B`** |
| `b`  | `B` | `B` |
| `c`  | `C` | `C` |
| `d`  | `D` | `D` |

The variable `a2` is the subtle case: it is *declared* `A` but *refers to* a `B`. The compiler will treat it as an `A`; the JVM will treat it as a `B`.

## 4. Detailed Resolution of Each Call

We now apply the two rules to each of the nine calls.

### Call 1 — `a1.f(b)`

- Receiver static type: `A`. Argument static type: `B`.
- **Rule 1.** On `A`, is there an `f(B)`? No. The compiler searches upward and finds `f(A)`; since `B` is a subtype of `A`, `f(A)` is applicable. Signature selected: **`f(A)`**.
- **Rule 2.** Receiver dynamic type is `A`. Does `A` define `f(A)`? Yes. Body executed: **`A.f(A)`**.
- Output: **` A and A `**

### Call 2 — `a1.f(c)`

- Receiver static type: `A`. Argument static type: `C`.
- **Rule 1.** On `A`, `f(C)`? No. `f(B)`? No. `f(A)`? Yes, since `C` is a subtype of `A`. Signature selected: **`f(A)`**.
- **Rule 2.** Receiver dynamic type `A` → **`A.f(A)`**.
- Output: **` A and A `**

### Call 3 — `a1.f(d)`

- Receiver static type: `A`. Argument static type: `D`.
- **Rule 1.** On `A`, `f(D)` exists. Signature selected: **`f(D)`**.
- **Rule 2.** Receiver dynamic type `A` → **`A.f(D)`**.
- Output: **` A and D `**

### Call 4 — `a2.f(b)`

- Receiver **static** type: `A`. Receiver **dynamic** type: **`B`**. Argument static type: `B`.
- **Rule 1.** The compiler sees only that `a2` is an `A`. On `A`, `f(B)` does not exist; the compiler cannot see `B.f(B)` because the receiver is declared `A`. The applicable signature is **`f(A)`**.
- **Rule 2.** The receiver's runtime class is `B`. Does `B` override `f(A)`? Yes — `B.f(A)` is declared. Body executed: **`B.f(A)`**.
- Output: **` B and A `**

This is the most instructive case: the compiler and the JVM each consult a **different type** of the same variable.

### Call 5 — `a2.f(c)`

- Receiver static type: `A`. Receiver dynamic type: `B`. Argument static type: `C`.
- **Rule 1.** On `A`, `f(C)` does not exist; `f(A)` is applicable. Signature: **`f(A)`**.
- **Rule 2.** `B` overrides `f(A)` → **`B.f(A)`**.
- Output: **` B and A `**

### Call 6 — `a2.f(d)`

- Receiver static type: `A`. Receiver dynamic type: `B`. Argument static type: `D`.
- **Rule 1.** `f(D)` exists on `A`. Signature: **`f(D)`**.
- **Rule 2.** Does `B` override `f(D)`? No. The inherited `A.f(D)` runs → **`A.f(D)`**.
- Output: **` A and D `**

### Call 7 — `b.f(b)`

- Receiver static type: `B`. Argument static type: `B`.
- **Rule 1.** On `B`, `f(B)` is declared. Signature: **`f(B)`**.
- **Rule 2.** Receiver dynamic type `B` → **`B.f(B)`**.
- Output: **` B and B `**

### Call 8 — `b.f(c)`

- Receiver static type: `B`. Argument static type: `C`.
- **Rule 1.** On `B`, `f(C)`? No. `f(B)`? Yes, since `C` is a subtype of `B`. Signature: **`f(B)`**.
- **Rule 2.** `B.f(B)` → **`B.f(B)`**.
- Output: **` B and B `**

### Call 9 — `b.f(d)`

- Receiver static type: `B`. Argument static type: `D`.
- **Rule 1.** On `B`, `f(D)`? Not declared in `B`, but inherited from `A`. Signature: **`f(D)`**.
- **Rule 2.** `B` does not override `f(D)` → **`A.f(D)`**.
- Output: **` A and D `**

## 5. Summary Table

| # | Call | Receiver static | Receiver dynamic | Argument static | Signature (compile) | Body (runtime) | Output |
|---|------|-----------------|------------------|-----------------|---------------------|----------------|--------|
| 1 | `a1.f(b)` | `A` | `A` | `B` | `f(A)` | `A.f(A)` | `A and A` |
| 2 | `a1.f(c)` | `A` | `A` | `C` | `f(A)` | `A.f(A)` | `A and A` |
| 3 | `a1.f(d)` | `A` | `A` | `D` | `f(D)` | `A.f(D)` | `A and D` |
| 4 | `a2.f(b)` | `A` | `B` | `B` | `f(A)` | `B.f(A)` | `B and A` |
| 5 | `a2.f(c)` | `A` | `B` | `C` | `f(A)` | `B.f(A)` | `B and A` |
| 6 | `a2.f(d)` | `A` | `B` | `D` | `f(D)` | `A.f(D)` | `A and D` |
| 7 | `b.f(b)` | `B` | `B` | `B` | `f(B)` | `B.f(B)` | `B and B` |
| 8 | `b.f(c)` | `B` | `B` | `C` | `f(B)` | `B.f(B)` | `B and B` |
| 9 | `b.f(d)` | `B` | `B` | `D` | `f(D)` | `A.f(D)` | `A and D` |

## 6. Observed Output

```
 A and A
 A and A
 A and D
 B and A
 B and A
 A and D
 B and B
 B and B
 A and D
```

This matches the program's output exactly.

## 7. Principal Lessons

1. **Overload resolution is static; overriding is dynamic.** The two decisions are made by different parties (compiler vs. JVM) using different types (static vs. dynamic) of the same expression. A correct analysis of any such call requires applying both rules in order.

2. **The compiler does not see methods declared only in subclasses.** `B.f(B)` is invisible when the receiver is declared `A`. Consequently, `a2.f(b)` resolves to `f(A)`, not `f(B)`, even though `a2` refers to a `B` at runtime. This is the source of the otherwise surprising `B and A` output.

3. **Override selection uses the receiver's dynamic type.** Once a signature has been chosen, the JVM substitutes any override present in the receiver's runtime class. This is why `a2.f(b)` executes `B.f(A)` rather than `A.f(A)`.

4. **Overloads declared in a subclass do not override parent overloads of a different signature.** `B.f(B)` is a *new* method, unrelated by inheritance to `A.f(A)` or `A.f(D)`. It participates in overload resolution only when the receiver's static type permits it.

5. **The most subtle line is `a2.f(b)`.** It is resolved by the compiler as `f(A)` and by the JVM as `B.f(A)`. Neither "the compiler's choice" nor "the runtime type" alone explains the result; the interaction of the two rules does. This single line encapsulates the entire lesson of the exercise.

## 8. Conclusion

The program's output is not the product of any single mechanism but of the systematic interaction between Java's two method-call semantics: **static overload resolution**, which fixes the signature at compile time based on declared types, and **dynamic dispatch**, which selects the implementation at runtime based on the receiver's actual class. Mastery of this distinction — and in particular of the fact that the compiler and the JVM consult *different* types of the same variable — is what allows one to predict the behavior of code such as `a2.f(b)` with confidence rather than by experiment.
