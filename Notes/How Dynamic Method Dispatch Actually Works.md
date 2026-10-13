# How Dynamic Method Dispatch Actually Works

Let me walk through the whole mechanism, from the bytecode the compiler emits to the exact steps the JVM takes at runtime. The key structures are the **constant pool**, the **vtable**, and the `invokevirtual` instruction.

## Step 1: What the Compiler Produces

For this code:

```java
class Animal {
    void speak() { System.out.println("..."); }
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Woof"); }
}

Animal a = new Dog();
a.speak();  // prints "Woof"
```

The compiler emits bytecode roughly like:

```
aload_1                      ; push local variable a (the reference)
invokevirtual #7             ; #7 = Methodref Animal.speak:()V
```

Notice:
- The compiler **does not know** the object is a `Dog`. It only sees the **static type** `Animal`.
- It writes `invokevirtual` with a **symbolic reference** to `Animal.speak` — a name, not a memory address.
- The `#7` is an index into the class's **constant pool**, where the entry looks like:
  ```
  #7 = Methodref  #8.#9        // Animal.speak:()V
  #8 = Class      Animal
  #9 = NameAndType speak:()V
  ```

So the bytecode says: *"call `speak` on whatever object is on the stack, resolved against class `Animal`."* The `Animal` part is only a **hint** used for verification and as the starting point for lookup — it is **not** the method that necessarily runs.

## Step 2: The Class Constant Pool

Every class has a **constant pool** — a table of symbolic references (to classes, methods, fields, strings, etc.). When the compiler needs to refer to `Animal.speak`, it adds an entry to this pool and emits the *index* of that entry into the bytecode.

At runtime, the JVM must turn each symbolic entry into a **direct reference** — an actual pointer to a class, method, or field in memory. This resolution step is exactly what **dynamic linking** does, and it's normally deferred until the entry is first used.

Heed:
- *"deferred until the entry is first used"* means: the JVM does not resolve a constant-pool entry when the class is loaded. It waits until the exact moment some bytecode instruction actually needs that entry, and only then does the resolution work.
# How Dynamic Method Dispatch Actually Works

Let me walk through the whole mechanism, from the bytecode the compiler emits to the exact steps the JVM takes at runtime. The key structures are the **constant pool**, the **vtable**, and the `invokevirtual` instruction.

Heed:
- **constant pool**, the **vtable**, and the `invokevirtual` instruction. Are further discussed below.

## Step 1: What the Compiler Produces

For this code:

```java
class Animal {
    void speak() { System.out.println("..."); }
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Woof"); }
}

Animal a = new Dog();
a.speak();  // prints "Woof"
```

The compiler emits bytecode roughly like:

```
aload_1                      ; push local variable a (the reference)
invokevirtual #7             ; #7 = Methodref Animal.speak:()V
```

Notice:
- The compiler **does not know** the object is a `Dog`. It only sees the **static type** `Animal`.

  Heed:
  - *"Static type"* mantionned just above means the type the compiler knows about, based on how the variable is declared. It's fixed in the source code and never changes. The word "static" here means "determined at compile time" — it has nothing to do with the static keyword.

- It writes `invokevirtual` with a **symbolic reference** to `Animal.speak` — a name, not a memory address.
- The `#7` is an index into the class's **constant pool**, where the entry looks like:
  ```
  #7 = Methodref  #8.#9        // Animal.speak:()V
  #8 = Class      Animal
  #9 = NameAndType speak:()V
  ```

So the bytecode says: *"call `speak` on whatever object is on the stack, resolved against class `Animal`."* The `Animal` part is only a **hint** used for verification and as the starting point for lookup — it is **not** the method that necessarily runs.

Heed:
- *"Resolved against class `Animal`"* means: when the JVM turns the symbolic reference `Animal.speak:()V` into something concrete, it does that lookup using `Animal` as the reference class — `Animal` is the starting point and frame of reference for the search. The word "against" means "using as the basis."

