import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import navigator.Navigator;

/**
 * SAÉ 1.01 – Gestion des notes d'une promotion, avec une interface web pilotée par Java.
 * IUT de Lens, BUT Informatique S1, groupe 20 : Benjamin BOUTOUT, Louis CARON, Marie TIERS.
 *
 * Fonctionnement de Navigator : nav.beginPage() attend la requête suivante du navigateur
 * (clic sur un lien ou envoi d'un formulaire) ; le programme lit alors la réponse,
 * puis écrit la page suivante et la termine par nav.endPage().
 *
 * Invariant du programme : en dehors de envoyerPage(), une page est toujours « ouverte »
 * (beginPage() a déjà été appelé), ce qui évite d'envoyer des pages vides au navigateur.
 *
 * Compilation : javac -cp ../../lib/nav.jar Notes.java
 * Exécution   : java -cp .:../../lib/nav.jar Notes   puis ouvrir http://localhost:8080
 */
public class Notes {

    /** Feuille de style commune à toutes les pages (mise en page en flexbox). */
    private static final String STYLE = """
            body { font-family: Arial, sans-serif; margin: 0; padding: 40px 16px; background: #f0f0f0; }
            .carte { background: white; padding: 30px; border-radius: 10px; max-width: 1100px; margin: auto;
                     display: flex; flex-direction: column; gap: 16px; }
            .etroite { max-width: 500px; }
            h1, h2 { color: #333; margin: 0; }
            form { display: flex; flex-direction: column; gap: 10px; }
            label { font-weight: bold; }
            input, select { padding: 10px; font-size: 16px; }
            input[type="submit"] { background: #4CAF50; color: white; border: none; cursor: pointer; }
            .erreur { color: #721c24; background: #f8d7da; padding: 10px; border-radius: 5px; }
            .mode { text-align: center; padding: 10px; font-weight: bold; }
            .mode-lecture { background: #2196F3; color: white; }
            .mode-edition { background: #FFC107; color: black; }
            table { width: 100%; border-collapse: collapse; }
            th, td { padding: 12px; text-align: center; border: 1px solid #ddd; }
            th { background: #4CAF50; color: white; }
            .nom { text-align: left; font-weight: bold; }
            .satisfaisant { background: #d4edda; color: #155724; font-weight: bold; }
            .moyen { background: #fff3cd; color: #856404; font-weight: bold; }
            .mauvais, .echec { background: #f8d7da; color: #721c24; font-weight: bold; }
            .boutons { display: flex; flex-wrap: wrap; justify-content: center; gap: 10px; }
            .btn { background: #2196F3; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; }
            .btn-edit { background: #FFC107; color: black; }
            .btn-save { background: #4CAF50; }
            .btn-danger { background: #f44336; }
            .classement { display: flex; align-items: center; gap: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px; }
            .rang { font-size: 24px; font-weight: bold; min-width: 50px; }
            .classement .nom { flex: 1; font-size: 18px; }
            """;

    private static final String FICHIER_EXPORT = "notes_export.csv";

    private final Navigator nav = new Navigator();

    // Données de la promotion
    private int nbEtudiants;
    private int nbMatieres;
    private String[] etudiants;
    private String[] matieres;
    private float[] coefficients;
    private float[][] notes; // notes[i][j] : note de l'étudiant i dans la matière j

    // État de l'interface
    private boolean modeEdition = false;

    // ------------------------------------------------------------------
    // Outils d'affichage et de saisie
    // ------------------------------------------------------------------

    /** Écrit une page HTML complète, la termine, puis attend la requête suivante. */
    private void envoyerPage(String titre, String contenu, boolean etroite) {
        nav.println("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + titre + "</title><style>" + STYLE + "</style></head><body>"
                + "<main class=\"carte" + (etroite ? " etroite" : "") + "\">" + contenu + "</main>"
                + "</body></html>");
        nav.endPage();
        nav.beginPage();
    }

