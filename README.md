# Hôtel Les Tilleuls - Reservation Management

Java console application for managing the reservations of a hotel: availability search, quotes with seasonal pricing and discounts, booking, cancellation with fees, and management statistics.

> The source code (class, method and variable names, comments and console messages) is written in French. This README explains each class in English.

## 1. Purpose of the application

A hotel offers several room types (standard, family, suite) whose prices vary with the season, the day of the week and the number of guests. The application lets the front-desk staff:

- find a free room suited to a group for given dates;
- produce a detailed quote, automatically applying the discounts the guest is entitled to;
- record a reservation while guaranteeing that no room is ever booked twice;
- cancel a reservation, computing the fees according to the policy of each room type;
- monitor activity (occupancy rate, revenue, best guests).

## 2. Running the application

**Requirements:** JDK 21 or newer, Maven (the one bundled with IntelliJ is enough).

**From IntelliJ IDEA**

1. `File > Open`, then select the project folder (IntelliJ detects the `pom.xml`).
2. Open `src/main/java/com/junia/hotel/Application.java` and click ▶ next to `main`.

**From the command line**

```bash
mvn compile exec:java
```

or, to build an executable JAR:

```bash
mvn package
java -jar target/hotel-reservation-1.0.jar
```

By default, data is read from `src/main/resources/data/`. To use another data set, pass as an argument a folder containing `chambres.csv`, `clients.csv` and `reservations.csv`:

```bash
java -jar target/hotel-reservation-1.0.jar path/to/my-data
```

## 3. Features

| Menu | Feature | Business rules |
|---|---|---|
| 1, 2 | List rooms and guests | - |
| 3 | Search available rooms | Sufficient capacity, no active reservation overlapping the period, sorted by ascending price |
| 4 | Request a quote | Nightly price specific to the room type, seasonal coefficient, discounts combined then capped |
| 5 | Book a room | Existing guest and room, capacity respected, no past date, no overlap |
| 6 | Cancel a reservation | Free if the room's notice period is respected, 50 % fee otherwise, impossible once the stay has started |
| 7 | View a guest's reservations | Sorted by arrival date |
| 8 | Statistics | Occupancy rate over a period, revenue collected per room type, top 3 guests |

**Pricing rules**

- **Standard room:** fixed price, free cancellation up to 2 days before arrival.
- **Family room:** base price for 2 people + a supplement for each additional person, free cancellation up to 3 days before arrival.
- **Suite:** surcharge on Friday and Saturday nights, free cancellation up to 7 days before arrival.
- **Seasons:** low (January, February, November) x0.85; mid x1.00; high (July, August, December) x1.30. Each night is priced according to its own season, so a stay spanning two seasons is handled correctly.
- **Discounts:** long stay (7 nights or more, -10 %), loyalty (Silver -5 %, Gold -10 %), early booking (60 days or more before arrival, -8 %). They can be combined up to a maximum of 25 % of the price.
- A guest's departure day can be the next guest's arrival day.

## 4. Architecture and main classes

```
src/main/java/com/junia/hotel/
├── Application.java              entry point: wires the dependencies and starts the menu
├── model/                        domain entities and value objects
├── service/                      business logic
│   └── remise/                   discount rules (strategies)
├── repository/                   entity storage
├── chargement/                   CSV file reading and import
├── exception/                    custom exceptions
├── ui/                           console interface
└── util/                         validation, rounding, formatting
src/main/resources/data/          data files loaded at startup
src/test/java/com/junia/hotel/    JUnit 5 unit tests
```

| Class | Responsibility |
|---|---|
| `Chambre` (abstract) | Room: common state, declares the pricing and the cancellation policy |
| `ChambreStandard`, `ChambreFamiliale`, `Suite` | Standard room, family room, suite: each implements its own nightly price and cancellation notice period |
| `Client`, `StatutFidelite` | Hotel guest and loyalty level, which determines the guest's discount |
| `Periode` (record) | Stay period, always valid; computes nights, overlap and intersection |
| `Reservation` | Reservation and its life cycle (confirmed, then possibly cancelled with fees) |
| `Saison` (enum) | Season: pricing coefficient based on the month |
| `Devis`, `LigneRemise`, `ResultatAnnulation` | Immutable results of business operations: quote, discount line, cancellation result |
| `CalculateurTarif` | Price calculator for a stay: nights, seasons, discounts, cap |
| `Remise` (interface) + 3 implementations | Interchangeable discount rules |
| `ReservationService` | Quotes, bookings, cancellations and all their checks |
| `DisponibiliteService` | Availability of a room and search for free rooms |
| `StatistiquesService` | Occupancy rate, revenue, guest ranking |
| `Repository<T, ID>` / `RepositoryMemoire` | Generic in-memory storage; `ReservationRepository` adds reservation-specific queries |
| `ChargeurCsv`, `LigneCsv`, `ParseurLigne<T>` | Generic CSV reading and typed conversion of columns |
| `ImportateurDonnees` | Data importer: loads the three files in order and produces an import report |
| `MenuPrincipal`, `SaisieConsole` | Console menu and validated user input |

