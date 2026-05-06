package com.vinrecipe.model;

public class Admin extends User {

    public Admin() {
        super();
        setRole("ADMIN");
    }

    public Admin(int userId, String username, String password, String email) {
        super(userId, username, password, email, "ADMIN");
    }

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
    }
}
