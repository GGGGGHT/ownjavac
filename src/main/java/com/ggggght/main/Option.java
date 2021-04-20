package com.ggggght.main;


import java.util.*;
import java.util.stream.Collectors;

import static com.ggggght.main.Option.OptionGroup.*;
import static com.ggggght.main.Option.OptionGroup.*;
import static com.ggggght.main.Option.OptionKind.*;

import com.sun.tools.javac.code.Lint;
import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.util.Log;


/**
 * @desc: com.ggggght.main
 * @date: 2021/4/19 16:57
 * @author: ggggght
 */
@SuppressWarnings("all")
public enum Option {
	G("-g", "option.g", STANDARD, BASIC),
	HELP("-help", "opt.help", STANDARD, INFO) {
		@Override
		public boolean process(OptionHelper helper, String option) {
			final Log log = helper.getLog();
			final String ownName = helper.getOwnName();
			log.printLines(Log.PrefixKind.JAVAC,"msg.usage.header",ownName);
			getJavaCompilerOptions().forEach(o -> o.help(log, STANDARD));
			log.printNewline();
			return false;
		}
	};

	private final OptionKind kind;
	final OptionGroup group;
	/**
	 * Documentation key for arguments.
	 */
	final String argsNameKey;
	/**
	 * Documentation key for description.
	 */
	final String descrKey;
	/**
	 * Suffix option (-foo=bar or -foo:bar)
	 */
	final boolean hasSuffix;
	/**
	 * The kind of choices for this option, if any.
	 */
	final ChoiceKind choiceKind;
	/**
	 * The choices for this option, if any, and whether or not the choices
	 * are hidden
	 */
	final Map<String, Boolean> choices;
	private final String text;

	Option(String text, String descrKey,
	       OptionKind kind, OptionGroup group) {
		this(text, null, descrKey, kind, group, null, null, false);
	}

	Option(String text, String argsNameKey, String descrKey,
	       OptionKind kind, OptionGroup group) {
		this(text, argsNameKey, descrKey, kind, group, null, null, false);
	}

	Option(String text, String argsNameKey, String descrKey,
	       OptionKind kind, OptionGroup group, boolean doHasSuffix) {
		this(text, argsNameKey, descrKey, kind, group, null, null, doHasSuffix);
	}


	Option(String text, String descrKey,
	       OptionKind kind, OptionGroup group,
	       ChoiceKind choiceKind, Map<String, Boolean> choices) {
		this(text, null, descrKey, kind, group, choiceKind, choices, false);
	}

	Option(String text, String descrKey,
	       OptionKind kind, OptionGroup group,
	       ChoiceKind choiceKind, String... choices) {
		this(text, null, descrKey, kind, group, choiceKind,
				createChoices(choices), false);
	}

	private Option(String text, String argsNameKey, String descrKey,
	               OptionKind kind, OptionGroup group,
	               ChoiceKind choiceKind, Map<String, Boolean> choices,
	               boolean doHasSuffix) {
		this.text = text;
		this.argsNameKey = argsNameKey;
		this.descrKey = descrKey;
		this.kind = kind;
		this.group = group;
		this.choiceKind = choiceKind;
		this.choices = choices;
		char lastChar = text.charAt(text.length() - 1);
		this.hasSuffix = doHasSuffix || lastChar == ':' || lastChar == '=';
	}

	// where
	private static Map<String, Boolean> createChoices(String... choices) {
		return Arrays.stream(choices).collect(Collectors.toMap(c -> c, c -> false, (a, b) -> b, LinkedHashMap::new));
	}

	private static Map<String, Boolean> getXLintChoices() {
		Map<String, Boolean> choices = new LinkedHashMap<String, Boolean>();
		choices.put("all", false);
		Arrays.stream(Lint.LintCategory.values()).forEach(c -> choices.put(c.option, c.hidden));
		Arrays.stream(Lint.LintCategory.values()).forEach(c -> choices.put("-" + c.option, c.hidden));
		choices.put("none", false);
		return choices;
	}

	static Set<Option> getJavaCompilerOptions() {
		return EnumSet.allOf(Option.class);
	}

	public static Set<Option> getJavacFileManagerOptions() {
		return getOptions(EnumSet.of(FILEMANAGER));
	}

	public static Set<Option> getJavacToolOptions() {
		return getOptions(EnumSet.of(BASIC));
	}

	static Set<Option> getOptions(Set<OptionGroup> desired) {
		Set<Option> options = Arrays.stream(values()).filter(option -> desired.contains(option.group)).collect(Collectors.toCollection(() -> EnumSet.noneOf(Option.class)));
		return Collections.unmodifiableSet(options);
	}

