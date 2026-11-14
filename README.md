# Hôtel Les Tilleuls - Gestion des réservations

Application console Java de gestion des réservations d'un hôtel : recherche de disponibilités, devis avec tarification saisonnière et remises, réservation, annulation avec frais, statistiques de gestion.

## 1. But de l'application

Un hôtel propose plusieurs types de chambres (standard, familiale, suite) dont les prix varient selon la saison, le jour de la semaine et le nombre d'occupants. L'application permet au personnel de la réception de :

- trouver une chambre libre adaptée à un groupe pour des dates données ;
- établir un devis détaillé en appliquant automatiquement les remises auxquelles le client a droit ;
- enregistrer une réservation en garantissant qu'aucune chambre n'est réservée deux fois ;
- annuler une réservation en calculant les frais selon la politique propre à chaque type de chambre ;
- suivre l'activité (taux d'occupation, chiffre d'affaires, meilleurs clients).

## 2. Lancer l'application

**Prérequis :** JDK 21 ou plus récent, Maven (celui intégré à IntelliJ suffit).

**Depuis IntelliJ IDEA**

1. `File > Open` puis sélectionner le dossier du projet (IntelliJ détecte le `pom.xml`).
2. Ouvrir `src/main/java/com/junia/hotel/Application.java` et cliquer sur ▶ à côté de `main`.

**En ligne de commande**

```bash
mvn compile exec:java
```

ou, pour produire un JAR exécutable :

```bash
mvn package
java -jar target/hotel-reservation-1.0.jar
```

Par défaut, les données sont lues dans `src/main/resources/data/`. Pour utiliser un autre jeu de données, passer en argument un dossier contenant `chambres.csv`, `clients.csv` et `reservations.csv` :

```bash
java -jar target/hotel-reservation-1.0.jar chemin/vers/mes-donnees
```

## 3. Fonctionnalités

| Menu | Fonctionnalité | Règles métier |
|---|---|---|
| 1, 2 | Afficher les chambres et les clients | - |
| 3 | Rechercher les chambres disponibles | Capacité suffisante, aucune réservation active qui chevauche la période, tri par prix croissant |
| 4 | Demander un devis | Prix à la nuit propre au type de chambre, coefficient saisonnier, remises cumulées puis plafonnées |
| 5 | Réserver une chambre | Client et chambre existants, capacité respectée, pas de date passée, pas de chevauchement |
| 6 | Annuler une réservation | Gratuite si le délai de la chambre est respecté, 50 % de frais sinon, impossible une fois le séjour commencé |
| 7 | Consulter les réservations d'un client | Triées par date d'arrivée |
| 8 | Statistiques | Taux d'occupation sur une période, chiffre d'affaires encaissé par type de chambre, top 3 des clients |

**Règles tarifaires**

