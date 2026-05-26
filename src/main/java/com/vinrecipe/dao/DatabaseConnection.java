package com.vinrecipe.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

/**
 * Singleton class managing the local SQLite database connection.
 * Bypasses MySQL server configuration completely by storing data locally in vinrecipe.db.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:vinrecipe.db";
    private static Connection instance = null;

    // Private constructor — prevents external instantiation
    private DatabaseConnection() {}

    /**
     * Returns the shared Connection, creating it if needed.
     * If the existing connection is closed/null, a new one is created.
     */
    public static Connection getInstance() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
                instance = DriverManager.getConnection(URL);
                System.out.println("[DB] Connected to local SQLite database (vinrecipe.db)");
                
                // Enable foreign keys in SQLite
                try (Statement stmt = instance.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
                
                // Initialize database structure and seed data
                initializeDatabase(instance);
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC Driver not found", e);
            }
        }
        return instance;
    }

    /**
     * Set up all necessary SQLite tables and insert seed data if empty.
     */
    private static void initializeDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Create rooms table (with access_code for room-join flow)
            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                         "room_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "room_name TEXT NOT NULL, " +
                         "leader_id INTEGER, " +
                         "access_code TEXT NOT NULL DEFAULT ''" +
                         ")");

            // Migration: add access_code column if it doesn't exist yet (for existing databases)
            boolean hasAccessCode = false;
            try (ResultSet cols = stmt.executeQuery("PRAGMA table_info(rooms)")) {
                while (cols.next()) {
                    if ("access_code".equals(cols.getString("name"))) {
                        hasAccessCode = true;
                        break;
                    }
                }
            }
            if (!hasAccessCode) {
                stmt.execute("ALTER TABLE rooms ADD COLUMN access_code TEXT NOT NULL DEFAULT ''");
                System.out.println("[DB Migration] Added access_code column to rooms table.");
            }


            // 2. Create users table (email is optional, only username is unique)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "username TEXT NOT NULL UNIQUE, " +
                         "password TEXT NOT NULL, " +
                         "email TEXT, " +
                         "role TEXT NOT NULL DEFAULT 'NORMAL_STUDENT', " +
                         "room_id INTEGER, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE SET NULL" +
                         ")");

            // 3. Create tags table
            stmt.execute("CREATE TABLE IF NOT EXISTS tags (" +
                         "tag_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "name TEXT NOT NULL UNIQUE" +
                         ")");

            // 4. Create recipes table
            stmt.execute("CREATE TABLE IF NOT EXISTS recipes (" +
                         "recipe_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "title TEXT NOT NULL, " +
                         "description TEXT, " +
                         "instructions TEXT, " +
                         "prep_time INTEGER DEFAULT 0, " +
                         "cook_time INTEGER DEFAULT 0, " +
                         "rating REAL DEFAULT 0.0, " +
                         "servings INTEGER DEFAULT 1, " +
                         "image_url TEXT, " +
                         "author_id INTEGER, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE SET NULL" +
                         ")");

            // 5. Create ingredients table
            stmt.execute("CREATE TABLE IF NOT EXISTS ingredients (" +
                         "ingredient_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "recipe_id INTEGER NOT NULL, " +
                         "name TEXT NOT NULL, " +
                         "quantity REAL DEFAULT 0, " +
                         "unit TEXT DEFAULT '', " +
                         "price_estimate REAL DEFAULT 0, " +
                         "FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE" +
                         ")");

            // 6. Create recipe_tags table
            stmt.execute("CREATE TABLE IF NOT EXISTS recipe_tags (" +
                         "recipe_id INTEGER NOT NULL, " +
                         "tag_id INTEGER NOT NULL, " +
                         "PRIMARY KEY (recipe_id, tag_id), " +
                         "FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE, " +
                         "FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE" +
                         ")");

            // 7. Create shopping_lists table
            stmt.execute("CREATE TABLE IF NOT EXISTS shopping_lists (" +
                         "list_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "room_id INTEGER DEFAULT NULL UNIQUE, " +
                         "user_id INTEGER DEFAULT NULL UNIQUE, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE, " +
                         "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                         ")");

            // 8. Create shopping_list_recipes table
            stmt.execute("CREATE TABLE IF NOT EXISTS shopping_list_recipes (" +
                         "list_id INTEGER NOT NULL, " +
                         "recipe_id INTEGER NOT NULL, " +
                         "PRIMARY KEY (list_id, recipe_id), " +
                         "FOREIGN KEY (list_id) REFERENCES shopping_lists(list_id) ON DELETE CASCADE, " +
                         "FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE" +
                         ")");

            // 9. Create saved_recipes table
            stmt.execute("CREATE TABLE IF NOT EXISTS saved_recipes (" +
                         "user_id INTEGER NOT NULL, " +
                         "recipe_id INTEGER NOT NULL, " +
                         "PRIMARY KEY (user_id, recipe_id), " +
                         "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                         "FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE" +
                         ")");

            // 10. Create announcements table
            stmt.execute("CREATE TABLE IF NOT EXISTS announcements (" +
                         "announcement_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "room_id INTEGER NOT NULL, " +
                         "user_id INTEGER NOT NULL, " +
                         "message TEXT NOT NULL, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE, " +
                         "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                         ")");

            // Seed announcements if empty
            boolean hasAnnouncements = false;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM announcements")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    hasAnnouncements = true;
                }
            } catch (SQLException e) {
                // Table might not exist yet, but create table executed above
            }
            if (!hasAnnouncements) {
                System.out.println("[DB Initialization] Seeding default announcements...");
                stmt.execute("INSERT INTO announcements (room_id, user_id, message) VALUES " +
                             "(1, 3, '🍳 Trang is cooking Tofu wih Tomato Sauce on Tuesday!'), " +
                             "(1, 4, '🧄 Nguyen: \"Who bought garlic? We have plenty in the pantry!\"'), " +
                             "(1, 2, '🧹 Leader Nhan: \"Remember to wipe down the hotpot after usage.\"');");
            }

            System.out.println("[DB Initialization] SQLite database structure verified.");

            // Check if seed data needs to be populated
            boolean isNewDatabase = true;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    isNewDatabase = false;
                }
            }

            if (isNewDatabase) {
                System.out.println("[DB Initialization] Populating SQLite database with seed data...");
                
                // Disable foreign keys temporarily during seeding to avoid insert order constraint issues
                stmt.execute("PRAGMA foreign_keys = OFF;");

                // Insert Rooms (with access_code for demo purposes)
                stmt.execute("INSERT INTO rooms (room_id, room_name, leader_id, access_code) VALUES (1, 'VinUni Dorm A-101', 2, 'DORM01');");

                // Insert Users
                stmt.execute("INSERT INTO users (user_id, username, password, email, role, room_id) VALUES " +
                             "(1, 'admin', 'admin123', 'admin@vinuni.edu.vn', 'ADMIN', NULL), " +
                             "(2, 'nhan', 'nhan123', 'nhan@vinuni.edu.vn', 'ROOM_LEADER', 1), " +
                             "(3, 'trang', 'trang123', 'trang@vinuni.edu.vn', 'NORMAL_STUDENT', 1), " +
                             "(4, 'nguyen', 'nguyen123', 'nguyen@vinuni.edu.vn', 'NORMAL_STUDENT', 1), " +
                             "(5, 'kiet', 'kiet123', 'kiet@vinuni.edu.vn', 'NORMAL_STUDENT', 1), " +
                             "(6, 'phuong', 'phuong123', 'phuong@vinuni.edu.vn', 'NORMAL_STUDENT', 1);");

                // Insert Tags
                stmt.execute("INSERT INTO tags (tag_id, name) VALUES " +
                             "(1, 'Breakfast'), (2, 'Lunch'), (3, 'Dinner'), (4, 'Vegetarian'), (5, 'Vegan'), " +
                             "(6, 'Quick'), (7, 'Healthy'), (8, 'Spicy'), (9, 'Asian'), (10, 'Western'), " +
                             "(11, 'Snacks'), (12, 'Desserts'), (13, 'Quick & Easy'), (14, 'Budget Friendly'), " +
                             "(15, 'Light'), (16, 'Home-cooked'), (17, 'Japanese'), (18, 'High Energy');");

                // Insert 8 Mockup Recipes
                stmt.execute("INSERT INTO recipes (recipe_id, title, description, instructions, prep_time, cook_time, rating, servings, image_url, author_id) VALUES " +
                             "(1, 'Avocado Egg Toast', 'Creamy mashed avocado topped with a perfectly fried egg.', '1. Toast your bread.\n2. Mash avocado with lemon juice, salt.\n3. Fry the egg.\n4. Assemble.', 5, 10, 4.8, 1, '/images/avocado_egg_toast.png', 2), " +
                             "(2, 'Fried Tofu wih Tomato Sauce', 'Crispy fried tofu cubes simmered in a rich sweet tomato glaze.', '1. Fry tofu cubes.\n2. Cook garlic and tomatoes.\n3. Combine and season.', 5, 10, 4.5, 2, '/images/fried_tofu_tomato.png', 3), " +
                             "(3, 'Pumpkin Soup', 'Vibrant creamy pumpkin soup drizzled with fresh cream.', '1. Boil pumpkin and broth.\n2. Puree until smooth.\n3. Top with seeds and cream.', 10, 10, 4.6, 2, '/images/pumpkin_soup.png', 4), " +
                             "(4, 'Chicken Curry Rice', 'Tender chicken curry with potatoes over white rice.', '1. Sauté chicken and curry.\n2. Add veggies and coconut milk.\n3. Simmer and serve over rice.', 10, 15, 4.7, 2, '/images/chicken_curry_rice.png', 2), " +
                             "(5, 'Fried rice with beef and pickles', 'Vietnamese Fried Rice with beef and sour pickles.', '1. Stir-fry beef and pickles.\n2. Stir-fry rice with egg.\n3. Combine and toss on high heat.', 15, 15, 4.8, 1, '/images/fried_rice_pickles.png', 3), " +
                             "(6, 'Sweet and sour ribs', 'Pork ribs coated in a glossy, tangy glaze.', '1. Fry ribs golden.\n2. Pour glaze over ribs.\n3. Simmer until sticky.', 10, 30, 4.9, 2, '/images/sweet_sour_ribs.png', 5), " +
                             "(7, 'Gimbap', 'Korean rice rolls with seasoned beef and vegetables.', '1. Season rice with sesame oil.\n2. Spread on seaweed sheet.\n3. Fill with beef and veggies, roll and slice.', 20, 25, 4.6, 3, '/images/gimbap.png', 2), " +
                             "(8, 'French Fries', 'Golden crispy double-fried potato fries.', '1. Cut potatoes into sticks.\n2. Fry at 150C, then double-fry at 180C.\n3. Salt and serve.', 10, 15, 4.4, 2, '/images/french_fries.png', 6);");

                // Insert Ingredients
                stmt.execute("INSERT INTO ingredients (recipe_id, name, quantity, unit, price_estimate) VALUES " +
                             "(1, 'bread slice', 1, 'pcs', 3000), " +
                             "(1, 'avocado', 1, 'pcs', 15000), " +
                             "(1, 'egg', 1, 'pcs', 3000), " +
                             "(1, 'salt', 1, 'pinch', 200), " +
                             "(2, 'tofu block', 1, 'pcs', 5000), " +
                             "(2, 'tomatoes', 2, 'pcs', 6000), " +
                             "(2, 'scallions', 2, 'stalk', 1000), " +
                             "(3, 'pumpkin', 500, 'g', 15000), " +
                             "(3, 'broth', 400, 'ml', 5000), " +
                             "(3, 'heavy cream', 30, 'ml', 6000), " +
                             "(4, 'chicken breast', 300, 'g', 25000), " +
                             "(4, 'curry powder', 15, 'g', 4000), " +
                             "(4, 'carrot', 1, 'pcs', 3000), " +
                             "(4, 'potato', 1, 'pcs', 3000), " +
                             "(4, 'coconut milk', 200, 'ml', 12000), " +
                             "(4, 'white rice', 200, 'g', 4000), " +
                             "(5, 'cooked rice', 300, 'g', 5000), " +
                             "(5, 'beef slices', 150, 'g', 30000), " +
                             "(5, 'mustard pickles', 100, 'g', 5000), " +
                             "(5, 'egg', 1, 'pcs', 3000), " +
                             "(6, 'pork ribs', 500, 'g', 65000), " +
                             "(6, 'sugar', 30, 'g', 1000), " +
                             "(6, 'vinegar', 20, 'ml', 1000), " +
                             "(7, 'seaweed sheets', 3, 'sheets', 10000), " +
                             "(7, 'cooked rice', 400, 'g', 6000), " +
                             "(7, 'seasoned beef', 150, 'g', 30000), " +
                             "(7, 'carrot', 1, 'pcs', 3000), " +
                             "(7, 'spinach', 100, 'g', 4000), " +
                             "(7, 'egg', 2, 'pcs', 6000), " +
                             "(8, 'potato', 500, 'g', 15000), " +
                             "(8, 'salt', 5, 'g', 500), " +
                             "(8, 'oil', 500, 'ml', 25000);");

                // Link Recipe Tags
                stmt.execute("INSERT INTO recipe_tags (recipe_id, tag_id) VALUES " +
                             "(1, 1), (1, 13), " + // Avocado Egg Toast: Breakfast, Quick & Easy
                             "(2, 5), (2, 14), " + // Fried Tofu: Vegan, Budget Friendly
                             "(3, 15), (3, 16), " + // Pumpkin Soup: Light, Home-cooked
                             "(4, 17), (4, 18), " + // Chicken Curry: Japanese, High Energy
                             "(5, 1), (5, 18), " + // Fried rice: Breakfast, High Energy
                             "(6, 3), (6, 16), " + // Sweet and sour ribs: Dinner, Home-cooked
                             "(7, 2), (7, 15), " + // Gimbap: Lunch, Light
                             "(8, 2), (8, 14);");  // French Fries: Lunch, Budget Friendly

                // Re-enable foreign keys after seeding
                stmt.execute("PRAGMA foreign_keys = ON;");
                System.out.println("[DB Initialization] Seeding complete! Database ready.");
            }

            addMoreRecipesIfMissing(conn);
        } catch (SQLException e) {
            System.err.println("[DB Initialization] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Programmatically insert additional beautiful recipes if they are missing from the SQLite database.
     */
    /**
     * Programmatically insert additional beautiful recipes if they are missing from the SQLite database.
     */
    private static void addMoreRecipesIfMissing(Connection conn) {
        // 1. Existing recipes
        insertRecipeIfMissing(conn, 
            "Egg Salad Sandwich", 
            "Classic creamy egg salad spread between toasted bread slices.", 
            "1. Boil eggs.\n2. Peel and mash eggs with mayonnaise, mustard, salt.\n3. Spread mixture over toasted bread slices.\n4. Close sandwich and cut in half.", 
            5, 5, 4.7, 1, "/images/egg_salad_sandwich.png", 2,
            new String[][] {{"egg", "2", "pcs", "6000"}, {"bread slice", "2", "pcs", "6000"}, {"mayonnaise", "15", "g", "2000"}, {"mustard", "5", "g", "1000"}},
            new int[] {1, 13} // Breakfast, Quick & Easy
        );

        insertRecipeIfMissing(conn, 
            "Beef and Broccoli", 
            "Tender stir-fried beef and fresh broccoli florets in a savory soy-garlic sauce.", 
            "1. Stir-fry sliced beef in a hot pan until browned, then set aside.\n2. Sauté minced garlic, then add broccoli florets and a splash of water to steam.\n3. Return beef to the pan.\n4. Pour in soy sauce, sugar, and cornstarch slurry. Stir until sauce thickens.", 
            10, 10, 4.8, 2, "/images/beef_broccoli.png", 3,
            new String[][] {{"beef slices", "200", "g", "40000"}, {"broccoli", "150", "g", "10000"}, {"soy sauce", "20", "ml", "2000"}, {"garlic", "1", "pcs", "500"}},
            new int[] {2, 3} // Lunch, Dinner
        );

        // 2. 15 New Diverse Recipes (Asian & Western)
        insertRecipeIfMissing(conn, 
            "Salmon Teriyaki", 
            "Pan-seared salmon fillet glazed in a rich, glossy homemade sweet teriyaki sauce.", 
            "1. Season salmon with salt.\n2. Sear salmon in a hot pan skin-side down until crispy, then flip.\n3. Pour in teriyaki sauce and let it bubble and thicken.\n4. Garnish with green onions and sesame seeds.", 
            5, 10, 4.8, 1, "/images/salmon_teriyaki.png", 2,
            new String[][] {{"salmon fillet", "200", "g", "50000"}, {"teriyaki sauce", "30", "ml", "5000"}, {"sesame seeds", "5", "g", "500"}, {"green onions", "10", "g", "500"}},
            new int[] {9, 3, 6, 17} // Asian, Dinner, Quick, Japanese
        );

        insertRecipeIfMissing(conn, 
            "Beef Pho (Phở Bò)", 
            "Traditional Vietnamese noodle soup with tender beef slices, fresh herbs, and aromatic broth.", 
            "1. Boil the pho aromatic broth with spices.\n2. Blanch the flat rice noodles and place in a bowl.\n3. Arrange thin raw beef slices on top.\n4. Pour hot boiling broth over the beef to cook it instantly.\n5. Serve with fresh basil, cilantro, and lime.", 
            15, 20, 4.9, 1, "/images/beef_pho.png", 3,
            new String[][] {{"flat rice noodles", "200", "g", "4000"}, {"beef slices", "150", "g", "30000"}, {"pho spices broth", "500", "ml", "15000"}, {"fresh herbs", "30", "g", "2000"}},
            new int[] {9, 16, 1, 2} // Asian, Home-cooked, Breakfast, Lunch
        );

        insertRecipeIfMissing(conn, 
            "Spaghetti Carbonara", 
            "Classic Italian pasta with creamy egg-and-cheese sauce, crispy bacon, and cracked black pepper.", 
            "1. Cook spaghetti in salted water.\n2. Fry bacon in a pan until crispy.\n3. Whisk eggs and grated parmesan cheese together.\n4. Toss hot pasta with bacon oil, remove from heat, and quickly stir in egg-cheese mixture to create a creamy sauce.", 
            5, 15, 4.7, 1, "/images/spaghetti_carbonara.png", 4,
            new String[][] {{"spaghetti", "150", "g", "5000"}, {"eggs", "2", "pcs", "6000"}, {"bacon", "50", "g", "15000"}, {"parmesan cheese", "30", "g", "12000"}},
            new int[] {10, 3, 16} // Western, Dinner, Home-cooked
        );

        insertRecipeIfMissing(conn, 
            "Pad Thai", 
            "Stir-fried Thai rice noodles with shrimp, tofu, eggs, and bean sprouts in a tangy tamarind sauce.", 
            "1. Soak rice noodles in warm water.\n2. Sauté shrimp and diced tofu in a hot wok.\n3. Push ingredients to the side, scramble eggs on the other side.\n4. Add noodles, tamarind sauce, and bean sprouts. Toss on high heat.", 
            10, 10, 4.6, 1, "/images/pad_thai.png", 2,
            new String[][] {{"rice noodles", "150", "g", "4000"}, {"shrimp", "100", "g", "25000"}, {"tofu block", "0.5", "pcs", "2500"}, {"bean sprouts", "50", "g", "1000"}},
            new int[] {9, 6, 2} // Asian, Quick, Lunch
        );

        insertRecipeIfMissing(conn, 
            "Classic Caesar Salad", 
            "Crisp romaine lettuce tossed in creamy Caesar dressing, topped with garlic croutons and shaved parmesan.", 
            "1. Chop fresh romaine lettuce into bite-size pieces.\n2. In a large bowl, toss lettuce with Caesar dressing.\n3. Garnish with garlic croutons and shaved parmesan cheese.\n4. Serve cold as a starter or light meal.", 
            10, 0, 4.5, 1, "/images/caesar_salad.png", 5,
            new String[][] {{"romaine lettuce", "150", "g", "8000"}, {"croutons", "30", "g", "3000"}, {"parmesan cheese", "20", "g", "8000"}, {"Caesar dressing", "30", "ml", "5000"}},
            new int[] {10, 15, 7, 4} // Western, Light, Healthy, Vegetarian
        );

        insertRecipeIfMissing(conn, 
            "Miso Ramen", 
            "Savory Japanese ramen noodles in a rich miso-infused broth, topped with a soft-boiled egg and scallions.", 
            "1. Boil broth and whisk in Japanese miso paste.\n2. Cook ramen noodles separately and place in a bowl.\n3. Pour hot miso broth over the noodles.\n4. Top with sweet corn, scallions, and a soft-boiled egg cut in half.", 
            5, 10, 4.7, 1, "/images/miso_ramen.png", 2,
            new String[][] {{"ramen noodles", "150", "g", "5000"}, {"miso broth", "400", "ml", "10000"}, {"egg", "1", "pcs", "3000"}, {"sweet corn", "30", "g", "2000"}},
            new int[] {9, 17, 13} // Asian, Japanese, Quick & Easy
        );

        insertRecipeIfMissing(conn, 
            "Margherita Pizza", 
            "Simple Italian pizza with tomato sauce, fresh mozzarella cheese, and fragrant sweet basil leaves.", 
            "1. Roll out pizza dough into a circle.\n2. Spread tomato sauce evenly over the dough.\n3. Top with sliced fresh mozzarella cheese.\n4. Bake at 220C until crust is golden and cheese is bubbly.\n5. Garnish with fresh basil leaves.", 
            10, 15, 4.8, 2, "/images/margherita_pizza.png", 3,
            new String[][] {{"pizza dough", "1", "pcs", "10000"}, {"tomato sauce", "50", "g", "3000"}, {"mozzarella cheese", "100", "g", "20000"}, {"basil leaves", "5", "g", "1000"}},
            new int[] {10, 16, 4} // Western, Home-cooked, Vegetarian
        );

        insertRecipeIfMissing(conn, 
            "Mapo Tofu", 
            "Sichuan style tofu cubes simmered in a spicy, numbing chili-bean sauce with minced pork.", 
            "1. Dice tofu into cubes and blanch in salted water.\n2. Fry minced pork with garlic and ginger.\n3. Add Gochujang/chili bean paste and chicken stock, bring to simmer.\n4. Add tofu and thicken with cornstarch slurry.\n5. Sprinkle with Sichuan peppercorns.", 
            5, 10, 4.6, 2, "/images/mapo_tofu.png", 4,
            new String[][] {{"tofu block", "1", "pcs", "5000"}, {"minced pork", "100", "g", "12000"}, {"chili bean paste", "15", "g", "3000"}, {"garlic", "1", "pcs", "500"}},
            new int[] {9, 8, 3, 16} // Asian, Spicy, Dinner, Home-cooked
        );

        insertRecipeIfMissing(conn, 
            "Beef Steak with Fries", 
            "Perfectly seared juicy beef steak served with a side of golden crispy French fries.", 
            "1. Season beef steak generously with salt and pepper.\n2. Sear in a screaming hot pan with butter, garlic, and rosemary.\n3. Cook to medium-rare, then rest for 5 minutes.\n4. Serve alongside crispy double-fried French fries.", 
            10, 15, 4.9, 1, "/images/beef_steak_fries.png", 5,
            new String[][] {{"beef steak", "200", "g", "60000"}, {"butter", "20", "g", "4000"}, {"garlic", "1", "pcs", "500"}, {"potato", "200", "g", "6000"}},
            new int[] {10, 18, 3} // Western, High Energy, Dinner
        );

        insertRecipeIfMissing(conn, 
            "Chicken Quesadilla", 
            "Warm toasted tortillas stuffed with spiced shredded chicken and melted cheddar cheese.", 
            "1. Place a tortilla in a dry skillet.\n2. Spread shredded spiced chicken and cheese over one half.\n3. Fold tortilla in half over the filling.\n4. Cook until crispy on both sides and cheese is perfectly melted.", 
            5, 10, 4.5, 1, "/images/chicken_quesadilla.png", 2,
            new String[][] {{"tortilla", "2", "pcs", "8000"}, {"shredded chicken", "100", "g", "10000"}, {"cheddar cheese", "50", "g", "10000"}, {"bell pepper", "30", "g", "2000"}},
            new int[] {10, 6, 11} // Western, Quick, Snacks
        );

        insertRecipeIfMissing(conn, 
            "Korean Fried Chicken", 
            "Extra crispy double-fried chicken glazed in a sweet, sticky, and spicy Gochujang sauce.", 
            "1. Coat chicken wings in cornstarch.\n2. Deep fry at 160C, drain, then fry again at 180C for extra crispiness.\n3. Boil Gochujang paste, honey, soy sauce, and garlic in a wok.\n4. Toss chicken wings in glaze, top with sesame seeds.", 
            10, 20, 4.8, 2, "/images/korean_fried_chicken.png", 3,
            new String[][] {{"chicken wings", "300", "g", "25000"}, {"cornstarch", "50", "g", "2000"}, {"Gochujang glaze", "40", "ml", "6000"}, {"sesame seeds", "5", "g", "500"}},
            new int[] {9, 8, 18, 11} // Asian, Spicy, High Energy, Snacks
        );

        insertRecipeIfMissing(conn, 
            "Greek Salad", 
            "Refreshing salad with crisp cucumbers, ripe tomatoes, red onions, and creamy feta cheese.", 
            "1. Chop cucumbers, tomatoes, and red onions.\n2. Mix in a bowl with Kalamata black olives.\n3. Drizzle with olive oil and vinegar, sprinkle oregano.\n4. Top with a whole block of creamy Greek feta cheese.", 
            10, 0, 4.6, 2, "/images/greek_salad.png", 6,
            new String[][] {{"cucumber", "1", "pcs", "3000"}, {"tomatoes", "2", "pcs", "6000"}, {"feta cheese", "50", "g", "15000"}, {"black olives", "20", "g", "5000"}},
            new int[] {10, 15, 7, 4} // Western, Light, Healthy, Vegetarian
        );

        insertRecipeIfMissing(conn, 
            "Mango Sticky Rice", 
            "Sweet glutinous rice cooked in rich coconut milk, served with fresh ripe mango slices.", 
            "1. Steam glutinous rice.\n2. Heat coconut milk with sugar and salt, then stir into the hot rice.\n3. Let sticky rice rest to absorb coconut glaze.\n4. Serve topped with fresh sweet ripe mango slices.", 
            10, 25, 4.7, 2, "/images/mango_sticky_rice.png", 2,
            new String[][] {{"glutinous rice", "150", "g", "4000"}, {"coconut milk", "100", "ml", "6000"}, {"mango", "1", "pcs", "15000"}, {"sugar", "20", "g", "500"}},
            new int[] {9, 12, 15} // Asian, Desserts, Light
        );

        insertRecipeIfMissing(conn, 
            "Garlic Butter Shrimp", 
            "Plump tender shrimp sautéed in a rich garlic butter sauce, finished with fresh parsley.", 
            "1. Heat butter in a skillet.\n2. Add lots of minced garlic and sauté until fragrant.\n3. Add shrimp and cook until pink and curled on both sides.\n4. Toss in fresh chopped parsley and lemon juice.", 
            5, 5, 4.8, 1, "/images/garlic_butter_shrimp.png", 5,
            new String[][] {{"shrimp", "200", "g", "50000"}, {"butter", "30", "g", "6000"}, {"garlic", "2", "pcs", "1000"}, {"parsley", "5", "g", "500"}},
            new int[] {10, 13, 16} // Western, Quick & Easy, Home-cooked
        );

        insertRecipeIfMissing(conn, 
            "Matcha Latte Pudding", 
            "Silky smooth cold eggless pudding infused with premium Japanese Uji matcha green tea.", 
            "1. Dissolve gelatin in warm milk.\n2. Whisk in premium matcha powder and sugar until smooth.\n3. Pour mixture into cups.\n4. Refrigerate for 4 hours until completely set. Serve chilled.", 
            5, 5, 4.6, 2, "/images/matcha_pudding.png", 4,
            new String[][] {{"milk", "200", "ml", "7000"}, {"gelatin", "10", "g", "3000"}, {"matcha powder", "5", "g", "5000"}, {"sugar", "15", "g", "500"}},
            new int[] {12, 15, 4} // Desserts, Light, Vegetarian
        );

        insertRecipeIfMissing(conn, 
            "Bún Đậu Mắm Tôm", 
            "Traditional Vietnamese platter of rice vermicelli noodles, crispy fried tofu, boiled pork, herbs, served with pungent fermented shrimp paste dipping sauce.", 
            "1. Slice tofu blocks into small cubes and deep-fry until golden-brown and crispy.\n2. Boil the pork leg meat in water with a pinch of salt until tender, then slice thin.\n3. Arrange blocks of rice vermicelli (bún), fried tofu, sliced pork, cucumber, and fresh herbs (coriander, fish mint) on a tray.\n4. Mix fermented shrimp paste (mắm tôm) with sugar, lime juice, a splash of hot oil, and sliced chili. Whisk until frothy.\n5. Dip vermicelli and ingredients into the shrimp paste sauce and enjoy.", 
            15, 15, 4.9, 2, "/images/bun_dau_mam_tom.png", 2,
            new String[][] {{"rice vermicelli", "300", "g", "6000"}, {"tofu block", "2", "pcs", "10000"}, {"boiled pork leg", "200", "g", "35000"}, {"fermented shrimp paste", "50", "ml", "5000"}, {"fresh herbs", "50", "g", "3000"}},
            new int[] {9, 16, 2, 3} // Asian, Home-cooked, Lunch, Dinner
        );
        
        insertRecipeIfMissing(conn, 
            "Cơm Tấm (Broken Rice)", 
            "Iconic Southern Vietnamese broken rice dish topped with a fragrant grilled pork chop, egg meatloaf, and sweet fish sauce.", 
            "1. Cook the broken rice (cơm tấm) in a rice cooker.\n2. Marinate pork chops with lemongrass, garlic, shallots, honey, and soy sauce, then grill until smoky.\n3. Prepare the steamed egg meatloaf (chả trứng) by mixing eggs, minced pork, glass noodles, and wood ear mushrooms, then steam.\n4. Serve broken rice on a plate, top with grilled pork chop, a slice of egg meatloaf, cucumber, and green onion oil.\n5. Drizzle sweet chili fish sauce over the dish before eating.", 
            15, 15, 4.8, 1, "/images/com_tam.png", 3,
            new String[][] {{"broken rice", "200", "g", "4000"}, {"grilled pork chop", "150", "g", "30000"}, {"egg meatloaf", "1", "slab", "8000"}, {"sweet fish sauce", "30", "ml", "2000"}},
            new int[] {9, 3, 16, 2} // Asian, Dinner, Home-cooked, Lunch
        );

        insertRecipeIfMissing(conn, 
            "Bánh Xèo (Sizzling Crepe)", 
            "Crispy savory rice flour crepe stuffed with shrimp, pork belly, bean sprouts, served wrapped in fresh lettuce and herbs.", 
            "1. Mix rice flour, turmeric powder, and coconut milk to create a smooth batter.\n2. Sauté a few pieces of shrimp and pork belly in a hot oiled skillet.\n3. Pour a thin ladle of crepe batter into the skillet, swirling it quickly so it sizzles (xèo).\n4. Toss bean sprouts on one half, cover with a lid for 2 minutes.\n5. Fold the crepe in half and cook until extremely crispy.\n6. Wrap crispy crepe pieces in lettuce leaves with fresh herbs, and dip in sweet chili fish sauce.", 
            15, 15, 4.7, 2, "/images/banh_xeo.png", 2,
            new String[][] {{"rice flour mix", "200", "g", "10000"}, {"shrimp", "100", "g", "25000"}, {"pork belly", "100", "g", "15000"}, {"bean sprouts", "100", "g", "2000"}, {"lettuce & herbs", "100", "g", "5000"}},
            new int[] {9, 16, 3} // Asian, Home-cooked, Dinner
        );
    }

    /**
     * Seeder helper method to securely add a missing recipe with its ingredients and tags.
     */
    private static void insertRecipeIfMissing(Connection conn, String title, String desc, String inst, 
                                            int prep, int cook, double rating, int servings, String img, int authorId,
                                            String[][] ingredients, int[] tagIds) {
        try {
            // Check if already exists
            String checkSql = "SELECT COUNT(*) FROM recipes WHERE title = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, title);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return; // already exists
                    }
                }
            }
            
            System.out.println("[DB Seeding] Seeding missing recipe: " + title + "...");
            String sql = "INSERT INTO recipes (title, description, instructions, prep_time, cook_time, rating, servings, image_url, author_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            int recipeId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, title);
                stmt.setString(2, desc);
                stmt.setString(3, inst);
                stmt.setInt(4, prep);
                stmt.setInt(5, cook);
                stmt.setDouble(6, rating);
                stmt.setInt(7, servings);
                stmt.setString(8, img);
                stmt.setInt(9, authorId);
                stmt.executeUpdate();
                
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        recipeId = keys.getInt(1);
                    }
                }
                if (recipeId == -1) {
                    try (Statement s = conn.createStatement();
                         ResultSet rs = s.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs.next()) {
                            recipeId = rs.getInt(1);
                        }
                    }
                }
            }
            
            if (recipeId != -1) {
                // Insert ingredients
                String ingSql = "INSERT INTO ingredients (recipe_id, name, quantity, unit, price_estimate) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ingStmt = conn.prepareStatement(ingSql)) {
                    for (String[] ing : ingredients) {
                        ingStmt.setInt(1, recipeId);
                        ingStmt.setString(2, ing[0]);
                        ingStmt.setDouble(3, Double.parseDouble(ing[1]));
                        ingStmt.setString(4, ing[2]);
                        ingStmt.setDouble(5, Double.parseDouble(ing[3]));
                        ingStmt.addBatch();
                    }
                    ingStmt.executeBatch();
                }
                
                // Link tags
                String tagSql = "INSERT OR IGNORE INTO recipe_tags (recipe_id, tag_id) VALUES (?, ?)";
                try (PreparedStatement tagStmt = conn.prepareStatement(tagSql)) {
                    for (int tagId : tagIds) {
                        tagStmt.setInt(1, recipeId);
                        tagStmt.setInt(2, tagId);
                        tagStmt.addBatch();
                    }
                    tagStmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB Seeding] Failed to seed recipe: " + title + " - " + e.getMessage());
        }
    }

    /** Close the connection (call on app shutdown). */
    public static void close() {
        if (instance != null) {
            try {
                instance.close();
                instance = null;
                System.out.println("[DB] Connection closed");
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            }
        }
    }
}
