package com.chrisgward.sbnc.iface;

import com.chrisgward.sbnc.iface.exception.SBNCException;

import java.util.ArrayList;

public class SBNCSerializer {
	public static String serialize(Object value) {
		StringBuilder builder = new StringBuilder();

		if(value instanceof ArrayList) {
			ArrayList list = (ArrayList)value;
			builder.append(serialize(list.toArray(new Object[list.size()])));
		}
		else if(value instanceof Object[]) {
			builder.append("{");

			for(Object obj : (Object[])value) {
				builder.append(serialize(obj));
			}

			builder.append("}");
		}
		else if(value instanceof String) {

			return "(" + escape((String)value) + ")";
		}


		return builder.toString();
	}

	private static final String[][] replacements = new String[][] {
			{"\r", "\\r"},
			{"\n", "\\n"},
			{"{", "\\{"},
			{"}", "\\}"},
			{"[", "\\["},
			{"]", "\\]"},
			{"(", "\\("},
			{")", "\\)"},
			{"\\", "\\\\"}
	};

	public static String escape(String toEscape) {
		for(String[] replacement : replacements) {
			toEscape.replace(replacement[0], replacement[1]);
		}
		return toEscape;
	}

	private static enum Types {
		EXCEPTION, STRING, LIST
	}

	public static Object[] parse(String value) {
		int offset = 0;
		StringBuilder data = new StringBuilder();
		ArrayList output = new ArrayList();
		int codeCount = 0;
		boolean escape = false;
		Types type = null;
		int controls = 0;

		for(int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);

			if(escape) {
				if(c == 'n') {
					data.append('\n');
				} else if (c == 'r') {
					data.append('\r');
				} else {
					data.append(c);
				}
				escape = false;
				continue;
			}

			if(c == '\\') {
				escape = true;
				continue;
			}

			switch(c) {
				case '[':
					controls++;
					if(type == null && controls == 1) {
						type = Types.EXCEPTION;
					} else {
						data.append(c);
					}
					break;
				case '{':
					controls++;
					if(type == null && controls == 1) {
						type = Types.LIST;
					} else {
						data.append(c);
					}
					break;
				case '(':
					controls++;
					if(type == null && controls == 1) {
						type = Types.STRING;
					} else {
						data.append(c);
					}
					break;
				case ']':
					controls--;
					if(type == Types.EXCEPTION && controls == 0) {
						output.add(new SBNCException(data.toString()));
						data = new StringBuilder();
						type = null;
					} else {
						data.append(c);
					}
					break;
				case '}':
					controls--;
					if(type == Types.LIST && controls == 0) {
						output.add(parse(data.toString()));
						data = new StringBuilder();
						type = null;
					} else {
						 data.append(c);
					}
					break;
				case ')':
					controls--;
					if(type == Types.STRING && controls == 0) {
						output.add(data.toString());
						data = new StringBuilder();
						type = null;
					} else {
						data.append(c);
					}
					break;
				default:
					data.append(c);
					continue;
			}
		}
		return output.toArray(new Object[output.size()]);
	}
}
