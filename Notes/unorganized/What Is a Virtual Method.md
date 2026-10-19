# What Is a Virtual Method?

A **virtual method** is an **instance method whose implementation is chosen at runtime** based on the **actual class of the object** — not the static type of the reference. In Java, **almost every non-static, non-private, non-final method is virtual** by default.

That's the short definition. Let me unpack it, because "virtual" is one of those words that makes sense once you see what problem it solves.

## The Problem Virtual Methods Solve

Consider:

```java
class Animal {
    void speak() { System.out.println("..."); }
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Woof"); }
}

Animal a = new Dog();
a.speak();   // prints "Woof", not "..."
```

The compiler only knows `a` is an `Animal`. Yet at runtime, `Dog.speak` runs, not `Animal.speak`. **That behavior is what makes `speak` a virtual method.** The method called depends on the object's **runtime type**, not the reference's **static type**.

If `speak` were **non-virtual**, `a.speak()` would always call `Animal.speak` — the compiler's choice would be final. That's how some methods in Java actually work (see below).

## Why the Word "Virtual"

The term comes from C++ and older OO languages. "Virtual" means **"not fixed at compile time"** — the actual function to run is decided later (at runtime). It's the opposite of a **static** or **direct** call, where the target is nailed down by the compiler.

In C++, you must explicitly mark methods `virtual` to get this behavior. In Java, it's the **default** — the language designers decided that's what OO programmers usually want.

## What Makes a Method Virtual in Java

| Method kind | Virtual? | Why |
|-------------|----------|-----|
| Normal instance method | **Yes** | Can be overridden; dispatch at runtime |
| `abstract` method | **Yes** (forced) | No body — subclass must provide it |
| Method overriding a parent's method | **Yes** | That's the whole point of overriding |
| `final` method | **No** | Can't be overridden → compiler can bind it directly |
| `private` method | **No** | Not inherited, not overridable → direct call |
| `static` method | **No** | Belongs to the class, not an object — no receiver |
| Constructors | **No** | Not inherited, not overridable in the usual sense |

So "virtual" in Java ≈ **"an overridable instance method."** The JVM calls it via `invokevirtual` (or `invokeinterface` for interface methods), which is the **dynamic dispatch** mechanism.

## What "Non-Virtual" Looks Like

```java
class Animal {
    final void breathe() { System.out.println("breathe"); }
    private void secret() { System.out.println("secret"); }
    static void info() { System.out.println("info"); }
}

class Dog extends Animal {
    // Cannot override breathe()  → compile error
    // Cannot override secret()   → private, not inherited
    // Cannot override info()     → static, hidden not overridden
}
```

For these, the compiler/JVM knows **exactly** which method runs — no runtime lookup needed. That's why they're **non-virtual**:

- `final` → can't be overridden.
- `private` → not visible to subclasses.
- `static` → belongs to the class, called on the class, no dynamic receiver.

## The Mechanism: The Vtable

This is where the vtable from the earlier discussion comes in. A **virtual method** is dispatched through the class's **vtable**:

```
Animal's vtable:              Dog's vtable:
┌───────────────────┐         ┌───────────────────┐
│ [0] Animal.speak  │         │ [0] Dog.speak     │  ← overridden
│ [1] Animal.eat    │         │ [1] Animal.eat    │  ← inherited, same
│ [2] Animal.sleep  │         │ [2] Animal.sleep  │
└───────────────────┘         └───────────────────┘
```

When `a.speak()` runs:

1. JVM reads the object's **actual class** from its header → `Dog`.
2. Looks up slot `0` in **`Dog`'s vtable**.
3. Finds `Dog.speak` → jumps there.

Only **virtual** methods go through this table. Non-virtual calls (`invokespecial`, `invokestatic`) bypass it because the target is already known.

## Virtual vs. Non-Virtual Dispatch — Side by Side

```java
class Animal {
    void speak() { System.out.println("..."); }      // virtual
    final void breathe() { System.out.println("breathe"); } // non-virtual
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Woof"); }     // overrides virtual
}

Animal a = new Dog();
a.speak();     // invokevirtual  → dynamic dispatch → "Woof"
a.breathe();   // invokespecial  → direct call      → "breathe"
```

- `speak` is **virtual**: the JVM checks the object's real class and picks `Dog.speak`.
- `breathe` is **non-virtual**: the JVM (and compiler) know it's `Animal.breathe`, so no dispatch is needed.

## Why "Virtual" Is the Default in Java

Java's design assumes you'll write OO code where **subclasses extend and override behavior**. Making methods virtual by default means:

- Subclass overrides **just work** — no special keyword needed.
- Frameworks and libraries can call your overrides (e.g., `toString`, `equals`).
- Polymorphism is the norm, not the exception.

C++ went the other way (`virtual` must be explicit) for performance and predictability reasons. Java traded a tiny dispatch cost for programmer convenience — and the JIT makes that cost nearly disappear in practice.

## The Word "Virtual" More Broadly

"Virtual" shows up elsewhere with the same flavor of meaning — **"not the literal thing, but behaving like it"**:

- **Virtual method** — not bound at compile time; chosen at runtime.
- **Virtual memory** — not physical RAM directly; mapped by the OS.
- **Virtual machine** — not a real machine; emulates one.

In all cases: an **indirection layer** decides the concrete reality later.

## Summary

> A **virtual method** is an **instance method whose implementation is selected at runtime based on the object's actual class**, not the static type of the reference. In Java, normal instance methods are virtual by default; `final`, `private`, and `static` methods are not. Virtual methods are dispatched through the **vtable** via `invokevirtual`/`invokeinterface`, which is exactly the mechanism that makes `a.speak()` run `Dog.speak` even though the compiler only saw `Animal`. The word "virtual" means "not fixed at compile time" — the concrete method is decided later, at runtime.
