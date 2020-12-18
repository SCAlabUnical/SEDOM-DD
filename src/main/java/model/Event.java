package model;

import java.util.Set;


public class Event {

    String name;
    String type;
    BaseShape entity;
    Set<String> descriptions;
    ShapeLevel level = ShapeLevel.CITY;

    public Event() {
    }

    public Event(String name, String type, BaseShape entity) {
        this.name = name;
        this.type = type;
        this.entity = entity;
        if (entity instanceof Poi) {
            level = ShapeLevel.POI;
        } else if (entity instanceof Street) {
            level = ShapeLevel.STREET;
        } else if (entity instanceof District) {
            level = ShapeLevel.DISTRICT;
        } else if (entity instanceof City) {
            level = ShapeLevel.CITY;
        }
    }

    public ShapeLevel getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BaseShape getEntity() {
        return entity;
    }

    public void setEntity(BaseShape entity) {
        if (entity instanceof Poi) {
            level = ShapeLevel.POI;
        } else if (entity instanceof Street) {
            level = ShapeLevel.STREET;
        } else if (entity instanceof District) {
            level = ShapeLevel.DISTRICT;
        }
        this.entity = entity;
    }

    public Set<String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Set<String> descriptions) {
        this.descriptions = descriptions;
    }

}
