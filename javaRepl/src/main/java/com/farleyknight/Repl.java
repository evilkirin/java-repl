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

	public Object compile(String line) {
		Compiler compiler = new Compiler(this);
		return compiler.tryRecoverableCompile(line);
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
