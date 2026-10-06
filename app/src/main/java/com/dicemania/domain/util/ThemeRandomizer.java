package com.dicemania.domain.util;

import com.dicemania.domain.model.Category;
import com.dicemania.domain.model.QuestionType;
import java.util.Random;

public class ThemeRandomizer {
    private static final Random random = new Random();

    public static Category getRandomCategory() {
        Category[] categories = Category.values();
        return categories[random.nextInt(categories.length)];
    }

    public static QuestionType getRandomType() {
        QuestionType[] types = QuestionType.values();
        return types[random.nextInt(types.length)];
    }
}