    /** Échappe les caractères spéciaux d'un texte saisi avant de l'insérer dans du HTML. */
    private static String echapper(String texte) {
        return texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String classeNote(float note) {
        if (note >= 10) return "satisfaisant";
        if (note >= 9) return "moyen";
        return "mauvais";
    }

    /** Formulaire à une seule question ; le champ HTML fourni doit avoir id="valeur". */
    private static String formulaire(String question, String champHtml) {
        return "<form method=\"POST\"><label for=\"valeur\">" + question + "</label>" + champHtml
                + "<input type=\"submit\" value=\"Valider\"></form>";
    }

    /** Demande un entier compris entre min et max, en reposant la question si la saisie est invalide. */
    private int demanderEntier(String titre, String question, int min, int max) {
        String champ = "<input type=\"number\" name=\"valeur\" id=\"valeur\" min=\"" + min + "\" max=\"" + max
                + "\" required autofocus>";
        String erreur = "";
        while (true) {
            envoyerPage(titre, "<h1>" + titre + "</h1>" + erreur + formulaire(question, champ), true);
            if (nav.isInt("valeur")) {
                int valeur = nav.getInt("valeur");
                if (valeur >= min && valeur <= max) return valeur;
            }
            erreur = "<p class=\"erreur\">Saisissez un entier entre " + min + " et " + max + ".</p>";
        }
    }

    /** Demande un réel compris entre min et max (pas de 0,5), en reposant la question si besoin. */
    private float demanderReel(String titre, String question, double min, double max) {
        String champ = "<input type=\"number\" name=\"valeur\" id=\"valeur\" min=\"" + min + "\" max=\"" + max
                + "\" step=\"0.5\" required autofocus>";
        String erreur = "";
        while (true) {
            envoyerPage(titre, "<h1>" + titre + "</h1>" + erreur + formulaire(question, champ), true);
            if (nav.isDouble("valeur")) {
                double valeur = nav.getDouble("valeur");
                if (valeur >= min && valeur <= max) return (float) valeur;
            }
            erreur = "<p class=\"erreur\">Saisissez un nombre entre " + min + " et " + max + ".</p>";
        }
    }

    /** Demande un texte non vide (le point-virgule est exclu car il sert de séparateur dans les fichiers). */
    private String demanderTexte(String titre, String question, String valeurParDefaut) {
        String champ = "<input type=\"text\" name=\"valeur\" id=\"valeur\" value=\"" + valeurParDefaut
                + "\" required autofocus>";
        String erreur = "";
        while (true) {
            envoyerPage(titre, "<h1>" + titre + "</h1>" + erreur + formulaire(question, champ), true);
            String valeur = nav.get("valeur");
            if (valeur != null && !valeur.isBlank() && !valeur.contains(";")) return valeur.trim();
            erreur = "<p class=\"erreur\">Saisissez un texte non vide, sans point-virgule.</p>";
        }
    }

    /** Page de message avec un lien de retour vers le tableau. */
    private void afficherMessage(String titre, String message) {
        envoyerPage(titre, "<h1>" + titre + "</h1><p>" + message + "</p>"
                + "<div class=\"boutons\"><a class=\"btn\" href=\"/\">Retour au tableau</a></div>", true);
    }

    // ------------------------------------------------------------------
    // Saisie initiale
    // ------------------------------------------------------------------

    private void saisirMatieres() {
        nbMatieres = demanderEntier("Configuration", "Nombre de matières :", 1, 20);
        matieres = new String[nbMatieres];
        coefficients = new float[nbMatieres];
        for (int j = 0; j < nbMatieres; j++) {
            matieres[j] = demanderTexte("Matière " + (j + 1), "Nom de la matière " + (j + 1) + " :", "");
        }
        for (int j = 0; j < nbMatieres; j++) {
            coefficients[j] = demanderReel("Coefficient – " + echapper(matieres[j]),
                    "Coefficient de la matière " + echapper(matieres[j]) + " :", 0.5, 10);
        }
    }

    private void saisirEtudiants() {
        nbEtudiants = demanderEntier("Configuration", "Nombre d'étudiants :", 1, 100);
        etudiants = new String[nbEtudiants];
        notes = new float[nbEtudiants][nbMatieres];
        for (int i = 0; i < nbEtudiants; i++) {
            etudiants[i] = demanderTexte("Étudiant " + (i + 1), "Nom de l'étudiant " + (i + 1) + " :", "");
        }
    }

    private void saisirNotes() {
        for (int i = 0; i < nbEtudiants; i++) {
            for (int j = 0; j < nbMatieres; j++) {
                notes[i][j] = demanderReel("Saisie des notes",
                        "Note de <strong>" + echapper(etudiants[i]) + "</strong> en <strong>"
                                + echapper(matieres[j]) + "</strong> :", 0, 20);
            }
        }
    }

    // ------------------------------------------------------------------
    // Calculs
    // ------------------------------------------------------------------

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

    /** Moyenne de la promotion dans une matière (fonctionnalité optionnelle). */
    private float moyenneMatiere(int j) {
        float somme = 0;
        for (int i = 0; i < nbEtudiants; i++) {
            somme += notes[i][j];
        }
        return somme / nbEtudiants;
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    private String tableauNotes() {
        StringBuilder html = new StringBuilder("<h1>Tableau des notes</h1>");
        html.append(modeEdition
                ? "<div class=\"mode mode-edition\">Mode édition</div>"
                : "<div class=\"mode mode-lecture\">Mode consultation</div>");
        html.append("<table><tr><th>Étudiant</th>");
        for (int j = 0; j < nbMatieres; j++) {
            html.append("<th>").append(echapper(matieres[j]))
                .append(String.format("<br><small>(coef. %.1f)</small></th>", coefficients[j]));
        }
        html.append("<th>Moyenne</th></tr>");
        for (int i = 0; i < nbEtudiants; i++) {
            float moyenne = moyenneEtudiant(i);
            html.append("<tr><td class=\"nom").append(moyenne < 10 ? " echec" : "").append("\">")
                .append(echapper(etudiants[i])).append("</td>");
            for (int j = 0; j < nbMatieres; j++) {
                html.append("<td class=\"").append(classeNote(notes[i][j])).append("\">")
                    .append(String.format("%.2f", notes[i][j])).append("</td>");
            }
            html.append("<td class=\"").append(classeNote(moyenne)).append("\">")
                .append(String.format("%.2f", moyenne)).append("</td></tr>");
        }
        html.append("</table><div class=\"boutons\">");
        html.append("<a href=\"/editer\" class=\"btn btn-edit\">")
            .append(modeEdition ? "Verrouiller" : "Passer en mode édition").append("</a>");
        if (modeEdition) {
            html.append("<a href=\"/modifier\" class=\"btn\">Modifier une note</a>");
        }
        html.append("""
                <a href="/sauvegarder" class="btn btn-save">Sauvegarder</a>
                <a href="/charger" class="btn">Charger</a>
                <a href="/moyennes-matieres" class="btn">Moyennes par matière</a>
                <a href="/classement" class="btn">Classement</a>
                <a href="/export" class="btn">Export CSV</a>
                <a href="/quitter" class="btn btn-danger">Quitter</a>
                </div>""");
        return html.toString();
    }

    private void modifierNote() {
        StringBuilder html = new StringBuilder("<h1>Modifier une note</h1><form method=\"POST\">");
        html.append("<label for=\"etu\">Étudiant :</label><select name=\"etu\" id=\"etu\">");
        for (int i = 0; i < nbEtudiants; i++) {
            html.append("<option value=\"").append(i).append("\">").append(echapper(etudiants[i])).append("</option>");
        }
        html.append("</select><label for=\"mat\">Matière :</label><select name=\"mat\" id=\"mat\">");
        for (int j = 0; j < nbMatieres; j++) {
            html.append("<option value=\"").append(j).append("\">").append(echapper(matieres[j])).append("</option>");
        }
        html.append("""
                </select>
                <label for="note">Nouvelle note (0 à 20) :</label>
                <input type="number" name="note" id="note" min="0" max="20" step="0.5" required autofocus>
                <input type="submit" value="Valider">
                </form>
                <div class="boutons"><a href="/" class="btn btn-danger">Annuler</a></div>""");
        envoyerPage("Modifier une note", html.toString(), true);

        // Le lien « Annuler » renvoie une requête sans champ : la note n'est pas modifiée.
        if (nav.isInt("etu") && nav.isInt("mat") && nav.isDouble("note")) {
            int i = nav.getInt("etu");
            int j = nav.getInt("mat");
            double note = nav.getDouble("note");
            if (i >= 0 && i < nbEtudiants && j >= 0 && j < nbMatieres && note >= 0 && note <= 20) {
                notes[i][j] = (float) note;
            }
        }
    }

    /** Refuse les noms de fichiers qui permettraient d'écrire hors du dossier courant. */
    private static boolean nomDeFichierValide(String nom) {
        return nom.matches("[\\w.-]+") && !nom.startsWith(".");
    }

    private void sauvegarder() {
        String nomFichier = demanderTexte("Sauvegarder", "Nom du fichier :", "notes.txt");
        if (!nomDeFichierValide(nomFichier)) {
            afficherMessage("Sauvegarde impossible", "Nom de fichier refusé : lettres, chiffres, point, tiret et tiret bas uniquement.");
            return;
        }
        try (PrintWriter ecrivain = new PrintWriter(new FileWriter(nomFichier))) {
            ecrivain.println(nbMatieres + " " + nbEtudiants);
            for (int j = 0; j < nbMatieres; j++) {
                ecrivain.println(matieres[j] + ";" + coefficients[j]);
            }
            for (int i = 0; i < nbEtudiants; i++) {
                ecrivain.print(etudiants[i]);
                for (int j = 0; j < nbMatieres; j++) {
                    ecrivain.print(";" + notes[i][j]);
                }
                ecrivain.println();
            }
            afficherMessage("Sauvegarde réussie", "Fichier : " + echapper(nomFichier));
        } catch (IOException e) {
            afficherMessage("Sauvegarde impossible", echapper(e.getMessage()));
        }
    }

    /** Relit un fichier au format de sauvegarder() ; les données actuelles ne changent qu'en cas de succès. */
    private void charger() {
        String nomFichier = demanderTexte("Charger", "Nom du fichier :", "notes.txt");
        if (!nomDeFichierValide(nomFichier)) {
            afficherMessage("Chargement impossible", "Nom de fichier refusé.");
            return;
        }
        try (BufferedReader lecteur = new BufferedReader(new FileReader(nomFichier))) {
            String[] dimensions = lecteur.readLine().trim().split(" ");
            int nouvellesMatieres = Integer.parseInt(dimensions[0]);
            int nouveauxEtudiants = Integer.parseInt(dimensions[1]);
            String[] nomsMatieres = new String[nouvellesMatieres];
            float[] nouveauxCoefficients = new float[nouvellesMatieres];
            String[] nomsEtudiants = new String[nouveauxEtudiants];
            float[][] nouvellesNotes = new float[nouveauxEtudiants][nouvellesMatieres];

            for (int j = 0; j < nouvellesMatieres; j++) {
                String[] champs = lecteur.readLine().split(";");
                nomsMatieres[j] = champs[0];
                nouveauxCoefficients[j] = Float.parseFloat(champs[1]);
            }
            for (int i = 0; i < nouveauxEtudiants; i++) {
                String[] champs = lecteur.readLine().split(";");
                nomsEtudiants[i] = champs[0];
                for (int j = 0; j < nouvellesMatieres; j++) {
                    nouvellesNotes[i][j] = Float.parseFloat(champs[j + 1]);
                }
            }
            nbMatieres = nouvellesMatieres;
            nbEtudiants = nouveauxEtudiants;
            matieres = nomsMatieres;
            coefficients = nouveauxCoefficients;
            etudiants = nomsEtudiants;
            notes = nouvellesNotes;
            afficherMessage("Chargement réussi", "Fichier : " + echapper(nomFichier));
        } catch (IOException | RuntimeException e) {
            afficherMessage("Chargement impossible", "Fichier introuvable ou mal formé : " + echapper(nomFichier));
        }
    }

    private void afficherMoyennesMatieres() {
        StringBuilder html = new StringBuilder("<h1>Moyennes par matière</h1><table>"
                + "<tr><th>Matière</th><th>Coefficient</th><th>Moyenne de la promotion</th></tr>");
        for (int j = 0; j < nbMatieres; j++) {
            float moyenne = moyenneMatiere(j);
            html.append("<tr><td class=\"nom\">").append(echapper(matieres[j])).append("</td>")
                .append(String.format("<td>%.1f</td>", coefficients[j]))
                .append("<td class=\"").append(classeNote(moyenne)).append("\">")
                .append(String.format("%.2f / 20", moyenne)).append("</td></tr>");
        }
        html.append("</table><div class=\"boutons\"><a href=\"/\" class=\"btn\">Retour</a></div>");
        envoyerPage("Moyennes par matière", html.toString(), false);
    }

    /** Classement par moyenne décroissante (tri par sélection sur un tableau d'indices). */
    private void afficherClassement() {
        float[] moyennes = new float[nbEtudiants];
        int[] ordre = new int[nbEtudiants];
        for (int i = 0; i < nbEtudiants; i++) {
            moyennes[i] = moyenneEtudiant(i);
            ordre[i] = i;
        }
        for (int i = 0; i < nbEtudiants - 1; i++) {
            int meilleur = i;
            for (int k = i + 1; k < nbEtudiants; k++) {
                if (moyennes[ordre[k]] > moyennes[ordre[meilleur]]) {
                    meilleur = k;
                }
            }
            int temporaire = ordre[i];
            ordre[i] = ordre[meilleur];
            ordre[meilleur] = temporaire;
        }

        StringBuilder html = new StringBuilder("<h1>Classement des étudiants</h1>");
        for (int rang = 0; rang < nbEtudiants; rang++) {
            int i = ordre[rang];
            html.append("<div class=\"classement\"><div class=\"rang\">").append(rang + 1).append(".</div>")
                .append("<div class=\"nom\">").append(echapper(etudiants[i])).append("</div>")
                .append("<div class=\"").append(classeNote(moyennes[i])).append("\">")
                .append(String.format("%.2f / 20", moyennes[i])).append("</div></div>");
        }
        html.append("<div class=\"boutons\"><a href=\"/\" class=\"btn\">Retour</a></div>");
        envoyerPage("Classement", html.toString(), false);
    }

    /** Export CSV (séparateur point-virgule), téléchargeable depuis la page de confirmation. */
    private void exporterCSV() {
        try (PrintWriter ecrivain = new PrintWriter(new FileWriter(FICHIER_EXPORT))) {
            ecrivain.print("Étudiant");
            for (int j = 0; j < nbMatieres; j++) {
                ecrivain.print(";" + matieres[j]);
            }
            ecrivain.println(";Moyenne");
            for (int i = 0; i < nbEtudiants; i++) {
                ecrivain.print(etudiants[i]);
                for (int j = 0; j < nbMatieres; j++) {
                    ecrivain.print(";" + notes[i][j]);
                }
                ecrivain.println(String.format(";%.2f", moyenneEtudiant(i)));
            }
            afficherMessage("Export CSV réussi", "Fichier créé : <a href=\"/" + FICHIER_EXPORT + "\" download>"
                    + FICHIER_EXPORT + "</a>");
        } catch (IOException e) {
            afficherMessage("Export impossible", echapper(e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // Programme principal
    // ------------------------------------------------------------------

    void run() {
        nav.allowDownload(FICHIER_EXPORT);
        nav.beginPage(); // attend la première connexion du navigateur

        saisirMatieres();
        saisirEtudiants();
        saisirNotes();

        boolean continuer = true;
        while (continuer) {
            envoyerPage("Tableau des notes", tableauNotes(), false);
            switch (nav.getPath()) {
                case "/editer" -> modeEdition = !modeEdition;
                case "/modifier" -> {
                    if (modeEdition) modifierNote();
                }
                case "/sauvegarder" -> sauvegarder();
                case "/charger" -> charger();
                case "/moyennes-matieres" -> afficherMoyennesMatieres();
                case "/classement" -> afficherClassement();
                case "/export" -> exporterCSV();
                case "/quitter" -> continuer = false;
                default -> { } // toute autre adresse réaffiche le tableau
            }
        }

        nav.println("""
                <!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8"><title>Au revoir</title></head>
                <body style="text-align: center; margin-top: 100px; font-family: Arial, sans-serif;">
                <h1>Au revoir !</h1><p>Merci d'avoir utilisé l'application.</p>
                <p style="color: #999;">SAÉ 1.01 – IUT de Lens – groupe 20</p></body></html>""");
        nav.endPage();
        nav.close();
    }

    public static void main(String[] args) {
        new Notes().run();
    }
}
