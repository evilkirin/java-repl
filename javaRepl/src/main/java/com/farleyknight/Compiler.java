package com.farleyknight;

import java.io.*;
import java.util.*;
import java.util.Map.*;

import java.net.URL;
import java.net.URLClassLoader;

import java.lang.reflect.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.lang.reflect.Field;

import com.farleyknight.*;

public class Compiler {
	File   tempFile;
	Repl   repl;
	String line;
	String fileName;
	String className;
	String prefix;
	String error;

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

	String sourceCode(String line) throws Exception {
		throw new Exception("Cannot use abstract method sourceCode!");
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
			String filePath            = tempFile.getCanonicalPath();
			int exitCode               =
				this.compiler.run(null, null, compilerError,
													new String[] { filePath });
			if (exitCode == 0) {
				this.error = "";
			} else {
				this.error = compilerError.toString().replace(filePath, "source");
			}

			return this.error;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public Object handleError() {
		try {
			repl.out.println("Couldn't parse source!");
			repl.out.println(this.error);
			return new Error(this.error);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean recoverable(String error) {
		if (error.indexOf("void cannot be converted to Object") != -1) {
			return true;
		}

		if (error.indexOf("illegal start of expression") != -1) {
			return true;
		}

		return false;
	}

	public static class Error {
		public String message;

		Error(String message) {
			this.message = message;
		}
	}


	public static class FindType {
		public static String getTypeNameFor(Object object) {
			return getTypeName(object.getClass());
		}

		public static Class getNextPublicClass(final Class<?> klass) {
			if (Modifier.isPublic(klass.getModifiers())) {
				return klass;
			} else {
				return getNextPublicClass(klass.getSuperclass());
			}
		}

		public static String getTypeName(final Class<?> klass) {
			Class myClass = getNextPublicClass(klass);

			if (Proxy.isProxyClass(myClass)) {
				return myClass.getInterfaces()[0].getCanonicalName();
			}

			String name = myClass.getCanonicalName();

			if (name != null) {
				return name;
			}

			if (myClass.getSuperclass() != Object.class) {
				name = myClass.getSuperclass().getCanonicalName();
				if (name != null) {
					return name;
				}
			}

			if (myClass.getInterfaces().length >= 1) {
				name = myClass.getInterfaces()[0].getCanonicalName();
				if (name != null) {
					return name;
				}
			}

			return "Object";
		}
	}
}
