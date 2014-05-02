package com.farleyknight;

import java.io.PrintWriter;

import org.fusesource.jansi.AnsiConsole;
import jline.console.ConsoleReader;

// import static org.fusesource.jansi.Ansi.*;
// import static org.fusesource.jansi.Ansi.Color.*;


// TODO: When adding an import, compile to make sure
// the import statement would actually work!

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		//System.out.println("Hello World!");
		try {
			ConsoleReader reader = new ConsoleReader();
			reader.setPrompt(">>> ");

			String line;
			PrintWriter out = new PrintWriter(reader.getOutput());

			while ((line = reader.readLine()) != null) {
				out.println("The line " + line.toString() + " has " + line.length() + " characters.");
				if (line.equals("quit")) {
					System.exit(0);
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
