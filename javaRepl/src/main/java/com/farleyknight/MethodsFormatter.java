package com.farleyknight;

import java.util.*;
import java.util.*;
import java.util.Map.*;

import java.lang.reflect.*;

import com.farleyknight.*;

public class MethodsFormatter {
	Object object;
	String className;
	Method[] methods;

	// TODO: We should strive for a format like this:
	//
	// Defined on: class Foo
	//   static methods:
	//     Foo.staticMethod()
	//
	//   instance methods:
	//     instanceMethod()

	Map<String, ArrayList<Method>> methodGrouping
		= new HashMap<String, ArrayList<Method>>();

	MethodsFormatter(Object object) {
		this.object = object;

		if (object instanceof Class) {
			this.methods = ((Class) object).getMethods();
			this.className = ((Class) object).getName();
		} else {
			this.methods = object.getClass().getMethods();
			this.className = object.getClass().getName();
		}

		for (Method m : this.methods) {
			String klass = m.getDeclaringClass().toString();
			if (this.methodGrouping.get(klass) == null) {
				this.methodGrouping.put(klass, new ArrayList<Method>());
			}

			this.methodGrouping.get(klass).add(m);
		}
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (Entry<String, ArrayList<Method>> entry : this.methodGrouping.entrySet()) {
			builder.append("Defined on: " + entry.getKey() + "\n");
			for (Method m : entry.getValue()) {
				if (Modifier.isStatic(m.getModifiers())) {
					builder.append("   " + className + ".");
				} else {
					builder.append("   ");
				}

				String args = Inspector.inspect(m.getParameterTypes())
					.replace("[", "(").replace("]", ")");
				builder.append(m.getName() + args + " => ");
				builder.append(m.getReturnType().getName() + "\n");
			}
			builder.append("\n");
		}

		return builder.toString();
	}
}
