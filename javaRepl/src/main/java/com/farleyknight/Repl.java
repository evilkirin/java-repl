package com.farleyknight;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.farleyknight.Command;

import jline.console.ConsoleReader;
import jline.console.completer.StringsCompleter;

import org.fusesource.jansi.AnsiConsole;

public class Repl {
	public ArrayList<String>  imports;
	public ConsoleReader      reader;
	public PrintWriter        out;

	public Map<String, Object> variables = new HashMap<String, Object>();
	public Pattern declareVarPattern     = Pattern.compile("^([a-z][a-zA-Z0-9]*|[a-z\\$][a-zA-Z0-9]+) *=[^=].*$");

	Repl() {
		try {
			this.imports = new ArrayList<String>();

			this.reader = new ConsoleReader();
			this.reader.setPrompt(">>> ");
			this.reader.addCompleter(new StringsCompleter("about", "exit", "quit", "clear", "import", "vars"));

			this.out = new PrintWriter(reader.getOutput());
			this.out.println("Java REPL by Farley Knight");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void compileFile(String fileName) {
		try {
			StringBuilder builder = new StringBuilder();
			String line;

			BufferedReader fileReader = new BufferedReader(new FileReader(fileName));

			while ((line = fileReader.readLine()) != null) {
				builder.append(line);
			}

			compile(builder.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addImport(String line) {
		this.imports.add(line);
	}

	public static class Error {
		public String message;

		Error(String message) {
			this.message = message;
		}
	}

	public Object compile(String line) {
		Compiler compiler = new Compiler(this);
		String error      = compiler.compile(line);
		if (error.length() > 0) {
			// NOTE: If the user provides a statement like "System.out.println"
			// which has return type of "void", then we will get an error
			// "void cannot be converted to Object".
			//
			// Our response? To test for that error. If we get that error, we
			// can re-write the expression to return "null; <user's command>",
			// so it won't give the result of the command, but at least it will
			// run it.
			if (compiler.recoverable(error)) {
				error = compiler.compile("null; " + line);
				if (error.length() > 0) {
					out.println("Couldn't parse source!");
					out.println(error);
					return new Error(error);
				} else {
					Object result = compiler.run();
					out.print("=> ");
					out.println(result);
					return result;
				}
			} else {
				out.println("Couldn't parse source!");
				out.println(error);
				return new Error(error);
			}
		} else {
			Object result = compiler.run();
			out.print("=> ");
			out.println(result);
			return result;
		}
	}

	public void declareVar(String line) {
		String name       = line.substring(0, line.indexOf('=')).trim();
		String expression = line.substring(line.indexOf('=') + 1).trim();

		out.println("Declaring name " + name);

		Object result = compile(expression);
		if (result.getClass() != Error.class) {
			variables.put(name, result);
		}
	}

	public void loop() {
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) {
					// TODO: We cannot capture multi-line method calls and definitions
					// if we sent the text straight to be executed. We may have to read
					// multiple lines and append them together to get the full method
					// call.
					boolean success = (new Command(this)).run(line);
					if (!success) {
						if (declareVarPattern.matcher(line).matches()) {
							declareVar(line);
						} else {
							compile(line);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
