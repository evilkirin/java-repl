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
		} else if (line.startsWith("type")) {
			showType(line);
			return true;
		}

		return false;
	}

	void methods(String line) {
		Evaluator eval = new Evaluator(repl);
		String error = eval.compile(line.substring(line.indexOf(" ") + 1).trim());
		if (error.length() > 0) {
			repl.out.println("Couldn't compile expression!");
			repl.out.println(error);
		} else {
			repl.out.print(new MethodsFormatter(eval.run()));
		}
	}

	void showType(String line) {
		Evaluator eval = new Evaluator(repl);
		String error   = eval.compile(line.substring(line.indexOf(" ") + 1).trim());
		if (error.length() > 0) {
			repl.out.println("Couldn't compile expression!");
			repl.out.println(error);
		} else {
			repl.out.println(eval.run().getClass());
		}
	}

	// TODO: Test if the import would actually work before adding it!
	// The user could add a bunch of garbage and would crash any new
	// commands that are entered.
	public void addImport(String line) {
		Importer importer = new Importer(repl);
		String error      = importer.compile(line.trim()).trim();

		if (error.length() > 0) {
			repl.out.println("Couldn't import!");
			repl.out.println(error);
		} else {
			repl.imports.add(line);
		}
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