## Step 2: The Class Constant Pool

Every class has a **constant pool** — a table of symbolic references (to classes, methods, fields, strings, etc.). When the compiler needs to refer to `Animal.speak`, it adds an entry to this pool and emits the *index* of that entry into the bytecode.

At runtime, the JVM must turn each symbolic entry into a **direct reference** — an actual pointer to a class, method, or field in memory. This resolution step is exactly what **dynamic linking** does, and it's normally deferred until the entry is first used.

## Step 3: The Vtable (Virtual Method Table)

Here's the key structure that makes dispatch fast.

Every class that the JVM loads gets a **vtable** — an array of pointers to the actual implementations of its virtual methods. 

Heed:
- [What Is a Virtual Method](What%20Is%20a%20Virtual%20Method.md)

Method slots are assigned consistently across the class hierarchy:

```
Animal's vtable:              Dog's vtable:
┌───────────────────┐         ┌───────────────────┐
│ [0] Animal.speak  │         │ [0] Dog.speak     │  ← overridden
│ [1] Animal.hashCode? ...    │ [1] ...           │
└───────────────────┘         └───────────────────┘
```

When `Dog` overrides `speak`, the JVM puts a pointer to **`Dog.speak`** in the **same slot** (`[0]`) where `Animal` had `Animal.speak`. Other inherited methods keep pointing at the parent's implementation.

This consistent slot numbering is what makes the lookup O(1): the JVM doesn't search by name at runtime. It computes a **fixed offset** and jumps.

If `Dog` had a method `bark()` that `Animal` doesn't have, it would go in a new slot (`[2]`, etc.), but it would not affect the slots used for overridden methods.

## Step 4: The `invokevirtual` Sequence at Runtime

When the JVM executes `invokevirtual #7`, here is what happens (conceptually):

1. **Pop the receiver** — the object reference `a` (which actually points to a `Dog`).
2. **Follow the object header** to find its class. Every Java object has an object header containing (among other things) a pointer to its `Class` object — the *runtime class* of the object, which here is `Dog`, not `Animal`.
3. **Look up the method in the vtable** of that runtime class. The JVM already knows which slot `speak` occupies (from the resolved symbolic reference / method resolution). It reads the pointer at that slot — which is `Dog.speak`.
4. **Jump** to that method's code, with `this` bound to the object.
5. (First time only) **Resolve and cache** the symbolic reference so future executions can skip straight to step 3.

The crucial point is in **step 2**: the JVM uses the object's **actual runtime class** (`Dog`), not the **static type** from the bytecode (`Animal`). That's the entire trick.

## Step 5: Where Resolution Fits In

The first time `invokevirtual #7` runs, the JVM must resolve constant-pool entry `#7`:

- It looks up class `Animal` (loading/linking it if needed).
- It finds `speak:()V` in `Animal` (or its superclasses) and determines its **vtable index**.
- It replaces the symbolic reference with either a direct pointer or an "inline cache" entry recording the resolved class + slot.
- Subsequent executions can use the cached result — often the JVM optimizes this into a **single compare + jump** (a *monomorphic inline cache*) if it sees the same class every time.

So **dynamic linking** does the *name → slot* conversion once; **dynamic dispatch** uses that slot every call, indexed by the receiver's actual class.

## Step 6: Why the "Static Type" Still Matters

Even though `Dog.speak` runs, the compiler's use of `Animal` is not pointless. The verifier and the resolution step use `Animal` to:

- **Verify** that `Animal` actually has a `speak()` method callable with no args returning `void` — otherwise the bytecode is invalid.
- **Determine the vtable slot** to use. Since `Dog` and `Animal` share the slot numbering, the JVM knows that "the slot for `Animal.speak`" in `Dog`'s vtable holds `Dog.speak`.

