package com.farleyknight;

import com.farleyknight.Repl;
import com.farleyknight.Compiler;
import com.farleyknight.Command;

// TODO: When adding an import, compile to make sure
// the import statement would actually work!

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		Repl repl = new Repl();
		repl.loop();
	}
}
