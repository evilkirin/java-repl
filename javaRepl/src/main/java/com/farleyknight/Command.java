package com.farleyknight;

import java.util.*;
import java.util.Map.Entry;

import org.reflections.*;
import org.reflections.util.*;
import org.reflections.scanners.*;


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
		} else if (line.startsWith("load")) {
			loadFile(line);
			return true;
		} else if (line.equals("classes")) {
			showClasses();
			return true;
		}

		return false;
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

	// NOTE: This is a **huge** list of classes. Find a way to narrow
	// the list down to "normal" usage.
	void showClasses() {
		Reflections reflections =
			new Reflections(ClasspathHelper.forClass(Object.class),
											new SubTypesScanner(false));

		Set<String> allClasses =
			reflections.getStore().getSubTypesOf(Object.class.getName());

		for (String className : allClasses) {
			this.repl.out.print(className + ",");
		}

		this.repl.out.println();
	}

	void about() {
		this.repl.out.println("Java REPL by Farley Knight");
	}
}
