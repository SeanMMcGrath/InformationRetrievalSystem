package edu.oswego;

import java.util.ArrayList;

public class Document {
    //have a list of words in class
    //<word, postings> inverted term paragraph index
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
}