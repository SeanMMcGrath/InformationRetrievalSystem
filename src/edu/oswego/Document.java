package edu.oswego;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Document {
    //have a list of words in class
    //<word, postings> inverted term paragraph index
    private final ConcurrentHashMap<Term, int[]> invertedIndex = new ConcurrentHashMap<Term, int[]>();
    String name;
    String fileName;

    //<index, paragraph at index>
    //final ConcurrentHashMap<Integer, Paragraph> paragraphs = new ConcurrentHashMap<Integer, Paragraph>();
    ArrayList<Paragraph> paragraphs;

    public Document(String name, ArrayList<Paragraph> paragraphs) {
        //reformat filename into
        this.fileName = name;
        String temp = name.replace(".txt", "");
        temp = temp.replace("-", " ");
        this.name = temp;

        this.paragraphs = paragraphs;
    }

    public static String cleanString(String string) {
        String result = string;
        result = result.replace("!", "");
        result = result.replace("?", "");
        result = result.replace(",", "");
        result = result.replace(";", "");
        result = result.replace(".", "");
        result = result.replace("(", "");
        result = result.replace(")", "");
        result = result.replace("[", "");
        result = result.replace("]", "");
        result = result.replace("{", "");
        result = result.replace("}", "");
        result = result.replace(":", "");
        result = result.replace("\"", "");
        result = result.replace("\'", "");
        result = result.replace("_", "");
        return result;
    }

    public String getName() {
        return name;
    }

    public ConcurrentHashMap<Term, int[]> getInvertedIndex() {
        return invertedIndex;
    }
}

 /*
        • basic term lookup  -   term-document index; per para and per whole thing?
        • “phrase search” 	 -	 positional indexes; in each paragraph
        • spelling correction - k-gram-term index and edit distance */
