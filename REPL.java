import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

// import org.fusesource.jansi.AnsiConsole;
// import static org.fusesource.jansi.Ansi.*;
// import static org.fusesource.jansi.Ansi.Color.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * A Read-Eval-Print-Loop for Java.
 */
public class REPL implements Runnable {

	private final static String prefix = "REPL$";

	final BufferedReader reader;
	final PrintStream out;
	final PrintStream err;

	private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private final Map<String, Object> variables = new HashMap<String, Object>();
	private final Set<String> imports = new HashSet<String>();

	private final Pattern importPattern = Pattern
		.compile("^import (static )?([a-z][a-zA-Z]*)(.[a-zA-Z_][a-zA-Z0-9_\\$]*)*(.\\*)?;?$");
	private final Pattern declareVariable = Pattern.compile("^([a-z][a-zA-Z0-9]*|[a-z\\$][a-zA-Z0-9]+) *=[^=].*$");

	private Object lastException = null;
	private Object lastResult = null;
	private String lastSource = "";

	/**
	 * Creates the Read-Eval-Print-Loop. Typically will be invoked like this:
	 * <code>new REPL(System.in, System.out, System.err).run();</code>
	 */
	public REPL(final InputStream in, final OutputStream out, final OutputStream err, final String... imports) {
		reader = new BufferedReader(new InputStreamReader(in));

		this.out = out instanceof PrintStream ? (PrintStream) out : new PrintStream(out);
		this.err = err instanceof PrintStream ? (PrintStream) err : new PrintStream(err);

		if ((imports == null) || (imports.length == 0)) {
			this.imports.add("import static java.lang.Math.*");
			this.imports.add("import java.math.*");
			this.imports.add("import java.util.*");
		} else {
			for (String _import : imports) {
				if (this.importPattern.matcher(_import).matches()) {
					this.imports.add(_import);
				}
			}
		}
	}

	/**
	 * Adds an import to this REPL (like \"import ...\" in the shell).
	 */
	public void addImport(final String _import) {
		imports.add("import " + _import);
	}

	/**
	 * Adds a variable to this REPL (like \"a = ...\").
	 */
	public void addVar(final String name, final Object object) {
		variables.put(name, object);
	}

	/**
	 * Starts the Read-Eval-Print-Loop.
	 */
	@Override
	public void run() {
		for (;;) {
			try {
				loop();
			} catch (Exception exc) {
				exc.printStackTrace(err);
			} finally {
				tmpFile.delete();
			}
		}
	}

	/**
	 * Each expression is compiled in a new class, this is the base class for
	 * these classes.
	 */
	public abstract static class BaseClass {
		private REPL repl = null;

		public void setREPL(final REPL repl) {
			this.repl = repl;
		}

		public void println(final Object object) {
			repl.out.println(object);
		}

		public void println() {
			repl.out.println();
		}
	}

	private File tmpFile;
	private PrintWriter file;
	private String className;

	private void prepare() throws Exception {
		tmpFile = File.createTempFile(prefix, ".java");

		String fileName = tmpFile.getName();
		className = fileName.substring(0, fileName.indexOf(".java"));

		file = new PrintWriter(new FileOutputStream(tmpFile));
		for (String _import : imports) {
			file.println(_import + ";");
		}
		file.println("public class " + className + " extends REPL.BaseClass {");
		file.println("  public Object doIt(final java.lang.Object _, final java.util.Map<java.lang.String,java.lang.Object> $$) throws Exception {");
		for (Entry<String, Object> variable : variables.entrySet()) {
			String type = getTypeName(variable.getValue().getClass());
			file.println("    " + type + " " + variable.getKey()
									 + " = (" + type + ") $$.get(\"" + variable.getKey() + "\");");
		}
		file.print("    final java.lang.Object $ = ");
	}

	private String compile(String expression) throws Exception {
		if ((lastResult != void.class) && (lastResult != null)) {
			String type = getTypeName(lastResult.getClass());
			expression = expression.replaceAll("_", "((" + type + ")_)");
		}

		file.print(expression);

		file.println(";\n    return $;\n  }\n}");
		file.close();

		OutputStream compilerError = new ByteArrayOutputStream();

		int exitCode = compiler.run(null, null, compilerError, new String[]{tmpFile.getCanonicalPath()});
		return (exitCode == 0) ? "" : compilerError.toString();
	}

	private static String getTypeName(final Class<?> _class) {
		if (Proxy.isProxyClass(_class)) {
			return _class.getInterfaces()[0].getCanonicalName();
		}
		String name = _class.getCanonicalName();
		if (name != null) {
			return name;
		}
		if (_class.getSuperclass() != Object.class) {
			name = _class.getSuperclass().getCanonicalName();
			if (name != null) {
				return name;
			}
		}
		if (_class.getInterfaces().length >= 1) {
			name = _class.getInterfaces()[0].getCanonicalName();
			if (name != null) {
				return name;
			}
		}
		return "Object";
	}

