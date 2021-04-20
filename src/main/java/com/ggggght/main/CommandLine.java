package com.ggggght.main;


import com.sun.tools.javac.util.ListBuffer;

/**
 * @desc: 解析命令行参数,不支持从文件中获取 即以@开头
 * @date: 2021/4/19 10:49
 * @author: ggggght
 */
public class CommandLine {
	public static String[] parse(String[] args) {
		ListBuffer<String> newArgs = new ListBuffer<>();
		for (String arg : args) {
			newArgs.append(arg);
		}
		return newArgs.toArray(new String[newArgs.length()]);
	}
}
