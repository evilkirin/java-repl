package com.farleyknight;

import java.io.*;
import java.util.*;

import java.net.URL;
import java.net.URLClassLoader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class Compiler {
	File tempFile;
	String line;
	String fileName;
	String className;
	String prefix;
	Repl repl;
	JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

	Compiler(Repl repl) {
		try {
			this.prefix    = "REPL$";
			this.repl      = repl;
			this.tempFile  = File.createTempFile(this.prefix, ".java");
			this.fileName  = tempFile.getName();
			this.className = fileName.substring(0, fileName.indexOf(".java"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	SourceBuilder sourceBuilder(String line) {
		return new SourceBuilder(repl.imports, line, className);
	}

	String sourceCode(String line) {
		return sourceBuilder(line).toString();
	}

	void writeSourceFile(String line) {
		try {
			PrintWriter file = new PrintWriter(new FileOutputStream(tempFile));
			file.println(sourceCode(line));
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String compile(String line) {
		try {
			writeSourceFile(line);
			OutputStream compilerError = new ByteArrayOutputStream();
			int exitCode =
				this.compiler.run(null, null, compilerError,
													new String[] { tempFile.getCanonicalPath() });
			if (exitCode == 0) {
				return "";
			} else {
				return compilerError.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// NOTE: Should never get here!
		return "";
	}

	// Run the command that was typed at the prompt
	Object run() {
		try {
			String directoryName = tempFile.getCanonicalPath();
			int length           = directoryName.indexOf(prefix);
			File directory       = new File(directoryName.substring(0, length));
			ClassLoader loader   = new URLClassLoader(new URL[]{
					directory.toURI().toURL()
				});

			Class<?> klass  = loader.loadClass(className);
			Object object   = klass.newInstance();
			Object result   = klass.getMethod("doIt").invoke(object);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// NOTE: Should never get here!
		return null;
	}

	public static class SourceBuilder {
		List<String> imports;
		String line;
		String className;

		SourceBuilder(List<String> imports, String line, String className) {
			this.imports   = imports;
			this.line      = line;
			this.className = className;
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();

			for (String _import : imports) {
				builder.append(_import + ";\n");
			}

			builder.append("public class " + className +" {\n");
			builder.append("  public Object doIt() {\n");
			builder.append("    Object result = " + line + ";\n");
			builder.append("    return result;\n");
			builder.append("  }\n");
			builder.append("}\n");
			return builder.toString();
		}
	}
}
