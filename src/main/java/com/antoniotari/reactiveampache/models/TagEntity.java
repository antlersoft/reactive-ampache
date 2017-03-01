package com.antoniotari.reactiveampache.models;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Created by mike on 2/24/17.
 */

public class TagEntity {
    @Attribute(name = "id", required = false)
    String id;

    @Element(name = "name", required = false)
    String name;

    @Element(name = "songs", required = false)
    int songs;

    public String getId() { return id; }
    public String getName() { return name; }
    public int getSongs() { return songs; }
}
