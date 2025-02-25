package com.github.novicezk.midjourney.util;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.enums.TaskAction;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
import lombok.experimental.UtilityClass;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class ConvertUtils {
	/**
	 * content正则匹配prompt和进度.
	 *  UPDATE - Midjourney Bot: **technology revolution --s 750** - <@1140456352959967403> (93%) (fast)
	 *  CREATE - Midjourney Bot: **technology revolution --s 750** - <@1140456352959967403> (fast)
	 */
	public static final String CONTENT_REGEX = ".*?\\*\\*(.*?)\\*\\*.+<@\\d+> \\((.*?)\\)";
	//private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+>";
	public static ContentParseData parseContent(String content) {
		return parseContent(content, CONTENT_REGEX);
	}

	public static ContentParseData parseContent(String content, String regex) {
		if (CharSequenceUtil.isBlank(content)) {
			return null;
		}
		Matcher matcher = Pattern.compile(regex).matcher(content);
		if (!matcher.find()) {
			return null;
		}
		ContentParseData parseData = new ContentParseData();
		parseData.setPrompt(matcher.group(1));
		//parseData.setNonce(matcher.group(2));
		if(matcher.groupCount()>1)
		  parseData.setStatus(matcher.group(2));
		return parseData;
	}

	public static List<DataUrl> convertBase64Array(List<String> base64Array) throws MalformedURLException {
		if (base64Array == null || base64Array.isEmpty()) {
			return Collections.emptyList();
		}
		IDataUrlSerializer serializer = new DataUrlSerializer();
		List<DataUrl> dataUrlList = new ArrayList<>();
		for (String base64 : base64Array) {
			DataUrl dataUrl = serializer.unserialize(base64);
			dataUrlList.add(dataUrl);
		}
		return dataUrlList;
	}

	public static TaskChangeParams convertChangeParams(String content) {
		List<String> split = CharSequenceUtil.split(content, " ");
		if (split.size() != 2) {
			return null;
		}
		String action = split.get(1).toLowerCase();
		TaskChangeParams changeParams = new TaskChangeParams();
		changeParams.setId(split.get(0));
		if (action.charAt(0) == 'u') {
			changeParams.setAction(TaskAction.UPSCALE);
		} else if (action.charAt(0) == 'v') {
			changeParams.setAction(TaskAction.VARIATION);
		} else if (action.equals("r")) {
			changeParams.setAction(TaskAction.REROLL);
			return changeParams;
		} else {
			return null;
		}
		try {
			int index = Integer.parseInt(action.substring(1, 2));
			if (index < 1 || index > 4) {
				return null;
			}
			changeParams.setIndex(index);
		} catch (Exception e) {
			return null;
		}
		return changeParams;
	}

}
