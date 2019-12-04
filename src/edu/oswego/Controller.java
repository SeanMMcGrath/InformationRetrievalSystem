package edu.oswego;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {

    private static final PositionalIndex positionalIndex = new PositionalIndex();
    //holds all past unique queries as: <query, ranked results>
    private final HashMap<String, Result[]> oldQueries = new HashMap<>();
    private final ArrayList<String> stopList = new ArrayList<>();
    @FXML
    public Label Book;
    @FXML
    public Hyperlink spellCheckLink;
    //holds current results in order
    private Result[] results;

    public static final ConcurrentHashMap<String, Integer> wordFreq = new ConcurrentHashMap<>();
    //index for spell checking
    private BiGramIndex biGramIndex;
    private static final ConcurrentHashMap<String, Document> bookList = new ConcurrentHashMap<String, Document>();

    @FXML
    public TextField Query;
    @FXML
    public TextArea Result;
    @FXML
    public Label ResultNum;
    //index of current displayed result; init on query completion to 1, + and - on result change
    private int index;
    //query that has been spell checked
    private String spellCheckedQuery;

    public Controller() {
        //initialize gui (aka add loading thing) ////or do i make the initial page the loading one and then switch off afterwards

        //setup stop-list
        stopList.add("a");
        stopList.add("an");
        stopList.add("and");
        stopList.add("are");
        stopList.add("as");
        stopList.add("at");
        stopList.add("be");
        stopList.add("by");
        stopList.add("for");
        stopList.add("from");
        stopList.add("has");
        stopList.add("he");
        stopList.add("in");
        stopList.add("is");
        stopList.add("its");
        stopList.add("of");
        stopList.add("on");
        stopList.add("that");
        stopList.add("the");
        stopList.add("to");
        stopList.add("was");
        stopList.add("were");
        stopList.add("will");
        stopList.add("with");


        //grab info and do stuff with them
        //this is the path where my unstructured txt files are located
        File f = new File("C:\\projects\\Java\\SearchEngine\\src\\edu\\oswego\\assets\\books");
        String[] fileName = f.list();
        if (fileName != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            for (int i = 0; i < fileName.length; i++) {
                if (fileName[i].endsWith(".txt")) {//if it is a text file
                    Loader temp = new Loader(fileName[i], f, bookList);//thread it
                    executor.execute(temp);
                } else {
                    System.out.println("ERROR: \"" + fileName[i] + "\" is not a valid .txt file");
                }
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
                //wait for executor to shutdown
            }

            //now create indexes
            createPositionalIndex();
            createBiGramIndex();
        } else {
            //system can't run without txt files so exit
            System.out.println("Can not find assets");
            System.out.println("Please make sure path file is correct and that the text files are in the correct locations");
            System.exit(0);
        }
    }

    /**
     * does main search analysis
     * if query textbox is empty then do nothing, possibly show warning "please enter a query"
     *
     * Flow: clear remenants from any past queries -> check if no query -> check if query already known, if so print known data without re-seraching
     * -> check if phrase or basic search
     * Phrase search: make sure all words known, if one isnt spell check it, promt user on new fully spellchecked phrase
     * Single Search: check if word is known, if not attempt spell correction -> search word or spell-checked word and display
     * @param e - unused
     */
    public void searchPressed(ActionEvent e) {
        //clear any old stuff
        spellCheckLink.setText("");
        spellCheckLink.setVisible(false);
        ResultNum.setText("0/0");
        Result.setText("");
        Book.setText("");
        results = null;
        index = 1;

        //get what is in search bar
        //if null do nothing(no query)

        if (Query.getText() != null && !Query.getText().equals("") && !Query.getText().trim().isEmpty()) {

            String query = Query.getText();
            System.out.println("Query: " + query.trim());
            Query.setText("");

            query = query.toLowerCase().trim();
            if (!oldQueries.containsKey(query)) {
                Pattern pattern = Pattern.compile("\\s");
                Matcher matcher = pattern.matcher(query);
                if (query.startsWith("\"") && query.endsWith("\"") && matcher.find()) {//if query is a phrase search (has at least one space in it and starts/ends with ")
                    phraseSearch(query);
                } else {
                    termSearch(query);
                }
            } else {
                //already have seen this query and have stored it
                //so just have to print it out and change current results to the new query
                System.out.println("Query already known");
                results = oldQueries.get(query);
                Result.setText(results[0].rawParagraph);
                Result.setWrapText(true);
                index = 1;
                ResultNum.setText(Integer.toString(index) + "/" + Integer.toString(results.length));
                Book.setText(results[0].bookName);
            }
        } else {
            System.out.println("Null query");
        }
    }

    /**
     * When query had been processed, shows first result in result array
     * if query has not been processed or already on top level result, do nothing
     *
     * @param e - unused
     */
    public void topTab(ActionEvent e) {
        if (results != null && index != 1) {//if not already displaying top and there are results to display
            index = 1;
            Result.setText(results[index - 1].rawParagraph);
            ResultNum.setText(Integer.toString(index) + "/" + Integer.toString(results.length));
            Book.setText(results[index - 1].bookName);
            System.out.println(results[index - 1].ranking);
        } //else do nothing
    }

    /**
     * Changes result display to next result, if last result do nothing...
     *
     * @param e - unused
     */
    public void nextTab(ActionEvent e) {
        if (results != null && index != results.length) {
            index++;
            Result.setText(results[index - 1].rawParagraph);
            Result.setWrapText(true);
            ResultNum.setText(Integer.toString(index) + "/" + Integer.toString(results.length));
            Book.setText(results[index - 1].bookName);
            System.out.println(results[index - 1].ranking);
        }
    }

    /**
     * Changes result display to previous result, if first result do nothing
     *
     * @param e - unused
     */
    public void prevTab(ActionEvent e) {
        if (results != null && index != 1) {
            index--;
            Result.setText(results[index - 1].rawParagraph);
            ResultNum.setText(Integer.toString(index) + "/" + Integer.toString(results.length));
            Book.setText(results[index - 1].bookName);
            System.out.println(results[index - 1].ranking);
        }
    }

    /**
     * user has indicated they want to check spelling, so do search on the new word
     *
     * @param e
     */
    public void spellCheckClick(ActionEvent e) {
        //on user press make hyprling+label invisable and clear them
        //searchFor(spellCheckedWord); <-basically
        spellCheckLink.setVisible(false);
        spellCheckLink.setText("");

        if (spellCheckedQuery != null) {
            String newQuery = spellCheckedQuery;
            spellCheckedQuery = null;

            Pattern pattern = Pattern.compile("\\s");
            Matcher matcher = pattern.matcher(newQuery);
            if (newQuery.startsWith("\"") && newQuery.endsWith("\"") && matcher.find()) {//if query is a phrase search (has space in it and starts/ends with ")
                phraseSearch(newQuery);
            } else {//if multiple words and now phrase search
                termSearch(newQuery);
            }
        } else {
            System.out.println("ERROR: spell check event pressed when there was no spell checking possible");
        }
        //might be good to now have spell check word in global string but instead use whatever the hyperlink is set to
    }

    /**
     * does spell checking on query
     * @param query - word to spell correct
     * @return - null if spellchecking failed, or word that has been spell corrected
     */
    private String spellCheck(String query) {
        return biGramIndex.process(query);
    }

    /**
     * checks whether query is a known word out of words in books
     * @param query - user query
     * @return - true if known, false if not known
     */
    private boolean queryIsKnown(String query) {
        for (String bookName : bookList.keySet()) {
            for (Paragraph p : bookList.get(bookName).paragraphs) {
                if (p.words.contains(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * searches for one or more word(s)
     * if multiple words queried, searches for instances that contain ALL words(and search)
     * no OR or NOT searches
     *
     * very ugly algorithm i apologise :(
     * @param query
     */
    private void termSearch(String query) {
        String[] splitQuery = query.split("\\s");

        for (int i = 0; i < splitQuery.length; i++) {
            if (stopList.contains(splitQuery[i])) {
                System.out.println("query too common");
                Result.setText("Query contains parts that are too vague. Please enter new query.");
                return;
            }
        }
        boolean changeMade = false;
        for (int i = 0; i < splitQuery.length; i++) {
            if (!queryIsKnown(splitQuery[i])) {
                //spell check
                if (splitQuery[i].length() <= 1) {//dont spell check single length words since using bi-gram
                    Result.setText("No Results.");
                    return;
                } else {
                    String tmp = spellCheck(splitQuery[i]);
                    if (tmp == null) {
                        Result.setText("No Results.");//spell checking failed
                        return;
                    } else {
                        splitQuery[i] = tmp;
                        changeMade = true;
                    }
                }

            }
        }
        if (changeMade) {
            Result.setText("No Results.");
            //set spell check prompt
            spellCheckedQuery = String.join(" ", splitQuery);
            spellCheckLink.setText("Did you mean " + spellCheckedQuery + "?");
            spellCheckLink.setVisible(true);
            return;
        }

        //else do search
        ArrayList<Result> initialResults = new ArrayList<>();

        for (String bookName : bookList.keySet()) {
            Result[] tmp = new Result[bookList.get(bookName).paragraphs.size()];
            for (int i = 0; i < splitQuery.length; i++) {
                ArrayList<Position> positions = positionalIndex.getPositions(splitQuery[i], bookName);

                if (positions != null) {

                    for (Position p : positions) {
                        if (tmp[p.paraNum - 1] == null) {
                            //store raw paragraph in result for easy display
                            String para = "...";
                            for (Paragraph pa : bookList.get(bookName).paragraphs) {
                                if (pa.index == p.paraNum) {
                                    para = pa.rawParagraph;
                                    break;
                                }
                            }
                            Result r = new Result(bookName, para, p.paraNum, query, splitQuery.length);
                            r.frequencies[i]++;
                            tmp[p.paraNum - 1] = r;
                        } else {
                            tmp[p.paraNum - 1].frequencies[i]++;
                        }
                    }
                }
            }
            for (int j = 0; j < tmp.length; j++) {
                if (tmp[j] != null) {
                    initialResults.add(tmp[j]);
                }
            }
        }


        //do rankings

        if (initialResults.size() > 0) {
            if (splitQuery.length == 1) {
                //if it is a single word query

                //paragraph idf
                int collectionSize = getCollectionSize();//number of paragraphs
                int parasWithQuery = paragraphsWithTerm(splitQuery[0]);
                double idf = Math.log((double) collectionSize / parasWithQuery);

                //book idf
                int bookCollectionSize = bookList.size();
                int booksWithQuery = booksWithTerm(splitQuery[0]);
                double book_idf = Math.log((double) bookCollectionSize / booksWithQuery);

                //book name, book tf-idf ranking
                HashMap<String, Integer> bookFreq = bookFrequency(splitQuery[0]);//get tf-idf of each book

                for (int i = 0; i < initialResults.size(); i++) {
                    //ranking = (para tf+para idf) + (book tf+book idf)
                    double rank = (initialResults.get(i).frequencies[0] + idf) + (bookFreq.get(initialResults.get(i).bookName) + book_idf);
                    initialResults.get(i).setRanking(rank);
                }


                results = initialResults.toArray(new Result[0]);
                //sort results
                Arrays.sort(results);
                //display initial results
                Result.setText(results[index - 1].rawParagraph);
                ResultNum.setText(Integer.toString(index) + "/" + Integer.toString(results.length));
                Book.setText(results[index - 1].bookName);
                System.out.println("top ranked is " + results[index - 1].ranking);
                oldQueries.put(query, results);
            } else {
                //multiple word query ranking

                //paragraph idf
                int collectionSize = getCollectionSize();
                int[] parasWithQuery = new int[splitQuery.length];
                double[] idf = new double[splitQuery.length];

                //book idf
                int bookCollectionSize = bookList.size();
                int[] booksWithQuery = new int[splitQuery.length];
                double[] book_idf = new double[splitQuery.length];

                ArrayList<HashMap<String, Integer>> list = new ArrayList<>();

                for (int i = 0; i < splitQuery.length; i++) {
                    //setup the idf's and book ranks for each query
                    parasWithQuery[i] = paragraphsWithTerm(splitQuery[i]);
                    idf[i] = Math.log((double) collectionSize / parasWithQuery[i]);
                    booksWithQuery[i] = booksWithTerm(splitQuery[i]);
                    book_idf[i] = Math.log((double) bookCollectionSize / booksWithQuery[i]);

                    HashMap<String, Integer> bookFreq = bookFrequency(splitQuery[i]);//get tf-idf of each book
                    list.add(bookFreq);

                }//if freq > 0 then add, don't add to a 0 or false results will happen

                Iterator<Result> iterator = initialResults.iterator();
                while (iterator.hasNext()) {
                    Result tmp = iterator.next();
                    for (int j = 0; j < tmp.frequencies.length; j++) {
                        if (tmp.frequencies[j] == 0) {
                            //if any frequencies are 0(not found) then the result is invalid and removed
                            iterator.remove();
                            break;
                        }
                    }
                }
                if (initialResults.size() != 0) {
                    for (int i = 0; i < initialResults.size(); i++) {
                        //ranking = (para tf+para idf) + (book tf+book idf)
                        double[] ranks = new double[splitQuery.length];
                        for (int j = 0; j < splitQuery.length; j++) {
                            if (initialResults.get(i).frequencies[j] > 0) {
                                ranks[j] = (initialResults.get(i).frequencies[j] + idf[j]) + (list.get(j).get(initialResults.get(i).bookName) + book_idf[j]);
                            } else {
                                ranks[j] = 0;
                            }
                        }
                        double rank = 0;
                        for (int j = 0; j < ranks.length; j++) {
                            rank = rank + ranks[j];
                        }
                        initialResults.get(i).setRanking(rank);
                    }
                    results = initialResults.toArray(new Result[0]);
                    //sort results
                    Arrays.sort(results);
                    //display initial results
                    Result.setText(results[index - 1].rawParagraph);
                    ResultNum.setText(Integer.toString(index) + "/" + Integer.toString(results.length));
                    Book.setText(results[index - 1].bookName);
                    System.out.println("top ranked is " + results[index - 1].ranking);
                    oldQueries.put(query, results);
                } else {
                    //no results including all queries
                    Result.setText("No Results.");
                }
            }
        } else {
            //no results found, this should be unreachable since spell checking is in place
            Result.setText("No Results.");
        }
    }

    /**
     * user positional index to
     * @param query -
     */
    private void phraseSearch(String query) {
        //split phrase and check each word in phrase for validity
        //attempt to spell check each word and promt user if change can be made through spell checking
        //if there are no results remember to put that in results
        boolean cont = true;
        String temp = query.replace("\"", "");
        String[] splitQuery = temp.split("\\s");
        for (int i = 0; i < splitQuery.length; i++) {
            if (!queryIsKnown(splitQuery[i])) {
                cont = false;
                //part of query not known so attempt spell check and bail on search
                if (splitQuery[i].length() > 1) {
                    // if (spellCheck(splitQuery[i])) {

                    // } else {
                    Result.setText("Query is not recognized.");
                    // }
                }
                Result.setText("No Results.");
            }
        }

        if (cont) {//continue if query is valid
            ArrayList<Result> initialResults = new ArrayList<>();

            for (String bookName : bookList.keySet()) {
                boolean valid = true;
                HashMap<String, ArrayList<Position>> allPositions = new HashMap<>();
                for (int i = 0; i < splitQuery.length; i++) {
                    ArrayList<Position> positions = positionalIndex.getPositions(splitQuery[i], bookName);
                    if (positions == null) {
                        valid = false;
                        break;
                    }
                    allPositions.put(splitQuery[i], positions);
                }
                if (valid) {
                    //compare first position to ones after it, looking for connected ones
                    for (Position p : allPositions.get(splitQuery[0])) {

                        boolean fullMatch = true;
                        int paraNum = p.paraNum;
                        int position = p.position;
                        for (int i = 1; i < splitQuery.length; i++) {
                            boolean match = false;
                            for (Position inner : allPositions.get(splitQuery[i])) {
                                if (inner.paraNum == paraNum && inner.position == position + i) {
                                    match = true;
                                    break;
                                }
                            }
                            if (!match) {
                                fullMatch = false;
                                break;//if no match for any part, then there is no phrase so break
                            }
                        }
                        if (fullMatch) {
                            //match found, create result or increment frequency of existing one
                            if (!initialResults.isEmpty()) {
                                boolean exists = false;
                                for (int i = 0; i < initialResults.size(); i++) {
                                    if (initialResults.get(i).bookName.equalsIgnoreCase(bookName) && initialResults.get(i).paragraphNum == paraNum) {
                                        exists = true;
                                        initialResults.get(i).frequency++;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    String para = "";
                                    for (Paragraph pa : bookList.get(bookName).paragraphs) {
                                        if (pa.index == paraNum) {
                                            para = pa.rawParagraph;
                                            break;
                                        }
                                    }
                                    initialResults.add(new Result(bookName, para, paraNum, query));
                                }
                            } else {
                                String para = "";
                                for (Paragraph pa : bookList.get(bookName).paragraphs) {
                                    if (pa.index == paraNum) {
                                        para = pa.rawParagraph;
                                        break;
                                    }
                                }
                                initialResults.add(new Result(bookName, para, paraNum, query));
                            }
                        }
                    }
                }//if not valid(one or more words not in book at all) continue to next book
            }

            //rank and display results
            if (initialResults.size() > 0) {
                for (int i = 0; i < initialResults.size(); i++) {
                    //for phrase search just using DF instead of DF IDF
                    initialResults.get(i).setRanking(initialResults.get(i).frequency);
                }

                results = initialResults.toArray(new Result[0]);
                //sort results
                Arrays.sort(results);
                //display initial results
                Result.setText(results[index - 1].rawParagraph);
                ResultNum.setText(Integer.toString(index) + "/" + Integer.toString(results.length));
                Book.setText(results[index - 1].bookName);
                System.out.println("top ranked is " + results[index - 1].ranking);
                oldQueries.put(query, results);
            } else {
                Result.setText("No Results.");
            }
        }
    }

    private HashMap<String, Integer> bookFrequency(String query) {
        HashMap<String, Integer> result = new HashMap<>();

        for (String bookName : bookList.keySet()) {
            ArrayList<Position> temp = positionalIndex.getPositions(query, bookName);
            if (temp == null) {
                result.put(bookName, 0);
            } else {
                result.put(bookName, temp.size());
            }
        }

        return result;
    }

    private int paragraphsWithTerm(String term) {
        int docCount = 0;
        for (String bookName : bookList.keySet()) {
            for (Paragraph p : bookList.get(bookName).paragraphs) {
                if (p.words.contains(term)) {
                    docCount++;
                }
            }
        }
        return docCount;
    }

    private int booksWithTerm(String term) {
        int docCount = 0;
        for (String bookName : bookList.keySet()) {
            for (Paragraph p : bookList.get(bookName).paragraphs) {
                if (p.words.contains(term)) {
                    docCount++;
                    break;//break from paragraphs so each book gets one counter
                }
            }
        }
        return docCount;
    }

    private int getCollectionSize() {
        int collectionCounter = 0;
        for (String bookName : bookList.keySet()) {
            collectionCounter = collectionCounter + bookList.get(bookName).paragraphs.size();
        }
        return collectionCounter;
    }

    private void createPositionalIndex() {
        System.out.println("creating positional index");
        for (String bookName : bookList.keySet()) {
            Document d = bookList.get(bookName);
            //go through each document and place the words from each into positional index
            for (Paragraph p : d.paragraphs) {
                String[] words = p.paraByWords;
                for (int i = 0; i < words.length; i++) {
                    positionalIndex.add(words[i].toLowerCase(), bookName, p.index, i);
                }
            }
        }
    }

    private void createBiGramIndex() {
        System.out.println("creating bigram");
        ArrayList<String> wordList = new ArrayList<>(wordFreq.keySet());
        biGramIndex = new BiGramIndex(wordList);
    }
}


/**
 * Takes a path and filename of book in txt file format
 * uses that to create a book object and populates it with the information from the text file
 * then places that Document object in possibly static concurrent hashmap inside Controller class
 */
class Loader extends Thread {
    private String filename;
    private File path;

    private ConcurrentHashMap<String, Document> bookList;

    public Loader(String filename, File path, ConcurrentHashMap<String, Document> bookList) {
        this.filename = filename;
        this.path = path;
        this.bookList = bookList;
    }

    public void run() {
        //grab data from file and process it
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(path.toString() + "\\" + filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ArrayList<Paragraph> paragraphs = new ArrayList<Paragraph>();
        String currentParagraph = "";
        int index = 1;

        while (true) {//read each line and seperate into paragraphs
            try {
                String tmp = in.readLine();

                if (tmp == null) {
                    if (!currentParagraph.isEmpty()) {
                        Paragraph temp = new Paragraph(currentParagraph, filename, index);
                        paragraphs.add(temp);
                    }
                    break;
                } else if (tmp.isEmpty()) {
                    if (!currentParagraph.isEmpty()) {
                        Paragraph temp = new Paragraph(currentParagraph, filename, index);
                        paragraphs.add(temp);
                        index++;
                    }
                    currentParagraph = "";
                } else {
                    if (currentParagraph.isEmpty()) {
                        currentParagraph = tmp;
                    } else {
                        currentParagraph += " " + tmp;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Document document = new Document(filename, paragraphs);
        bookList.put(document.name, document);
    }
}

/**
 * holds a ranked result
 */
class Result implements Comparable<Result> {
    String bookName;
    String rawParagraph;
    int paragraphNum;//index of para inside document object
    String query;
    int frequency;
    double ranking;

    int[] frequencies;

    public Result(String bookName, String rawParagraph, int paragraphNum, String query, int freqNum) {
        this.bookName = bookName;
        this.paragraphNum = paragraphNum;
        this.query = query;
        this.rawParagraph = rawParagraph;
        this.frequencies = new int[freqNum];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = 0;
        }
    }

    public Result(String bookName, String rawParagraph, int paragraphNum, String query) {
        this.bookName = bookName;
        this.paragraphNum = paragraphNum;
        this.query = query;
        this.frequency = 1;
        this.rawParagraph = rawParagraph;
    }


    public void setRanking(double ranking) {
        this.ranking = ranking;
    }

    @Override
    public int compareTo(Result o) {
        return Double.compare(o.ranking, this.ranking);
    }
}

//todo: hold results for old queries inside query object in concurrenthashmap so if user re-does a query its instant