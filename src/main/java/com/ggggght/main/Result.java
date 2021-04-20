package com.ggggght.main;

/**
 * @desc: 返回结果
 * @date: 2021/4/19 17:24
 * @author: ggggght
 */
@SuppressWarnings("all")
public enum Result {
	OK(0),        // Compilation completed with no errors.
	ERROR(1),     // Completed but reported errors.
	CMDERR(2),    // Bad command-line arguments
	SYSERR(3),    // System error or resource exhaustion.
	ABNORMAL(4);  // Compiler terminated abnormally

	public final int exitCode;

	Result(int exitCode) {
		this.exitCode = exitCode;
	}
}
