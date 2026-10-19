# Polymorphism in Java

**Polymorphism** means **"many forms"** (Greek: *poly* = many, *morph* = form). In Java, it's the ability of **one reference, one method name, or one interface to represent different types and behave differently at runtime** depending on what it actually is.

It's one of the four pillars of OOP (with encapsulation, inheritance, and abstraction), and it's the reason `Animal a = new Dog(); a.speak();` can print `"Woof"` even though the compiler only saw `Animal`.

## The Core Idea in One Example

```java
Animal a = new Dog();
a.speak();   // "Woof"

a = new Cat();
a.speak();   // "Meow"
```

Same variable `a`, same method call `a.speak()`, **different behavior** — because the object underneath changed. That's polymorphism: **one interface, many implementations.**

## The Two Main Kinds in Java

Java has **two distinct kinds** of polymorphism:

| Kind | When resolved | Mechanism | Example |
|------|---------------|-----------|---------|
| **Compile-time** (static) | At compile time | Method **overloading** | `print(int)` vs `print(String)` |
| **Runtime** (dynamic) | At runtime | Method **overriding** + dynamic dispatch | `a.speak()` → `Dog.speak` |

Most discussions of "polymorphism" mean the **runtime** kind, but both are real and worth knowing.

## 1. Runtime Polymorphism (Subtype / Dynamic)

This is the big one. It relies on **inheritance** and **overriding**, and it's powered by the **vtable** and **dynamic dispatch** we've been discussing.

### The Setup

```java
class Animal {
    void speak() { System.out.println("..."); }
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Woof"); }
}
class Cat extends Animal {
    @Override
    void speak() { System.out.println("Meow"); }
}
```

### The Magic

```java
Animal a;             // static type: Animal
a = new Dog();
a.speak();            // runtime type: Dog  → "Woof"
a = new Cat();
a.speak();            // runtime type: Cat  → "Meow"
```

- The **compiler** sees `a` as `Animal` and checks that `Animal` has a `speak()` method.
- The **JVM** sees the object's actual class (`Dog`, `Cat`) and runs the right override.

### Why This Is Powerful

You can write code that operates on the **general type** and let subclasses supply the specifics:

```java
void makeAllSpeak(Animal[] animals) {
    for (Animal a : animals) {
        a.speak();   // works for any Animal subclass
    }
}

Animal[] zoo = { new Dog(), new Cat(), new Dog() };
makeAllSpeak(zoo);   // Woof, Meow, Woof
```

The method `makeAllSpeak` **doesn't know or care** what concrete animals it gets. It only knows they're `Animal`s and can `speak()`. Add a new `Bird` class tomorrow — `makeAllSpeak` works unchanged.

**This is the whole point of polymorphism: code to the interface, not the implementation.**

## 2. Compile-Time Polymorphism (Overloading)

This is resolved entirely by the **compiler**, based on the **static types** of the arguments.

```java
class Printer {
    void print(int x)    { System.out.println("int: " + x); }
    void print(String s) { System.out.println("String: " + s); }
    void print(double d) { System.out.println("double: " + d); }
}

Printer p = new Printer();
p.print(5);        // compiler picks print(int)
p.print("hi");     // compiler picks print(String)
p.print(3.14);     // compiler picks print(double)
```

Same method name `print`, **different signatures**. The compiler chooses the right one at compile time based on argument types. No runtime dispatch involved.

> Note: overloading is sometimes called **"ad-hoc polymorphism"** and some argue it's not "true" polymorphism (it's just name reuse). Overriding is the real deal. But Java texts often present both.

## Overloading vs. Overriding — Don't Confuse Them

| Aspect | Overloading | Overriding |
|--------|-------------|------------|
| Where | Same class (or subclass) | Subclass replaces parent's method |
| Signature | **Different** parameters | **Same** signature |
| Resolved | Compile time | Runtime |
| Polymorphism kind | Compile-time (static) | Runtime (dynamic) |
| Keyword | none needed | `@Override` (recommended) |
| Example | `print(int)` / `print(String)` | `Dog.speak()` replaces `Animal.speak()` |

```java
class Animal {
    void speak() {}                 // ← overridden
    void eat(String food) {}        // ← overloaded vs eat(int)
    void eat(int amount) {}         // ← overloaded vs eat(String)
}
```

## The Rules for Runtime Polymorphism

For overriding to work:

1. **Inheritance** — the subclass must extend (or implement) the parent/interface.
2. **Same signature** — same name, same parameter types, compatible return type (covariant returns allowed since Java 5).
3. **Access can't be more restrictive** — a `public` method can't be overridden as `protected`.
4. **Not `final`, `static`, or `private`** — those can't be overridden (they're non-virtual).
5. **The reference type must declare the method** — you can only call methods visible on the static type.

```java
Animal a = new Dog();
a.speak();    // OK — Animal declares speak()
a.fetch();    // COMPILE ERROR — Animal doesn't declare fetch(), even though Dog does
```

To call `fetch()`, you'd need a `Dog` reference or a cast:

```java
Dog d = (Dog) a;   // cast — runtime check
d.fetch();         // OK
```

## Polymorphism With Interfaces

Interfaces are the cleanest form of polymorphism — no inheritance hierarchy baggage, just a contract:

