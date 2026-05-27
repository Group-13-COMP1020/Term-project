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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RoomController implements ContextAware {

    @FXML private Label roomNameLabel;
    @FXML private Label roomSubtitleLabel;
    


    // Members list container
    @FXML private VBox membersBox;

    // Announcement components
    @FXML private VBox announcementList;
    @FXML private TextField announcementInput;

    // Cooking Plan & Groceries summary components
    @FXML private Label recipesSummaryLabel;
    @FXML private ListView<String> roomGroceriesListView;
    @FXML private DatePicker planDatePicker;

    private User currentUser;
    private RecipeService recipeService;
    private SearchService searchService;
    private MainController mainController;

    private final UserDAO userDAO = new UserDAO();
    private final ShoppingListDAO shoppingListDAO = new ShoppingListDAO();
    private final ShoppingListService shoppingListService = new ShoppingListService();
    private final ObservableList<UserDAO.Announcement> announcements = FXCollections.observableArrayList();

    @Override
    public void setContext(User currentUser, RecipeService recipeService,
                           SearchService searchService, MainController mainController) {
        this.currentUser    = currentUser;
        this.recipeService  = recipeService;
        this.searchService  = searchService;
        this.mainController = mainController;

        // Bind custom cell factory for groceries list view to guarantee high-fidelity visibility and bypass CSS overrides
        roomGroceriesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #2D3748; -fx-font-weight: 500;");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        int roomId = getUserRoomId(currentUser);
        
        try {
            // Load dynamic Room Name and Members
            String roomName = userDAO.getRoomName(roomId);
            roomNameLabel.setText(roomName);
            roomSubtitleLabel.setText("VinUni sharing panel. Cook together, live better.");
            
            List<User> members = userDAO.findByRoomId(roomId);
            renderMembers(members);
            
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
                new UserDAO.Announcement("🍳 Trang is cooking Tofu wih Tomato Sauce on Tuesday!", "2026-05-25 14:40:46"),
                new UserDAO.Announcement("🧄 Nguyen: \"Who bought garlic? We have plenty in the pantry!\"", "2026-05-25 14:40:46"),
                new UserDAO.Announcement("🧹 Leader Nhan: \"Remember to wipe down the hotpot after usage.\"", "2026-05-25 14:40:46")
            );
        }
        renderAnnouncements();

        // Load room cooking plan and groceries
        loadRoomCookingPlanAndGroceries(roomId);

        // Load plan date and setup DatePicker listener
        try {
            int listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
            String savedDateStr = shoppingListDAO.getPlanDate(listId);
            if (savedDateStr != null && !savedDateStr.trim().isEmpty()) {
                try {
                    planDatePicker.setValue(LocalDate.parse(savedDateStr));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
            final int finalListId = listId;
            planDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        shoppingListDAO.updatePlanDate(finalListId, newVal.toString());
                        postPlanDateAnnouncement(newVal.toString());
                        // Refresh announcements dynamically in memory!
                        announcements.setAll(userDAO.getAnnouncements(roomId));
                        renderAnnouncements();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        for (UserDAO.Announcement a : announcements) {
            VBox card = new VBox(6);
            card.setStyle(
                "-fx-background-color: #FFFDF9; " +
                "-fx-border-color: #FFE0CC; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px; " +
                "-fx-padding: 12 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.01), 6, 0, 0, 1);"
            );
            card.setMaxWidth(Double.MAX_VALUE);

            Label msgLabel = new Label(a.getMessage());
            msgLabel.setStyle(
                "-fx-font-size: 13.5px; " +
                "-fx-text-fill: #2D3748; " +
                "-fx-wrap-text: true;"
            );
            msgLabel.setMaxWidth(Double.MAX_VALUE);

            String timeStr = formatTimestamp(a.getCreatedAt());
            Label timeLabel = new Label("• " + timeStr);
            timeLabel.setStyle(
                "-fx-font-size: 10.5px; " +
                "-fx-text-fill: #A0AEC0; " +
                "-fx-font-weight: 500;"
            );

            HBox bottomRow = new HBox();
            bottomRow.setAlignment(Pos.CENTER_RIGHT);
            bottomRow.getChildren().add(timeLabel);

            card.getChildren().addAll(msgLabel, bottomRow);
            announcementList.getChildren().add(0, card); // new ones on top
        }
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isEmpty()) {
            return "Just now";
        }
        try {
            String clean = rawTimestamp.replace(" ", "T");
            if (!clean.contains("T")) {
                return rawTimestamp;
            }
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(clean);
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm");
            return ldt.format(formatter);
        } catch (Exception e) {
            return rawTimestamp;
        }
    }

    @FXML
    private void handlePostAnnouncement() {
        String msg = announcementInput.getText().trim();
        if (!msg.isEmpty()) {
            String author = capitalize(currentUser.getUsername());
            String formattedMsg = "📣 " + author + ": \"" + msg + "\"";
            
            int roomId = getUserRoomId(currentUser);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String nowStr = java.time.LocalDateTime.now().format(formatter);
            
            try {
                userDAO.insertAnnouncement(roomId, currentUser.getUserId(), formattedMsg);
                announcements.add(new UserDAO.Announcement(formattedMsg, nowStr));
                announcementInput.clear();
                renderAnnouncements();
            } catch (SQLException e) {
                e.printStackTrace();
                // Fallback to local
                announcements.add(new UserDAO.Announcement(formattedMsg, nowStr));
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

    private void postPlanDateAnnouncement(String targetDate) {
        try {
            int roomId = getUserRoomId(currentUser);
            if (roomId > 0) {
                int listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
                List<Recipe> selected = shoppingListDAO.getSelectedRecipes(listId, recipeService);
                
                String scheduleMsg;
                if (selected.isEmpty()) {
                    scheduleMsg = String.format("📅 %s updated the room cooking plan target date to %s.", 
                        currentUser.getUsername(), targetDate);
                } else {
                    String recipesStr = selected.stream().map(Recipe::getTitle).collect(Collectors.joining(", "));
                    scheduleMsg = String.format("📅 %s scheduled a cooking plan for %s! Selected: %s", 
                        currentUser.getUsername(), targetDate, recipesStr);
                }
                
                userDAO.insertAnnouncement(roomId, currentUser.getUserId(), scheduleMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Recipe> filterRecipesByRoom(List<Recipe> recipes) {
        if (recipes == null) return new ArrayList<>();
        if (currentUser instanceof Admin) {
            return new ArrayList<>(recipes);
        }
        
        int userRoomId = getUserRoomId(currentUser);
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe r : recipes) {
            boolean isDefault = r.getRecipeId() <= 30 
                    || r.getAuthor() == null 
                    || "ADMIN".equalsIgnoreCase(r.getAuthor().getRole());
            
            if (isDefault) {
                filtered.add(r);
                continue;
            }
            
            int authorRoomId = getUserRoomId(r.getAuthor());
            if (userRoomId != 0 && userRoomId == authorRoomId) {
                filtered.add(r);
            }
        }
        return filtered;
    }
}
