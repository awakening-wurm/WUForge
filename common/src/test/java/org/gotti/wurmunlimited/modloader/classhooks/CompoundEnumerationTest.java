package org.gotti.wurmunlimited.modloader.classhooks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CompoundEnumerationTest {

	@Test
	public void test() {

		List<Enumeration<Integer>> enumerations = new ArrayList<>();

		enumerations.add(Collections.enumeration(Arrays.asList(1, 2)));
		enumerations.add(Collections.emptyEnumeration());
		enumerations.add(Collections.enumeration(Arrays.asList(3)));
		enumerations.add(Collections.emptyEnumeration());
		enumerations.add(null);
		enumerations.add(Collections.enumeration(Arrays.asList(4, 5)));

		List<Integer> enumeration = Collections.list(new CompoundEnumeration<>(enumerations));
		Assertions.assertThat(enumeration).containsExactly(1, 2, 3, 4, 5);
	}

}
