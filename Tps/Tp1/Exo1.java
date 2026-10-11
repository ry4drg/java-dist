class Exo1{
    private int val =0;
    public static void affichePlus ( int a ) {
        a ++;
        System.out.println ( a );
    }

    public static void affichePlus ( Exo1 a ) {
        a.val ++;
        System.out.println ( a.val );
    }

    public static void main ( String [] args ) {
        Exo1 O1 = new Exo1 ();
        Exo1 O2 = new Exo1 ();
        Exo1.affichePlus ( O1.val );
        Exo1.affichePlus ( O1.val );
        Exo1.affichePlus ( O1 );
        Exo1.affichePlus ( O1 );
        Exo1.affichePlus ( O2 );
        Exo1.affichePlus ( O2 );

        if ( O1 == O2 )
            System.out.println ( " Equals " );
        else
            System.out.println ( " Different " );
    }
}
