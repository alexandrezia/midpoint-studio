package com.evolveum.midpoint.studio.impl.metrics;

import java.awt.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Node {

    private int id;

    private boolean enabled;

    private Color color;

    private String oid;

    private String name;

    private String url;

    public Node(int id, String oid, String name, String url) {
        this.id = id;
        this.oid = oid;
        this.name = name;
        this.url = url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}