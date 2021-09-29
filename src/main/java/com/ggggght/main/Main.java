package com.ggggght.main;

import com.sun.source.util.JavacTask;
import com.sun.tools.doclint.DocLint;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.CommandLine;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.processing.AnnotationProcessingError;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ServiceLoader;

import javax.annotation.processing.Processor;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.ggggght.main.Option.HELP;
import static com.sun.tools.javac.main.Option.*;
import static com.sun.tools.javac.main.Option.XDOCLINT_CUSTOM;

public class Main {
	private String ownName;
	private PrintWriter out;
	Log log;
	private JavaFileManager fileManager;
	public Set<File> filenames = null;
	public ListBuffer<String> classnames = null;
	private Options options = null;
	private com.ggggght.main.Option[] recognizedOptions = com.ggggght.main.Option.getJavaCompilerOptions().toArray(new com.ggggght.main.Option[0]);

	public Main(String name) {
		this(name, new PrintWriter(System.err, true));
	}

	public Main(String name, PrintWriter out) {
		this.ownName = name;
		this.out = out;
	}

	public Result compile(String[] args) {
		Context context = new Context();
		JavacFileManager.preRegister(context); // can't create it until Log has been set up
		Result result = compile(args, context);
		if (fileManager instanceof JavacFileManager) {
			// A fresh context was created above, so jfm must be a JavacFileManager
			((JavacFileManager) fileManager).close();
		}
		return result;
	}

	public Result compile(String[] args, Context context) {
		return compile(args, context, List.<JavaFileObject>nil(), null);
	}

	/**
	 * Programmatic interface for main function.
	 *
	 * @param args The command line parameters.
	 */
	public Result compile(String[] args,
	                      Context context,
	                      List<JavaFileObject> fileObjects,
	                      Iterable<? extends Processor> processors) {
		return compile(args, null, context, fileObjects, processors);
	}

	public Result compile(String[] args,
	                      String[] classNames,
	                      Context context,
	                      List<JavaFileObject> fileObjects,
	                      Iterable<? extends Processor> processors) {
		context.put(Log.outKey, out);
		log = Log.instance(context);

		if (options == null)
			options = Options.instance(context); // creates a new one

		filenames = new LinkedHashSet<File>();
		classnames = new ListBuffer<String>();
		com.sun.tools.javac.main.JavaCompiler comp = null;
		/*
		 * TODO: Logic below about what is an acceptable command line
		 * should be updated to take annotation processing semantics
		 * into account.
		 */
		try {
			if (args.length == 0
					&& (classNames == null || classNames.length == 0)
					&& fileObjects.isEmpty()) {
				Option.HELP.process(optionHelper, "-help");
				return Result.CMDERR;
			}

			Collection<File> files;
			try {
				files = processArgs(CommandLine.parse(args), classNames);
				if (files == null) {
					// null signals an error in options, abort
					return Result.CMDERR;
				} else if (files.isEmpty() && fileObjects.isEmpty() && classnames.isEmpty()) {
					// it is allowed to compile nothing if just asking for help or version info
					if (options.isSet(HELP.argsNameKey)
							|| options.isSet(X)
							|| options.isSet(VERSION)
							|| options.isSet(FULLVERSION))
						return Result.OK;

					return Result.CMDERR;
				}
			} catch (java.io.FileNotFoundException e) {
				// warning("err.file.not.found", e.getMessage());
				return Result.SYSERR;
			}

			boolean forceStdOut = options.isSet("stdout");
			if (forceStdOut) {
				log.flush();
				log.setWriters(new PrintWriter(System.out, true));
			}

			// allow System property in following line as a Mustang legacy
			boolean batchMode = (options.isUnset("nonBatchMode")
					&& System.getProperty("nonBatchMode") == null);
			if (batchMode)
				CacheFSInfo.preRegister(context);

			// FIXME: this code will not be invoked if using JavacTask.parse/analyze/generate
			// invoke any available plugins
			comp = com.sun.tools.javac.main.JavaCompiler.instance(context);

			// FIXME: this code will not be invoked if using JavacTask.parse/analyze/generate
			String xdoclint = options.get(XDOCLINT);
			String xdoclintCustom = options.get(XDOCLINT_CUSTOM);
			if (xdoclint != null || xdoclintCustom != null) {
				Set<String> doclintOpts = new LinkedHashSet<String>();
				if (xdoclint != null)
					doclintOpts.add(DocLint.XMSGS_OPTION);
				if (xdoclintCustom != null) {
					for (String s : xdoclintCustom.split("\\s+")) {
						if (s.isEmpty())
							continue;
						doclintOpts.add(s.replace(XDOCLINT_CUSTOM.text, DocLint.XMSGS_CUSTOM_PREFIX));
					}
				}
				if (!(doclintOpts.size() == 1
						&& doclintOpts.iterator().next().equals(DocLint.XMSGS_CUSTOM_PREFIX + "none"))) {
					JavacTask t = BasicJavacTask.instance(context);
					// standard doclet normally generates H1, H2
					doclintOpts.add(DocLint.XIMPLICIT_HEADERS + "2");
					new DocLint().init(t, doclintOpts.toArray(new String[doclintOpts.size()]));
					comp.keepComments = true;
				}
			}

			fileManager = context.get(JavaFileManager.class);

			if (!files.isEmpty()) {
				// add filenames to fileObjects
				comp = JavaCompiler.instance(context);
				List<JavaFileObject> otherFiles = List.nil();
				JavacFileManager dfm = (JavacFileManager) fileManager;
				for (JavaFileObject fo : dfm.getJavaFileObjectsFromFiles(files))
					otherFiles = otherFiles.prepend(fo);
				for (JavaFileObject fo : otherFiles)
					fileObjects = fileObjects.prepend(fo);
			}
			comp.compile(
					fileObjects,
					classnames.toList(),
					processors
			);

			if (log.expectDiagKeys != null) {
				if (log.expectDiagKeys.isEmpty()) {
					log.printRawLines("all expected diagnostics found");
					return Result.OK;
				} else {
					log.printRawLines("expected diagnostic keys not found: " + log.expectDiagKeys);
					return Result.ERROR;
				}
			}

			if (comp.errorCount() != 0)
				return Result.ERROR;
		} catch (IOException ex) {
			return Result.SYSERR;
		} catch (OutOfMemoryError ex) {
			return Result.SYSERR;
		} catch (StackOverflowError ex) {
			return Result.SYSERR;
		} catch (FatalError ex) {
			return Result.SYSERR;
		} catch (AnnotationProcessingError ex) {
			return Result.SYSERR;
		} catch (ClientCodeException ex) {
			// as specified by javax.tools.JavaCompiler#getTask
			// and javax.tools.JavaCompiler.CompilationTask#call
			throw new RuntimeException(ex.getCause());
		} catch (PropagatedException ex) {
			throw ex.getCause();
		} catch (Throwable ex) {
			// Nasty.  If we've already reported an error, compensate
			// for buggy compiler error recovery by swallowing thrown
			// exceptions.
			if (comp == null || comp.errorCount() == 0 ||
					options == null || options.isSet("dev"))
			return Result.ABNORMAL;
		} finally {
			if (comp != null) {
				try {
					comp.close();
				} catch (ClientCodeException ex) {
					throw new RuntimeException(ex.getCause());
				}
			}
			filenames = null;
			options = null;
		}
		return Result.OK;
	}

