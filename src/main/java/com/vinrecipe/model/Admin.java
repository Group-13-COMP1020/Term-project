package com.vinrecipe.model;

<<<<<<< HEAD
/**
 * Admin user — has highest permission level.
 * Demonstrates Inheritance from User.
 */
=======
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
public class Admin extends User {

    public Admin() {
        super();
        setRole("ADMIN");
    }

    public Admin(int userId, String username, String password, String email) {
        super(userId, username, password, email, "ADMIN");
    }

<<<<<<< HEAD
    /** Admins have the highest permission level (3). — Polymorphism */
    @Override
    public int getPermissionLevel() {
        return 3;
    }

    /** Admin-specific: manage all users in the system. */
    public void manageUsers() {
        System.out.println("[Admin] " + getUsername() + " is managing users.");
    }

    /** Admin-specific: delete any recipe regardless of author. */
    public void deleteAnyRecipe(int recipeId) {
        System.out.println("[Admin] " + getUsername() + " deleted recipe id=" + recipeId);
=======
    @Override
    public int getPermissionLevel() {
        return 3; // Highest permission level
    }

    public void manageUsers() {
        System.out.println("Admin " + getUsername() + " is managing users.");
    }

    public void deleteAnyRecipe(int recipeId) {
        System.out.println("Admin " + getUsername() + " deleted recipe with ID: " + recipeId);
    }

    @Override
    public String toString() {
        return "Admin{userId=" + getUserId() + ", username='" + getUsername() + "'}";
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    }
}
