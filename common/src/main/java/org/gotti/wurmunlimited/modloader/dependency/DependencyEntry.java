package org.gotti.wurmunlimited.modloader.dependency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyEntry {

    private final String name;

    // Set of required entries
    private final Set<String> requires = new HashSet<>();
    // Set of conflicting entries
    private final Set<String> conflicts = new HashSet<>();

    // Set of entries to initialize before this.
    private final Set<String> before = new HashSet<>();

    // Set of entries to initialize after this.
    private final Set<String> after = new HashSet<>();

    // Load element only if it's requested by another element
    private final boolean onDemand;

    protected DependencyEntry(DependencyProvider entry) {
        this.name = entry.getName();

        this.requires.addAll(parse(entry.getRequires()));
        this.conflicts.addAll(parse(entry.getConflicts()));

        this.before.addAll(this.requires);
        this.before.addAll(entry.getBefore());
        this.after.addAll(entry.getAfter());

        this.onDemand = entry.isOnDemand();
    }

    protected static String parse(String input) {
        String[] parts = input.split("@",2);
        return parts[0];
    }

    protected static Collection<String> parse(Collection<String> input) {
        return input.stream().map(DependencyEntry::parse).collect(Collectors.toList());
    }

    protected Set<String> getRequires() {
        return requires;
    }

    protected Set<String> getConflicts() {
        return conflicts;
    }

    protected Set<String> getBefore() {
        return before;
    }

    protected Set<String> getAfter() {
        return after;
    }

    public String getName() {
        return name;
    }

    public void addBefore(String name) {
        this.before.add(name.trim());
    }

    public void addAfter(String name) {
        this.after.add(name.trim());
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isOnDemand() {
        return onDemand;
    }

}
