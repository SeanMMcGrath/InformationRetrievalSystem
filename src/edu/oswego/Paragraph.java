package edu.oswego;

import java.util.ArrayList;
import java.util.Collections;

import static edu.oswego.Controller.wordFreq;

class Paragraph {
    final ArrayList<String> words = new ArrayList<String>();
    String[] paraByWords;
    String rawParagraph;
    String bookName;
    int index;

    public Paragraph(String paragraph, String bookName, int index) {
        this.rawParagraph = paragraph;
        this.bookName = bookName;
        this.index = index;

        String temp = Document.cleanString(paragraph);
        paraByWords = temp.split("\\s+");


        for (String word : temp.split("\\s+")) {
            if (word.length() != 0) {
                if (!words.contains(word)) {
                    words.add(word.toLowerCase().trim());//setup word list
                }
                if (wordFreq.containsKey(word.toLowerCase())) {
                    wordFreq.put(word.toLowerCase().trim(), wordFreq.get(word.toLowerCase().trim()) + 1);
                } else {
                    wordFreq.put(word.toLowerCase().trim(), 1);
                }
            }
        }
        Collections.sort(words);
    }

}

