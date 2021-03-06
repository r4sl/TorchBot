package me.woder.json;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ChatMessageDe implements JsonDeserializer<ChatMessage> {
    
    @Override
    public ChatMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ChatMessage message = new ChatMessage();
        JsonObject obj = json.getAsJsonObject();
        JsonArray array = obj.getAsJsonArray("extra");
        for(int i = 0; i < array.size(); i++){
            if(array.get(i).isJsonPrimitive()){
               message.getExtra().add(array.get(i).getAsString());
            }else{
               message.getExtra().add(context.deserialize(array.get(i), Node.class));
            }
        }
        return message;
    }
}
