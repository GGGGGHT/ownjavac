package com.ggggght.main;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

/**
 * @desc: 流式解析Token
 * @date: 2021/4/19 11:04
 * @author: ggggght
 */
public class TokenizerParser extends StreamTokenizer {

	public TokenizerParser(Reader r) {
		super(r);
	}

	@Override
	public int nextToken() throws IOException {
		return 0;
	}
}
