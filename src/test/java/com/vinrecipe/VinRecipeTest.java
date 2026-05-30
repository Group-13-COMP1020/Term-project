package com.vinrecipe;

import com.vinrecipe.model.*;
import com.vinrecipe.service.SearchService;
import com.vinrecipe.service.ShoppingListService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VinRecipeTest {

    @Test
    public void testUserInheritancePermissions() {
        User student = new NormalStudent(1, "student_user", "pass", 101);
        User leader = new RoomLeader(2, "leader_user", "pass", 101);
        User admin = new Admin(3, "admin_user", "pass");

        // Verify polymorphism via getPermissionLevel() overrides
        assertEquals(1, student.getPermissionLevel(), "NormalStudent permission level must be 1");
        assertEquals(2, leader.getPermissionLevel(), "RoomLeader permission level must be 2");
        assertEquals(3, admin.getPermissionLevel(), "Admin permission level must be 3");

        // Verify room assignment attributes
        assertEquals(101, ((NormalStudent) student).getRoomId());
        assertEquals(101, ((RoomLeader) leader).getRoomId());
    }

    @Test
    public void testShoppingListAggregation() {
        ShoppingListService listService = new ShoppingListService();

        // Recipe 1: Fried Tofu (1 tofu block, 2 tomatoes)
        Recipe r1 = new Recipe();
        r1.setRecipeId(1);
        r1.setTitle("Fried Tofu");
        List<Ingredient> ing1 = new ArrayList<>();
        ing1.add(new Ingredient(1, "tofu block", 1.0, "pcs", 5000.0));
        ing1.add(new Ingredient(2, "tomatoes", 2.0, "pcs", 6000.0));
        r1.setIngredients(ing1);

        // Recipe 2: Tofu Tomato Soup (0.5 tofu block, 1 tomatoes, 300ml broth)
        Recipe r2 = new Recipe();
        r2.setRecipeId(2);
        r2.setTitle("Tofu Tomato Soup");
        List<Ingredient> ing2 = new ArrayList<>();
        ing2.add(new Ingredient(3, "tofu block", 0.5, "pcs", 2500.0));
        ing2.add(new Ingredient(4, "tomatoes", 1.0, "pcs", 3000.0));
        ing2.add(new Ingredient(5, "broth", 300.0, "ml", 5000.0));
        r2.setIngredients(ing2);

        // Aggregate
        List<Recipe> selected = List.of(r1, r2);
        ShoppingList list = listService.generate(selected);
        Map<String, Double> aggregated = list.getAggregatedItems();

        // Assert quantities are combined correctly (1.0 + 0.5 = 1.5 tofu block, 2.0 + 1.0 = 3.0 tomatoes)
        assertEquals(1.5, aggregated.get("tofu block"), 0.01);
        assertEquals(3.0, aggregated.get("tomatoes"), 0.01);
        assertEquals(300.0, aggregated.get("broth"), 0.01);

        // Check units
        Map<String, String> units = list.getItemUnits();
        assertEquals("pcs", units.get("tofu block"));
        assertEquals("pcs", units.get("tomatoes"));
        assertEquals("ml", units.get("broth"));
    }

    @Test
    public void testSearchServiceInvertedIndex() {
        SearchService searchService = new SearchService();

        // Recipe 1: Egg Salad (egg, mayonnaise)
        Recipe r1 = new Recipe();
        r1.setRecipeId(1);
        r1.setTitle("Egg Salad");
        r1.setPrepTime(5);
        List<Ingredient> ing1 = new ArrayList<>();
        ing1.add(new Ingredient(1, "egg", 2.0, "pcs", 6000));
        ing1.add(new Ingredient(2, "mayonnaise", 1.0, "tbsp", 2000));
        r1.setIngredients(ing1);

        // Recipe 2: French Fries (potato, oil, salt)
        Recipe r2 = new Recipe();
        r2.setRecipeId(2);
        r2.setTitle("French Fries");
        r2.setPrepTime(10);
        List<Ingredient> ing2 = new ArrayList<>();
        ing2.add(new Ingredient(3, "potato", 500.0, "g", 15000));
        ing2.add(new Ingredient(4, "oil", 200.0, "ml", 10000));
        ing2.add(new Ingredient(5, "salt", 5.0, "g", 500));
        r2.setIngredients(ing2);

        // Recipe 3: Potato Salad (potato, egg, mayonnaise)
        Recipe r3 = new Recipe();
        r3.setRecipeId(3);
        r3.setTitle("Potato Salad");
        r3.setPrepTime(8);
        List<Ingredient> ing3 = new ArrayList<>();
        ing3.add(new Ingredient(6, "potato", 200.0, "g", 6000));
        ing3.add(new Ingredient(7, "egg", 1.0, "pcs", 3000));
        ing3.add(new Ingredient(8, "mayonnaise", 1.0, "tbsp", 2000));
        r3.setIngredients(ing3);

        // Compile index
        List<Recipe> allRecipes = List.of(r1, r2, r3);
        searchService.buildIndex(allRecipes);

        // Search with ["egg", "mayonnaise"] list
        List<Recipe> results = searchService.searchByIngredients(List.of("egg", "mayonnaise"));

        // Verify matching and ranking
        assertFalse(results.isEmpty());
        // Egg Salad (r1) has 100% completion (2 out of 2 ingredients) -> should be first!
        assertEquals("Egg Salad", results.get(0).getTitle());
        
        // Potato Salad (r3) has 66.6% completion (2 out of 3 ingredients) -> should be second!
        assertEquals("Potato Salad", results.get(1).getTitle());
    }

    @Test
    public void testSortingAlgorithms() {
        SearchService searchService = new SearchService();

        Recipe r1 = new Recipe();
        r1.setTitle("Banh Xeo");
        r1.setPrepTime(10);
        r1.setCookTime(15); // Total = 25

        Recipe r2 = new Recipe();
        r2.setTitle("Avocado Toast");
        r2.setPrepTime(5);
        r2.setCookTime(5); // Total = 10

        Recipe r3 = new Recipe();
        r3.setTitle("Com Tam");
        r3.setPrepTime(15);
        r3.setCookTime(20); // Total = 35

        List<Recipe> list = List.of(r1, r2, r3);

        // 1. Alphabetical A-Z
        List<Recipe> az = searchService.sortAlphabeticalAZ(list);
        assertEquals("Avocado Toast", az.get(0).getTitle());
        assertEquals("Banh Xeo", az.get(1).getTitle());
        assertEquals("Com Tam", az.get(2).getTitle());

        // 2. Alphabetical Z-A
        List<Recipe> za = searchService.sortAlphabeticalZA(list);
        assertEquals("Com Tam", za.get(0).getTitle());
        assertEquals("Banh Xeo", za.get(1).getTitle());
        assertEquals("Avocado Toast", za.get(2).getTitle());

        // 3. Quickest (Total Time)
        List<Recipe> quickest = searchService.sortByPrepTime(list);
        assertEquals("Avocado Toast", quickest.get(0).getTitle()); // 10 min
        assertEquals("Banh Xeo", quickest.get(1).getTitle());       // 25 min
        assertEquals("Com Tam", quickest.get(2).getTitle());       // 35 min
    }
}
