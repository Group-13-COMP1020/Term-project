package com.vinrecipe.controller;

import com.vinrecipe.model.*;
import com.vinrecipe.dao.UserDAO;
import com.vinrecipe.dao.ShoppingListDAO;
import com.vinrecipe.service.RecipeService;
import com.vinrecipe.service.SearchService;
import com.vinrecipe.service.ShoppingListService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController implements ContextAware {

    @FXML private Label roomNameLabel;
    @FXML private Label roomSubtitleLabel;
    
    // Stats labels
    @FXML private Label statMembersCount;
    @FXML private Label statRecipesShared;

    // Members list container
    @FXML private VBox membersBox;

    // Announcement components
    @FXML private VBox announcementList;
    @FXML private TextField announcementInput;

    // Cooking Plan & Groceries summary components
    @FXML private Label recipesSummaryLabel;
    @FXML private ListView<String> roomGroceriesListView;

    private User currentUser;
    private RecipeService recipeService;
    private SearchService searchService;
    private MainController mainController;

    private final UserDAO userDAO = new UserDAO();
    private final ShoppingListDAO shoppingListDAO = new ShoppingListDAO();
    private final ShoppingListService shoppingListService = new ShoppingListService();
    private final ObservableList<String> announcements = FXCollections.observableArrayList();

    @Override
    public void setContext(User currentUser, RecipeService recipeService,
                           SearchService searchService, MainController mainController) {
        this.currentUser    = currentUser;
        this.recipeService  = recipeService;
        this.searchService  = searchService;
        this.mainController = mainController;

        int roomId = getUserRoomId(currentUser);
        
        try {
            // Load dynamic Room Name and Members
            String roomName = userDAO.getRoomName(roomId);
            roomNameLabel.setText(roomName);
            roomSubtitleLabel.setText("VinUni sharing panel. Cook together, live better.");
            
            List<User> members = userDAO.findByRoomId(roomId);
            renderMembers(members);
            
            // Set dynamic stats
            statMembersCount.setText(String.valueOf(members.size()));
            statRecipesShared.setText(String.valueOf(recipeService.getAllRecipes().size()));
            
        } catch (SQLException e) {
            e.printStackTrace();
            roomNameLabel.setText("VinUni Dorm A-101");
            roomSubtitleLabel.setText("Sharing panel. Cook together, live better.");
        }

        // Initialize dynamic announcements from the SQLite database
        try {
            announcements.setAll(userDAO.getAnnouncements(roomId));
        } catch (SQLException e) {
            e.printStackTrace();
            // Fallback default
            announcements.setAll(
                "🍳 Trang is cooking Tofu wih Tomato Sauce on Tuesday!",
                "🧄 Nguyen: \"Who bought garlic? We have plenty in the pantry!\"",
                "🧹 Leader Nhan: \"Remember to wipe down the hotpot after usage.\""
            );
        }
        renderAnnouncements();

        // Load room cooking plan and groceries
        loadRoomCookingPlanAndGroceries(roomId);
    }

    private int getUserRoomId(User user) {
        if (user instanceof NormalStudent) {
            return ((NormalStudent) user).getRoomId();
        } else if (user instanceof RoomLeader) {
            return ((RoomLeader) user).getRoomId();
        }
        return 0;
    }

    private void renderMembers(List<User> members) {
        membersBox.getChildren().clear();
        for (User u : members) {
            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(10, 14, 10, 14));
            card.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #E2E8F0; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 12px;"
            );

            // Member Initials avatar
            StackPane avatar = new StackPane();
            avatar.setPrefSize(36, 36);
            avatar.setMinSize(36, 36);
            avatar.setMaxSize(36, 36);
            
            String roleColor = u.getRole().equals("ROOM_LEADER") ? "#FFF2E6" : "#F8FAFC";
            String textFillColor = u.getRole().equals("ROOM_LEADER") ? "#E76F51" : "#4A5568";
            
            avatar.setStyle(
                "-fx-background-color: " + roleColor + "; " +
                "-fx-background-radius: 50%;"
            );
            
            String initials = u.getUsername().substring(0, Math.min(u.getUsername().length(), 2)).toUpperCase();
            Label avatarLabel = new Label(initials);
            avatarLabel.setStyle("-fx-text-fill: " + textFillColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");
            avatar.getChildren().add(avatarLabel);

            // Text Info
            VBox textContainer = new VBox(2);
            
            HBox nameRow = new HBox(4);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label(capitalize(u.getUsername()));
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2D3748;");
            nameRow.getChildren().add(nameLabel);
            
            if (u.getRole().equals("ROOM_LEADER")) {
                Label crown = new Label("👑");
                crown.setStyle("-fx-font-size: 12px;");
                nameRow.getChildren().add(crown);
            }
            
            String roleText = u.getRole().equals("ROOM_LEADER") ? "Room Leader" : "Dormmate";
            Label roleLabel = new Label(roleText);
            roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
            
            textContainer.getChildren().addAll(nameRow, roleLabel);
            card.getChildren().addAll(avatar, textContainer);

            // Highlight current user card subtly
            if (u.getUsername().equals(currentUser.getUsername())) {
                card.setStyle(
                    "-fx-background-color: #FFF2E6; " +
                    "-fx-background-radius: 12px; " +
                    "-fx-border-color: #E76F51; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 12px;"
                );
            }

            membersBox.getChildren().add(card);
        }
    }

    private void renderAnnouncements() {
        announcementList.getChildren().clear();
        for (String msg : announcements) {
            Label lbl = new Label(msg);
            lbl.setStyle(
                "-fx-font-size: 13px; " +
                "-fx-text-fill: #2D3748; " +
                "-fx-background-color: #FFFDF9; " +
                "-fx-border-color: #FFE0CC; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-padding: 10 14; " +
                "-fx-wrap-text: true;"
            );
            lbl.setMaxWidth(Double.MAX_VALUE);
            announcementList.getChildren().add(0, lbl); // new ones on top
        }
    }

    @FXML
    private void handlePostAnnouncement() {
        String msg = announcementInput.getText().trim();
        if (!msg.isEmpty()) {
            String author = capitalize(currentUser.getUsername());
            String formattedMsg = "📣 " + author + ": \"" + msg + "\"";
            
            int roomId = getUserRoomId(currentUser);
            try {
                userDAO.insertAnnouncement(roomId, currentUser.getUserId(), formattedMsg);
                announcements.add(formattedMsg);
                announcementInput.clear();
                renderAnnouncements();
            } catch (SQLException e) {
                e.printStackTrace();
                // Fallback to local
                announcements.add(formattedMsg);
                announcementInput.clear();
                renderAnnouncements();
            }
        }
    }

    private void loadRoomCookingPlanAndGroceries(int roomId) {
        try {
            int listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
            List<Recipe> selected = shoppingListDAO.getSelectedRecipes(listId, recipeService);
            
            if (selected.isEmpty()) {
                recipesSummaryLabel.setText("No recipes selected yet! Go to the 'Shopping Lists' tab to select recipes.");
                roomGroceriesListView.getItems().clear();
                return;
            }
            
            // Build recipe titles summary
            String titles = selected.stream().map(Recipe::getTitle).collect(Collectors.joining(", "));
            recipesSummaryLabel.setText(titles);
            
            // Consolidate ingredients
            ShoppingList list = shoppingListService.generate(selected);
            Map<String, Double> aggregated = list.getAggregatedItems();
            Map<String, String> units = list.getItemUnits();
            
            List<String> displayList = new ArrayList<>();
            for (Map.Entry<String, Double> entry : aggregated.entrySet()) {
                String name = entry.getKey();
                double qty = entry.getValue();
                String unit = units.getOrDefault(name, "");
                String emoji = getEmojiForIngredient(name);
                
                displayList.add(String.format("%s %s: %.1f %s", emoji, capitalize(name), qty, unit));
            }
            
            // Sort alphabetically
            displayList.sort(String::compareToIgnoreCase);
            roomGroceriesListView.getItems().setAll(displayList);
            
        } catch (SQLException e) {
            e.printStackTrace();
            recipesSummaryLabel.setText("⚠ Database error loading cooking plan.");
            roomGroceriesListView.getItems().clear();
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getEmojiForIngredient(String name) {
        name = name.toLowerCase().trim();
        if (name.contains("apple") || name.contains("fruit")) return "🍎";
        if (name.contains("avocado")) return "🥑";
        if (name.contains("egg")) return "🥚";
        if (name.contains("bread") || name.contains("toast")) return "🍞";
        if (name.contains("chicken")) return "🍗";
        if (name.contains("beef") || name.contains("steak")) return "🥩";
        if (name.contains("pork") || name.contains("ribs")) return "🥓";
        if (name.contains("potato") || name.contains("fry") || name.contains("fries")) return "🍟";
        if (name.contains("tofu")) return "🍲";
        if (name.contains("tomato")) return "🍅";
        if (name.contains("pumpkin") || name.contains("soup")) return "🎃";
        if (name.contains("rice")) return "🍚";
        if (name.contains("pickle")) return "🥒";
        if (name.contains("gimbap") || name.contains("seaweed") || name.contains("roll")) return "🍣";
        if (name.contains("salt") || name.contains("pepper") || name.contains("sugar") || name.contains("spice")) return "🧂";
        if (name.contains("onion") || name.contains("garlic")) return "🧄";
        if (name.contains("scallion") || name.contains("spinach") || name.contains("carrot") || name.contains("vegetable")) return "🥕";
        if (name.contains("milk") || name.contains("cream") || name.contains("cheese") || name.contains("butter")) return "🥛";
        if (name.contains("pasta") || name.contains("noodle")) return "🍝";
        if (name.contains("sauce") || name.contains("vinegar") || name.contains("oil")) return "🍶";
        return "🛒";
    }
}