## 5. Technical decisions

1. **`BigDecimal` for every amount.** A `double` cannot represent 0.10 exactly: discounts and rounding would produce wrong cents. Amounts are rounded to the cent in a single place (`Montants`), with the same rule everywhere.
2. **Unchecked business exceptions, checked format exception.** A violated business rule (room already taken, capacity exceeded...) cannot be fixed by the direct caller: it propagates up to the menu or the import, which catch all of them through the parent class `HotelException`. Conversely, `FormatDonneesException` is checked: the compiler forces the loader to handle it, so it can skip the faulty line and continue.
3. **Injected clock (`java.time.Clock`).** "Booking in the past", "cancelling less than 7 days before" or "booking 60 days in advance" all depend on today's date. Injecting the clock lets the tests run at a fixed date and stay reproducible.
4. **Discounts as a Strategy pattern with a global cap.** Each discount is an independent class returning an `Optional<LigneRemise>` (empty if the stay is not eligible). The calculator applies them without knowing them, then caps the total at 25 % to preserve profitability.
5. **Reservations imported through the business service.** Reservations from the file are not inserted directly: they go through `ReservationService.reserver()`, and therefore through the same checks as keyboard input. An invalid or conflicting line is reported in the startup report without blocking the application.

## 6. Object-oriented concepts

### Inheritance

`Chambre` is the parent class of `ChambreStandard`, `ChambreFamiliale` and `Suite`. In a hotel, these are all rooms (an "is a" relationship): they share a number, a floor, a capacity, a base price and their validations, but differ in their pricing and their cancellation policy.

### Polymorphism

- `CalculateurTarif` calls `chambre.prixNuit(nuit, nbPersonnes)` without knowing the actual type: the suite applies its weekend surcharge, the family room its per-person supplement.
- `ReservationService.calculerFraisAnnulation()` calls `chambre.delaiAnnulationGratuiteJours()`: 2, 3 or 7 days depending on the actual object.
- `CalculateurTarif` iterates over a `List<Remise>` and calls `calculer()` on each element, whether it is a long-stay, loyalty or early-booking discount.
- `ChargeurCsv.charger()` accepts any `ParseurLigne<T>` and any `SourceDonnees` (classpath, folder, or in-memory string in the tests).

### Abstract class and interfaces

- **`Chambre` is an abstract class** because rooms share state and code (attributes, validation, `equals`, `toString`) on top of a contract. Its abstract methods `prixNuit`, `delaiAnnulationGratuiteJours` and `getType` force each room type to define its own behaviour.
- **`Remise` is an interface** because discounts have nothing in common except the `calculer` contract: no shared state, and a new promotion can be added without any hierarchy.
- Other interfaces: `Repository<T, ID>` and `ReservationRepository` (storage), `SourceDonnees` (origin of the files), `ParseurLigne<T>` (conversion of one line), `Identifiable<ID>` (what a repository needs).

## 7. Custom exceptions

| Exception | Thrown when |
|---|---|
| `HotelException` (abstract) | Parent class of every business error, caught by the menu and the import |
| `ChambreIndisponibleException` | The room is already booked for part of the period |
| `CapaciteDepasseeException` | The number of guests exceeds the room's capacity |
| `ElementIntrouvableException` | Unknown guest, room or reservation |
| `ReservationInvalideException` | Reservation requested for a past date |
| `AnnulationImpossibleException` | Reservation already cancelled or stay already started |
| `FormatDonneesException` (checked) | Malformed line in a CSV file |

Standard exceptions are also used where they make sense: `IllegalArgumentException` for an invalid argument (inconsistent period, negative price...), `NullPointerException` via `Objects.requireNonNull`, `IOException` when a file is missing, and `NumberFormatException` / `DateTimeParseException`, which are caught and translated into clear messages.

## 8. Data loading

At startup, `ImportateurDonnees` reads three CSV files (`;` separator, UTF-8 encoding) in this order, because reservations refer to rooms and guests:

