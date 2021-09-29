package com.ggggght.main;

import com.ggggght.main.file.RegularFileObject;
import com.sun.tools.javac.file.BaseFileObject;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;

import java.io.File;
import java.util.*;

import com.sun.tools.javac.util.Log.PrefixKind;

import javax.tools.JavaFileObject;

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
	private JavacFileManager fileManager;
	public ListBuffer<String> classnames = null;

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



	private static JavaCompiler instance() {
		return new JavaCompiler();
	}




	private void error(String key, Object... args) {
		warning(key, args);
		log.printLines(Log.PrefixKind.JAVAC, "msg.usage", ownName);
	}

	private void warning(String key, Object... args) {
		log.printRawLines(ownName + ": " + log.localize(PrefixKind.JAVAC, key, args));
	}
}
