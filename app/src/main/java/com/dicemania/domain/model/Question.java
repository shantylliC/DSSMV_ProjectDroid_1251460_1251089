package com.dicemania.domain.model;

import java.util.List;

public class Question {
    private Category category;
    private QuestionType type;
    private String text;
    private String correctAnswer;
    private List<String> shuffledOptions;

    public Question(Category category, QuestionType type, String text, String correctAnswer, List<String> shuffledOptions) {
        this.category = category;
        this.type = type;
        this.text = text;
        this.correctAnswer = correctAnswer;
        this.shuffledOptions = shuffledOptions;
    }

    // Getters para a interface ler a pergunta e desenhar os botões
    public Category getCategory() { return category; }
    public QuestionType getType() { return type; }
    public String getText() { return text; }
    public String getCorrectAnswer() { return correctAnswer; }
    public List<String> getShuffledOptions() { return shuffledOptions; }
}
