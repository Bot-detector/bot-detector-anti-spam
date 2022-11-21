package com.botdetectorantispam.model;
import  com.botdetectorantispam.enums.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NaiveBayes {
    public Map<String,Token> tokens = new HashMap<>();
    public Token allMessages = new Token();
    public List<String> excludeList = new ArrayList<>();

    private String[] messageToTokens(String message){
        message = message.toLowerCase().replace(".","").replace(",","");
        return message.split(" ");
    }
    private void count(){
        allMessages = new Token();
        for (Token t: tokens.values()){
            allMessages.ham += t.ham;
            allMessages.spam += t.spam;
        }
    }

    public void markMessage(String message, Type dataType){
        // TODO: log.info()
        System.out.println(message + " marked as " + dataType);

        String[] _tokens = messageToTokens(message);

        for (String token: _tokens){
            Token _token = tokens.getOrDefault(token, new Token());
            _token.add(1, dataType);
            allMessages.add(1, dataType);
            tokens.put(token, _token);
        }
    }
    public float predict(String message){
        // convert message into array of words
        String[] _tokens = messageToTokens(message);

        int total = allMessages.total();
        // pham = probability ham, pspam = probability spam
        float pHam = (float) allMessages.ham / (float) total;
        float pSpam = (float) allMessages.spam / (float) total;

        for(String token: _tokens){
            if(excludeList.contains(token)){
                continue;
            }
            // get token from corpus
            Token _token = tokens.getOrDefault(token, new Token());
            // calculate probabilities
            pHam *= (float) _token.ham / (float) allMessages.ham;
            pSpam *= (float) _token.spam / (float) allMessages.spam;
        }
        return pHam / (pSpam + pHam);
    }

    public void load(Map<String,Token> _tokens){
        for (Map.Entry<String, Token> _token: _tokens.entrySet()){
            String key = _token.getKey();
            Token value = _token.getValue();
            // check if tokens exists, if not create new token
            Token token = tokens.getOrDefault(key, new Token());
            // add our values to the token
            token.add(value.ham, Type.HAM);
            token.add(value.spam, Type.SPAM);
            // update tokens, with our new key
            tokens.put(key, token);
            // update allMessage
            allMessages.add(value.ham, Type.HAM);
            allMessages.add(value.spam, Type.SPAM);
        }
    }
    public Map<String,Token> save(){
        return tokens;
    }

    @Override
    public String toString() {
        String str="";
        str += "allMessage: " + allMessages.toString() + ", ";
        str += "tokens: {";
        for (Map.Entry<String, Token> _token: tokens.entrySet()){
            str += _token.getKey() + ": " + _token.getValue().toString() + ", ";
        }
        // remove last ", "
        str = str.substring(0, str.length() - 2);
        str += "}";
        return str;
    }
}