	public com.ggggght.main.Result compile(String[] args,
	                                       String[] classNames,
	                                       OptionHelper helper) {
		if (args.length == 0 && classNames.length == 0) {
			HELP.process(helper, "-help");
			return com.ggggght.main.Result.CMDERR;
		}

		// 处理命令行参数
		final Collection<File> files = processArgs(com.ggggght.main.CommandLine.parse(args), classNames);

		if (Objects.isNull(files)) {
			return com.ggggght.main.Result.CMDERR;
		}
		if (files.isEmpty()) {
			if (options.isSet(HELP.argsNameKey)) {
				return com.ggggght.main.Result.OK;
			}

			return com.ggggght.main.Result.CMDERR;
		}

		JavaCompiler comp = JavaCompiler.instance(null);
		List<JavaFileObject> fileObjects = List.nil();

		JavacFileManager dfm = (JavacFileManager) fileManager;
		for (JavaFileObject fo : dfm.getJavaFileObjectsFromFiles(files)) {
			fileObjects.add(fo);
		}
		com.sun.tools.javac.util.List<String> classnames = this.classnames.toList();
		comp.compile(fileObjects, classnames, null);
		return com.ggggght.main.Result.OK;
	}

	/**
	 * 处理参数
	 *
	 * @param args
	 * @param classNames
	 */
	public Collection<File> processArgs(String[] args, String[] classNames) {
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

		return filenames;
	}

	public enum Result {
		OK(0),        // Compilation completed with no errors.
		ERROR(1),     // Completed but reported errors.
		CMDERR(2),    // Bad command-line arguments
		SYSERR(3),    // System error or resource exhaustion.
		ABNORMAL(4);  // Compiler terminated abnormally

		Result(int exitCode) {
			this.exitCode = exitCode;
		}

		public boolean isOK() {
			return (exitCode == 0);
		}

		public final int exitCode;
	}

	private OptionHelper optionHelper = null;

	// 		new OptionHelper() {
	// 	@Override
	// 	public String get(Option option) {
	// 		return options.get(option);
	// 	}
	//
	// 	@Override
	// 	public void put(String name, String value) {
	// 		options.put(name, value);
	// 	}
	//
	// 	@Override
	// 	public void remove(String name) {
	// 		options.remove(name);
	// 	}
	//
	// 	@Override
	// 	public Log getLog() {
	// 		return log;
	// 	}
	//
	// 	@Override
	// 	public String getOwnName() {
	// 		return ownName;
	// 	}
	//
	// 	@Override
	// 	public void error(String key, Object... args) {
	// 		this.error(key, args);
	// 	}
	//
	// 	@Override
	// 	public void addFile(File f) {
	// 		filenames.add(f);
	// 	}
	//
	// 	@Override
	// 	public void addClassName(String s) {
	// 		classnames.append(s);
	// 	}
	//
	// };
}
