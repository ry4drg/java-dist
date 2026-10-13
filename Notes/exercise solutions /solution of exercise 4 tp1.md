# Solution of exercise 4 tp1 explained

This is a classic case of **overload resolution** combined with **dynamic dispatch**. The rule is:

> **The overload is chosen at compile time based on the static type of the argument. The implementation that actually runs is chosen at runtime based on the dynamic type of the receiver.**

In other words, there are **two separate steps**:
1. **Compile time**: which `f(...)` *signature* is called? → based on the **static** type of the argument.
2. **Runtime**: which *body* runs? → based on the **dynamic** type of the object you call it on.

## The Available Methods

```java
class A {
    String f(D obj) { return " A and D "; }
    String f(A obj) { return " A and A "; }
}
class B extends A {
    String f(B obj) { return " B and B "; }   // overload (different signature)
    String f(A obj) { return " B and A "; }   // override of A.f(A)
}
```

Methods of `A`: `f(D)`, `f(A)`
Methods of `B`: `f(D)` (inherited from A), `f(B)` (new), `f(A)` (overridden)

## Step 1 — Which `f` Is Called? (Static Choice)

The compiler looks at the **declared type** of the argument:

| Call | Static type of argument | Signature chosen |
|------|-------------------------|------------------|
| `a1.f(b)` | `b` is `B` | `f(B)` → not in `A` → walk up to `f(A)` |
| `a1.f(c)` | `c` is `C` | `f(C)` → doesn't exist → walk up to `f(A)` |
| `a1.f(d)` | `d` is `D` | `f(D)` exists in `A` |
| `b.f(b)` | `b` is `B` | `f(B)` exists in `B` |
| `b.f(c)` | `c` is `C` | `f(C)` → walk up to `f(B)` |
| `b.f(d)` | `d` is `D` | `f(D)` inherited from `A` |

Overload resolution walks up the hierarchy of **static types** until it finds a compatible method.

## Step 2 — Which Body Runs? (Dynamic Choice)

The receiver (`a1`, `a2`, `b`) has a **dynamic type**:

- `a1` → `A`
- `a2` → `B` (declared `A`, but it's a `B`)
- `b` → `B`

If the signature chosen in step 1 is **overridden** in the dynamic class, the overridden version runs.

## Breaking Down the 9 Calls

### 1. `a1.f(b)` → static argument `B`, dynamic receiver `A`

- Step 1: `f(B)` doesn't exist in `A` → walk up → **`f(A)`**.
- Step 2: receiver `A` → `A.f(A)`.
- Result: **`A and A`** 

### 2. `a1.f(c)` → static argument `C`, receiver `A`

- Step 1: `f(C)` doesn't exist → walk up → `B`? not in `A` either → **`f(A)`**.
- Step 2: `A.f(A)`.
- Result: **`A and A`** 

### 3. `a1.f(d)` → static argument `D`, receiver `A`

- Step 1: `f(D)` exists in `A` → **`f(D)`**.
- Step 2: `A.f(D)`.
- Result: **`A and D`** 

### 4. `a2.f(b)` → static argument `B`, dynamic receiver **`B`**

- Step 1: `f(B)` doesn't exist in `A` (static type of receiver) → walk up → **`f(A)`**.
- Step 2: receiver `B` → `B.f(A)` **overrides** `A.f(A)` → so `B.f(A)` runs.
- Result: **`B and A`** 

### 5. `a2.f(c)` → static argument `C`, receiver `B`

- Step 1: `f(C)` doesn't exist → walk up → **`f(A)`**.
- Step 2: `B.f(A)`.
- Result: **`B and A`** 

### 6. `a2.f(d)` → static argument `D`, receiver `B`

- Step 1: `f(D)` exists in `A` → **`f(D)`**.
- Step 2: `B` doesn't override `f(D)` → inherits `A.f(D)`.
- Result: **`A and D`** 

### 7. `b.f(b)` → static argument `B`, receiver `B`

- Step 1: `f(B)` exists in `B` → **`f(B)`**.
- Step 2: `B.f(B)`.
- Result: **`B and B`** 

### 8. `b.f(c)` → static argument `C`, receiver `B`

- Step 1: `f(C)` doesn't exist → walk up → `f(B)` exists in `B` → **`f(B)`**.
- Step 2: `B.f(B)`.
- Result: **`B and B`** 

### 9. `b.f(d)` → static argument `D`, receiver `B`

- Step 1: `f(D)` exists (inherited from `A`) → **`f(D)`**.
- Step 2: `B` doesn't override `f(D)` → `A.f(D)`.
- Result: **`A and D`** 

## Summary Table

| Call | Static arg | Dynamic receiver | Signature chosen | Body executed | Output |
|------|-----------|------------------|------------------|---------------|--------|
| `a1.f(b)` | `B` | `A` | `f(A)` | `A.f(A)` | `A and A` |
| `a1.f(c)` | `C` | `A` | `f(A)` | `A.f(A)` | `A and A` |
| `a1.f(d)` | `D` | `A` | `f(D)` | `A.f(D)` | `A and D` |
| `a2.f(b)` | `B` | `B` | `f(A)` | `B.f(A)` | `B and A` |
| `a2.f(c)` | `C` | `B` | `f(A)` | `B.f(A)` | `B and A` |
| `a2.f(d)` | `D` | `B` | `f(D)` | `A.f(D)` | `A and D` |
| `b.f(b)` | `B` | `B` | `f(B)` | `B.f(B)` | `B and B` |
| `b.f(c)` | `C` | `B` | `f(B)` | `B.f(B)` | `B and B` |
| `b.f(d)` | `D` | `B` | `f(D)` | `A.f(D)` | `A and D` |

## Key Takeaways

1. **Overloading = static choice (compile time).** The compiler picks the *signature* based on the **declared type** of the argument, walking up the class hierarchy if needed. It **never** looks at the argument's actual runtime type.

2. **Overriding = dynamic choice (runtime).** The JVM picks the *body* based on the **actual type of the receiver** (`a1`, `a2`, `b`).

3. **The signature chosen may not be what you expect.** For example, in `a1.f(b)` where `b` is a `B`: you might think `f(B)` is called, but since the static receiver type is `A` and `A` has no `f(B)`, the compiler walks up to `f(A)`. That's why you get `A and A`, not `B and B`.

4. **`B.f(B)` does not override anything — it's an overload.** It's only visible if the receiver's static type is `B` (or a subclass). That's why `a2.f(b)` (static receiver type `A`) can't see it, while `b.f(b)` (static receiver type `B`) can.

5. **The subtlest case: `a2.f(b)`.** `a2` is *declared* `A` but holds a `B`. The compiler, seeing `A`, chooses `f(A)` (since `A` has no `f(B)`). At runtime, because the real receiver is `B` and `B` overrides `f(A)`, `B.f(A)` runs → `B and A`. This is exactly the **overload + override** interaction that makes this code tricky.

## In One Sentence

> The **static type of the argument** decides *which signature* is called (overloading, at compile time), and the **dynamic type of the receiver** decides *which body* runs (overriding, at runtime). The two steps are independent, and their combination produces the output `A and A / A and A / A and D / B and A / B and A / A and D / B and B / B and B / A and D`.
