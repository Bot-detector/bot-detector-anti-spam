package com.botdetectorantispam.model;
import com.google.gson.Gson;
import net.runelite.client.RuneLite;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.reflect.TypeToken;

public class DataPersister {
    private static Gson gson = new Gson();
    public static final File PARENT_DIRECTORY = new File(RuneLite.RUNELITE_DIR, "botdetectorantispam");

    public static void setup() throws IOException{
        if (!PARENT_DIRECTORY.exists()){
            if (!PARENT_DIRECTORY.mkdir()) {
                throw new IOException("unable to create parent directory!");
            }
        }
    }
    public static void storeData(String fileName, Object data) throws IOException {
        File dataFile = new File(PARENT_DIRECTORY, fileName + ".json");
        final String json = gson.toJson(data);
        Files.write(dataFile.toPath(), json.getBytes());
    }

    public static void writeTokens(Map<String,Token> tokens) throws IOException {
        File dataFile = new File(PARENT_DIRECTORY, "tokens.json");
        final String json = gson.toJson(tokens);
        Files.write(dataFile.toPath(), json.getBytes());
    }
    public Map<String,Token> readTokens() throws  IOException {
        File dataFile = new File(PARENT_DIRECTORY, "tokens.json");
        if (dataFile.exists()){
            String jsonData = new String(Files.readAllBytes(dataFile.toPath()));
            Type type = new TypeToken<Map<String, Token>>(){}.getType();
            return gson.fromJson(jsonData, type);
        } else {
            return new HashMap<>() ;
        }

    }
}