	public String getText() {
		return text;
	}

	public OptionKind getKind() {
		return kind;
	}

	public boolean hasArg() {
		return argsNameKey != null && !hasSuffix;
	}

	public boolean matches(String option) {
		if (!hasSuffix) {
			return option.equals(text);
		}

		if (!option.startsWith(text)) {
			return false;
		}

		if (choices != null) {
			String arg = option.substring(text.length());
			if (choiceKind == ChoiceKind.ONEOF) {
				return choices.keySet().contains(arg);
			} else {
				for (String a : arg.split(",+")) {
					if (!choices.keySet().contains(a)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public boolean process(OptionHelper helper, String option, String arg) {
		if (choices != null) {
			if (choiceKind == ChoiceKind.ONEOF) {
				// some clients like to see just one of option+choice set
				choices.keySet().stream().map(s -> option + s).forEach(helper::remove);
				String opt = option + arg;
				helper.put(opt, opt);
				// some clients like to see option (without trailing ":")
				// set to arg
				String nm = option.substring(0, option.length() - 1);
				helper.put(nm, arg);
			} else {
				// set option+word for each word in arg
				for (String a : arg.split(",+")) {
					String opt = option + a;
					helper.put(opt, opt);
				}
			}
		}
		helper.put(option, arg);
		return false;
	}

	public boolean process(OptionHelper helper, String option) {
		if (hasSuffix) {
			return process(helper, text, option.substring(text.length()));
		}

		return process(helper, option, option);
	}

	void help(Log log, OptionKind kind) {
		if (this.kind != kind) {
			return;
		}

		log.printRawLines(Log.WriterKind.NOTICE,
				String.format("  %-26s %s",
						helpSynopsis(log),
						log.localize(Log.PrefixKind.JAVAC, descrKey)));

	}

	private String helpSynopsis(Log log) {
		StringBuilder sb = new StringBuilder();
		sb.append(text);
		if (argsNameKey == null) {
			if (choices != null) {
				String sep = "{";
				for (Map.Entry<String, Boolean> e : choices.entrySet()) {
					if (!e.getValue()) {
						sb.append(sep);
						sb.append(e.getKey());
						sep = ",";
					}
				}
				sb.append("}");
			}
		} else {
			if (!hasSuffix) {
				sb.append(" ");
			}
			sb.append(log.localize(Log.PrefixKind.JAVAC, argsNameKey));

		}

		return sb.toString();
	}

	public enum OptionKind {
		/**
		 * A standard option, documented by -help.
		 */
		STANDARD,
		/**
		 * An extended option, documented by -X.
		 */
		EXTENDED,
		/**
		 * A hidden option, not documented.
		 */
		HIDDEN,
	}

	/**
	 * The group for an Option. This determines the situations in which the
	 * option is applicable.
	 */
	enum OptionGroup {
		/**
		 * A basic option, available for use on the command line or via the
		 * Compiler API.
		 */
		BASIC,
		/**
		 * An option for javac's standard JavaFileManager. Other file managers
		 * may or may not support these options.
		 */
		FILEMANAGER,
		/**
		 * A command-line option that requests information, such as -help.
		 */
		INFO,
		/**
		 * A command-line "option" representing a file or class name.
		 */
		OPERAND
	}

	/**
	 * The kind of choice for "choice" options.
	 */
	enum ChoiceKind {
		/**
		 * The expected value is exactly one of the set of choices.
		 */
		ONEOF,
		/**
		 * The expected value is one of more of the set of choices.
		 */
		ANYOF
	}

	// For -XpkgInfo:value
	public enum PkgInfo {
		/**
		 * Always generate package-info.class for every package-info.java file.
		 * The file may be empty if there annotations with a RetentionPolicy
		 * of CLASS or RUNTIME.  This option may be useful in conjunction with
		 * build systems (such as Ant) that expect javac to generate at least
		 * one .class file for every .java file.
		 */
		ALWAYS,
		/**
		 * Generate a package-info.class file if package-info.java contains
		 * annotations. The file may be empty if all the annotations have
		 * a RetentionPolicy of SOURCE.
		 * This value is just for backwards compatibility with earlier behavior.
		 * Either of the other two values are to be preferred to using this one.
		 */
		LEGACY,
		/**
		 * Generate a package-info.class file if and only if there are annotations
		 * in package-info.java to be written into it.
		 */
		NONEMPTY;

		// public static PkgInfo get(Options options) {
			// String v = options.get(XPKGINFO);
			// return (v == null
			// 		? PkgInfo.LEGACY
			// 		: PkgInfo.valueOf(v.toUpperCase()));
		// }
	}

}
