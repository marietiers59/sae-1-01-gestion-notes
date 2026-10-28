import navigator.Navigator;

/**
 * SAÉ 1.01, étape 1 – Premier contact avec Navigator : afficher une page web générée par Java.
 *
 * Compilation : javac -cp ../../lib/nav.jar Test.java
 * Exécution   : java -cp .:../../lib/nav.jar Test   puis ouvrir http://localhost:8080
 */
class Test {

    private final Navigator nav = new Navigator();

    void run() {
        nav.beginPage();
        nav.println("""
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"><title>Page de test</title></head>
                <body>
                    <h1>Ceci est une page de test</h1>
                    <section>
                        <p>Un chasseur sachant chasser sans son chien est un bon chasseur.</p>
                    </section>
                    <section>
                        <ul>
                            <li>1</li>
                            <li>3</li>
                            <li>12</li>
                        </ul>
                    </section>
                </body>
                </html>
                """);
        nav.endPage();
        nav.close();
    }

    public static void main(String[] args) {
        new Test().run();
    }
}
