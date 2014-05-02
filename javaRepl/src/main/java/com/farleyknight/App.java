package com.farleyknight;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import java.lang.instrument.*;
import java.lang.reflect.Field;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.fusesource.jansi.AnsiConsole;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;

import org.reflections.*;
import org.reflections.util.*;
import org.reflections.scanners.*;


// TODO: When adding an import, compile to make sure
// the import statement would actually work!

/**
 * Hello world!
 *
 */
public class App {
	public static class Repl {
		ConsoleReader reader;

		Repl() {
			try {
				this.reader = new ConsoleReader();
				this.reader.setPrompt(">>> ");
				this.reader.addCompleter(new StringsCompleter("about", "exit", "quit", "clear"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void tryRunning(String line, PrintWriter out) {
			Compiler compiler = new Compiler(out);
			String result     = compiler.compile(line);
			if (result.length() > 0) {
				if (result.indexOf("void cannot be converted to Object") != -1) {
					result = compiler.compile("null; " + line);
					if (result.length() > 0) {
						out.println("Couldn't parse source!");
						out.println(result);
					} else {
						out.print("=> ");
						out.println(compiler.load());
					}
				} else {
					out.println("Couldn't parse source!");
					out.println(result);
				}
			} else {
				out.print("=> ");
				out.println(compiler.load());
			}
		}

		public void loop() {
			try {
				String line;
				PrintWriter out = new PrintWriter(reader.getOutput());
				out.println("Java REPL by Farley Knight");

				while ((line = reader.readLine()) != null) {
					if (line.length() > 0) {
						// TODO: We cannot capture multi-line method calls and definitions
						// if we sent the text straight to be executed. We may have to read
						// multiple lines and append them together to get the full method
						// call.
						Command cmd     = new Command(out, reader);
						boolean success = cmd.run(line.trim());
						if (!success) {
							tryRunning(line, out);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class Command {
		PrintWriter out;
		ConsoleReader reader;

		Command(PrintWriter out, ConsoleReader reader) {
			this.out    = out;
			this.reader = reader;
		}

		boolean run(String line) {
			if (line.equals("about")) {
				about();
				return true;
			} else if (line.equals("quit")) {
				System.exit(0);
			} else if (line.equals("exit")) {
				System.exit(0);
			} else if (line.equals("clear")) {
				clear();
				return true;
			} else if (line.equals("classes")) {
				showClasses();
				return true;
			}

			return false;
		}

		void clear() {
			try {
				this.reader.clearScreen();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		void showClasses() {
			Reflections reflections =
				new Reflections(ClasspathHelper.forClass(Object.class),
												new SubTypesScanner(false));

			Set<String> allClasses =
				reflections.getStore().getSubTypesOf(Object.class.getName());

			for (String className : allClasses) {
				out.print(className + ",");
			}
			out.println();
		}

		void about() {
			out.println("Java REPL by Farley Knight");
		}
	}

	public static class SourceBuilder {
		String line;
		String className;

		SourceBuilder(String line, String className) {
			this.line      = line;
			this.className = className;
		}

		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("public class " + className +" {\n");
			builder.append("  public Object doIt() {\n");
			builder.append("    Object result = " + line + ";\n");
			builder.append("    return result;\n");
			builder.append("  }\n");
			builder.append("}\n");
			return builder.toString();
		}
	}

	public static class Compiler {
		File tempFile;
		String line;
		String fileName;
		String className;
		String prefix;
		PrintWriter out;
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		Compiler(PrintWriter out) {
			try {
				this.prefix    = "REPL$";
				this.out       = out;
				this.tempFile  = File.createTempFile(this.prefix, ".java");
				this.fileName  = tempFile.getName();
				this.className = fileName.substring(0, fileName.indexOf(".java"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String sourceCode(String line) {
			return (new SourceBuilder(line, className)).toString();
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

		Object load() {
			try {
				String directoryName = tempFile.getCanonicalPath();
				int length           = directoryName.indexOf(prefix);
				File directory       = new File(directoryName.substring(0, length));
				ClassLoader loader   = new URLClassLoader(new URL[]{
						directory.toURI().toURL()
					});

				Class<?> _class = loader.loadClass(className);
				Object object = _class.newInstance();
				Object result = _class.getMethod("doIt").invoke(object);

				return result;
			} catch (Exception e) {
				e.printStackTrace();
			}

			// NOTE: Should never get here!
			return null;
		}
	}

	public static void main(String[] args) {
		Repl repl = new Repl();
		repl.loop();
	}
}
