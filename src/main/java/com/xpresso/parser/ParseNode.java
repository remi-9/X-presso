package com.xpresso.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Node in the S-presso parse tree.
 */
public class ParseNode {
    private final String kind;
    private final String value;
    private final int line;
    private final int column;
    private final List<ParseNode> children = new ArrayList<>();

    public ParseNode(String kind) {
        this(kind, null, -1, -1);
    }

    public ParseNode(String kind, String value) {
        this(kind, value, -1, -1);
    }

    public ParseNode(String kind, String value, int line, int column) {
        this.kind = kind;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    public void add(ParseNode child) {
        if (child != null) {
            children.add(child);
        }
    }

    public String getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public List<ParseNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", kind);
        if (value != null) {
            map.put("value", value);
        }
        if (line >= 0) {
            map.put("line", line);
            map.put("column", column);
        }
        if (!children.isEmpty()) {
            List<Map<String, Object>> childMaps = new ArrayList<>();
            for (ParseNode child : children) {
                childMaps.add(child.toMap());
            }
            map.put("children", childMaps);
        }
        return map;
    }
}
