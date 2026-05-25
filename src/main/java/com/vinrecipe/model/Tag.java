package com.vinrecipe.model;

<<<<<<< HEAD
/**
 * Represents a tag/category for filtering recipes.
 * POJO — simple encapsulated data class.
 */
public class Tag {

=======
public class Tag {
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    private int tagId;
    private String name;

    public Tag() {}

    public Tag(int tagId, String name) {
        this.tagId = tagId;
<<<<<<< HEAD
        setName(name);
    }

    // ---- Getters / Setters ----
=======
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
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
    public int getTagId() { return tagId; }
    public void setTagId(int tagId) { this.tagId = tagId; }

    public String getName() { return name; }
<<<<<<< HEAD
    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Tag name must not be blank");
        this.name = name.trim();
    }

    @Override
    public String toString() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;
        Tag tag = (Tag) o;
        return tagId == tag.tagId;
    }

    @Override
    public int hashCode() { return Integer.hashCode(tagId); }
=======
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Tag{tagId=" + tagId + ", name='" + name + "'}";
    }
>>>>>>> b0f37c551070ff5bdc31bf72d9268deb60f159a3
}
