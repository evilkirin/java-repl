package com.farleyknight;

import java.lang.reflect.Field;

public class Inspector {
	public static String inspectInts(int[] ints) {
		StringBuilder array = new StringBuilder();
		array.append("[");

		for (int i = 0; i < ints.length; i++) {
			array.append(ints[i]);
			if ((i+1) < ints.length) {
				array.append(", ");
			}
		}

		array.append("]");
		return array.toString();
	}

	public static String inspectObjects(Object[] objects) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");

		for (int i = 0; i < objects.length; i++) {
			builder.append(inspect(objects[i]));
			if ((i+1) < objects.length) {
				builder.append(", ");
			}
		}

		builder.append("]");
		return builder.toString();
	}

	public static String deepInspect(Object object) {
		StringBuilder builder = new StringBuilder();
		builder.append(object.getClass().getName());
		Field[] fields = object.getClass().getFields();

		if (fields.length == 0)
			return builder.toString();

		builder.append(" {");

		try {
			for (int i = 0; i < fields.length; i++) {
				builder.append(fields[i].getName());
				builder.append(" => ");
				builder.append(fields[i].get(object));

				if ((i+1) < fields.length) {
					builder.append(", ");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		builder.append("} ");
		return builder.toString();
	}

	public static String basicInspect(Object object) {
		if ((object instanceof Class)   ||
				(object instanceof Boolean) ||
				(object instanceof Integer) ||
				(object instanceof Double)  ||
				(object instanceof Float)   ||
				(object instanceof Byte)    ||
				(object instanceof Character)) {
			return object.toString();
		} else if (object.getClass() == String.class) {
			return "\"" + object + "\"";
		} else {
			return null;
		}
	}

	public static String inspect(Object object) {
		String result = basicInspect(object);

		if (result != null) {
			return result;
		} else {
			return extendedInspect(object);
		}
	}


	public static String extendedInspect(Object object) {
		if (object.getClass().isArray()) {
			if (object.getClass().getCanonicalName().equals("int[]")) {
				return inspectInts((int[]) object);
			} else {
				return inspectObjects((Object[]) object);
			}
		} else {
			if (object.toString().indexOf("@") == -1) {
				return object.toString();
			} else {
				return deepInspect(object);
			}
		}
	}
}
