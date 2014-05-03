package com.farleyknight;

import java.util.*;
import java.util.Map.Entry;

import org.reflections.*;
import org.reflections.util.*;
import org.reflections.scanners.*;

import java.lang.reflect.*;

import com.farleyknight.*;


public class Command {
	public Repl repl;

	Command(Repl repl) {
		this.repl = repl;
	}

	boolean run(String line) {
		if (line.equals("about")) {
			about();
			return true;
		} else if (line.equals("quit") || line.equals("exit")) {
			System.exit(0);
			return true;
		} else if (line.equals("vars")) {
			showVars();
			return true;
		} else if (line.equals("clear")) {
			clear();
			return true;
		} else if (line.startsWith("import")) {
			addImport(line);
			return true;
		} else if (line.startsWith("methods")) {
			methods(line);
			return true;
		} else if (line.startsWith("load")) {
			loadFile(line);
			return true;
		} else if (line.startsWith("class")) {
			showClass(line);
			return true;
		}

		return false;
	}

	public static class MethodsFormatter {
		Method[] methods;
		Map<String, ArrayList<Method>> methodGrouping
			= new HashMap<String, ArrayList<Method>>();

		MethodsFormatter(Object object) {
			this.methods = object.getClass().getMethods();

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
					String args = Inspector.inspect(m.getParameterTypes())
						.replace("[", "(").replace("]", ")");
					builder.append("   " + m.getName() + args + "\n");
				}
				builder.append("\n");
			}

			return builder.toString();
		}
	}

	void methods(String line) {
		Compiler compiler = new Compiler(repl);
		String error = compiler.compile(line.substring(line.indexOf(" ") + 1).trim());
		if (error.length() > 0) {
			repl.out.println("Couldn't compile expression!");
			repl.out.println(error);
		} else {
			repl.out.print(new MethodsFormatter(compiler.run()));
			/*
			for (Method method : object.getClass().getMethods()) {
				repl.out.println(method);
			}
			*/
		}
	}

	void showClass(String line) {
		Compiler compiler = new Compiler(repl);
		String error = compiler.compile(line.substring(line.indexOf(" ") + 1).trim());
		if (error.length() > 0) {
			repl.out.println("Couldn't compile expression!");
			repl.out.println(error);
		} else {
			Object object = compiler.run();
			repl.out.println(object.getClass());
		}
	}

	void addImport(String line) {
		this.repl.addImport(line);
	}

	// Load a file
	void loadFile(String line) {
		this.repl.compileFile(line.substring(line.indexOf(" ") + 1));
	}

	void clear() {
		try {
			repl.reader.clearScreen();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void showVars() {
		for (Entry<String, Object> variable : repl.variables.entrySet()) {
			repl.out.println(variable.getKey() + " => " + variable.getValue());
		}
	}

	/*
	// NOTE: This is a **huge** list of classes. Find a way to narrow
	// the list down to "normal" usage.
	void showClasses() {
		Reflections reflections =
			new Reflections(ClasspathHelper.forClass(Object.class),
											new SubTypesScanner(false));

		Set<String> allClasses = reflections.getStore().getSubTypesOf(Object.class.getName());

		for (String className : allClasses) {
			this.repl.out.print(className + ",");
		}

		this.repl.out.println();
	}
	*/

	void about() {
		this.repl.out.println("Java REPL by Farley Knight");
	}
}
