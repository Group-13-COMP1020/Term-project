# VinRECIPE - Smart Recipe and Grocery Planner

**Group 13 | COMP1020 | VinUniversity**

## Team Members

| Name | Student ID | Role |
|---|---|---|
| Nguyen Trong Nhan | V202502098 | Team Leader, Frontend Controllers, GitHub Manager |
| Nguyen Thi Quynh Trang | V202502665 | Frontend UI & Styling (CSS/FXML Layouts) |
| Nguyen Tien Nhat Nguyen | V202502943 | Algorithms & Data Structures (Inverted Index, Sorting) |
| Dang Tuan Kiet | V202502041 | Relational Database & JDBC/DAO Layer |
| Do Viet Phuong | V202502638 | OOP Core Models, Encapsulation & Inheritance |

## Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17+ (tested with Temurin JDK 21) |
| Maven | No separate install required; use the included Maven wrapper |
| Database | SQLite local file (`vinrecipe.db`) |

## Setup & Running Instructions

### 1. Database Setup

VinRECIPE uses the local SQLite database file `vinrecipe.db`.

- No MySQL/PostgreSQL server installation is required.
- No `.env` credentials are required for normal use.
- No manual schema script execution is required.
- On first launch, the app verifies the schema and seeds demo data automatically.

### 2. Demo Accounts

| Username | Password | Role / Subclass | Room |
|---|---|---|---|
| `admin` | `admin123` | Admin | Global |
| `nhan` | `nhan123` | Room Leader | VinUni Dorm A-101 |
| `trang` | `trang123` | Normal Student | VinUni Dorm A-101 |
| `nguyen` | `nguyen123` | Normal Student | VinUni Dorm A-101 |
| `kiet` | `kiet123` | Normal Student | VinUni Dorm A-101 |
| `phuong` | `phuong123` | Normal Student | VinUni Dorm A-101 |

### 3. Build & Run

From the project root:

```bash
./mvnw compile
./mvnw javafx:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd compile
.\mvnw.cmd javafx:run
```

Windows users can also run `run-app.bat`.

## Project Architecture

```text
Model (OOP) -> DAO (JDBC) -> Service (Business Logic) -> View/Controller (JavaFX)
```

| Layer | Package | Responsibility |
|---|---|---|
| Model | `com.vinrecipe.model` | `User` base class and subclasses, `Recipe`, `Ingredient`, `Room`, `Tag`, `ShoppingList` |
| DAO | `com.vinrecipe.dao` | SQLite JDBC transaction layer mapping rows to OOP objects |
| Service | `com.vinrecipe.service` | Search indexing, aggregation, login, recipe workflows |
| Controller | `com.vinrecipe.controller` | JavaFX view switching and user-event handling |

## OOP Concepts Demonstrated

| Concept | Where |
|---|---|
| Inheritance | `Admin`, `NormalStudent`, and `RoomLeader` extend `User` |
| Abstraction | `User` is abstract and defines `getPermissionLevel()` |
| Polymorphism | Each subclass overrides `getPermissionLevel()` |
| Encapsulation | Core fields are private with public getters/setters |
| Composition | `Recipe` has `List<Ingredient>`; `Room` has members and a leader |

## Key Algorithms & Data Structures

### Inverted Index Search (`SearchService`)

```text
Input: userIngredients = ["chicken", "garlic", "rice"]
invertedIndex: HashMap<String, List<Recipe>>
matchCount:    HashMap<Recipe, Integer>
Sort: completion% DESC -> prepTime ASC
```

### Shopping List Aggregation (`ShoppingList`)

```text
Input:  List<Recipe> selectedRecipes
Method: HashMap.merge(name, quantity, Double::sum)
Output: Map<String, Double> aggregatedItems ("tofu" -> 2.0 blocks)
```

### Data Structures Used

| Structure | Where | Purpose |
|---|---|---|
| `ArrayList<Recipe>` | DAO, Service | Dynamic recipe lists |
| `ArrayList<Ingredient>` | Recipe model | Ordered ingredients per recipe |
| `HashSet<String>` | SearchService | Unique tag names for filters |
| `HashMap<String, Double>` | ShoppingList | Consolidating duplicate ingredients |
| `HashMap<String, List<Recipe>>` | SearchService | Ingredient-to-recipes inverted index |
| `HashMap<Recipe, Integer>` | SearchService | Match frequency tracking |

## Features

- Login and role-based flow for Admin, Room Leader, and Normal Student.
- Recipe browsing with details, images, ingredients, and tags.
- Recipe create/edit/delete flow.
- Smart recommendations from available ingredients using an inverted index.
- Sorting and filtering by rating, prep time, estimated price, and tags.
- Shopping list aggregation across selected recipes.
- Room management with member roster and announcements.
