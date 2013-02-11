package com.jrtc27.stevechat.listeners;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import junit.framework.Assert;

/**
 * Ensure we aren't missing any {@link EventHandler} annotations, and that all listeners implement {@link Listener}
 *
 * @author Feildmaster, jrtc27
 */
public class TestListeners {
	private Class[] knownListeners = new Class[] { PlayerListener.class };

	@Test
	public void testForUnknown() {
		final Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("com.jrtc27.stevechat.listeners")));

		final Set<Class<? extends Listener>> foundListeners = reflections.getSubTypesOf(Listener.class);
		for (Class clazz : this.knownListeners) {
			foundListeners.remove(clazz);
		}

		final Iterator<Class<? extends Listener>> unknownListeners = foundListeners.iterator();

		if (unknownListeners.hasNext()) {
			boolean plural = false;
			final StringBuilder builder = new StringBuilder(unknownListeners.next().getCanonicalName());

			while (unknownListeners.hasNext()) {
				plural = true;
				final Class unknownListener = unknownListeners.next();
				if (unknownListeners.hasNext()) {
					builder.append(", ");
				} else {
					builder.append(" and ");
				}
				builder.append(unknownListener.getCanonicalName());
			}

			Assert.fail("The listener" + (plural ? "s " : " ") + builder.toString() + " ha" + (plural ? "ve" : "s") + " not been added to the knownListeners array!");
		}
	}

	@Test
	public void testAllKnown() {
		for (Class clazz : this.knownListeners) {
			testListener(clazz);
		}
	}

	/**
	 * Test a class which should implement {@link Listener} and contain {@link EventHandler} annotations
	 * <p/>
	 * <em>Author: Feildmaster</em>
	 */
	private void testListener(Class clazz) {
		// Assert the class is even a listener
		Assert.assertTrue("Class: " + clazz.getSimpleName() + " does not implement Listener!", Listener.class.isAssignableFrom(clazz));

		for (Method method : clazz.getDeclaredMethods()) {
			// We only care about public functions.
			if (!Modifier.isPublic(method.getModifiers())) continue;
			// Don't mess with non-void
			if (!Void.TYPE.equals(method.getReturnType())) continue;
			// Only look for functions with 1 parameter
			if (method.getParameterTypes().length != 1) continue;

			// This is an event function...
			if (Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
				// Make sure @EventHandler is present!
				Assert.assertTrue(method.getName() + " is missing @EventHandler!", method.isAnnotationPresent(EventHandler.class));
			}
		}
	}
}
