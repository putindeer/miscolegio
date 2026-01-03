package me.putindeer.miscolegio.question;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.putindeer.miscolegio.zone.ZoneLocation;
import org.apache.commons.csv.CSVRecord;

@Data
@AllArgsConstructor
public class Question {
    private String id;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String answer;
    private QuestionLevel level;
    private boolean guaranteed;

    public static Question fromCSVRecord(CSVRecord record) {
        try {
            return new Question(
                    record.get("id"),
                    record.get("question"),
                    record.get("optionA"),
                    record.get("optionB"),
                    record.get("optionC"),
                    record.get("optionD"),
                    record.get("answer").toUpperCase(),
                    QuestionLevel.valueOf(record.get("level").toUpperCase()),
                    Boolean.parseBoolean(record.get("guaranteed"))
            );
        } catch (Exception e) {
            return null;
        }
    }

    public Object[] toCSVArray() {
        return new Object[]{
                id,
                question,
                optionA,
                optionB,
                optionC,
                optionD,
                answer,
                level.toString(),
                guaranteed
        };
    }

    public ZoneLocation getAnswerLocation() {
        return ZoneLocation.valueOf(answer.toUpperCase());
    }

    public String getGuaranteedRoundString() {
        return guaranteed ? "SI" : "NO";
    }
}