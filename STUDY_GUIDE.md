# VinRECIPE — Tài liệu ôn tập thuyết trình cuối kỳ

---

## MỤC LỤC

1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Kiến trúc hệ thống (Architecture)](#2-kiến-trúc-hệ-thống)
3. [OOP — 4 tính chất cốt lõi](#3-oop--4-tính-chất-cốt-lõi)
4. [Design Patterns được dùng](#4-design-patterns-được-dùng)
5. [Cấu trúc dữ liệu & Giải thuật](#5-cấu-trúc-dữ-liệu--giải-thuật)
6. [Database — SQLite & JDBC](#6-database--sqlite--jdbc)
7. [JavaFX — UI Framework](#7-javafx--ui-framework)
8. [Luồng chạy chính của ứng dụng](#8-luồng-chạy-chính)
9. [Câu hỏi thường gặp khi thuyết trình](#9-câu-hỏi-thường-gặp)

---

## 1. Tổng quan dự án

**VinRECIPE** là ứng dụng quản lý công thức nấu ăn dành cho sinh viên sống trong ký túc xá VinUni.

**Vấn đề giải quyết:** Sinh viên cùng phòng cần chia sẻ công thức, lên kế hoạch mua sắm chung, và tìm món ăn từ nguyên liệu sẵn có.

**Tech stack:**
- Ngôn ngữ: Java 17+
- UI: JavaFX + FXML + CSS
- Database: SQLite (file local `vinrecipe.db`)
- Build tool: Maven
- Dependencies: `sqlite-jdbc`, `fontawesomefx`, `junit-jupiter`

**Chạy app:**
```bash
.\mvnw.cmd javafx:run
```

---

## 2. Kiến trúc hệ thống

```
[User nhấn nút trên UI]
        ↓
   JavaFX Controller        ← xử lý sự kiện UI, không chứa logic
        ↓
   Service Layer            ← chứa business logic (tính toán, xử lý)
        ↓
   DAO Layer                ← chứa SQL queries, giao tiếp với DB
        ↓
   SQLite Database          ← lưu trữ dữ liệu
```

**Tại sao tách thành 3 tầng?**
- **Controller** chỉ biết cách hiển thị UI, không quan tâm DB.
- **Service** chứa logic nghiệp vụ — có thể test độc lập, không cần UI.
- **DAO** chỉ biết SQL — nếu đổi sang MySQL, chỉ cần sửa DAO, không động vào Service/Controller.

→ Đây là nguyên lý **Separation of Concerns** (phân tách trách nhiệm).

**Các package chính:**
```
com.vinrecipe/
  App.java               ← Entry point JavaFX
  model/                 ← Domain objects (User, Recipe, Ingredient...)
  controller/            ← JavaFX UI controllers
  service/               ← Business logic
  dao/                   ← Database Access Objects
```

---

## 3. OOP — 4 tính chất cốt lõi

### 3.1 Encapsulation (Đóng gói)

**Định nghĩa:** Che giấu dữ liệu bên trong, chỉ lộ qua getter/setter có kiểm soát.

**Ví dụ thực tế trong code — `User.java`:**
```java
// Field private — bên ngoài không truy cập trực tiếp
private String username;
private String password;

// Setter có validation — bảo vệ dữ liệu
public void setUsername(String username) {
    if (username == null || username.isBlank())
        throw new IllegalArgumentException("Username must not be blank");
    this.username = username.trim();
}

public void setPassword(String password) {
    if (password == null || password.length() < 4)
        throw new IllegalArgumentException("Password must be at least 4 characters");
    this.password = password;
}
```

**Tại sao quan trọng?** Nếu `username` là public, bất kỳ đâu trong code đều có thể gán `user.username = ""` → gây bug. Với setter, ta kiểm soát được.

**Ví dụ khác — `Recipe.java`:**
```java
public void setRating(double rating) {
    if (rating < 0.0 || rating > 5.0)
        throw new IllegalArgumentException("Rating must be between 0.0 and 5.0");
    this.rating = rating;
}
```

---

### 3.2 Inheritance (Kế thừa)

**Định nghĩa:** Lớp con nhận toàn bộ thuộc tính/phương thức của lớp cha, chỉ cần viết thêm phần riêng.

**Cây kế thừa trong project:**
```
User (abstract)
├── Admin            → permissionLevel = 3
├── RoomLeader       → permissionLevel = 2
└── NormalStudent    → permissionLevel = 1
```

**Code `User.java` (lớp cha abstract):**
```java
public abstract class User {
    private int userId;
    private String username;
    private String password;
    private String role;

    // Phương thức abstract — buộc lớp con phải implement
    public abstract int getPermissionLevel();

    // Phương thức chung — lớp con dùng lại
    public boolean login(String inputUsername, String inputPassword) {
        return this.username.equals(inputUsername) && this.password.equals(inputPassword);
    }
}
```

**Code `Admin.java` (lớp con):**
```java
public class Admin extends User {
    public Admin(int userId, String username, String password, String email) {
        super(userId, username, password, email, "ADMIN"); // gọi constructor lớp cha
    }

    @Override
    public int getPermissionLevel() { return 3; } // implement abstract method

    // Phương thức riêng chỉ Admin có
    public void manageUsers() { ... }
    public void deleteAnyRecipe(int recipeId) { ... }
}
```

**Code `RoomLeader.java`:**
```java
public class RoomLeader extends User {
    private int roomId; // thêm field riêng

    @Override
    public int getPermissionLevel() { return 2; }

    public void assignMembers(List<NormalStudent> members) { ... } // method riêng
}
```

**Code `NormalStudent.java`:**
```java
public class NormalStudent extends User {
    private int roomId;

    @Override
    public int getPermissionLevel() { return 1; }

    public ShoppingList viewSharedList() { ... } // method riêng
}
```

---

### 3.3 Polymorphism (Đa hình)

**Định nghĩa:** Cùng một lời gọi phương thức → hành vi khác nhau tuỳ đối tượng thực sự.

**Ví dụ trực tiếp:**
```java
User user = new Admin(...);
user.getPermissionLevel(); // trả về 3

user = new RoomLeader(...);
user.getPermissionLevel(); // trả về 2  ← cùng lời gọi, kết quả khác

user = new NormalStudent(...);
user.getPermissionLevel(); // trả về 1
```

**Ứng dụng thực tế trong `UserService.login()`:**
```java
public User login(String username, String password) {
    return userDAO.authenticate(username, password);
    // trả về Admin, RoomLeader, hoặc NormalStudent
    // nhưng code chỉ cần biết đó là "User"
}
```

**Khi controller nhận về:**
```java
User currentUser = userService.login("admin", "admin123");
if (currentUser.getPermissionLevel() >= 3) {
    // cho phép xoá bất kỳ recipe
}
```
→ Không cần `instanceof`, không cần if/else theo role.

**Polymorphism với `Comparator` (trong `SearchService`):**
```java
sorted.sort(Comparator.comparingDouble(Recipe::getRating).reversed());
// Method reference Recipe::getRating là polymorphism ở dạng functional
```

---

### 3.4 Abstraction (Trừu tượng hoá)

**Định nghĩa:** Ẩn chi tiết cài đặt, chỉ lộ giao diện cần dùng.

**Cách 1 — Abstract class `User`:**
```java
public abstract class User {
    public abstract int getPermissionLevel(); // "PHẢI implement" nhưng không biết HOW
}
```
Người dùng `User` chỉ cần gọi `getPermissionLevel()`, không biết mỗi subclass tính như thế nào.

**Cách 2 — Interface `ContextAware`:**
```java
// ContextAware.java
public interface ContextAware {
    void setContext(User currentUser, RecipeService recipeService,
                    SearchService searchService, MainController mainController);
}
```
`MainController` không cần biết controller con cụ thể là gì — chỉ cần biết nó `ContextAware`:
```java
Object controller = loader.getController();
if (controller instanceof ContextAware) {
    ((ContextAware) controller).setContext(currentUser, recipeService, searchService, this);
}
```

**Cách 3 — DAO layer là abstraction cho database:**
- Service gọi `recipeDAO.findAll()` — không biết SQL là gì.
- DAO viết SQL — không biết controller là gì.

---

### 3.5 Composition (Thành phần — quan trọng bổ sung)

**Định nghĩa:** "HAS-A" — đối tượng chứa đối tượng khác.

**Ví dụ — `Recipe` HAS-A list of `Ingredient`:**
```java
public class Recipe {
    private List<Ingredient> ingredients; // Composition
    private List<Tag> tags;               // Composition
    private User author;                  // Composition (recipe "thuộc về" user)
}
```

**Ví dụ — `Room` HAS-A `RoomLeader` và list `NormalStudent`:**
```java
public class Room {
    private RoomLeader leader;              // Composition
    private List<NormalStudent> members;    // Composition
}
```

**Tại sao dùng Composition thay Inheritance?**  
Nếu `Recipe extends Ingredient` → sai về ngữ nghĩa. Recipe "có" ingredient, không "là" ingredient.

---

## 4. Design Patterns được dùng

### 4.1 Singleton Pattern — `DatabaseConnection`

**Vấn đề:** Mở nhiều connection DB cùng lúc → tốn tài nguyên, xung đột.

**Giải pháp:** Chỉ tạo duy nhất 1 connection, mọi nơi dùng chung.

```java
public class DatabaseConnection {
    private static Connection instance = null;  // duy nhất 1 instance

    private DatabaseConnection() {}  // constructor private — không ai tạo được từ bên ngoài

    public static Connection getInstance() throws SQLException {
        if (instance == null || instance.isClosed()) {
            instance = DriverManager.getConnection("jdbc:sqlite:vinrecipe.db");
        }
        return instance;  // luôn trả về CÙNG 1 connection
    }
}
```

**Câu hỏi hay gặp:** "Singleton có thread-safe không?" — Trong project này chưa handle, nhưng JavaFX là single-thread UI, nên OK cho bài tập.

---

### 4.2 DAO Pattern (Data Access Object)

**Vấn đề:** SQL code lẫn lộn trong Controller → khó maintain, khó test.

**Giải pháp:** Tách SQL ra riêng, mỗi entity có 1 DAO.

```
RecipeDAO   → CRUD cho recipes table
UserDAO     → CRUD cho users table
IngredientDAO → CRUD cho ingredients table
TagDAO      → CRUD cho tags + recipe_tags
ShoppingListDAO → CRUD cho shopping_lists
```

**Ví dụ `RecipeDAO.insert()`:**
```java
public int insert(Recipe recipe) throws SQLException {
    String sql = "INSERT INTO recipes (title, description, ...) VALUES (?, ?, ...)";
    try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        stmt.setString(1, recipe.getTitle());
        // ...
        stmt.executeUpdate();
        ResultSet keys = stmt.getGeneratedKeys();
        return keys.getInt(1); // trả về ID vừa tạo
    }
}
```

**Lưu ý PreparedStatement (quan trọng!):**  
Dùng `?` placeholder thay vì nối string trực tiếp. Tránh được **SQL Injection** — bảo mật cơ bản nhất.

---

### 4.3 MVC Pattern (Model-View-Controller)

```
Model      = model/ package     (User, Recipe, Ingredient...)
View       = FXML files         (LoginView.fxml, RecipesView.fxml...)
Controller = controller/ package (LoginController, RecipesController...)
```

**JavaFX cụ thể:** FXML là View (XML mô tả UI), Controller là Java class xử lý event.

---

### 4.4 Service Layer Pattern

Controller không gọi DAO trực tiếp → gọi qua Service.

```
Controller → RecipeService → RecipeDAO → SQLite
```

**Ví dụ `RecipeService.createRecipe()`:**
```java
public int createRecipe(Recipe recipe) {
    int recipeId = recipeDAO.insert(recipe);          // insert recipe
    ingredientDAO.insertForRecipe(recipeId, ...);    // insert ingredients
    tagDAO.insert(tag); tagDAO.addTagToRecipe(...);  // insert & link tags
    return recipeId;
}
```
→ Service phối hợp nhiều DAO → đảm bảo tính nhất quán dữ liệu.

---

## 5. Cấu trúc dữ liệu & Giải thuật

### 5.1 Inverted Index — Thuật toán tìm kiếm theo nguyên liệu

**Bài toán:** User nhập danh sách nguyên liệu họ có → tìm các recipe phù hợp nhất.

**Giải pháp: Inverted Index** (giống index trong Google Search, Elasticsearch)

**Bước 1: Build Index — `SearchService.buildIndex()`**
```java
// HashMap<String, List<Recipe>> — key: tên nguyên liệu, value: list recipe chứa nó
private final Map<String, List<Recipe>> invertedIndex = new HashMap<>();

public void buildIndex(List<Recipe> recipes) {
    invertedIndex.clear();
    for (Recipe recipe : recipes) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            String key = ingredient.getName().toLowerCase().trim();
            invertedIndex
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(recipe);
        }
    }
}
```

**Ví dụ index được tạo ra:**
```
"egg"       → [AvocadoEggToast, FriedRice, MisoRamen, Gimbap]
"potato"    → [ChickenCurry, BeefSteak, FrenchFries]
"garlic"    → [BeefBroccoli, MapoTofu, GarlicButterShrimp, BeefSteak]
```

**Bước 2: Search — `SearchService.searchByIngredients()`**
```java
public List<Recipe> searchByIngredients(List<String> userIngredients) {
    Map<Recipe, Integer> matchCount = new HashMap<>(); // đếm số nguyên liệu match

    for (String userIng : userIngredients) {
        String key = userIng.toLowerCase().trim();
        List<Recipe> matches = invertedIndex.getOrDefault(key, Collections.emptyList());
        for (Recipe recipe : matches) {
            matchCount.merge(recipe, 1, Integer::sum); // +1 cho mỗi nguyên liệu khớp
        }
    }

    // Sắp xếp theo completion% DESC, rồi prepTime ASC
    List<Recipe> results = new ArrayList<>(matchCount.keySet());
    results.sort((a, b) -> {
        double completionA = (double) matchCount.get(a) / a.getIngredients().size();
        double completionB = (double) matchCount.get(b) / b.getIngredients().size();
        if (completionB != completionA)
            return Double.compare(completionB, completionA); // DESC
        return Integer.compare(a.getPrepTime(), b.getPrepTime()); // ASC nếu bằng nhau
    });

    return results;
}
```

**Ví dụ:** User có `["egg", "potato"]`
- FrenchFries: có potato → match 1/2 nguyên liệu → completion = 0.5
- AvocadoEggToast: có egg → match 1/4 nguyên liệu → completion = 0.25
- Kết quả: FrenchFries đứng trên

**Tại sao dùng Inverted Index mà không dùng SQL LIKE?**  
SQL `LIKE '%egg%'` phải scan toàn bộ DB mỗi lần. Inverted Index build 1 lần vào memory, mỗi lookup O(1).

---

### 5.2 HashMap.merge() — Shopping List Aggregation

**Bài toán:** User chọn nhiều recipe → tổng hợp nguyên liệu, cộng dồn số lượng trùng tên.

**Ví dụ:**
- ChickenCurry cần: 200g gạo
- FriedRice cần: 300g gạo
- Kết quả shopping list: 500g gạo

**Code `ShoppingList.generateList()`:**
```java
public Map<String, Double> generateList() {
    aggregatedItems.clear();
    for (Recipe recipe : selectedRecipes) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            String key = ingredient.getName();
            aggregatedItems.merge(key, ingredient.getQuantity(), Double::sum);
            // merge(key, value, remappingFn)
            // nếu key chưa có → đặt value
            // nếu key đã có  → áp dụng remappingFn(oldValue, newValue) = tổng cộng
        }
    }
    return aggregatedItems;
}
```

**`HashMap.merge()` là gì?**
```java
map.merge("rice", 200.0, Double::sum);  // map["rice"] = 200.0
map.merge("rice", 300.0, Double::sum);  // map["rice"] = 200.0 + 300.0 = 500.0
```
Tương đương:
```java
map.put(key, map.containsKey(key) ? map.get(key) + value : value);
```
Nhưng ngắn gọn và functional hơn.

---

### 5.3 Các cấu trúc dữ liệu khác

| Cấu trúc | Dùng ở đâu | Lý do chọn |
|---|---|---|
| `ArrayList<Recipe>` | Danh sách recipe, ingredient, member | Duy trì thứ tự, truy xuất theo index |
| `ArrayList<Ingredient>` | `Recipe.ingredients` | Composition, duy trì thứ tự thêm vào |
| `HashSet<String>` | `SearchService.getUniqueTagNames()` | Tự động loại duplicate, O(1) lookup |
| `HashMap<String, List<Recipe>>` | Inverted index | Lookup O(1) theo tên nguyên liệu |
| `HashMap<Recipe, Integer>` | matchCount trong search | Đếm match O(1) per recipe |
| `HashMap<String, Double>` | `ShoppingList.aggregatedItems` | Cộng dồn số lượng O(1) per ingredient |
| `Map<String, String>` | `ShoppingList.itemUnits` | Lưu đơn vị theo tên nguyên liệu |

---

### 5.4 Sorting — Comparator

**Sort theo rating giảm dần:**
```java
sorted.sort(Comparator.comparingDouble(Recipe::getRating).reversed());
```

**Sort theo prepTime tăng dần:**
```java
sorted.sort(Comparator.comparingInt(Recipe::getPrepTime));
```

**Sort theo giá tăng dần (dùng method `getTotalPrice()` của Recipe):**
```java
sorted.sort(Comparator.comparingDouble(Recipe::getTotalPrice));
// getTotalPrice() = sum của priceEstimate tất cả ingredients
```

**Multi-key sort (completion% DESC, rồi prepTime ASC):**
```java
results.sort((a, b) -> {
    double cA = (double) matchCount.get(a) / a.getIngredients().size();
    double cB = (double) matchCount.get(b) / b.getIngredients().size();
    if (cB != cA) return Double.compare(cB, cA);       // primary key
    return Integer.compare(a.getPrepTime(), b.getPrepTime()); // tie-breaker
});
```

---

## 6. Database — SQLite & JDBC

### 6.1 SQLite là gì?

- **Serverless**: Không cần cài MySQL server, không có process riêng.
- **File-based**: Toàn bộ DB là 1 file `vinrecipe.db`.
- **Embedded**: Chạy cùng process với app Java.
- Phù hợp cho: desktop app, mobile app, prototype, ứng dụng không cần nhiều user đồng thời.

### 6.2 Schema tổng quan

```
users ──────────────────────────── rooms
  user_id (PK)                       room_id (PK)
  username (UNIQUE)                  room_name
  password                           leader_id → users(user_id)
  role                               access_code
  room_id → rooms(room_id)

recipes ───────────────────────── ingredients
  recipe_id (PK)                     ingredient_id (PK)
  title                              recipe_id → recipes(recipe_id) CASCADE
  author_id → users(user_id)         name, quantity, unit, price_estimate
  prep_time, cook_time
  rating, servings

recipe_tags (junction table)      tags
  recipe_id → recipes              tag_id (PK)
  tag_id → tags                    name (UNIQUE)

saved_recipes                     shopping_lists
  user_id + recipe_id (PK)          list_id (PK)
                                    room_id / user_id

announcements
  room_id → rooms
  user_id → users
  message, created_at
```

### 6.3 JDBC — Cách kết nối Java với SQLite

```java
// Load driver
Class.forName("org.sqlite.JDBC");

// Tạo connection
Connection conn = DriverManager.getConnection("jdbc:sqlite:vinrecipe.db");

// Tắt PRAGMA foreign keys off khi cần seed (tránh constraint lúc insert)
stmt.execute("PRAGMA foreign_keys = ON;");
```

### 6.4 PreparedStatement vs Statement

```java
// BAD — SQL Injection vulnerability
String sql = "SELECT * FROM users WHERE username = '" + input + "'";
// Nếu input = "' OR '1'='1" → query trả về ALL users!

// GOOD — PreparedStatement
String sql = "SELECT * FROM users WHERE username = ?";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, input); // được escape tự động
```

### 6.5 Foreign Key & CASCADE

```sql
-- Khi xoá recipe, tự xoá ingredients liên quan
FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE

-- Khi xoá user, room_id của user đó set về NULL
FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE SET NULL
```

### 6.6 `INSERT OR IGNORE`

```java
// Khi save recipe — nếu đã tồn tại thì bỏ qua, không báo lỗi
"INSERT OR IGNORE INTO saved_recipes (user_id, recipe_id) VALUES (?, ?)"
```

### 6.7 Singleton trong Database

`DatabaseConnection` là Singleton để đảm bảo toàn app chỉ dùng 1 connection object duy nhất, tránh tạo nhiều connection tốn tài nguyên.

---

## 7. JavaFX — UI Framework

### 7.1 Cấu trúc JavaFX cơ bản

```
Stage (cửa sổ)
  └── Scene (nội dung)
        └── Parent (root node)
              └── Node tree (Button, Label, VBox, BorderPane...)
```

### 7.2 FXML + Controller

**FXML** là file XML mô tả layout UI. **Controller** là Java class xử lý event.

Kết nối bằng `@FXML`:
```java
// Controller
@FXML private TextField topSearchField; // bind với fx:id="topSearchField" trong FXML
@FXML private Button dashboardBtn;

@FXML
public void initialize() {
    // gọi tự động sau khi FXML load xong
    topSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
        // phản ứng mỗi khi user gõ
    });
}
```

### 7.3 `FXMLLoader` — load view động

```java
FXMLLoader loader = new FXMLLoader(url);
Node view = loader.load();
Object controller = loader.getController();

// Inject dependencies vào controller vừa tạo
if (controller instanceof ContextAware) {
    ((ContextAware) controller).setContext(currentUser, recipeService, searchService, this);
}
```

### 7.4 ContextAware Interface — Dependency Injection đơn giản

**Vấn đề:** Controller con cần biết `currentUser`, `RecipeService`, `SearchService` là ai.

**Giải pháp:** `MainController` inject vào controller con qua interface:
```java
// Interface
public interface ContextAware {
    void setContext(User currentUser, RecipeService recipeService,
                    SearchService searchService, MainController mainController);
}

// Controller con implement
public class RecipesController implements ContextAware {
    @Override
    public void setContext(User user, RecipeService rs, SearchService ss, MainController mc) {
        this.currentUser = user;
        this.recipeService = rs;
        this.searchService = ss;
        this.mainController = mc;
        loadRecipes(); // gọi sau khi đã có đủ dependencies
    }
}
```

### 7.5 Property Binding

```java
// Lắng nghe thay đổi text → search realtime
topSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
    activeRecipesController.setSearchQuery(newValue);
});
```

### 7.6 App Entry Point

```java
public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        Parent root = FXMLLoader.load(url); // load FXML
        Scene scene = new Scene(root, 950, 580);
        scene.getStylesheets().add("/css/style.css"); // CSS styling
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseConnection.close(); // đóng DB khi tắt app
    }

    public static void main(String[] args) {
        launch(args); // khởi động JavaFX
    }
}
```

---

## 8. Luồng chạy chính

### 8.1 Startup
```
main() → Application.launch() → App.start()
  → load LoginView.fxml
  → DatabaseConnection.getInstance() → initializeDatabase() → seed data
  → Hiển thị màn hình Login
```

### 8.2 Login
```
User nhập username/password → LoginController.handleLogin()
  → UserService.login(username, password)
  → UserDAO.authenticate() → SELECT + kiểm tra role
  → Tạo Admin/RoomLeader/NormalStudent object
  → Load MainLayout.fxml
  → MainController.setCurrentUser(user)
  → SearchService.buildIndex(allRecipes) ← build inverted index 1 lần
  → Hiển thị RecipesView
```

### 8.3 Search theo nguyên liệu
```
User nhập ingredients → SearchController
  → SearchService.searchByIngredients(userIngredients)
  → Loop: invertedIndex.get(ingredient) → matchCount.merge(recipe, 1, Integer::sum)
  → Sort: completion% DESC, prepTime ASC
  → Hiển thị kết quả
```

### 8.4 Tạo Shopping List
```
User chọn recipes → ShoppingListController
  → ShoppingListService.generate(selectedRecipes)
  → ShoppingList.addRecipe() × n
  → ShoppingList.generateList()
    → HashMap.merge(ingredientName, quantity, Double::sum) × tất cả ingredients
  → ShoppingListService.formatForDisplay()
  → Hiển thị ListView
```

### 8.5 Tạo Recipe mới
```
User điền form → RecipeFormController
  → Tạo Recipe object + Ingredient objects + Tag objects
  → RecipeService.createRecipe(recipe)
  → RecipeDAO.insert() → INSERT recipes
  → IngredientDAO.insertForRecipe() → INSERT ingredients (batch)
  → TagDAO.insert() + TagDAO.addTagToRecipe() → INSERT tags + recipe_tags
  → Rebuild search index: SearchService.buildIndex()
```

---

## 9. Câu hỏi thường gặp

### OOP

**Q: Tại sao User là abstract class thay vì interface?**  
A: Vì User có code chung (`login()`, `logout()`, getters/setters với validation). Interface chỉ khai báo method signature, không có implementation. Abstract class có thể có cả hai.

**Q: Sự khác nhau giữa abstract class và interface?**  
A: Abstract class có thể có fields, constructors, method với code. Interface (Java 8+) chỉ có abstract methods, default methods, static methods — không có fields (trừ constants). Một class chỉ extends 1 abstract class nhưng implements nhiều interface.

**Q: `ContextAware` là interface hay abstract class? Tại sao chọn interface?**  
A: Interface. Vì đây chỉ là "khả năng" — controller muốn được inject context. Controller đã kế thừa JavaFX Controller mechanism, không thể extends thêm 1 class nữa (Java single inheritance). Dùng interface là giải pháp.

**Q: Polymorphism ở đâu trong project?**  
A: `user.getPermissionLevel()` trả về 1/2/3 tuỳ object thực sự. `UserDAO.authenticate()` trả về `User` nhưng thực tế là Admin/RoomLeader/NormalStudent. Comparator lambdas trong sort.

**Q: Composition khác Inheritance thế nào?**  
A: Inheritance = "IS-A" (Admin IS-A User). Composition = "HAS-A" (Recipe HAS-A list of Ingredients). Nếu dùng Inheritance sai chỗ (Recipe extends Ingredient) → vô nghĩa về ngữ nghĩa.

---

### Thuật toán

**Q: Tại sao dùng Inverted Index thay vì query SQL?**  
A: SQL `LIKE '%egg%'` chạy O(n) scan mỗi query. Inverted Index build 1 lần O(n), sau đó mỗi lookup O(1). Cũng tính được completion% dễ hơn — SQL sẽ phải dùng nhiều JOIN phức tạp.

**Q: HashMap.merge() hoạt động như thế nào?**  
A: `merge(key, value, remappingFn)`. Nếu key chưa có: đặt value. Nếu đã có: gọi `remappingFn(oldValue, value)` và đặt kết quả. Với `Double::sum`: cộng dồn số lượng.

**Q: computeIfAbsent() khác put() thế nào?**  
A: `computeIfAbsent(key, k -> new ArrayList<>())` — chỉ tạo ArrayList mới khi key chưa có. `put()` luôn ghi đè. Dùng trong inverted index để thêm vào list hiện có thay vì tạo mới.

---

### Database

**Q: Tại sao dùng SQLite không dùng MySQL?**  
A: SQLite không cần server, không cần cấu hình, chạy offline. Phù hợp desktop app. MySQL cần server riêng, cần network.

**Q: PreparedStatement tránh SQL Injection như thế nào?**  
A: Placeholder `?` được driver xử lý riêng — user input được escape hoàn toàn, không bao giờ được interpret như SQL code.

**Q: Foreign Key CASCADE hoạt động thế nào?**  
A: `ON DELETE CASCADE` — khi record cha bị xoá, tất cả record con tham chiếu đến nó cũng tự xoá. Ví dụ: xoá recipe → tự xoá tất cả ingredients của recipe đó.

**Q: `INSERT OR IGNORE` là gì?**  
A: SQLite extension. Nếu insert vi phạm UNIQUE constraint (đã tồn tại) → bỏ qua, không throw exception. Dùng khi cần "insert nếu chưa có".

---

### Design

**Q: Tại sao cần Service layer? Controller gọi DAO trực tiếp không được à?**  
A: Được, nhưng: (1) Logic nghiệp vụ bị lẫn trong UI code, khó test. (2) Nếu nhiều controller cần cùng logic → copy/paste. Service layer centralise business logic, testable độc lập.

**Q: Singleton có vấn đề gì không?**  
A: Multi-threading: nếu 2 thread cùng gọi `getInstance()` lúc `instance == null`, có thể tạo 2 connection. JavaFX single-thread → OK. Ngoài ra Singleton làm code khó test (không mock được). Trade-off: đơn giản vs. testability.

**Q: MVC pattern trong JavaFX hoạt động thế nào?**  
A: FXML = View (khai báo UI), Controller Java class = Controller (xử lý event, gọi Model), model/ package = Model (dữ liệu + business objects). JavaFX binding giúp View tự cập nhật khi Model thay đổi.

---

### Demo

**Accounts:**
| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Admin — permission 3 |
| `nhan` | `nhan123` | Room Leader — permission 2 |
| `trang` | `trang123` | Normal Student — permission 1 |

**Demo flow khuyến nghị:**
1. Login `nhan` → Dashboard → duyệt recipe
2. Xem Recipe Detail → thấy ingredients, tags, actions
3. Search → nhập `egg, rice` → thấy ranking theo completion%
4. Chọn recipe → Generate Shopping List → thấy HashMap.merge hoạt động
5. My Room → members, announcements
6. Logout → Login `admin` → thấy quyền xoá bất kỳ recipe
7. Tạo recipe mới → điền form → submit → xuất hiện trong danh sách

---

*File này được tạo tự động từ source code. Luôn kiểm tra lại với code thực tế trước khi thuyết trình.*