- **Chambre standard :** prix fixe, annulation gratuite jusqu'à 2 jours avant l'arrivée.
- **Chambre familiale :** prix de base pour 2 personnes + supplément par personne en plus, annulation gratuite jusqu'à 3 jours avant.
- **Suite :** majoration les nuits du vendredi et du samedi, annulation gratuite jusqu'à 7 jours avant.
- **Saisons :** basse (janvier, février, novembre) x0,85 ; moyenne x1,00 ; haute (juillet, août, décembre) x1,30. Chaque nuit est tarifée selon sa propre saison, ce qui gère correctement un séjour à cheval sur deux saisons.
- **Remises :** long séjour (7 nuits et plus, -10 %), fidélité (Argent -5 %, Or -10 %), réservation anticipée (60 jours et plus avant l'arrivée, -8 %). Elles se cumulent dans la limite de 25 % du prix.
- Le jour de départ d'un client peut être le jour d'arrivée du suivant.

## 4. Architecture et classes principales

```
src/main/java/com/junia/hotel/
├── Application.java              point d'entrée : assemble les dépendances et lance le menu
├── model/                        entités et objets valeur du domaine
├── service/                      logique métier
│   └── remise/                   règles de remise (stratégies)
├── repository/                   stockage des entités
├── chargement/                   lecture des fichiers CSV et import
├── exception/                    exceptions personnalisées
├── ui/                           interface console
└── util/                         validation, arrondis, mise en forme
src/main/resources/data/          fichiers de données chargés au démarrage
src/test/java/com/junia/hotel/    tests unitaires JUnit 5
```

| Classe | Rôle |
|---|---|
| `Chambre` (abstraite) | État commun d'une chambre, déclare la tarification et la politique d'annulation |
| `ChambreStandard`, `ChambreFamiliale`, `Suite` | Implémentent chacune leur propre prix à la nuit et leur délai d'annulation |
| `Client`, `StatutFidelite` | Client de l'hôtel et niveau de fidélité qui détermine sa remise |
| `Periode` (record) | Séjour toujours valide ; calcule nuits, chevauchement et intersection |
| `Reservation` | Réservation et son cycle de vie (confirmée puis éventuellement annulée avec frais) |
| `Saison` (enum) | Coefficient tarifaire selon le mois |
| `Devis`, `LigneRemise`, `ResultatAnnulation` | Résultats immuables des opérations métier |
| `CalculateurTarif` | Calcule le prix d'un séjour : nuits, saisons, remises, plafond |
| `Remise` (interface) + 3 implémentations | Règles de remise interchangeables |
| `ReservationService` | Devis, réservation, annulation et toutes leurs vérifications |
| `DisponibiliteService` | Disponibilité d'une chambre et recherche de chambres libres |
| `StatistiquesService` | Taux d'occupation, chiffre d'affaires, classement des clients |
| `Repository<T, ID>` / `RepositoryMemoire` | Stockage générique ; `ReservationRepository` ajoute les requêtes propres aux réservations |
| `ChargeurCsv`, `LigneCsv`, `ParseurLigne<T>` | Lecture générique d'un CSV et conversion typée des colonnes |
| `ImportateurDonnees` | Charge les trois fichiers dans l'ordre et produit un rapport d'import |
| `MenuPrincipal`, `SaisieConsole` | Menu console et saisies validées |

## 5. Décisions techniques

1. **`BigDecimal` pour tous les montants.** Un `double` ne représente pas exactement 0,10 : les remises et les arrondis produiraient des centimes faux. Les montants sont arrondis au centime en un seul endroit (`Montants`), avec la même règle partout.
2. **Exceptions métier non vérifiées, exception de format vérifiée.** Une règle métier violée (chambre déjà prise, capacité dépassée...) ne peut pas être corrigée par l'appelant direct : elle remonte jusqu'au menu ou à l'import, qui l'interceptent toutes via la classe mère `HotelException`. À l'inverse, `FormatDonneesException` est vérifiée : le compilateur oblige le chargeur à la traiter pour écarter la ligne fautive et continuer.
3. **Horloge injectée (`java.time.Clock`).** « Réserver dans le passé », « annuler moins de 7 jours avant » ou « réserver 60 jours à l'avance » dépendent de la date du jour. En injectant l'horloge, les tests s'exécutent à une date fixe et restent reproductibles.
4. **Remises en patron Stratégie avec plafond global.** Chaque remise est une classe indépendante qui renvoie un `Optional<LigneRemise>` (vide si le séjour n'y a pas droit). Le calculateur les applique sans les connaître, puis plafonne le cumul à 25 % pour préserver la rentabilité.
5. **Import des réservations via le service métier.** Les réservations du fichier ne sont pas insérées directement : elles passent par `ReservationService.reserver()`, donc par les mêmes contrôles qu'une saisie au clavier. Une ligne invalide ou en conflit est signalée dans le rapport de démarrage sans bloquer l'application.

## 6. Concepts orientés objet

### Héritage

`Chambre` est la classe mère de `ChambreStandard`, `ChambreFamiliale` et `Suite`. Dans un hôtel, ce sont bien des chambres (relation « est un ») : elles partagent un numéro, un étage, une capacité, un prix de base et leurs validations, mais se distinguent par leur tarification et leur politique d'annulation.

### Polymorphisme

- `CalculateurTarif` appelle `chambre.prixNuit(nuit, nbPersonnes)` sans connaître le type réel : la suite applique sa majoration de week-end, la chambre familiale son supplément par personne.
- `ReservationService.calculerFraisAnnulation()` appelle `chambre.delaiAnnulationGratuiteJours()` : 2, 3 ou 7 jours selon l'objet réel.
- `CalculateurTarif` parcourt une `List<Remise>` et appelle `calculer()` sur chaque élément, qu'il s'agisse d'une remise long séjour, fidélité ou anticipée.
- `ChargeurCsv.charger()` accepte n'importe quel `ParseurLigne<T>` et `SourceDonnees` (classpath, dossier, ou chaîne en mémoire dans les tests).

### Classe abstraite et interfaces

- **`Chambre` est une classe abstraite** parce que les chambres partagent de l'état et du code (attributs, validation, `equals`, `toString`) en plus d'un contrat. Ses méthodes abstraites `prixNuit`, `delaiAnnulationGratuiteJours` et `getType` obligent chaque type à définir son comportement.
- **`Remise` est une interface** parce que les remises n'ont rien en commun hormis le contrat `calculer` : aucun état partagé, et une nouvelle promotion peut être ajoutée sans hiérarchie.
- Autres interfaces : `Repository<T, ID>` et `ReservationRepository` (stockage), `SourceDonnees` (origine des fichiers), `ParseurLigne<T>` (conversion d'une ligne), `Identifiable<ID>` (ce dont un dépôt a besoin).

## 7. Exceptions personnalisées

| Exception | Levée quand |
|---|---|
| `HotelException` (abstraite) | Classe mère de toutes les erreurs métier, interceptée par le menu et l'import |
| `ChambreIndisponibleException` | La chambre est déjà réservée sur une partie de la période |
| `CapaciteDepasseeException` | Le nombre de personnes dépasse la capacité de la chambre |
| `ElementIntrouvableException` | Client, chambre ou réservation inexistant |
| `ReservationInvalideException` | Réservation demandée pour une date passée |
| `AnnulationImpossibleException` | Réservation déjà annulée ou séjour déjà commencé |
| `FormatDonneesException` (vérifiée) | Ligne de fichier CSV mal formée |

Des exceptions standard sont aussi utilisées là où elles ont du sens : `IllegalArgumentException` pour un argument invalide (période incohérente, prix négatif...), `NullPointerException` via `Objects.requireNonNull`, `IOException` quand un fichier est absent, et `NumberFormatException` / `DateTimeParseException` interceptées et traduites en messages clairs.

## 8. Chargement des données

Au démarrage, `ImportateurDonnees` lit trois fichiers CSV (séparateur `;`, encodage UTF-8) dans cet ordre, car les réservations font référence aux chambres et aux clients :

| Fichier | Colonnes |
|---|---|
| `chambres.csv` | `type;numero;etage;capacite;prix_base;option` (option = supplément par personne pour FAMILIALE, taux de majoration week-end pour SUITE) |
| `clients.csv` | `id;nom;email;statut` (STANDARD, ARGENT ou OR) |
| `reservations.csv` | `client;chambre;arrivee;depart;personnes` (dates au format AAAA-MM-JJ) |

- La première ligne utile est l'en-tête ; les lignes vides et celles commençant par `#` sont ignorées.
- Chaque ligne est convertie par un `ParseurLigne` dédié. Une ligne invalide est écartée avec son numéro et la raison, les autres sont chargées.
- Les identifiants en double sont signalés au lieu d'écraser la première occurrence.
- Les réservations passent par le service métier (voir décision 5). Le fichier fourni contient volontairement une réservation en conflit pour montrer ce mécanisme : elle apparaît dans le rapport affiché au démarrage.
- Si un fichier est absent, l'application s'arrête avec un message explicite.

## 9. Principes SOLID

- **S - Responsabilité unique.** Chaque classe a une seule raison de changer : `CalculateurTarif` le calcul du prix, `DisponibiliteService` la disponibilité, `ChargeurCsv` la lecture de fichier, `MenuPrincipal` l'affichage, `Application` uniquement l'assemblage. Il n'y a pas de classe « fourre-tout ».
- **O - Ouvert/fermé.** Ajouter une remise (par exemple « -15 % en semaine ») se fait en créant une classe qui implémente `Remise`, sans modifier le calculateur. De même, ajouter une option de menu se limite à une ligne dans `enregistrerOptions()`.
- **L - Substitution de Liskov.** Toute sous-classe de `Chambre` peut remplacer `Chambre` : chacune respecte le contrat (prix au moins égal au prix de base, délai positif) et l'égalité est définie une fois pour toutes (`equals` final) sur le numéro.
- **I - Ségrégation des interfaces.** `Repository` ne contient que les opérations communes ; les recherches par chambre ou par client sont dans `ReservationRepository`, que seuls les services concernés utilisent. `Remise`, `SourceDonnees` et `ParseurLigne` n'ont chacune qu'une méthode.
- **D - Inversion des dépendances.** Les services reçoivent par constructeur des abstractions (`Repository`, `ReservationRepository`, `List<Remise>`, `Clock`, `SourceDonnees`) et ne créent jamais leurs dépendances. Seule `Application` choisit les implémentations concrètes, ce qui permet aux tests d'injecter une horloge fixe ou des données en mémoire.

## 10. Collections et fonctionnalités Java utilisées

- **`List`** pour les résultats ordonnés, **`Map`** (`LinkedHashMap`) pour l'accès par identifiant dans les dépôts, **`EnumMap`** pour le chiffre d'affaires par type de chambre, **`EnumSet`/`Set`** pour les mois d'une saison et les nuits de week-end.
- **Streams et lambdas** pour les recherches, totaux et regroupements (`groupingBy`, `reducing`, `flatMap(Optional::stream)`).
- **`Optional`** pour les recherches qui peuvent échouer (`trouverParId`) et les remises qui peuvent ne pas s'appliquer.
- **Enums avec comportement** (`Saison.de(date)`, `StatutFidelite` avec son taux).
- **`LocalDate`**, `ChronoUnit`, `datesUntil` pour toute la logique de dates.
- **Génériques** : `Repository<T extends Identifiable<ID>, ID>`, `ParseurLigne<T>`, `ResultatChargement<T>`, `LigneCsv.enumeration(..., Class<E>)`.
- **Records** pour les objets valeur immuables (`Periode`, `Devis`, `DemandeReservation`...).

## 11. Tests

Les 93 tests JUnit 5 couvrent la logique métier, pas les getters :

| Classe de test | Ce qui est vérifié |
|---|---|
| `PeriodeTest` | Calcul des nuits, chevauchements (y compris départ = arrivée), intersection, périodes invalides |
| `ChambreTest` | Tarif de chaque type (supplément, majoration week-end), capacité, délais polymorphes, validations |
| `ReservationTest` | Annulation, montant encaissé, double annulation, frais incohérents |
| `CalculateurTarifTest` | Saisons, séjour à cheval sur deux saisons, chaque remise à sa limite exacte, plafonnement, arrondi |
| `ReservationServiceTest` | Réservation normale, conflit, capacité, éléments inconnus, date passée, annulation gratuite / tardive / impossible |
| `DisponibiliteServiceTest` | Filtrage par capacité et par dates, tri par prix, réservations annulées |
| `StatistiquesServiceTest` | Taux d'occupation sur période partielle, chiffre d'affaires avec frais d'annulation, classement |
| `ChambreCsvParserTest`, `ChargeurCsvTest`, `ImportateurDonneesTest` | Parsing, lignes invalides signalées avec leur numéro, doublons, conflits à l'import, fichier absent, validité des données fournies |

Chaque fonctionnalité est testée dans les trois situations : cas normal, cas limite (exactement 7 nuits, exactement 60 jours, arrivée le jour du départ précédent...) et cas invalide (exception attendue).

**Lancer les tests**

- IntelliJ : clic droit sur `src/test/java` puis `Run 'All Tests'`.
- Ligne de commande :

```bash
mvn test
```