	private void help() {
		out.println("Commands: (use \"help <command>\" for further info)");
		out.println("  about, clear, exit, gc, help, import,");
		out.println("  imports, meminfo, source, trace, vars");
		out.println("Usage:");
		out.println("  Just enter a Java expression (\"help example\" for an example).");
		out.println("  a = ... declares a variable (\"vars\" for an overview of declared variables)");
		out.println("  _ always contains the last non-null result.");
	}

	private void help(final String topic) {
		if (topic.equals("import")) {
			out.println("import [static] <packages>.<Class>[.*] / <packages>.*");
		} else if (topic.equals("help")) {
			out.println("help <topic>, e.g. \"help import\"");
		} else if (topic.equals("meminfo")) {
			out.println("Information about current heap-size.");
		} else if (topic.equals("gc")) {
			out.println("Calls \"System.gc()\" twice.");
		} else if (topic.equals("imports")) {
			out.println("Shows all current imports.");
		} else if (topic.equals("vars")) {
			out.println("Enumerates all currently assigned variables.");
			out.println("Simply type the name of a variable to show it’s value.");
		} else if (topic.equals("trace")) {
			out.println("Show detailed information on the last error.");
		} else if (topic.equals("exit")) {
			out.println("Exits the REPL (using \"System.exit(0);\").");
		} else if (topic.equals("source")) {
			out.println("Show the last generated source.");
		} else if (topic.equals("clear")) {
			out.println("Clears imports and variables.");
			out.println("  Use \"clear imports\" or \"clear vars\"\n  to clear only imports / vars.");
		} else if (topic.equals("example")) {
			out.println("Swing-Example:");
			out.println("  import javax.swing.*");
			out.println("  frame = new JFrame()");
			out.println("  button = new JButton(\"Hello World\")");
			out.println("  frame.setContentPane(button)");
			out.println("  frame.pack()");
			out.println("  frame.setVisible(true)");
			out.println("  button.setText(\"Another text\")");
			out.println();

			out.println("Anonymous class:");
			out.println("  new Thread(new Runnable(){");
			out.println("  public void run(){");
			out.println("  System.out.println(\"Hello from the background thread\");");
			out.println("  }}).start()");

			out.println();
		} else {
			out.println("Unknown topic.");
		}
		out.flush();
	}

	private void about() {
		out.println("Java-REPL, by Julian Fleischer");
	}

	private void vars() {
		if (variables.isEmpty()) {
			out.println("No variables defined.");
		}
		for (Entry<String, Object> entry : variables.entrySet()) {
			out.printf("%s : %s\n", entry.getKey(), getTypeName(entry.getValue().getClass()));
		}
	}

	private void imports() {
		for (String _import : imports) {
			out.println(_import);
		}
	}

	private void meminfo() {
		out.println(new MemoryInfo());
	}

	private void clear() {
		clearVars();
		clearImports();
	}

	private void clearVars() {
		variables.clear();
	}

	private void clearImports() {
		imports.clear();
	}

	private static void exit() {
		System.exit(0);
	}

	private String compilerMessage(final String message) {
		try {
			return message
				.replace(tmpFile.getCanonicalPath(), "source");
		} catch (IOException exc) {
			return message;
		}
	}

	private void showResult(final Object result) {
		out.print("=> ");
		if (result instanceof Object[]) {
			out.println("{" + implode(", ", (Object[]) result) + "}");
			return;
		}
		out.println(result);
	}

	public static String safe(final String line) {
		StringBuilder string = new StringBuilder();
		boolean inString = false;
		boolean inEscape = false;
		for (int i = 0; i < line.length(); i++) {
			if (inString) {
				switch (line.charAt(i)) {
				case '"':
					inString = inEscape;
					break;
				case '\\':
					inEscape = true;
					continue;
				}
				inEscape = false;
			} else {
				if (line.charAt(i) == '"') {
					inString = true;
				} else {
					string.append(line.charAt(i));
				}
			}
		}
		return string.toString();
	}

