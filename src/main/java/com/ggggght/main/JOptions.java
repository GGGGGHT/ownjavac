package com.ggggght.main;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;

import java.util.LinkedHashMap;

/**
 * @desc: com.ggggght.main
 * @date: 2021/4/19 18:11
 * @author: ggggght
 */
public class JOptions extends Options {
	private LinkedHashMap<String,String> values;

	protected JOptions(Context context) {
		super(context);
		values = new LinkedHashMap<>();
	}

	public boolean isSet(Option option) {
		return this.values.get(option.getText()) != null;
	}
}
