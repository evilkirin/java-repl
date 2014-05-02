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

	public static class Error {
		public String message;

		Error(String message) {
			this.message = message;
		}
	}

	public Object tryRecoverableCompile(String line) {
		String error = compile(line);
		if (error.length() > 0) {
			// NOTE: If the user provides a statement like "System.out.println"
			// which has return type of "void", then we will get an error
			// "void cannot be converted to Object".
			//
			// Our response? To test for that error. If we get that error, we
			// can re-write the expression to return "null; <user's command>",
			// so it won't give the result of the command, but at least it will
			// run it.
			if (recoverable(error)) {
				error = compile("null;\n" + line);
				if (error.length() > 0) {
					repl.out.println("Couldn't parse source!");
					repl.out.println(error);
					return new Error(error);
				} else {
					Object result = run();
					repl.out.print("=> ");
					repl.out.println(result);
					return result;
				}
			} else {
				repl.out.println("Couldn't parse source!");
				repl.out.println(error);
				return new Error(error);
			}
		} else {
			Object result = run();
			repl.out.print("=> ");
			repl.out.println(result);
			return result;
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

	boolean recoverable(String error) {
		if (error.indexOf("void cannot be converted to Object") != -1) {
			return true;
		}

		if (error.indexOf("illegal start of expression") != -1) {
			return true;
		}

		return false;
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
