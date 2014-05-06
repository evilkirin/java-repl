
package com.farleyknight;

import com.farleyknight.Repl;

public class SourceBuilder {
	Repl   repl;
	String line;
	String className;

	SourceBuilder(Repl repl, String line, String className) {
		this.repl      = repl;
		this.line      = line;
		this.className = className;
	}

}
