package com.github.novicezk.midjourney.wss.handle;

import com.github.novicezk.midjourney.enums.MessageType;
import com.github.novicezk.midjourney.enums.TaskAction;
import com.github.novicezk.midjourney.support.TaskCondition;
import com.github.novicezk.midjourney.util.ContentParseData;
import com.github.novicezk.midjourney.util.ConvertUtils;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * variation消息处理.
 * 完成(create): **cat** - Variations (Strong或Subtle) by <@1012983546824114217> (relaxed)
 * 完成(create): **cat** - Variations by <@1012983546824114217> (relaxed)
 */
@Component
public class VariationSuccessHandler extends MessageHandler {
	private static final String CONTENT_REGEX_1 = "\\*\\*(.*?)\\*\\* - Variations by <@\\d+> \\((.*?)\\)";
	//private static final String CONTENT_REGEX_1 = "\\*\\*(.*?)\\*\\* - Variations by <@\\d+>";

	private static final String CONTENT_REGEX_2 = "\\*\\*(.*?)\\*\\* - Variations \\(.*?\\) by <@\\d+> \\((.*?)\\)";
	//private static final String CONTENT_REGEX_2 = "\\*\\*(.*?)\\*\\* - Variations \\(.*?\\) by <@\\d+>";

	@Override
	public void handle(MessageType messageType, DataObject message) {
		String content = getMessageContent(message);
		ContentParseData parseData = getParseData(content);
		if (MessageType.CREATE.equals(messageType) && parseData != null && hasImage(message)) {
			TaskCondition condition = new TaskCondition()
					.setActionSet(Set.of(TaskAction.VARIATION))
					.setFinalPromptEn(parseData.getPrompt());
			findAndFinishImageTask(condition, parseData.getPrompt(), message);
		}
	}

	private ContentParseData getParseData(String content) {
		ContentParseData parseData = ConvertUtils.parseContent(content, CONTENT_REGEX_1);
		if (parseData == null) {
			parseData = ConvertUtils.parseContent(content, CONTENT_REGEX_2);
		}
		return parseData;
	}

}