```java
interface Shape {
    double area();
}
class Circle implements Shape {
    double r;
    public double area() { return Math.PI * r * r; }
}
class Square implements Shape {
    double side;
    public double area() { return side * side; }
}

Shape[] shapes = { new Circle(), new Square() };
double total = 0;
for (Shape s : shapes) {
    total += s.area();   // dispatched to the right implementation
}
```

`Shape` says **what** can be done; each class says **how**. The loop only knows `Shape` — it works for any current or future shape.

This is often called **"programming to an interface"** and is the recommended style.

## How Java Implements Runtime Polymorphism (Under the Hood)

Tying back to everything we discussed:

1. Compiler emits `invokevirtual Animal.speak` (or `invokeinterface` for interfaces).
2. The receiver's **actual class** is read from the object header at runtime.
3. The JVM uses that class's **vtable** (or **itable** for interfaces) to find the method's implementation.
4. **Dynamic dispatch** jumps to the right override.
5. **Inline caches** make repeated calls fast — often monomorphic, so nearly free.

So polymorphism isn't magic — it's **dynamic linking + vtables + dynamic dispatch**, all decided at runtime.

## What Polymorphism Enables

### 1. Extensibility

Add new subclasses without changing existing code:

```java
class Bird extends Animal {
    @Override void speak() { System.out.println("Tweet"); }
}
// makeAllSpeak(zoo) still works — no change needed
```

### 2. Loose coupling

Code depends on abstractions, not concrete classes:

```java
void save(Storage s) { s.write(data); }   // works for FileStorage, DBStorage, CloudStorage...
```

### 3. Framework design

Frameworks call **your** code through interfaces you implement:

```java
list.sort(Comparator.comparing(Employee::name));  // you supply the comparison logic
```

### 4. Collections and generics

```java
List<Animal> animals = new ArrayList<>();
animals.add(new Dog());
animals.add(new Cat());
for (Animal a : animals) a.speak();
```

## A Worked End-to-End Example

```java
abstract class Employee {
    String name;
    Employee(String name) { this.name = name; }
    abstract double pay();                    // virtual, no body
    void report() {
        System.out.println(name + " earns " + pay());  // calls the override
    }
}

class SalariedEmployee extends Employee {
    double salary;
    SalariedEmployee(String n, double s) { super(n); salary = s; }
    @Override double pay() { return salary; }
}

class HourlyEmployee extends Employee {
    double rate; int hours;
    HourlyEmployee(String n, double r, int h) { super(n); rate = r; hours = h; }
    @Override double pay() { return rate * hours; }
}

public class Main {
    public static void main(String[] args) {
        Employee[] staff = {
            new SalariedEmployee("Alice", 5000),
            new HourlyEmployee("Bob", 20, 160)
        };
        for (Employee e : staff) {
            e.report();       // dispatches to the right pay()
        }
    }
}
```

Output:

```
Alice earns 5000.0
Bob earns 3200.0
```

Notice:
- `report()` is defined **once** in `Employee`, yet works for every subclass.
- `pay()` is **abstract** — each subclass must provide it.
- The loop calls `e.report()` on the general type; **dispatch** picks the right `pay()` inside.

## Common Mistakes and Gotchas

### 1. Fields are NOT polymorphic

```java
class Animal { String name = "Animal"; }
class Dog extends Animal { String name = "Dog"; }

Animal a = new Dog();
System.out.println(a.name);   // "Animal" — static type wins for fields
```

Methods dispatch dynamically; **fields are resolved by static type**. Big source of confusion.

### 2. `static` methods are not overridden, they're hidden

```java
class Animal { static void info() { System.out.println("Animal"); } }
class Dog extends Animal { static void info() { System.out.println("Dog"); } }

Animal a = new Dog();
a.info();    // "Animal" — static methods don't dispatch dynamically
```

### 3. Overloading is chosen by static type

```java
void f(Animal a)  { System.out.println("Animal"); }
void f(Dog d)     { System.out.println("Dog"); }

Animal a = new Dog();
f(a);   // "Animal" — compiler picks based on static type Animal
```

Same object, but overload resolution happens at compile time using the **static** type.

### 4. Calling overridable methods from constructors

```java
class Parent {
    Parent() { speak(); }       // BAD: calls overridable method
    void speak() { System.out.println("Parent"); }
}
class Child extends Parent {
    int x = 5;
    @Override void speak() { System.out.println("Child " + x); }
}
new Child();   // prints "Child 0" — x not yet initialized!
```

The override runs **before** the subclass's fields are initialized. Avoid calling overridable methods in constructors.

## Summary

> **Polymorphism** in Java is the ability of **one reference or method name to take many forms**. It comes in two kinds:
>
> - **Compile-time (static)** — method **overloading**, resolved by the compiler using static types.
> - **Runtime (dynamic)** — method **overriding** + **dynamic dispatch**, resolved by the JVM using the object's actual class, via the vtable and `invokevirtual`.
>
> Runtime polymorphism is the important one: it lets you write code against a **general type** (`Animal`, `Shape`, an interface) and have the **specific behavior** supplied by subclasses at runtime. It's enabled by inheritance, overriding, and the vtable mechanism, and it's what makes Java code extensible, loosely coupled, and able to "program to an interface, not an implementation." The classic line — *`Animal a = new Dog(); a.speak();` prints `"Woof"`* — is polymorphism in action.
