package edu.oswego;

import java.util.ArrayList;
import java.util.Collections;

class Paragraph {
    final ArrayList<String> words = new ArrayList<String>();
    String[] paraByWords;
    String rawParagraph;
    String bookName;
    int index;
    private int totalWorCount = 0;

    public Paragraph(String paragraph, String bookName, int index) {
        this.rawParagraph = paragraph;
        this.bookName = bookName;
        this.index = index;

        String temp = Document.cleanString(paragraph);
        paraByWords = temp.split("\\s+");


        for (String word : temp.split("\\s+")) {
            if (!words.contains(word)) {
                words.add(word.toLowerCase().trim());//setup word list
            }
            totalWorCount++;
        }
        Collections.sort(words);
    }

}

