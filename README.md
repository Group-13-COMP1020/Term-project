# VinRECIPE — Smart Recipe and Grocery Planner

**Group 13 | COMP1020 | VinUniversity**

## Team Members
| Name | Student ID | Role |
|---|---|---|
| Nguyen Trong Nhan | V202502098 | Team Leader, Frontend Controllers, GitHub Manager |
| Nguyen Thi Quynh Trang | V202502665 | Frontend UI & Styling (CSS/FXML Layouts) |
| Nguyen Tien Nhat Nguyen | V202502943 | Algorithms & Data Structures (Inverted Index, Sorting) |
| Dang Tuan Kiet | V202502041 | Relational Database & JDBC/DAO Layer |
| Do Viet Phuong | V202502638 | OOP Core Models, Encapsulation & Inheritance |

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 17+ (tested on Java 17-25) |
| Maven | 3.8+ |
| Database | SQLite (Self-contained, zero-configuration) |

---

## Setup & Running Instructions

### 1. Zero-Dependency Database Setup
The application is built on a highly portable local **SQLite** database (`vinrecipe.db`). 
* **No database server installation** (like MySQL or PostgreSQL) is required!
* **No manual schema script execution** is required.
* The system automatically initializes the schema and seeds 28 premium gourmet recipes (including diverse Asian, Western, and traditional Vietnamese dishes) and all user roles on the first launch.

**Pre-seeded Demo Accounts:**
| Username | Password | Role / Subclass | Room |
|---|---|---|---|
| `admin` | `admin123` | Admin | *Global* |
| `nhan` | `nhan123` | Room Leader | VinUni Dorm A-101 |
| `trang` | `trang123` | Normal Student | VinUni Dorm A-101 |
| `nguyen` | `nguyen123` | Normal Student | VinUni Dorm A-101 |
| `kiet` | `kiet123` | Normal Student | VinUni Dorm A-101 |
| `phuong` | `phuong123` | Normal Student | VinUni Dorm A-101 |

### 2. Auto-Configured JDBC Connection
Since the application connects locally to `vinrecipe.db`, the database configuration is fully encapsulated. You do not need to configure any username, password, or local host settings.

### 3. Build & Run
Simply open your terminal at the project root and execute:
```bash
mvn compile
mvn javafx:run
```
*(For Windows users, you can also simply double-click the `run-app.bat` script in the root directory to build and launch the application instantly!)*

---

## Project Architecture

```
Model (OOP) → DAO (JDBC) → Service (Business Logic) → View/Controller (JavaFX)
```

### Layer Overview

| Layer | Package | Responsibility |
|---|---|---|
| **Model** | `com.vinrecipe.model` | Abstract `User` base class and subclasses, `Recipe`, `Ingredient`, `Room`, `Tag`, `ShoppingList` |
| **DAO** | `com.vinrecipe.dao` | SQLite JDBC transaction layer mapping database rows to OOP objects |
| **Service** | `com.vinrecipe.service` | Encapsulates core business logic (Search index, Aggregation engines) |
| **Controller** | `com.vinrecipe.controller` | Swaps view contexts inside `MainLayout.fxml` and processes user events |

---

## OOP Concepts Demonstrated

| Concept | Where |
|---|---|
| **Inheritance** | `Admin`, `NormalStudent`, and `RoomLeader` extend the abstract `User` parent class |
| **Abstraction** | `User` declared as `public abstract class` with abstract method `getPermissionLevel()` |
| **Polymorphism** | `getPermissionLevel()` overridden by each subclass to return different privilege tiers (1, 2, 3) |
| **Encapsulation** | All core fields are private; input validation is managed inside public getters/setters |
| **Composition** | `Recipe` HAS-A `List<Ingredient>`, `Room` HAS-A `List<NormalStudent>` |

---

## Key Algorithms & Data Structures

### Inverted Index Search (`SearchService`)
```
Input: userIngredients = ["chicken", "garlic", "rice"]
invertedIndex: HashMap<String, List<Recipe>>
matchCount:    HashMap<Recipe, Integer>
Sort: completion% DESC → prepTime ASC
```

### Shopping List Aggregation (`ShoppingList`)
```
Input:  List<Recipe> selectedRecipes
Method: HashMap.merge(name, quantity, Double::sum)
Output: Map<String, Double> aggregatedItems ("tofu" → 2.0 blocks, "garlic" → 3.0 pcs)
```

### Data Structures Used
| Structure | Where | Purpose |
|---|---|---|
| `ArrayList<Recipe>` | DAO, Service | Dynamic lists of recipes |
| `ArrayList<Ingredient>` | Recipe model | Ordered ingredients per recipe |
| `HashSet<String>` | SearchService | Unique tag names for filter ComboBox |
| `HashMap<String, Double>` | ShoppingList | Consolidating duplicate ingredients mathematically |
| `HashMap<String, List<Recipe>>` | SearchService | Inverted index mapping ingredients to containing recipes |
| `HashMap<Recipe, Integer>` | SearchService | Track match frequencies during multi-ingredient queries |

---

## Features

* 🔐 **Login & Role-Based Flow** — Dedicated pathways and features depending on whether the user is an Admin, a Room Leader, or a Student.
* 📋 **Dynamic Sorting & Filtering** — Sort all recipes by Rating (DESC), Prep Time (ASC), or estimated Total Price (ASC) with a simple click.
* ✏️ **Complete Recipe CRUD** — Create new recipes with custom ingredients and tags, edit details, and delete custom-authored recipes.
* 🔍 **Smart Recommendations** — Match dishes instantly using your available ingredients via the high-speed Inverted Index algorithm.
* 🛒 **Mathematical Shopping Lists** — Select multiple recipes and let the aggregation engine compile a single consolidated grocery list with dynamic total cost estimates.
* 🏠 **Room Management** — Join your college roommates, view member rosters, and coordinate using the live Announcements Board.
