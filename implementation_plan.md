# VinRECIPE Implementation Plan

This document reflects the current JavaFX + SQLite implementation.

## 1. Architecture

```text
JavaFX Views/FXML + Controllers
        -> Service Layer
        -> DAO / JDBC Layer
        -> SQLite database file (vinrecipe.db)
```

Main rule: controllers handle UI events, services handle business logic, DAOs handle SQL/database access.

## 2. Project Structure

```text
src/main/java/com/vinrecipe/
  App.java
  controller/      JavaFX controllers
  dao/             SQLite JDBC access
  model/           OOP domain classes
  service/         Business logic and algorithms

src/main/resources/
  css/             Application styling
  fxml/layout/     Main application shell
  fxml/views/      Screens
  images/          Recipe and login assets
  db/schema.sql    Legacy/reference schema only; runtime schema is created in DatabaseConnection

pom.xml            Maven config: JavaFX, SQLite JDBC, FontAwesomeFX, JUnit
mvnw, mvnw.cmd     Maven wrapper
vinrecipe.db       Local SQLite database
```

## 3. Implemented Features

- Login with seeded demo accounts.
- Role-based behavior for Admin, Room Leader, and Normal Student.
- Dashboard and recipe browsing.
- Recipe detail view with ingredients, tags, images, and actions.
- Recipe create/edit/delete flow.
- Ingredient-based recipe recommendation using an inverted index.
- Sorting/filtering by rating, prep time, estimated price, and tag.
- Shopping list aggregation with `HashMap.merge`.
- Room view with members, announcements, and shared shopping-list context.
- Local SQLite schema initialization and seed data on startup.

## 4. Database

The app currently uses SQLite, not MySQL.

- JDBC URL: `jdbc:sqlite:vinrecipe.db`
- Driver dependency: `org.xerial:sqlite-jdbc`
- Runtime schema setup: `DatabaseConnection.initializeDatabase(...)`
- No DB server or `.env` credentials are required.

Core tables:

- `users`
- `rooms`
- `tags`
- `recipes`
- `ingredients`
- `recipe_tags`
- `shopping_lists`
- `shopping_list_recipes`
- `saved_recipes`
- `announcements`

## 5. OOP Concepts

| Concept | Implementation |
|---|---|
| Abstraction | `User` is an abstract base class |
| Inheritance | `Admin`, `NormalStudent`, `RoomLeader` extend `User` |
| Polymorphism | subclasses implement role-specific permission levels |
| Encapsulation | model fields are private with public methods |
| Composition | `Recipe` owns ingredients/tags; `Room` owns members/leader |

## 6. Data Structures and Algorithms

| Structure | Use |
|---|---|
| `ArrayList` | recipe, ingredient, room-member collections |
| `HashSet` | unique tags and selected shopping-list items |
| `HashMap<String, List<Recipe>>` | inverted index from ingredient to matching recipes |
| `HashMap<Recipe, Integer>` | match counts during recommendation |
| `HashMap<String, Double>` | shopping-list quantity aggregation |

Search ranking:

```text
completion = matchedIngredients / totalIngredients
sort by completion DESC, then prepTime ASC
```

Shopping-list aggregation:

```text
aggregated.merge(ingredientName, quantity, Double::sum)
```

## 7. Dependencies

Current `pom.xml` dependencies:

- `org.openjfx:javafx-controls`
- `org.openjfx:javafx-fxml`
- `org.xerial:sqlite-jdbc`
- `de.jensd:fontawesomefx-fontawesome`
- `org.junit.jupiter:junit-jupiter` for tests

No MySQL connector is required for the current SQLite implementation.

## 8. Demo Checklist

Use these accounts:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Admin |
| `nhan` | `nhan123` | Room Leader |
| `trang` | `trang123` | Normal Student |

Recommended demo flow:

1. Build with `./mvnw compile` or `.\mvnw.cmd compile`.
2. Run with `./mvnw javafx:run` or `.\mvnw.cmd javafx:run`.
3. Login as `nhan / nhan123`.
4. Open Dashboard and Recipes.
5. View a recipe detail page.
6. Use Search with available ingredients such as `egg`, `rice`, or `garlic`.
7. Generate a shopping list from selected recipes.
8. Open Room to show members and announcements.
9. Login as `admin / admin123` to demonstrate role difference.

## 9. Remaining Improvements

- Add automated unit tests for service-layer search and shopping-list aggregation.
- Replace demo plaintext passwords with hashes if security is evaluated.
- Decide whether to keep or remove the legacy `src/main/resources/db/schema.sql` reference file.
- Run a full manual GUI demo before submission.