| File | Columns |
|---|---|
| `chambres.csv` (rooms) | `type;numero;etage;capacite;prix_base;option` (type, number, floor, capacity, base price; option = per-person supplement for FAMILIALE, weekend surcharge rate for SUITE) |
| `clients.csv` (guests) | `id;nom;email;statut` (id, name, email, status: STANDARD, ARGENT or OR, i.e. standard, silver or gold) |
| `reservations.csv` | `client;chambre;arrivee;depart;personnes` (guest, room, arrival, departure, number of guests; dates in YYYY-MM-DD format) |

- The first meaningful line is the header; empty lines and lines starting with `#` are ignored.
- Each line is converted by a dedicated `ParseurLigne`. An invalid line is skipped and reported with its number and the reason; the other lines are loaded.
- Duplicate identifiers are reported instead of overwriting the first occurrence.
- Reservations go through the business service (see decision 5). The provided file deliberately contains a conflicting reservation to demonstrate this mechanism: it appears in the report displayed at startup.
- If a file is missing, the application stops with an explicit message.

## 9. SOLID principles

- **S - Single responsibility.** Each class has one reason to change: `CalculateurTarif` price calculation, `DisponibiliteService` availability, `ChargeurCsv` file reading, `MenuPrincipal` display, `Application` only the wiring. There is no "do-everything" class.
- **O - Open/closed.** Adding a discount (for example "-15 % on weekdays") means creating a class that implements `Remise`, without modifying the calculator. Likewise, adding a menu option is a single line in `enregistrerOptions()`.
- **L - Liskov substitution.** Any subclass of `Chambre` can replace `Chambre`: each one respects the contract (price at least equal to the base price, positive notice period), and equality is defined once and for all (`final equals`) on the room number.
- **I - Interface segregation.** `Repository` only contains the common operations; searches by room or by guest live in `ReservationRepository`, which only the services that need them use. `Remise`, `SourceDonnees` and `ParseurLigne` each have a single method.
- **D - Dependency inversion.** Services receive abstractions through their constructor (`Repository`, `ReservationRepository`, `List<Remise>`, `Clock`, `SourceDonnees`) and never create their own dependencies. Only `Application` chooses the concrete implementations, which lets the tests inject a fixed clock or in-memory data.

## 10. Collections and Java features used

- **`List`** for ordered results, **`Map`** (`LinkedHashMap`) for access by identifier in the repositories, **`EnumMap`** for revenue per room type, **`EnumSet`/`Set`** for the months of a season and the weekend nights.
- **Streams and lambdas** for searches, totals and groupings (`groupingBy`, `reducing`, `flatMap(Optional::stream)`).
- **`Optional`** for lookups that may fail (`trouverParId`) and discounts that may not apply.
- **Enums with behaviour** (`Saison.de(date)`, `StatutFidelite` with its rate).
- **`LocalDate`**, `ChronoUnit`, `datesUntil` for all date logic.
- **Generics**: `Repository<T extends Identifiable<ID>, ID>`, `ParseurLigne<T>`, `ResultatChargement<T>`, `LigneCsv.enumeration(..., Class<E>)`.
- **Records** for immutable value objects (`Periode`, `Devis`, `DemandeReservation`...).

## 11. Tests

The 93 JUnit 5 tests cover the business logic, not the getters:

| Test class | What is verified |
|---|---|
| `PeriodeTest` | Night count, overlaps (including departure = arrival), intersection, invalid periods |
| `ChambreTest` | Price of each room type (supplement, weekend surcharge), capacity, polymorphic notice periods, validations |
| `ReservationTest` | Cancellation, amount collected, double cancellation, inconsistent fees |
| `CalculateurTarifTest` | Seasons, stay spanning two seasons, each discount at its exact threshold, capping, rounding |
| `ReservationServiceTest` | Normal booking, conflict, capacity, unknown items, past date, free / late / impossible cancellation |
| `DisponibiliteServiceTest` | Filtering by capacity and dates, sorting by price, cancelled reservations |
| `StatistiquesServiceTest` | Occupancy rate over a partial period, revenue including cancellation fees, ranking |
| `ChambreCsvParserTest`, `ChargeurCsvTest`, `ImportateurDonneesTest` | Parsing, invalid lines reported with their number, duplicates, conflicts at import, missing file, validity of the provided data |

Each feature is tested in all three situations: normal case, edge case (exactly 7 nights, exactly 60 days, arrival on the previous guest's departure day...) and invalid case (expected exception).

**Running the tests**

- IntelliJ: right-click `src/test/java`, then `Run 'All Tests'`.
- Command line:

```bash
mvn test
```
