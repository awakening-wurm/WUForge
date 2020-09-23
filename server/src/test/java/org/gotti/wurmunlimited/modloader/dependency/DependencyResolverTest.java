package org.gotti.wurmunlimited.modloader.dependency;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DependencyResolverTest {
	
	private static class Entry implements DependencyProvider {
		
		private String name;
		private Collection<String> requires = Collections.emptyList();
		private Collection<String> conflicts = Collections.emptyList();;
		private Collection<String> before = Collections.emptyList();
		private Collection<String> after = Collections.emptyList();
		private boolean onDemand = false;
		
		public Entry(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Collection<String> getRequires() {
			return requires;
		}

		@Override
		public Collection<String> getConflicts() {
			return conflicts;
		}

		@Override
		public Collection<String> getBefore() {
			return before;
		}

		@Override
		public Collection<String> getAfter() {
			return after;
		}
		
		@Override
		public boolean isOnDemand() {
			return onDemand;
		}
	}

	private Entry a;
	private Entry b;
	private Entry c;
	
	@Rule
	public ExpectedException expected = ExpectedException.none();
	
	@Before
	public void init() {
		this.a = new Entry("A");
		this.b = new Entry("B");
		this.c = new Entry("C");
	}

	@Test
	public void testSimple() {
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("A", "B", "C");
	}
	
	@Test
	public void testBefore() {
		b.before = Collections.singleton("C");
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("A", "C", "B");
	}
	
	@Test
	public void testAfter() {
		b.after = Collections.singleton("A");
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("B", "A", "C");
	}
	
	@Test
	public void testCycle() {
		a.after = Collections.singleton("B");
		b.after = Collections.singleton("C");
		c.after = Collections.singleton("A");
		expected.expect(DependencyException.class);
		expected.expectMessage("Unresolved order for the following elements: A, B, C");
		new DependencyResolver<>().order(Arrays.asList(c, a, b));
	}
	
	@Test
	public void testBlock() {
		a.after = Collections.singleton("B");
		a.before = Collections.singleton("B");
		expected.expect(DependencyException.class);
		expected.expectMessage("Unresolved order for the following elements: A, B");
		new DependencyResolver<>().order(Arrays.asList(c, a, b));
	}
	
	@Test
	public void testBlock2() {
		a.after = Collections.singleton("B");
		b.after = Collections.singleton("A");
		expected.expect(DependencyException.class);
		expected.expectMessage("Unresolved order for the following elements: A, B");
		new DependencyResolver<>().order(Arrays.asList(c, a, b));
	}
	
	@Test
	public void testSelfAfter() {
		a.after = Collections.singleton("A");
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("A", "B", "C");
	}
	
	@Test
	public void testSelfBefore() {
		a.before = Collections.singleton("A");
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("A", "B", "C");
	}
	
	@Test
	public void testRequireMissing() {
		a.requires = Collections.singleton("E");
		expected.expect(DependencyException.class);
		expected.expectMessage("A requires E which is unavailable");
		new DependencyResolver<>().order(Arrays.asList(c, a, b));
	}
	
	@Test
	public void testRequire() {
		a.requires = Collections.singleton("B");
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("B", "A", "C");
	}
	
	@Test
	public void testConflict() {
		a.conflicts = Collections.singleton("B");
		expected.expect(DependencyException.class);
		expected.expectMessage("A conflicts with B");
		new DependencyResolver<>().order(Arrays.asList(c, a, b));
	}
	
	/**
	 * Prepare for a future support of versioned elements
	 */
	@Test
	public void testRequireVersion() {
		a.requires = Collections.singleton("B@1.0");
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("B", "A", "C");
	}
	
	@Test
	public void testProvided() {
		a.requires = Collections.singleton("modloader");
		List<DependencyProvider> result = new DependencyResolver<>().provided(Collections.singleton("modloader@0.24")).order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("A", "B", "C");
	}
	
	@Test
	public void testOnDemand() {
		a.onDemand = true;
		b.onDemand = true;
		a.requires = Collections.singleton("B");
		c.requires = Collections.singleton("B");
		List<DependencyProvider> result = new DependencyResolver<>().order(Arrays.asList(c, a, b));
		Assertions.assertThat(result).extracting("name").containsExactly("B", "C");
	}
}
