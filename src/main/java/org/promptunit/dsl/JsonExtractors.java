package org.promptunit.dsl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JsonExtractors {
	private JsonExtractors() {}

	static String extract(String text) {
		String s = extractMarkdownJsonFence(text);
		if (s != null) return s;
		s = extractAnyFence(text);
		if (s != null && looksJsonish(s)) return s;
		s = extractBalanced(text);
		return s;
	}

	private static String extractMarkdownJsonFence(String text) {
		Pattern p = Pattern.compile("```json\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(text);
		if (m.find()) return m.group(1).trim();
		return null;
	}

	private static String extractAnyFence(String text) {
		Pattern p = Pattern.compile("```\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(text);
		if (m.find()) return m.group(1).trim();
		return null;
	}

	private static boolean looksJsonish(String s) {
		s = s.trim();
		return (s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]"));
	}

	private static String extractBalanced(String text) {
		int objIdx = text.indexOf('{');
		int arrIdx = text.indexOf('[');
		if (objIdx == -1 && arrIdx == -1) return null;
		int start = (objIdx >= 0 && (arrIdx < 0 || objIdx < arrIdx)) ? objIdx : arrIdx;
		char open = text.charAt(start);
		char close = open == '{' ? '}' : ']';
		int depth = 0;
		boolean inString = false;
		boolean escaped = false;
		for (int i = start; i < text.length(); i++) {
			char c = text.charAt(i);
			if (escaped) { escaped = false; continue; }
			if (c == '\\' && inString) { escaped = true; continue; }
			if (c == '"') { inString = !inString; continue; }
			if (inString) continue;
			if (c == open) depth++;
			if (c == close) {
				depth--;
				if (depth == 0) return text.substring(start, i + 1);
			}
		}
		return null;
	}
}
