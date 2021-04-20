package com.ggggght.main.com.ggggght.parser;

/**
 * @desc: 自定义TOKEN
 * @date: 2021/4/19 18:46
 * @author: ggggght
 */
@SuppressWarnings("all")
public enum TokenKind {
	FOR("循环"),
	IF("如果"),
	LT("小于"),
	GT("大于"),
	PLUS("加"),
	EQ("等于"),
	SUB("减"),
	SOUT("输出");


	private final String text;
	private final Tag tag;

	TokenKind() {
		this(null, Tag.DEFAULT);
	}

	TokenKind(String name) {
		this(name, Tag.DEFAULT);
	}

	TokenKind(Tag tag) {
		this(null, tag);
	}

	TokenKind(String text, Tag tag) {
		this.tag = tag;
		this.text = text;
	}


	static enum Tag {
		DEFAULT,
		NAMED,
		STRING,
		NUMERIC;

		private Tag() {
		}
	}
}
