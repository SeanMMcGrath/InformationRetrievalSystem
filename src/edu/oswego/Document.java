package edu.oswego;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Document {
    private String name;
    //have a list of words in class
    //<word, postings> inverted term paragraph index
    private final ConcurrentHashMap<Term, int[]> invertedIndex = new ConcurrentHashMap<Term, int[]>();


    public Document(String name){
        this.name = name;
    }



    public String getName() {
        return name;
    }

    public ConcurrentHashMap<Term, int[]> getInvertedIndex() {
        return invertedIndex;
    }

    public static String cleanString(String string){
        String result = string;
        result = result.replace("!","");
        result = result.replace("?","");
        result = result.replace(",","");
        result = result.replace(";","");
        result = result.replace(".","");
        result = result.replace("(","");
        result = result.replace(")","");
        result = result.replace("[","");
        result = result.replace("]","");
        result = result.replace("{","");
        result = result.replace("}","");
        result = result.replace(":","");
        result = result.replace("\"","");
        result = result.replace("\'","");
        return result;
    }
}

class Paragraph{
    private final ArrayList<String> words = new ArrayList<String>();
    private final AtomicReference<String> paragraph = new AtomicReference<String>();//paragraph is untrimmed
    private int totalWorCount = 0;

    public Paragraph(String paragraph) {
        this.paragraph.set(paragraph);

        String temp = Document.cleanString(paragraph);

        for (String word : temp.split("\\s+")) {
            if (!words.contains(word)) {
                words.add(word.toLowerCase().trim());//setup word list
            }
            totalWorCount++;
        }
        Collections.sort(words);
    }

}

 /*
        • basic term lookup  -   term-document index; per para and per whole thing?
        • “phrase search” 	 -	 positional indexes; in each paragraph
        • spelling correction - k-gram-term index and edit distance */
