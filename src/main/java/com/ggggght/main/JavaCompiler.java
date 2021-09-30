package com.ggggght.main;

import com.ggggght.main.file.RegularFileObject;
import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.file.BaseFileObject;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.sun.tools.javac.util.Log.PrefixKind;

import javax.annotation.processing.Processor;
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
	private static final String ownName = "own java compiler";
	private JOptions options = null;
	private OptionHelper optionHelper;
	private Set<File> fileNames = null;
	private JavacFileManager fileManager;
	public ListBuffer<String> classnames = null;
	protected ParserFactory parserFactory;
	protected TreeMaker make;
	protected Set<JavaFileObject> inputFiles = new HashSet<JavaFileObject>();


	public static JavaCompiler instance(Context context) {
		return new JavaCompiler();
	}


	private void error(String key, Object... args) {
		warning(key, args);
		log.printLines(Log.PrefixKind.JAVAC, "msg.usage", ownName);
	}

	private void warning(String key, Object... args) {
		log.printRawLines(ownName + ": " + log.localize(PrefixKind.JAVAC, key, args));
	}

	public void compile(List<JavaFileObject> sourceFileObjects, List<String> classnames, Iterable<? extends Processor> processors) {
		if (sourceFileObjects.isEmpty()) return;

		parseFiles(sourceFileObjects);
	}


	public List<JCTree.JCCompilationUnit> parseFiles(Iterable<JavaFileObject> fileObjects) {
		//parse all files
		ListBuffer<JCTree.JCCompilationUnit> trees = new ListBuffer<>();
		Set<JavaFileObject> filesSoFar = new HashSet<JavaFileObject>();
		for (JavaFileObject fileObject : fileObjects) {
			if (!filesSoFar.contains(fileObject)) {
				filesSoFar.add(fileObject);
				trees.append(parse(fileObject));
			}
		}
		return trees.toList();
	}

	public JCTree.JCCompilationUnit parse(JavaFileObject filename) {
		JavaFileObject prev = log.useSource(filename);
		try {
			JCTree.JCCompilationUnit t = parse(filename, readSource(filename));
			if (t.endPositions != null)
				log.setEndPosTable(filename, t.endPositions);
			return t;
		} finally {
			log.useSource(prev);
		}
	}

	protected JCTree.JCCompilationUnit parse(JavaFileObject filename, CharSequence content) {
		JCTree.JCCompilationUnit tree = make.TopLevel(List.<JCTree.JCAnnotation>nil(),
		                                              null, List.<JCTree>nil()
		);
		if (content != null) {
			Parser parser = parserFactory.newParser(content, false, false, true);
			tree = parser.parseCompilationUnit();
		}

		tree.sourcefile = filename;

		if (content != null) {
			TaskEvent e = new TaskEvent(TaskEvent.Kind.PARSE, tree);
		}

		return tree;
	}

	public CharSequence readSource(JavaFileObject filename) {
		try {
			inputFiles.add(filename);
			return filename.getCharContent(false);
		} catch (IOException e) {
			log.error("error.reading.file", filename, JavacFileManager.getMessage(e));
			return null;
		}
	}

	public void close() {
		// release resources...
	}
}