This is why you can't get a `NoSuchMethodError` from calling a method that doesn't exist — the compiler and verifier catch that. You *can* get one if the classpath has a different version of a class at runtime (a classic `NoSuchMethodError` scenario).

## Step 7: The Different `invoke*` Instructions

Not every call is dynamically dispatched. The JVM has several invoke instructions:

| Instruction | Dispatch | Example |
|-------------|----------|---------|
| `invokestatic` | No receiver — the class is fixed | `Math.abs(x)` |
| `invokespecial` | Fixed — no overriding | constructors, `super.m()`, private methods |
| `invokevirtual` | **Dynamic** via vtable | normal instance methods |
| `invokeinterface` | **Dynamic** via itable | calls through an interface reference |
| `invokedynamic` | Fully dynamic, resolved by a bootstrap method | lambdas, `String::concat`, dynamic languages |

For `invokevirtual`, the receiver's runtime class decides the method. For `invokeinterface`, the JVM uses a similar structure called the **itable** (interface method table), since a class can implement multiple interfaces and slots can't be assigned as simply.

## Step 8: A Worked Example

```java
Animal a = new Dog();
a.speak();
a = new Cat();
a.speak();
```

Execution:

1. `a` holds a reference to a `Dog` object (its header says `Class = Dog`).
2. `invokevirtual Animal.speak` → look up slot for `speak` in `Dog`'s vtable → `Dog.speak` → prints "Woof".
3. `a` is reassigned to a `Cat` object (header says `Class = Cat`).
4. Same `invokevirtual` instruction → look up slot for `speak` in `Cat`'s vtable → `Cat.speak` → prints "Meow".

**Same bytecode instruction**, **different methods executed** — because the JVM looks at the object's actual class each time. That is dynamic method dispatch.

## Step 9: What Makes It "Dynamic Linking"?

It's helpful to separate two ideas that are often conflated:

- **Dynamic dispatch** — choosing *which* method implementation to run based on the runtime type of the receiver. Handled by the vtable + `invokevirtual`.
- **Dynamic linking** — resolving *symbolic references* (names in the constant pool) into *direct references* (pointers/offsets) at runtime. This is what produces the vtable slot or the resolved method pointer in the first place.

