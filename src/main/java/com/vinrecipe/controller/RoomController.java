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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.util.StringConverter;

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

        // Bind custom cell factory for groceries list view to keep items aligned.
        roomGroceriesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String[] parts = item.split("\\|", 3);
                    String name = parts.length > 0 ? parts[0] : item;
                    String amount = parts.length > 1 ? parts[1] : "";
                    String unit = parts.length > 2 ? parts[2] : "";

                    Label itemName = new Label(name);
                    itemName.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #2D3748; -fx-font-weight: 600;");

                    Label itemAmount = new Label((amount + " " + unit).trim());
                    itemAmount.setStyle("-fx-font-size: 12px; -fx-text-fill: #718096;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    HBox row = new HBox(10, itemName, spacer, itemAmount);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(4, 0, 4, 0));
                    setGraphic(row);
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

        // Load plan date first, then groceries for that date, then announcements
        try {
            int listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
            String savedDateStr = shoppingListDAO.getPlanDate(listId);
            LocalDate planDate = (savedDateStr != null && !savedDateStr.trim().isEmpty())
                ? LocalDate.parse(savedDateStr) : LocalDate.now();

            loadRoomCookingPlanAndGroceries(roomId, planDate.toString());

            DateTimeFormatter displayFmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
            planDatePicker.setConverter(new StringConverter<LocalDate>() {
                @Override public String toString(LocalDate d) {
                    return d == null ? "" : d.format(displayFmt);
                }
                @Override public LocalDate fromString(String s) {
                    if (s == null || s.trim().isEmpty()) return null;
                    try { return LocalDate.parse(s, displayFmt); } catch (Exception e) { return null; }
                }
            });
            planDatePicker.setValue(planDate);

            // Load announcements filtered by plan date
            try {
                announcements.setAll(userDAO.getAnnouncementsByDate(roomId, planDate.toString()));
            } catch (Exception ex) {
                ex.printStackTrace();
                announcements.setAll(userDAO.getAnnouncements(roomId));
            }
            renderAnnouncements();

            final int finalListId = listId;
            planDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    try {
                        shoppingListDAO.updatePlanDate(finalListId, newVal.toString());
                        postPlanDateAnnouncement(newVal);
                        loadRoomCookingPlanAndGroceries(roomId, newVal.toString());
                        announcements.setAll(userDAO.getAnnouncementsByDate(roomId, newVal.toString()));
                        renderAnnouncements();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
            loadRoomCookingPlanAndGroceries(roomId, LocalDate.now().toString());
            try {
                announcements.setAll(userDAO.getAnnouncements(roomId));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            renderAnnouncements();
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
        members = new ArrayList<>(members);
        members.sort((a, b) -> {
            boolean aIsCurrent = a.getUsername().equals(currentUser.getUsername());
            boolean bIsCurrent = b.getUsername().equals(currentUser.getUsername());
            if (aIsCurrent != bIsCurrent) return aIsCurrent ? -1 : 1;
            boolean aIsLeader = "ROOM_LEADER".equals(a.getRole());
            boolean bIsLeader = "ROOM_LEADER".equals(b.getRole());
            return Boolean.compare(!aIsLeader, !bIsLeader);
        });
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

            Label typeBadge = new Label(getAnnouncementType(a.getMessage()));
            typeBadge.setStyle(getAnnouncementBadgeStyle(typeBadge.getText()));

            Label msgLabel = new Label(cleanAnnouncementMessage(a.getMessage()));
            msgLabel.setStyle(
                "-fx-font-size: 13.5px; " +
                "-fx-text-fill: #2D3748; " +
                "-fx-wrap-text: true;"
            );
            msgLabel.setMaxWidth(Double.MAX_VALUE);

            HBox messageRow = new HBox(8, typeBadge, msgLabel);
            messageRow.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(msgLabel, Priority.ALWAYS);

            String timeStr = formatTimestamp(a.getCreatedAt());
            Label timeLabel = new Label(timeStr);
            timeLabel.setStyle(
                "-fx-font-size: 10.5px; " +
                "-fx-text-fill: #A0AEC0; " +
                "-fx-font-weight: 500;"
            );

            HBox bottomRow = new HBox();
            bottomRow.setAlignment(Pos.CENTER_RIGHT);
            bottomRow.getChildren().add(timeLabel);

            card.getChildren().addAll(messageRow, bottomRow);
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
            String formattedMsg = author + ": \"" + msg + "\"";
            
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

    private void loadRoomCookingPlanAndGroceries(int roomId, String planDate) {
        try {
            int listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
            List<Recipe> selected = shoppingListDAO.getSelectedRecipesByDate(listId, planDate, recipeService);
            
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
                displayList.add(String.format("%s|%.1f|%s", capitalize(name), qty, unit));
            }
            
            // Sort alphabetically
            displayList.sort(String::compareToIgnoreCase);
            roomGroceriesListView.getItems().setAll(displayList);
            
        } catch (SQLException e) {
            e.printStackTrace();
            recipesSummaryLabel.setText("Database error loading cooking plan.");
            roomGroceriesListView.getItems().clear();
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String getAnnouncementType(String message) {
        String clean = cleanAnnouncementMessage(message).toLowerCase();
        if (clean.contains("cooking plan") || clean.contains("target date") || clean.contains("selected:")) return "PLAN";
        if (clean.contains("bought") || clean.contains("pantry") || clean.contains("shopping")) return "SHOP";
        if (clean.contains("wipe") || clean.contains("remember")) return "ROOM";
        return "POST";
    }

    private String getAnnouncementBadgeStyle(String type) {
        String color = switch (type) {
            case "PLAN" -> "#E76F51";
            case "SHOP" -> "#2B6CB0";
            case "ROOM" -> "#2F855A";
            default -> "#4A5568";
        };
        String background = switch (type) {
            case "PLAN" -> "#FFF2E6";
            case "SHOP" -> "#EAF2FF";
            case "ROOM" -> "#EAF7EF";
            default -> "#EDF2F7";
        };
        return "-fx-background-color: " + background + "; " +
               "-fx-text-fill: " + color + "; " +
               "-fx-font-size: 10px; " +
               "-fx-font-weight: bold; " +
               "-fx-background-radius: 999px; " +
               "-fx-padding: 3 7;";
    }

    private String cleanAnnouncementMessage(String message) {
        if (message == null) return "";
        return message.replaceAll("^[^A-Za-z0-9]+\\s*", "").trim();
    }

    private String formatDateFriendly(LocalDate date) {
        if (date.equals(LocalDate.now())) return "Today, " + date.format(DateTimeFormatter.ofPattern("MMM d"));
        if (date.equals(LocalDate.now().plusDays(1))) return "Tomorrow, " + date.format(DateTimeFormatter.ofPattern("MMM d"));
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private void postPlanDateAnnouncement(LocalDate targetDate) {
        try {
            int roomId = getUserRoomId(currentUser);
            if (roomId > 0) {
                int listId = shoppingListDAO.getOrCreateListId(roomId, currentUser.getUserId());
                List<Recipe> selected = shoppingListDAO.getSelectedRecipesByDate(listId, targetDate.toString(), recipeService);
                String author = capitalize(currentUser.getUsername());
                String friendly = formatDateFriendly(targetDate);

                String scheduleMsg;
                if (selected.isEmpty()) {
                    scheduleMsg = String.format("%s set cook date to %s.", author, friendly);
                } else {
                    String recipesStr = selected.stream().map(Recipe::getTitle).collect(Collectors.joining(", "));
                    scheduleMsg = String.format("%s planned to cook %s on %s.", author, recipesStr, friendly);
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
