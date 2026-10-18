class A {
    public String f ( D obj ) { 
        return ( " A and D " );
    }

    public String f ( A obj ) {
        return ( " A and A " );
    }
}

class B extends A {
    public String f ( B obj ) { 
        return ( " B and B " );
    }

    public String f ( A obj ) {
        return ( " B and A " );
    }
}
class C extends B {
}

class D extends B {
}

class Exo3 {
    public static void main ( String [] args ){
        A a1 = new A ();
        A a2 = new B ();
        B b = new B ();
        C c = new C ();
        D d = new D ();
        System.out.println ( a1.f ( b ));
        System.out.println ( a1.f ( c ));
        System.out.println ( a1.f ( d ));
        System.out.println ( a2.f ( b ));
        System.out.println ( a2.f ( c ));
        System.out.println ( a2.f ( d ));
        System.out.println ( b. f ( b ));
        System.out.println ( b. f ( c ));
        System.out.println ( b. f ( d ));
    }
}
