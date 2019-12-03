package edu.oswego;

import java.util.ArrayList;
import java.util.HashMap;

public class PositionalIndex {
    private HashMap<String, WordData> index = new HashMap<>();

    /**
     * inserts word into index, with related data
     *
     * @param word     - string of the word
     * @param bookName - the name of book word is in
     * @param paraNum  - paragraph index the word is in
     * @param position - position in paragraph of word
     */
    public void add(String word, String bookName, int paraNum, int position) {

        if (index.containsKey(word)) {
            WordData w = index.get(word);
            w.add(bookName, paraNum, position);
            index.put(word, w);
        } else {
            WordData w = new WordData(word);
            w.add(bookName, paraNum, position);
            index.put(word, w);
        }
    }

    /**
     * gets positions for word in book
     *
     * @param word     - word to look up
     * @param bookName - name of book
     * @return - array list of positions in book of word, null if word is not in book
     */
    public ArrayList<Position> getPositions(String word, String bookName) {
        if (index.containsKey(word)) {
            return index.get(word).words.get(bookName);
        } else {
            return null;
        }
    }
}

class WordData {
    //<bookname, list of indexes>
    HashMap<String, ArrayList<Position>> words = new HashMap<>();
    String word;

    public WordData(String word) {
        this.word = word;
    }

    /**
     * @param bookName - name of book
     * @param paraNum  - which paragraph word is in
     * @param position - position of word in paragraph
     */
    public void add(String bookName, int paraNum, int position) {
        Position p = new Position(paraNum, position);
        if (words.containsKey(bookName)) {
            ArrayList<Position> tmp = words.get(bookName);
            tmp.add(p);
            words.put(bookName, tmp);
        } else {
            ArrayList<Position> tmp = new ArrayList<>();
            tmp.add(p);
            words.put(bookName, tmp);
        }
    }
}

class Position {
    int paraNum;
    int position;

    public Position(int paraNum, int position) {
        this.paraNum = paraNum;
        this.position = position;
    }
}