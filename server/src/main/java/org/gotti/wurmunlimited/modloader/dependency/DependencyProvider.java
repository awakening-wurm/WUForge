package org.gotti.wurmunlimited.modloader.dependency;

import java.util.Collection;

public interface DependencyProvider {
	
	String getName();
	
	/**
	 * Required entities.
	 */
	Collection<String> getRequires();
	
	/**
	 * Conflicting entities.
	 */
	Collection<String> getConflicts();
	
	/**
	 * Entities that should appear before this entry.
	 */
	Collection<String> getBefore();
	
	/**
	 * Entities that should appear after this entry.
	 */
	Collection<String> getAfter();
	
	/**
	 * Load entity only if it's demanded by other entities.
	 */
	boolean isOnDemand();

}
