package org.gotti.wurmunlimited.modloader.dependency;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DependencyResolver<T extends DependencyProvider> {

    private final Set<String> provided = new HashSet<>();

    public DependencyResolver() {
    }

    /**
     * Order the list according to the dependencies.
     *
     * @param mods Elements
     * @return Ordered elements
     */
    public List<T> order(List<? extends T> mods) {

        Map<String,T> byName = new HashMap<>();
        mods.forEach(mod -> byName.put(mod.getName(),mod));

        Map<String,DependencyEntry> entries = new LinkedHashMap<>();
        mods.forEach(mod -> entries.put(mod.getName(),new DependencyEntry(mod)));

        checkRequires(entries);
        checkConflicts(entries);
        removeMissing(entries);
        removeSelfReference(entries);
        resolveAfter(entries);
        resolveBefore(entries);

        do {} while(pruneOnDemand(entries));

        List<DependencyEntry> order = resolveOrder(entries);
        return order.stream().map(DependencyEntry::getName).map(byName::get).collect(Collectors.toList());

    }

    /**
     * Set a list of provided dependencies. Those are virtual and not actual elements. The provided elements
     * are checked for requires and conflicts but are ignored in ordering the elements.
     *
     * @param provided Provided elements
     * @return this
     */
    public DependencyResolver<T> provided(Collection<String> provided) {
        this.provided.addAll(DependencyEntry.parse(provided));
        return this;
    }

    private List<DependencyEntry> resolveOrder(Map<String,DependencyEntry> entries) {
        Set<DependencyEntry> blocked = new HashSet<DependencyEntry>();
        TreeSet<DependencyEntry> ready = new TreeSet<DependencyEntry>(Comparator.comparing(DependencyEntry::getName));
        List<DependencyEntry> order = new LinkedList<>();

        for(DependencyEntry mod : entries.values()) {
            if(mod.getBefore().isEmpty()) {
                ready.add(mod);
            } else {
                blocked.add(mod);
            }
        }

        while(!ready.isEmpty()) {
            DependencyEntry entry = ready.pollFirst();
            for(String after : entry.getAfter()) {
                DependencyEntry mod = entries.get(after);
                mod.getBefore().remove(entry.getName());
                if(mod.getBefore().isEmpty()) {
                    blocked.remove(mod);
                    ready.add(mod);
                }
            }
            order.add(entry);
        }

        if(!blocked.isEmpty()) {
            throw new DependencyException("Unresolved order for the following elements: "+blocked.stream().map(DependencyEntry::getName).sorted().collect(Collectors.joining(", ")));
        }

        return order;
    }

    private void checkRequires(Map<String,DependencyEntry> entries) {
        for(DependencyEntry entry : entries.values()) {
            for(String required : entry.getRequires()) {
                if(!entries.keySet().contains(required) && !provided.contains(required)) {
                    throw new DependencyException(String.format("%s requires %s which is unavailable",entry.getName(),required));
                }
            }
        }
    }

    private void checkConflicts(Map<String,DependencyEntry> entries) {
        for(DependencyEntry entry : entries.values()) {
            for(String conflict : entry.getConflicts()) {
                if(entries.keySet().contains(conflict) || provided.contains(conflict)) {
                    throw new DependencyException(String.format("%s conflicts with %s",entry.getName(),conflict));
                }
            }
        }
    }

    private void resolveAfter(Map<String,DependencyEntry> entries) {
        for(DependencyEntry mod : entries.values()) {
            mod.getAfter().forEach(info -> entries.get(info).addBefore(mod.getName()));
        }
    }

    private void resolveBefore(Map<String,DependencyEntry> entries) {
        for(DependencyEntry mod : entries.values()) {
            mod.getBefore().forEach(info -> entries.get(info).addAfter(mod.getName()));
        }
    }

    private void removeSelfReference(Map<String,DependencyEntry> entries) {
        for(DependencyEntry mod : entries.values()) {
            String name = mod.getName();
            mod.getBefore().removeIf(name::equals);
            mod.getAfter().removeIf(name::equals);
        }
    }

    private void removeMissing(Map<String,DependencyEntry> entries) {
        final Predicate<String> present = entries::containsKey;
        final Predicate<String> missing = present.negate();
        for(DependencyEntry mod : entries.values()) {
            mod.getBefore().removeIf(missing);
            mod.getAfter().removeIf(missing);
        }
    }

    private boolean pruneOnDemand(Map<String,DependencyEntry> entries) {
        final Set<String> toRemove = new HashSet<>();
        for(DependencyEntry mod : entries.values()) {
            if(mod.isOnDemand() && mod.getAfter().isEmpty()) {
                for(String before : mod.getBefore()) {
                    entries.get(before).getAfter().remove(mod.getName());
                }
                toRemove.add(mod.getName());
            }
        }
        return entries.keySet().removeAll(toRemove);
    }
}
