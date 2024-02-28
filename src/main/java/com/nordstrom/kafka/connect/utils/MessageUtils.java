package com.nordstrom.kafka.connect.utils;

import com.alibaba.fastjson2.JSONObject;
import com.amazonaws.services.sqs.model.Message;
import com.alibaba.fastjson2.JSON;

public class MessageUtils {
    public static String getStringValueFromJSONPath(String jsonPath, Message message) {
        String messageId = message.getMessageId();
        if (StringUtils.isBlank(jsonPath)) {
            return messageId;
        }
        String messageBody = message.getBody();
        if (!JSON.isValid(messageBody)) {
            return messageId;
        }
        JSONObject obj = JSON.parseObject(messageBody);
        Object result = obj.getByPath(jsonPath);
        if (result == null) {
            return messageId;
        }
        return result.toString();
    }
}
