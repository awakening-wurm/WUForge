package org.gotti.wurmunlimited.modloader.classhooks;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CompoundEnumeration<T> implements Enumeration<T> {

	private final Iterator<Enumeration<T>> iterator;
	private Enumeration<T> current = null;

	public CompoundEnumeration(Iterable<Enumeration<T>> iterable) {
		this.iterator = iterable.iterator();
	}

	@Override
	public boolean hasMoreElements() {
		while (current == null || !current.hasMoreElements()) {
			if (!iterator.hasNext()) {
				return false;
			}
			current = iterator.next();
		}
		return true;
	}

	@Override
	public T nextElement() {
		if (hasMoreElements()) {
			return current.nextElement();
		}
		throw new NoSuchElementException();
	}

}
