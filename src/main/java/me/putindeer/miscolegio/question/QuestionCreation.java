package me.putindeer.miscolegio.question;

import lombok.Data;

@Data
public class QuestionCreation {
    String id;
    String question;
    String optionA;
    String optionB;
    String optionC;
    String optionD;
    String answer;
    QuestionLevel level;
    boolean guaranteed;
    int step = 0;

    QuestionCreation(String id) {
        this.id = id;
    }
}