They work together: dynamic linking sets up the machinery (resolves `Animal.speak` to a slot, wires up vtables when `Dog` is loaded), and dynamic dispatch uses it on each call (indexes that slot with the receiver's real class).

## The Fast Path (What Really Happens in Practice)

Modern JVMs (HotSpot) optimize this heavily:

1. **First call**: full resolution — slow path.
2. **Subsequent calls with the same receiver class**: an **inline cache** compares the receiver's class to the cached one; if they match, it jumps straight to the method. This is a **monomorphic** call site.
3. **Multiple receiver classes**: a **polymorphic inline cache** checks a few classes; if too many, it falls back to a full vtable lookup.
4. **JIT compilation**: the JIT may inline the method and insert a **guard** (`if receiver.class == Dog then ...`). If the guard fails, it deoptimizes and falls back to the interpreter.

So in steady state, a "virtual" call can be as fast as a direct call — the dynamic part becomes a single class comparison.

## Putting It All Together

```
Compile time:
   a.speak()  →  invokevirtual #7  (Methodref Animal.speak:()V)

Class loading:
   Animal loaded → vtable slot 0 = Animal.speak
   Dog loaded    → vtable slot 0 = Dog.speak  (overrides)

Runtime, first call:
   Resolve #7 → find slot 0 for speak
   Object header → Class = Dog
   vtable[Dog][0] → Dog.speak
   Jump there

Runtime, later calls:
   Inline cache: if receiver.class == Dog → jump to Dog.speak
```

## Summary

> The compiler emits `invokevirtual` with a **symbolic reference** to `Animal.speak` and records its **vtable slot**. Each class has a **vtable** where overridden methods occupy the **same slot** as in the parent. At runtime, the JVM reads the receiver's **actual class** from its object header, indexes that class's vtable at the precomputed slot, and jumps to the implementation found there. **Dynamic linking** resolves the symbolic reference to that slot/pointer (once, lazily), and **dynamic dispatch** uses it on every call to pick the right override — which is why `a.speak()` runs `Dog.speak` even though the bytecode only mentions `Animal`.
## Step 3: The Vtable (Virtual Method Table)

Here's the key structure that makes dispatch fast.

Every class that the JVM loads gets a **vtable** — an array of pointers to the actual implementations of its virtual methods. Method slots are assigned consistently across the class hierarchy:

```
Animal's vtable:              Dog's vtable:
┌───────────────────┐         ┌───────────────────┐
│ [0] Animal.speak  │         │ [0] Dog.speak     │  ← overridden
│ [1] Animal.hashCode? ...    │ [1] ...           │
└───────────────────┘         └───────────────────┘
```

When `Dog` overrides `speak`, the JVM puts a pointer to **`Dog.speak`** in the **same slot** (`[0]`) where `Animal` had `Animal.speak`. Other inherited methods keep pointing at the parent's implementation.

This consistent slot numbering is what makes the lookup O(1): the JVM doesn't search by name at runtime. It computes a **fixed offset** and jumps.

If `Dog` had a method `bark()` that `Animal` doesn't have, it would go in a new slot (`[2]`, etc.), but it would not affect the slots used for overridden methods.

## Step 4: The `invokevirtual` Sequence at Runtime

When the JVM executes `invokevirtual #7`, here is what happens (conceptually):

1. **Pop the receiver** — the object reference `a` (which actually points to a `Dog`).
2. **Follow the object header** to find its class. Every Java object has an object header containing (among other things) a pointer to its `Class` object — the *runtime class* of the object, which here is `Dog`, not `Animal`.
3. **Look up the method in the vtable** of that runtime class. The JVM already knows which slot `speak` occupies (from the resolved symbolic reference / method resolution). It reads the pointer at that slot — which is `Dog.speak`.
4. **Jump** to that method's code, with `this` bound to the object.
5. (First time only) **Resolve and cache** the symbolic reference so future executions can skip straight to step 3.

The crucial point is in **step 2**: the JVM uses the object's **actual runtime class** (`Dog`), not the **static type** from the bytecode (`Animal`). That's the entire trick.

## Step 5: Where Resolution Fits In

The first time `invokevirtual #7` runs, the JVM must resolve constant-pool entry `#7`:

- It looks up class `Animal` (loading/linking it if needed).
- It finds `speak:()V` in `Animal` (or its superclasses) and determines its **vtable index**.
- It replaces the symbolic reference with either a direct pointer or an "inline cache" entry recording the resolved class + slot.
- Subsequent executions can use the cached result — often the JVM optimizes this into a **single compare + jump** (a *monomorphic inline cache*) if it sees the same class every time.

So **dynamic linking** does the *name → slot* conversion once; **dynamic dispatch** uses that slot every call, indexed by the receiver's actual class.

## Step 6: Why the "Static Type" Still Matters

Even though `Dog.speak` runs, the compiler's use of `Animal` is not pointless. The verifier and the resolution step use `Animal` to:

- **Verify** that `Animal` actually has a `speak()` method callable with no args returning `void` — otherwise the bytecode is invalid.
- **Determine the vtable slot** to use. Since `Dog` and `Animal` share the slot numbering, the JVM knows that "the slot for `Animal.speak`" in `Dog`'s vtable holds `Dog.speak`.

This is why you can't get a `NoSuchMethodError` from calling a method that doesn't exist — the compiler and verifier catch that. You *can* get one if the classpath has a different version of a class at runtime (a classic `NoSuchMethodError` scenario).

## Step 7: The Different `invoke*` Instructions

Not every call is dynamically dispatched. The JVM has several invoke instructions:

| Instruction | Dispatch | Example |
|-------------|----------|---------|
| `invokestatic` | No receiver — the class is fixed | `Math.abs(x)` |
| `invokespecial` | Fixed — no overriding | constructors, `super.m()`, private methods |
| `invokevirtual` | **Dynamic** via vtable | normal instance methods |
| `invokeinterface` | **Dynamic** via itable | calls through an interface reference |
| `invokedynamic` | Fully dynamic, resolved by a bootstrap method | lambdas, `String::concat`, dynamic languages |

For `invokevirtual`, the receiver's runtime class decides the method. For `invokeinterface`, the JVM uses a similar structure called the **itable** (interface method table), since a class can implement multiple interfaces and slots can't be assigned as simply.

## Step 8: A Worked Example

```java
Animal a = new Dog();
a.speak();
a = new Cat();
a.speak();
```

Execution:

1. `a` holds a reference to a `Dog` object (its header says `Class = Dog`).
2. `invokevirtual Animal.speak` → look up slot for `speak` in `Dog`'s vtable → `Dog.speak` → prints "Woof".
3. `a` is reassigned to a `Cat` object (header says `Class = Cat`).
4. Same `invokevirtual` instruction → look up slot for `speak` in `Cat`'s vtable → `Cat.speak` → prints "Meow".

**Same bytecode instruction**, **different methods executed** — because the JVM looks at the object's actual class each time. That is dynamic method dispatch.

## Step 9: What Makes It "Dynamic Linking"?

It's helpful to separate two ideas that are often conflated:

- **Dynamic dispatch** — choosing *which* method implementation to run based on the runtime type of the receiver. Handled by the vtable + `invokevirtual`.
- **Dynamic linking** — resolving *symbolic references* (names in the constant pool) into *direct references* (pointers/offsets) at runtime. This is what produces the vtable slot or the resolved method pointer in the first place.

They work together: dynamic linking sets up the machinery (resolves `Animal.speak` to a slot, wires up vtables when `Dog` is loaded), and dynamic dispatch uses it on each call (indexes that slot with the receiver's real class).

## The Fast Path (What Really Happens in Practice)

Modern JVMs (HotSpot) optimize this heavily:

1. **First call**: full resolution — slow path.
2. **Subsequent calls with the same receiver class**: an **inline cache** compares the receiver's class to the cached one; if they match, it jumps straight to the method. This is a **monomorphic** call site.
3. **Multiple receiver classes**: a **polymorphic inline cache** checks a few classes; if too many, it falls back to a full vtable lookup.
4. **JIT compilation**: the JIT may inline the method and insert a **guard** (`if receiver.class == Dog then ...`). If the guard fails, it deoptimizes and falls back to the interpreter.

So in steady state, a "virtual" call can be as fast as a direct call — the dynamic part becomes a single class comparison.

## Putting It All Together

```
Compile time:
   a.speak()  →  invokevirtual #7  (Methodref Animal.speak:()V)

Class loading:
   Animal loaded → vtable slot 0 = Animal.speak
   Dog loaded    → vtable slot 0 = Dog.speak  (overrides)

Runtime, first call:
   Resolve #7 → find slot 0 for speak
   Object header → Class = Dog
   vtable[Dog][0] → Dog.speak
   Jump there

Runtime, later calls:
   Inline cache: if receiver.class == Dog → jump to Dog.speak
```

## Summary

> The compiler emits `invokevirtual` with a **symbolic reference** to `Animal.speak` and records its **vtable slot**. Each class has a **vtable** where overridden methods occupy the **same slot** as in the parent. At runtime, the JVM reads the receiver's **actual class** from its object header, indexes that class's vtable at the precomputed slot, and jumps to the implementation found there. **Dynamic linking** resolves the symbolic reference to that slot/pointer (once, lazily), and **dynamic dispatch** uses it on every call to pick the right override — which is why `a.speak()` runs `Dog.speak` even though the bytecode only mentions `Animal`.
