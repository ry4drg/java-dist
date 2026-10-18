# Polymorphism in Java

## 1. Definition

**Polymorphism** (from Greek *polys* = many, *morphē* = form) is the ability of a single reference, method name, or interface to represent **different types** and exhibit **different behavior** depending on the actual object it refers to at runtime. It is one of the four fundamental pillars of object-oriented programming, alongside encapsulation, inheritance, and abstraction.

In Java, polymorphism allows you to write code that operates on a **general type** while the **specific behavior** is supplied by subclasses. The same method call can produce different results depending on the object that receives it.

## 2. The Two Forms of Polymorphism in Java

Java supports two distinct kinds of polymorphism:

| Form | Also called | Resolved | Mechanism |
|------|-------------|----------|-----------|
| **Compile-time polymorphism** | Static polymorphism, early binding | At compile time | Method **overloading** |
| **Runtime polymorphism** | Dynamic polymorphism, late binding | At runtime | Method **overriding** + dynamic dispatch |

The runtime form is the more important one and is what most discussions of polymorphism refer to. Both are examined below.

## 3. Compile-Time Polymorphism (Method Overloading)

**Method overloading** occurs when two or more methods in the same class share the same name but have **different parameter lists** (different number, types, or order of parameters). The compiler selects the appropriate method based on the **static types** of the arguments at the call site.

### Example

```java
class Calculator {
    int add(int a, int b) {
        return a + b;
    }

    double add(double a, double b) {
        return a + b;
    }

    int add(int a, int b, int c) {
        return a + b + c;
    }
}

public class Main {
    public static void main(String[] args) {
        Calculator calc = new Calculator();
        System.out.println(calc.add(2, 3));          // calls add(int, int)       → 5
        System.out.println(calc.add(2.5, 3.5));      // calls add(double, double) → 6.0
        System.out.println(calc.add(1, 2, 3));       // calls add(int, int, int)  → 6
    }
}
```

The compiler examines the number and types of arguments and selects the matching signature. No runtime dispatch is involved — the method is fixed in the bytecode.

### Key Characteristics

- Occurs **within a single class** (or across a class hierarchy, but the resolution is still static).
- Signatures must differ in **parameter types, number, or order** — return type alone is not sufficient.
- Resolved entirely at **compile time**.
- Sometimes called *ad-hoc polymorphism*.

## 4. Runtime Polymorphism (Method Overriding)

**Method overriding** occurs when a subclass provides a **new implementation** of a method that is already declared in its superclass, using the **same signature** (same name, same parameter types, compatible return type). The version that executes is determined at **runtime** by the **actual type** of the object, not the declared type of the reference.

This is the form of polymorphism that gives Java its flexibility and extensibility.

### Example

```java
class Animal {
    void speak() {
        System.out.println("Some generic animal sound");
    }
}

class Dog extends Animal {
    @Override
    void speak() {
        System.out.println("Woof");
    }
}

class Cat extends Animal {
    @Override
    void speak() {
        System.out.println("Meow");
    }
}

public class Main {
    public static void main(String[] args) {
        Animal a1 = new Dog();
        Animal a2 = new Cat();

        a1.speak();   // Woof
        a2.speak();   // Meow
    }
}
```

Both `a1` and `a2` are declared as `Animal`. The compiler only knows that `Animal` has a `speak()` method. At runtime, however, the JVM inspects the actual class of each object (`Dog`, `Cat`) and invokes the appropriate override. This is **dynamic dispatch**.

### Why This Matters: Programming to an Interface

The real power of runtime polymorphism emerges when you write code that operates on the general type:

```java
public class Zoo {
    public static void main(String[] args) {
        Animal[] animals = { new Dog(), new Cat(), new Dog() };
        for (Animal a : animals) {
            a.speak();   // dispatches to the correct override each time
        }
    }
}
```

Output:
```
Woof
Meow
Woof
```

The loop does not know — and does not need to know — which concrete animals it is dealing with. It only knows they are `Animal`s and can `speak()`. If a new subclass `Bird` is added later, the loop works unchanged. This is the principle of **"program to an interface, not an implementation."**

### Key Characteristics

- Requires **inheritance** (or interface implementation) and **overriding**.
- The subclass method must have the **same signature** as the superclass method.
- The subclass method cannot have **more restrictive access** (e.g., `public` cannot become `protected`).
- The method must not be `final`, `static`, or `private` (these are non-virtual and cannot be overridden).
- Resolved at **runtime** by the JVM using the object's actual class.

## 5. Polymorphism Through Interfaces

Interfaces provide the cleanest expression of polymorphism: they define a **contract** without specifying an implementation. Any class that implements the interface can be used wherever the interface type is expected.

### Example

