package com.botdetectorantispam.model;

import com.botdetectorantispam.model.Token;
import  com.botdetectorantispam.enums.Type;
import java.util.HashMap;
import java.util.Map;

public class NaiveBayes {
    public Map<String,Token> tokens = new HashMap<>();
    public Token allMessages = new Token();


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
        }
    }

    public float predict(String message){
        // convert message into array of words
        String[] _tokens = messageToTokens(message);

        int lenTokens = 0;
        int total = allMessages.total();
        // pham = probability ham, pspam = probability spam
        float pHam = (float) allMessages.ham / (float) total;
        float pSpam = (float) allMessages.spam / (float) total;

        for(String token: _tokens){
            // get token from corpus
            Token _token = tokens.getOrDefault(token, new Token());
            lenTokens += _token.total();
            // calculate probabilities
            pHam *= (float) _token.ham / (float) allMessages.ham;
            pSpam *= (float) _token.spam / (float) allMessages.spam;
        }
        // give default prediction if insufficient data
        if (lenTokens * 2 < _tokens.length){
            // TODO: return null?
            return (float) 0.5;
        }
        return pHam / (pSpam + pHam);
    }

    public void load(Map<String,Token> _tokens){
        for (Map.Entry<String, Token> _token: _tokens.entrySet()){
            String key = _token.getKey();
            Token value = _token.getValue();
            tokens.get(key).add(value.ham, Type.HAM);
            tokens.get(key).add(value.spam, Type.SPAM);
            allMessages.add(value.ham, Type.HAM);
            allMessages.add(value.spam, Type.SPAM);
        }
    }
    public Map<String,Token> save(){
        return tokens;
    }
}
