package com.recipeapp;

public class Tag {
    private int tagId;
    private String name;

    public Tag() {}

    public Tag(int tagId, String name) {
        this.tagId = tagId;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return tagId == tag.tagId && name.equals(tag.name);
    }

    @Override
    public int hashCode() {
        int result = tagId;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    // Getters and Setters
    public int getTagId() { return tagId; }
    public void setTagId(int tagId) { this.tagId = tagId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Tag{tagId=" + tagId + ", name='" + name + "'}";
    }
}
