import navigator.Navigator;

/**
 * SAÉ 1.01, étape 1 – Gestion des disponibilités (ÉBAUCHE NON TERMINÉE).
 *
 * Ce qui fonctionne : le formulaire de paramétrage (nombre de jours, heures de début et de fin,
 * durée d'un créneau), puis l'affichage d'une page de tableau.
 * Ce qui reste à faire : générer les lignes du tableau à partir des valeurs saisies,
 * proposer une liste déroulante oui / non / peut-être par case, puis afficher le récapitulatif.
 * Le groupe a ensuite consacré le temps de la SAÉ à l'étape 2 (Notes.java).
 *
 * Compilation : javac -cp ../../lib/nav.jar Dispos.java
 * Exécution   : java -cp .:../../lib/nav.jar Dispos   puis ouvrir http://localhost:8080
 */
class Dispos {

    private final Navigator nav = new Navigator();

    void run() {
        // Page 1 : paramétrage
        nav.beginPage();
        nav.println("""
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"><title>Disponibilités</title></head>
                <body>
                <form method="POST">
                    <p><label>Nombre de jours disponibles : <input type="number" name="nbjour" min="1" required></label></p>
                    <p><label>Heure de début : <input type="time" name="dheure" required></label></p>
                    <p><label>Heure de fin : <input type="time" name="fheure" required></label></p>
                    <p><label>Durée d'un créneau : <input type="time" name="creneau" required></label></p>
                    <input type="submit" value="Valider">
                </form>
                </body>
                </html>
                """);
        nav.endPage();

        // Page 2 : tableau (encore statique dans cette ébauche)
        nav.beginPage();
        nav.println("""
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"><title>Disponibilités</title></head>
                <body>
                <table>
                    <caption>Disponibilités</caption>
                    <thead style="font-weight: bolder; color: blue;">
                        <tr><th>Heure</th><th>Jour 1</th><th>Jour 2</th></tr>
                    </thead>
                    <tbody>
                        <tr><td>dheure</td><td>Oui/Non/Peut-être</td><td>Oui/Non/Peut-être</td></tr>
                        <tr><td>dheure + creneau</td><td>Oui/Non/Peut-être</td><td>Oui/Non/Peut-être</td></tr>
                    </tbody>
                </table>
                </body>
                </html>
                """);
        nav.endPage();
        nav.close();
    }

    public static void main(String[] args) {
        new Dispos().run();
    }
}
