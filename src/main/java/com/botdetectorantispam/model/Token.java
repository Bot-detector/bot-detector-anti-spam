package com.botdetectorantispam.model;
import com.botdetectorantispam.enums.Type;

// {"viagra":Token(ham=100, spam=1000), "free":Token(ham=10, spam=100)}
public class Token {
    public int ham = 1;
    public int spam = 1;

    public void add(int amount, Type dataType) {
        switch (dataType){
            case HAM:
                ham += amount;
                break;
            case SPAM:
                spam += amount;
                break;
        }
    }

    public int total(){
        return ham + spam;
    }

    @Override
    public String toString() {
        return "{" + "ham:" + ham + ", spam:" + spam + '}';
    }
}
