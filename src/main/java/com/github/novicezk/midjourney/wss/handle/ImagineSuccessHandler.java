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
 * imagine消息处理.
 * 完成(create): **cat** - <@1012983546824114217> (relaxed)
 */
@Component
public class ImagineSuccessHandler extends MessageHandler {
	private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+> \\((.*?)\\)";
	//private static final String CONTENT_REGEX = "\\*\\*(.*?)\\*\\* - <@\\d+>";
    //BlueWillow: **sunny night with blinking star,a red dressed girl walking slowly** - <@1091519978018185326>
	@Override
	public void handle(MessageType messageType, DataObject message) {
		//System.out.println("DataObject pst "+  message.toPrettyString());
		String content = getMessageContent(message);
		ContentParseData parseData = ConvertUtils.parseContent(content, CONTENT_REGEX);
		if (MessageType.CREATE.equals(messageType) && parseData != null && hasImage(message)) {
			TaskCondition condition = new TaskCondition()
					.setActionSet(Set.of(TaskAction.IMAGINE))
					.setFinalPromptEn(parseData.getPrompt());
			findAndFinishImageTask(condition, parseData.getPrompt(), message);
		}
	}

	public static void main(String[] args) {
		String url = "**city tower under the sunset** - <@1091519978018185326>";
		System.out.println(ConvertUtils.parseContent(url, CONTENT_REGEX));
	}

}
