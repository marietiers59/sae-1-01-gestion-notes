import java.util.Scanner;

/**
 * SAÉ 1.01 – Prototype en console de la gestion des notes (avant le passage au navigateur).
 * Sert à valider la logique : saisies, matrice étudiants × matières, moyennes pondérées.
 * Groupe 20 : Benjamin BOUTOUT, Louis CARON, Marie TIERS.
 *
 * Compilation et exécution : javac RecueilNotes2D.java && java RecueilNotes2D
 * Les nombres décimaux acceptent indifféremment la virgule ou le point (12,5 ou 12.5).
 */
public class RecueilNotes2D {

    private final Scanner in = new Scanner(System.in);
    private int nbEtudiants;
    private int nbMatieres;
    private String[] etudiants;
    private String[] matieres;
    private float[][] notes; // notes[i][j] : note de l'étudiant i dans la matière j
    private float[] coefficients;

    public RecueilNotes2D() {
        System.out.print("Entrez le nombre de matières : ");
        nbMatieres = in.nextInt();
        in.nextLine();
        matieres = new String[nbMatieres];
        coefficients = new float[nbMatieres];
    }

    private void demanderNombreEtudiants() {
        System.out.print("Entrez le nombre d'étudiants : ");
        nbEtudiants = in.nextInt();
        in.nextLine();
        etudiants = new String[nbEtudiants];
        notes = new float[nbEtudiants][nbMatieres];
    }

    private void saisirNomsMatieres() {
        for (int j = 0; j < nbMatieres; j++) {
            System.out.print("Entrez le nom de la matière " + (j + 1) + " : ");
            matieres[j] = in.nextLine();
        }
        System.out.println("Les matières sont : " + String.join(", ", matieres));
    }

    private void saisirNomsEtudiants() {
        for (int i = 0; i < nbEtudiants; i++) {
            System.out.print("Entrez le nom de l'étudiant " + (i + 1) + " : ");
            etudiants[i] = in.nextLine();
        }
        System.out.println("Les étudiants sont : " + String.join(", ", etudiants));
    }

    /** Lit un nombre décimal sur une ligne, avec virgule ou point, et redemande en cas d'erreur. */
    private float lireReel() {
        while (true) {
            String saisie = in.nextLine().trim().replace(',', '.');
            try {
                return Float.parseFloat(saisie);
            } catch (NumberFormatException e) {
                System.out.print("Nombre invalide, recommencez : ");
            }
        }
    }

    private void saisirCoefficients() {
        for (int j = 0; j < nbMatieres; j++) {
            System.out.print("Entrez le coefficient de la matière " + matieres[j] + " : ");
            coefficients[j] = lireReel();
        }
    }

    private void saisirNotes() {
        for (int i = 0; i < nbEtudiants; i++) {
            for (int j = 0; j < nbMatieres; j++) {
                System.out.print("Entrez la note de " + etudiants[i] + " en " + matieres[j] + " : ");
                notes[i][j] = lireReel();
            }
        }
    }

    /** Moyenne générale pondérée par les coefficients. */
    private float moyenneEtudiant(int i) {
        float sommeNotes = 0;
        float sommeCoefficients = 0;
        for (int j = 0; j < nbMatieres; j++) {
            sommeNotes += notes[i][j] * coefficients[j];
            sommeCoefficients += coefficients[j];
        }
        return sommeNotes / sommeCoefficients;
    }

    private void afficherNotes() {
        for (int i = 0; i < nbEtudiants; i++) {
            StringBuilder ligne = new StringBuilder("Notes de " + etudiants[i] + " : ");
            for (int j = 0; j < nbMatieres; j++) {
                if (j > 0) ligne.append(" ; ");
                ligne.append(matieres[j]).append(" ").append(notes[i][j])
                     .append(" (coef. ").append(coefficients[j]).append(")");
            }
            ligne.append(String.format("  | moyenne : %.2f", moyenneEtudiant(i)));
            System.out.println(ligne);
        }
    }

    public static void main(String[] args) {
        RecueilNotes2D recueil = new RecueilNotes2D();
        recueil.saisirNomsMatieres();
        recueil.saisirCoefficients();
        recueil.demanderNombreEtudiants();
        recueil.saisirNomsEtudiants();
        recueil.saisirNotes();
        recueil.afficherNotes();
    }
}
