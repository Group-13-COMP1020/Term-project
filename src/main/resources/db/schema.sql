-- ========== USERS ==========
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    role        ENUM('ADMIN', 'NORMAL_STUDENT', 'ROOM_LEADER') NOT NULL DEFAULT 'NORMAL_STUDENT',
    room_id     INT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========== ROOMS ==========
CREATE TABLE rooms (
    room_id     INT AUTO_INCREMENT PRIMARY KEY,
    room_name   VARCHAR(100) NOT NULL,
    leader_id   INT,
    FOREIGN KEY (leader_id) REFERENCES users(user_id)
);

ALTER TABLE users ADD FOREIGN KEY (room_id) REFERENCES rooms(room_id);

-- ========== TAGS ==========
CREATE TABLE tags (
    tag_id   INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(50) NOT NULL UNIQUE
);

-- ========== RECIPES ==========
CREATE TABLE recipes (
    recipe_id    INT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    instructions TEXT,
    prep_time    INT,          -- in minutes
    cook_time    INT,          -- in minutes
    rating       DECIMAL(2,1) DEFAULT 0.0,
    servings     INT DEFAULT 1,
    author_id    INT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(user_id)
);

-- ========== INGREDIENTS ==========
CREATE TABLE ingredients (
    ingredient_id  INT AUTO_INCREMENT PRIMARY KEY,
    recipe_id      INT NOT NULL,
    name           VARCHAR(100) NOT NULL,
    quantity       DECIMAL(10,2),
    unit           VARCHAR(30),
    price_estimate DECIMAL(10,2),
    FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE
);

-- ========== RECIPE ↔ TAG (many-to-many) ==========
CREATE TABLE recipe_tags (
    recipe_id  INT,
    tag_id     INT,
    PRIMARY KEY (recipe_id, tag_id),
    FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id)    REFERENCES tags(tag_id) ON DELETE CASCADE
);

-- ========== SHOPPING LISTS ==========
CREATE TABLE shopping_lists (
    list_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE shopping_list_recipes (
    list_id   INT,
    recipe_id INT,
    PRIMARY KEY (list_id, recipe_id),
    FOREIGN KEY (list_id)   REFERENCES shopping_lists(list_id) ON DELETE CASCADE,
    FOREIGN KEY (recipe_id) REFERENCES recipes(recipe_id) ON DELETE CASCADE
);
