class A {
    int i ;
    int f () { return i ; }
    static String g () { return ( " A " ); }
    String h () { return g (); }
}

class B extends A {
    int i =2;
    int f () { return -i ; }
    static String g () { return ( " B " ); }
    String h () { return g (); }
}

class Exo4 {
    public static void main ( String [] args ) {
        B b = new B ();
        System.out.println ( b.i );
        System.out.println ( b.f ());
        System.out.println ( b.g ());
        System.out.println ( b.h ());
        A a = b;
        System.out.println ( a.i );
        System.out.println ( a.f ());
        System.out.println ( a.g ());
        System.out.println ( a.h ());
    }
}
