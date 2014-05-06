// NOTE: The Importer is a type of compiler that will only attempt to import a directive
// given at the repl. It does not execute or evaluate any actual code.

package com.farleyknight;

import java.io.*;

import com.farleyknight.*;

public class Importer extends Compiler {
	Importer(Repl repl) {
		super(repl);
	}

	String sourceCode(String line) {
		return (new ImporterSourceBuilder(repl, line, className)).toString();
	}

	public static class ImporterSourceBuilder extends SourceBuilder {
		ImporterSourceBuilder(Repl repl, String line, String className) {
			super(repl, line, className);
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append(line + ";\n");

			// Add main source
			builder.append("public class " + className +" {\n");
			builder.append("}\n");

			return builder.toString();
		}
	}
}