	private void parseBraces(StringBuilder lines)  {
		try {
			LinkedList<Character> braces = new LinkedList<Character>();
			do {
				out.print(">>> ");
				String line = reader.readLine();
				if (line == null) {
					exit();
					return;
				}
				lines.append(line);
				String safeLine = safe(line);
				for (int i = 0; i < safeLine.length(); i++) {
					char _char = safeLine.charAt(i);
					switch (_char) {
					case '(':
					case '{':
					case '[':
						braces.push(_char);
						break;
					case ')':
						if (braces.isEmpty() || (braces.pop() != '(')) {
							err.println("Mismatched parenthesis.");
							return;
						}
						break;
					case '}':
						if (braces.isEmpty() || (braces.pop() != '{')) {
							err.println("Mismatched curly brace.");
							return;
						}
						break;
					case ']':
						if (braces.isEmpty() || (braces.pop() != '[')) {
							err.println("Mismatched bracket.");
							return;
						}
						break;
					}
				}
			} while (!braces.isEmpty());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loop() throws Exception {
		prepare();

		StringBuilder lines = new StringBuilder();

		parseBraces(lines);

		String line = lines.toString().trim();
		if (line.equals("exit")) {
			exit();
			return;
		} else if (line.equals("help")) {
			help();
			return;
		} else if (line.startsWith("help ")) {
			String topic = line.substring(5);
			help(topic);
			return;
		} else if (importPattern.matcher(line).matches()) {
			int size = imports.size();
			imports.add(line);
			if (imports.size() > size) {
				out.println("Added " + line + " (use \"clear\" to undo)");
			}
			return;
		} else if (line.equals("about")) {
			about();
			return;
		} else if (line.equals("trace")) {
			if (lastException instanceof Throwable) {
				((Throwable) lastException).printStackTrace(out);
			} else if (lastException instanceof String) {
				out.println(lastException);
			}
			return;
		} else if (line.equals("vars")) {
			vars();
			return;
		} else if (line.equals("imports")) {
			imports();
			return;
		} else if (line.equals("clear")) {
			clear();
			return;
		} else if (line.equals("clear imports")) {
			clearImports();
			return;
		} else if (line.equals("clear vars")) {
			clearVars();
			return;
		} else if (line.equals("source")) {
			out.println(lastSource);
			return;
		} else if (line.equals("gc")) {
			System.gc();
			System.gc();
			return;
		} else if (line.equals("meminfo")) {
			meminfo();
			return;
		}

		String expression = line;
		if (declareVariable.matcher(line).matches()) {
			expression = line.substring(line.indexOf('=') + 1).trim();
		}

		String firstError = null;
		String error = compile(expression);
		lastSource = fileGetContents(tmpFile);

		if (!error.isEmpty()) {
			firstError = compilerMessage(error);
			prepare();
			error = compile("void.class;\n" + expression);
			lastSource = fileGetContents(tmpFile);
		}
		if (!error.isEmpty()) {
			err.println("Compilation failed (use \"trace\" for diagnostics).");
			lastException = firstError;
			return;
		}

		String directoryName = tmpFile.getCanonicalPath();
		File directory = new File(directoryName.substring(0, directoryName.indexOf(prefix)));

		ClassLoader loader = new URLClassLoader(new URL[]{directory.toURI().toURL()});

		File classFile = new File(directory.getCanonicalPath() + '/' + className + ".class");

		Object object = null;
		try {
			executeConsoleCode(object, line, loader);
		} catch (Throwable exc) {
			handleException(exc);
		} finally {
			object = null; // Clear reference to the only instance
			loader = null; // Clear reference to the class loader

			classFile.delete();
		}
	}

	void executeConsoleCode(Object object, String line, ClassLoader loader) throws Exception {
		Class<?> _class = loader.loadClass(className);
		object = _class.newInstance();
		_class.getMethod("setREPL", getClass()).invoke(object, this);
		Object result = _class
			.getMethod("doIt", Object.class, Map.class)
			.invoke(object, lastResult, variables);
		if (result != void.class) {
			if (result != null) {
				if (declareVariable.matcher(line).matches()) {
					String variableName = line.substring(0, line.indexOf('=')).trim();
					variables.put(variableName, result);
				}
				lastResult = result;
			}
			showResult(result);
		}
	}

	void handleException(Throwable exc) {
		if (exc instanceof InvocationTargetException) {
			exc = exc.getCause();
		}
		lastException = exc;
		err.printf("%s: %s (use \"trace\" for details)\n", exc.getClass().getSimpleName(), exc.getMessage());
	}

	/**
	 * For Convenience: Lets you directly start the REPL.
	 */
	public static void main(final String... args) throws Exception {
		System.out.println("Java Read-Eval-Print-Loop (better: Read-Compile-Execute), \"help\" for help.");

		// AnsiConsole.systemInstall();
		new REPL(System.in, System.out, System.err).run();
		// AnsiConsole.systemUninstall();
	}

	private class MemoryInfo {

		private final long total;
		private final long free;

		public MemoryInfo() {
			total = Runtime.getRuntime().totalMemory();
			free = Runtime.getRuntime().freeMemory();
		}

		@Override
		public String toString() {
			return toString(1, "Bytes");
		}

		public String toString(final int factor, final String unit) {
			return "Memory (total, used, free): "
				+ (total / factor) + " " + unit + ", "
				+ ((total - free) / factor) + " " + unit + ", "
				+ (free / factor) + " " + unit;
		}
	}

	private static byte[] getBytes(final File file) {
		try {
			InputStream f = new FileInputStream(file);
			int size = (int) file.length();
			byte[] result = new byte[size];
			f.read(result, 0, size);
			return result;
		} catch (FileNotFoundException exc) {
			return null;
		} catch (IOException exc) {
			return null;
		}
	}

	private static String fileGetContents(final File file) {
		return new String(getBytes(file));
	}

	private static StringBuilder implode(final String delimiter, final Object[] strings,
																			 final StringBuilder result) {
		if ((strings == null) || (delimiter == null) || (result == null)) {
			throw new IllegalArgumentException();
		}
		if (strings.length > 0) {
			result.append(strings[0]);
			for (int i = 1; i < strings.length; i++) {
				result.append(delimiter);
				result.append(strings[i] == null ? "null" : strings[i].toString());
			}
		}
		return result;
	}

	private static String implode(final String delimiter, final Object[] strings) {
		return implode(delimiter, strings, new StringBuilder()).toString();
	}

}
