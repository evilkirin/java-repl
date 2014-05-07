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
		} else if (line.equals("packages")) {
			showPackages();
			return true;
		} else if (line.startsWith("classes")) {
			showClasses(line);
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

	// TODO: Load a file
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

	public static class LoadableClasses {
		Reflections reflections;
		Set<String> allClasses;

		public LoadableClasses() {
			this.reflections =
				new Reflections(ClasspathHelper.forClass(Object.class),
												new SubTypesScanner(false));

			this.allClasses = reflections.getStore().getSubTypesOf(Object.class.getName());
		}

		public String[] packageNames() {
			Set<String> packages = new HashSet<String>();

			for (String fullClassName : allClasses) {
				packages.add(fullClassName.substring(0, fullClassName.lastIndexOf(".")));
			}

			return sortedStrings(packages);
		}

		public String[] classNames() {
			return new String[]{};
		}

		public String[] classNames(String packageName) {
			Set<String> classes = new HashSet<String>();

			for (String fullClassName : allClasses) {
				String _package = fullClassName.substring(0, fullClassName.lastIndexOf("."));
				if (_package.equals(packageName)) {
					classes.add(fullClassName.substring(fullClassName.lastIndexOf(".") + 1));
				}
			}

			return sortedStrings(classes);
		}

		public String[] publicClasses(String packageName) {
			try {
				String[] names = classNames(packageName);

				Set<String> publics = new HashSet<String>();

				for (String name : names) {
					if (Modifier.isPublic(Class.forName(packageName + "." + name).getModifiers())) {
						publics.add(name);
					}
				}

				return sortedStrings(publics);
			} catch (Exception e) {
				e.printStackTrace();
				return new String[]{};
			}
		}

		public String[] sortedStrings(Set<String> strings) {
			Object[] stringsAsObjects = strings.toArray();
			String[] arrayOfStrings   = Arrays.copyOf(stringsAsObjects, stringsAsObjects.length, String[].class);

			Arrays.sort(arrayOfStrings);

			return arrayOfStrings;
		}
	}

	void showPackages() {
		String[] packages = (new LoadableClasses()).packageNames();
		for (String _package : packages) {
			repl.out.println(_package);
		}
	}

	void showClasses(String line) {
		String _package  = line.substring(line.indexOf(" ") + 1).trim();
		String[] classes = (new LoadableClasses()).classNames(_package);
		for (String _class : classes) {
			repl.out.println(_class);
		}
	}

	void about() {
		this.repl.out.println("Java REPL by Farley Knight");
	}
}
