package com.ggggght.main;

import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.util.Log;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.sun.tools.javac.util.Log.PrefixKind;
import com.sun.tools.javac.util.Log.WriterKind;
import com.sun.tools.javac.util.Options;

import static com.ggggght.main.Option.*;

/**
 * @desc: com.ggggght.main
 * @date: 2021/4/19 16:55
 * @author: ggggght
 */
public class JavaCompiler {
	private Option[] recognizedOptions = Option.getJavaCompilerOptions().toArray(new Option[0]);
	private Log log;
	private static final String ownName = "java compiler";
	private JOptions options = null;
	private OptionHelper optionHelper;
	private Set<File> fileNames = null;

	// {
	// 	optionHelper = new OptionHelper() {
	// 		@Override
	// 		public String get(com.sun.tools.javac.main.Option option) {
	// 			return null;
	// 		}
	//
	// 		@Override
	// 		public void put(String s, String s1) {
	// 			options.put(s, s1);
	// 		}
	//
	// 		@Override
	// 		public void remove(String s) {
	// 			options.remove(s);
	// 		}
	//
	// 		@Override
	// 		public Log getLog() {
	// 			return log;
	// 		}
	//
	// 		@Override
	// 		public String getOwnName() {
	// 			return ownName;
	// 		}
	// 	};
	// }

	public Result compile(String[] args,
	                      String[] classNames,
	                      OptionHelper helper) {
		if (args.length == 0 && classNames.length == 0) {
			HELP.process(helper, "-help");
			return Result.CMDERR;
		}

		// 处理命令行参数
		final Collection<File> files = processArgs(CommandLine.parse(args), classNames);

		if (Objects.isNull(files)) {
			return Result.CMDERR;
		}
		if (files.isEmpty()) {
			if (options.isSet(HELP)) {
				return Result.OK;
			}

			return Result.CMDERR;
		}

		return Result.OK;
	}



	/**
	 * 处理参数
	 *
	 * @param args
	 * @param classNames
	 */
	private Collection<File> processArgs(String[] args, String[] classNames) {
		int i = 0;
		while (i < args.length) {
			final String arg = args[i++];

			Option option = null;
			if (arg.length() > 0) {
				final int firstOptionToCheck = arg.charAt(0) == '-' ? 0 : recognizedOptions.length - 1;
				for (int j = firstOptionToCheck; j < recognizedOptions.length; j++) {
					if (recognizedOptions[j].matches(arg)) {
						option = recognizedOptions[j];
						break;
					}
				}
			}

			if (Objects.isNull(option)) {
				return null;
			}

			if (option.hasArg()) {
				if (i == args.length) {
					error("err.req.arg", args);
					return null;
				}
				final String operand = args[i++];
				if (option.process(optionHelper, arg, operand)) {
					return null;
				}
			} else {
				if (option.process(optionHelper, arg)) {
					return null;
				}
			}
		}


		return fileNames;
	}

	private void error(String key, Object... args) {
		warning(key, args);
		log.printLines(Log.PrefixKind.JAVAC, "msg.usage", ownName);
	}

	private void warning(String key, Object... args) {
		log.printRawLines(ownName + ": " + log.localize(PrefixKind.JAVAC, key, args));
	}
}
