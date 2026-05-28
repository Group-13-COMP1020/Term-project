package com.vinrecipe;

import com.vinrecipe.dao.DatabaseConnection;
import com.vinrecipe.model.Recipe;
import com.vinrecipe.model.ShoppingList;
import com.vinrecipe.model.User;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import com.vinrecipe.service.ShoppingListService;
import com.vinrecipe.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSmokeTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void closeDatabase() {
        DatabaseConnection.close();
        System.clearProperty("vinrecipe.db.path");
    }

    @Test
    void seededLoginRecipeSearchAndShoppingListFlowWorks() {
        System.setProperty("vinrecipe.db.path", tempDir.resolve("vinrecipe-test.db").toString());

        UserService userService = new UserService();
        RecipeService recipeService = new RecipeService();
        SearchService searchService = new SearchService();
        ShoppingListService shoppingListService = new ShoppingListService();

        User user = userService.login("nhan", "nhan123");
        assertNotNull(user, "seeded room leader account should log in");

        List<Recipe> recipes = recipeService.getAllRecipes();
        assertFalse(recipes.isEmpty(), "seeded recipes should load from SQLite");

        searchService.buildIndex(recipes);
        List<Recipe> matches = searchService.searchByIngredients(List.of("egg", "rice"));
        assertFalse(matches.isEmpty(), "ingredient search should return recommendations");

        ShoppingList shoppingList = shoppingListService.generate(matches.subList(0, Math.min(2, matches.size())));
        assertFalse(shoppingList.getAggregatedItems().isEmpty(), "shopping list should aggregate ingredients");
        assertTrue(shoppingListService.formatForDisplay(shoppingList).size() > 0);
    }
}