```java
interface Shape {
    double area();
}

class Circle implements Shape {
    private double radius;

    Circle(double radius) { this.radius = radius; }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }
}

class Rectangle implements Shape {
    private double width, height;

    Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() {
        return width * height;
    }
}

public class Main {
    public static void main(String[] args) {
        Shape[] shapes = { new Circle(2), new Rectangle(3, 4) };
        double total = 0;
        for (Shape s : shapes) {
            total += s.area();
        }
        System.out.println("Total area: " + total);
    }
}
```

Output:
```
Total area: 24.566370614359172
```

The `Shape` interface declares *what* can be done. Each implementing class defines *how*. The loop is written entirely in terms of `Shape` and remains correct for any current or future implementation.

## 6. How Java Implements Runtime Polymorphism

Under the hood, Java achieves dynamic dispatch through the following mechanism:

1. The compiler emits an `invokevirtual` (or `invokeinterface`) instruction with a symbolic reference to the method.
2. At runtime, the JVM reads the receiver object's **actual class** from its object header.
3. The JVM uses that class's **virtual method table (vtable)** — an array of pointers to method implementations — to locate the correct override.
4. Modern JVMs optimize repeated calls using **inline caches** and **JIT compilation**, often reducing the dispatch cost to a single class comparison.

The key insight is that the **static type** (what the compiler knows) determines *which method signature* is called, while the **dynamic type** (what the object actually is) determines *which implementation* runs.

## 7. Rules and Restrictions

For runtime polymorphism to work correctly:

1. **Inheritance or interface implementation** must exist between the classes.
2. The method must be **overridden** with the same signature.
3. The overriding method cannot have **more restrictive access** than the overridden method.
4. The method cannot be `final`, `static`, or `private`.
5. The reference type must **declare** the method for it to be callable — even if the actual object has additional methods.

### Example of Restriction 5

```java
class Animal {
    void speak() { System.out.println("..."); }
}

class Dog extends Animal {
    @Override
    void speak() { System.out.println("Woof"); }
    void fetch() { System.out.println("Fetching"); }
}

Animal a = new Dog();
a.speak();    // OK — Animal declares speak()
a.fetch();    // COMPILE ERROR — Animal does not declare fetch()
```

To call `fetch()`, a cast is required:

```java
Dog d = (Dog) a;
d.fetch();    // OK
```

## 8. Common Pitfalls

### 8.1 Fields Are Not Polymorphic

Unlike methods, **fields are resolved by static type**, not dynamic type:

```java
class Animal { String name = "Animal"; }
class Dog extends Animal { String name = "Dog"; }

Animal a = new Dog();
System.out.println(a.name);   // prints "Animal", not "Dog"
```

Method calls dispatch dynamically; field access does not.

### 8.2 Static Methods Are Not Overridden — They Are Hidden

```java
class Animal {
    static void info() { System.out.println("Animal"); }
}
class Dog extends Animal {
    static void info() { System.out.println("Dog"); }
}

Animal a = new Dog();
a.info();   // prints "Animal" — static methods do not dispatch dynamically
```

### 8.3 Overload Resolution Uses Static Types

```java
void print(Animal a) { System.out.println("Animal"); }
void print(Dog d)    { System.out.println("Dog"); }

Animal a = new Dog();
print(a);   // prints "Animal" — the compiler picks based on the static type
```

The argument's **static** type determines which overload is selected, regardless of the object's actual class.

### 8.4 Calling Overridable Methods from Constructors

```java
class Parent {
    Parent() { speak(); }
    void speak() { System.out.println("Parent"); }
}
class Child extends Parent {
    int x = 5;
    @Override
    void speak() { System.out.println("Child " + x); }
}

new Child();   // prints "Child 0" — x is not yet initialized
```

The override runs before the subclass's fields are initialized. Avoid calling overridable methods in constructors.

## 9. Benefits of Polymorphism

1. **Extensibility** — New subclasses can be added without modifying existing code that uses the general type.
2. **Loose coupling** — Code depends on abstractions rather than concrete classes, making systems easier to modify and test.
3. **Code reuse** — Common behavior can be written once against a base type and reused for all subclasses.
4. **Framework design** — Frameworks define contracts (often as interfaces) that user code implements, enabling inversion of control.
5. **Collections and generics** — Collections of a base type can hold instances of any subclass, enabling uniform processing.

## 10. Summary

> **Polymorphism** is the ability of a single reference or method name to represent multiple types and exhibit different behavior depending on the actual object involved. Java supports two forms:
>
> - **Compile-time polymorphism** (method overloading), where the compiler selects the method based on static argument types.
> - **Runtime polymorphism** (method overriding), where the JVM selects the implementation based on the receiver's actual class, using dynamic dispatch via the vtable.
>
> Runtime polymorphism is the more powerful form. It enables you to write code against a general type (`Animal`, `Shape`, an interface) and have the specific behavior supplied by subclasses at runtime. It is what makes Java applications extensible, loosely coupled, and able to follow the principle of programming to an interface. The canonical illustration — `Animal a = new Dog(); a.speak();` printing `"Woof"` — captures the essence: one reference type, many possible behaviors, decided at runtime.
