// NOTE: The Executor is a type of compiler that will only execute a line of code from the repl.
// It will not return a value.
package com.farleyknight;

import java.io.*;
import java.util.*;
import java.util.Map.*;

import java.net.URL;
import java.net.URLClassLoader;

import com.farleyknight.*;

public class Executor extends Compiler {
	Executor(Repl repl) {
		super(repl);
	}

	// Run the command that was typed at the prompt
	void run() {
		try {
			String directoryName = tempFile.getCanonicalPath();
			int length           = directoryName.indexOf(prefix);
			File directory       = new File(directoryName.substring(0, length));

			// TODO: If we want to load jars dynamically, we need to add the
			// file name for that jar to this list
			URL[] fileNames      = new URL[]{ directory.toURI().toURL() };
			ClassLoader loader   = new URLClassLoader(fileNames);

			Class<?> klass       = loader.loadClass(className);
			Object object        = klass.newInstance();
			// NOTE: The "doIt" method is only accessible for the SourceBuilder that
			// is used for Eval / Exec source builders. We may want to make this
			// a method on the SourceBuilder object instead of the Compiler.
			klass.getMethod("execute", Map.class).invoke(object, repl.variables);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	Object handleSuccess() {
		run();
		repl.out.print("=> ");
		repl.out.println("null");
		return null;
	}

	public static class ExecSourceBuilder extends SourceBuilder {
		ExecSourceBuilder(Repl repl, String line, String className) {
			super(repl, line, className);
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();

			// Add imports
			for (String _import : repl.imports) {
				builder.append(_import + ";\n");
			}
			// Necessary for variables
			builder.append("import java.util.Map;\n");

			// Add main source
			builder.append("public class " + className +" {\n");
			builder.append("  public void execute(Map<String, Object> variables) throws Exception {\n");

			for (Entry<String, Object> variable : repl.variables.entrySet()) {
				String type = FindType.getTypeNameFor(variable.getValue());
				builder.append("    " + type + " " + variable.getKey()
											 + " = (" + type + ") variables.get(\"" + variable.getKey() + "\");\n");
			}

			builder.append("    " + line + "\n;\n");
			builder.append("  }\n");
			builder.append("}\n");

			return builder.toString();
		}
	}

	String sourceCode(String line) {
		return (new ExecSourceBuilder(repl, line, className)).toString();
	}
}
