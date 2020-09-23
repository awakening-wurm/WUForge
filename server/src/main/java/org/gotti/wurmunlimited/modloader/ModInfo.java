package org.gotti.wurmunlimited.modloader;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.gotti.wurmunlimited.modloader.dependency.DependencyProvider;

/**
 * Information about a mod.
 */
class ModInfo implements DependencyProvider {
	private Properties properties;
	private String name;
	public ModInfo(Properties properties, String name) {
		this.properties = properties;
		this.name = name;
	}
	@Override
	public String getName() {
		return name;
	}
	public Properties getProperties() {
		return properties;
	}
	@Override
	public Collection<String> getRequires() {
		final List<String> set = parseList(getProperties().getProperty("depend.requires", ""));
		set.addAll(getImport());
		return set;
	}
	@Override
	public Collection<String> getConflicts() {
		return parseList(getProperties().getProperty("depend.conflicts", ""));
	}
	@Override
	public Collection<String> getBefore() {
		return parseList(getProperties().getProperty("depend.suggests", ""));
	}
	@Override
	public Collection<String> getAfter() {
		return parseList(getProperties().getProperty("depend.precedes", ""));
	}
	@Override
	public boolean isOnDemand() {
		return Boolean.parseBoolean(getProperties().getProperty("depend.ondemand", "false"));
	}
	public Collection<String> getImport() {
		return parseList(getProperties().getProperty("depend.import", ""));
	}
	
	private List<String> parseList(String list) {
		return Arrays.stream(list.split(","))
				.map(String::trim)
				.filter(string -> !string.isEmpty())
				.collect(Collectors.toList());
	}
}