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

    private static final ConcurrentHashMap<String, Document> bookList = new ConcurrentHashMap<String, Document>();
    private final ConcurrentHashMap<String, Long> englishWordFreq = new ConcurrentHashMap<String, Long>();
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
        System.out.println("Start Check");

        //setup stoplist
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
            createKGramIndex();
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
     * Single Search: check if word is known, if not attempt spell correction -> search word or spell-checked word
     * @param e - unused
     */
    public void searchPressed(ActionEvent e) {
        //clear any old stuff
        if (spellCheckLink.isVisible()) {
            spellCheckLink.setText("");
            spellCheckLink.setVisible(false);
        }
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
                if (query.startsWith("\"") && query.endsWith("\"") && matcher.find()) {//if query is a phrase search (has space in it and starts/ends with ")
                    phraseSearch(query);
                } else if (matcher.find()) {//if multiple words and now phrase search
                    termSearch(query);
                } else {//if only single word
                    singleSearch(query);
                }
            } else {
                //already have seen this query and have stored it
                //so just have to print it out and change current results to the new query
                System.out.println("Query already known");
                results = oldQueries.get(query);
                Result.setText(bookList.get(results[0].bookName).paragraphs.get(results[0].paragraphNum).rawParagraph);
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
        if (results != null && index != 1) {
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
            if (matcher.find()) {//if query is a phrase search (has space in it)
                phraseSearch(newQuery);
            } else {
                singleSearch(newQuery);
            }

        } else {
            System.out.println("ERROR: spell check event pressed when there was no spell checking possible");
        }
        //might be good to now have spell check word in global string but instead use whatever the hyperlink is set to
    }

    private boolean spellCheck(String query) {
        String result;
        //does spell checking

        //chose best word putting in global spellcheckword string

        //put in hyperlink button and make it visible for user (prob with a thread?) to wait on user OK

        //keep result in global var that waits on user to OK the spell check
        //on user OK do query'ing on the word in another method
        //searchFor(result);
        return false;
    }

    /**
     * checks whether query is a known word out of known books
     *
     * @param query - user query
     * @return - true if known, false if not known
     */
    private boolean queryIsKnown(String query) {
        System.out.println("checking known");
        for (String bookName : bookList.keySet()) {
            for (Paragraph p : bookList.get(bookName).paragraphs) {
                if (p.words.contains(query)) {
                    System.out.println("known!");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * decides what to do with single query
     * calls getSingleResult to search for result if query vali
     * displays top result and stores all results ranked greater than 0
     *
     * @param query - user query
     */
    private void singleSearch(String query) {
        //check if word is known and if no attempt spellchecking
        if (queryIsKnown(query)) {
            //if word size 4 or less check if word is too common in english language by =>630072115 (somewhat arbitrary number)
            if (stopList.contains(query)) {
                System.out.println("query too common");
                Result.setText("Query is too vague. Please enter new query.");
            } else {
                getSingleResult(query);
            }
        } else {
            //query not known so attempt spell checking
            // String newQuery = spellCheck(query);
            System.out.println("Attempting spellchecking");

            // String attemptSpellcheck = spellCheck(query);

            if (spellCheck(query)) {//true if succeeds
                //foud new wordf
                Result.setText("No Results.");
                //set up the prompt for user to accept the spell checked word sugestion

            } else {
                //spellchecking failed so tell user their query is not recognized
                Result.setText("Query is not recognized.");
            }
        }



        //idf = log(collection size/docs with term)
        //tf * idf = ranking result
        //larger the ranking the better
        //can store results as <bookname, ranking>? in array[].. as [Result] object
        //where 0 is one that doesnt have the term at all
    }

    /**
     * searches for multiple words
     *
     * @param query
     */
    private void termSearch(String query) {
        String[] splitQuery = query.split("\\s");

        for (int i = 0; i < splitQuery.length; i++) {
            if (stopList.contains(splitQuery[i])) {
                System.out.println("multi-query too common");
                Result.setText("Query contains parts that are too vague. Please enter new query.");
                return;
            }
        }
        for (int i = 0; i < splitQuery.length; i++) {
            if (!queryIsKnown(query)) {
                //spell check

                return;
            }
        }

        //else do search
        ArrayList<Result> initialResults = new ArrayList<>();

        for (String bookName : bookList.keySet()) {
            for (int i = 0; i < splitQuery.length; i++) {
                ArrayList<Position> positions = positionalIndex.getPositions(splitQuery[i], bookName);

                if (positions != null) {
                    Result[] tmp = new Result[bookList.get(bookName).paragraphs.size()];
                    for (Position p : positions) {
                        if (tmp[p.paraNum - 1] == null) {
                            String para = "...";
                            for (Paragraph pa : bookList.get(bookName).paragraphs) {
                                if (pa.index == p.paraNum) {
                                    para = pa.rawParagraph;
                                    break;
                                }
                            }
                            Result r = new Result(bookName, para, p.paraNum, query, splitQuery.length);///query...?
                            r.frequencies[i]++;
                            tmp[p.paraNum - 1] = r;
                        } else {
                            tmp[p.paraNum - 1].frequencies[i]++;
                        }
                    }
                    for (int j = 0; j < tmp.length; j++) {
                        if (tmp[j] != null) {
                            initialResults.add(tmp[j]);
                        }
                    }
                }
            }
        }//take all rankings or DF per book, adds them together and does something with them on the rankings of that book

        //do rankings
        for (int i = 0; i < initialResults.size(); i++) {

        }

        //then sort

        //then display

    }

    private void getSingleResult(String query) {
        ArrayList<Result> initialResults = new ArrayList<>();

        int collectionSize = getCollectionSize();//number of paragraphs
        int docsWithQuery = docsWithTerm(query);
        double idf = Math.log((double) collectionSize / docsWithQuery);

        //get all the doc frequencies as well as initializing all the results
        for (String bookName : bookList.keySet()) {
            ArrayList<Position> positions = positionalIndex.getPositions(query, bookName);

            if (positions != null) {
                Result[] tmp = new Result[bookList.get(bookName).paragraphs.size()];
                for (Position p : positions) {
                    if (tmp[p.paraNum - 1] == null) {
                        String para = "...";
                        for (Paragraph pa : bookList.get(bookName).paragraphs) {
                            if (pa.index == p.paraNum) {
                                para = pa.rawParagraph;
                                break;
                            }
                        }
                        Result r = new Result(bookName, para, p.paraNum, query);
                        tmp[p.paraNum - 1] = r;
                    } else {
                        tmp[p.paraNum - 1].frequency++;
                    }
                }
                for (int i = 0; i < tmp.length; i++) {
                    if (tmp[i] != null) {
                        initialResults.add(tmp[i]);
                    }
                }
            }
        }

        if (initialResults.size() > 0) {
            //ranking is df * idf
            for (int i = 0; i < initialResults.size(); i++) {
                initialResults.get(i).setRanking(initialResults.get(i).frequency * idf);
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
                if (spellCheck(splitQuery[i])) {
                    Result.setText("No Results.");
                    break;
                } else {
                    Result.setText("Query is not recognized.");
                    break;
                }
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

    private int docsWithTerm(String term) {
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

    private int getCollectionSize() {
        int collectionCounter = 0;
        for (String bookName : bookList.keySet()) {
            collectionCounter = collectionCounter + bookList.get(bookName).paragraphs.size();
        }
        return collectionCounter;
    }

    private void createPositionalIndex() {
        System.out.println("creating positional index");
        System.out.println(bookList.size());
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

    private void createKGramIndex(){

    }


    /**
     * loads file of english words and an int of how common the word is into a hashmap
     *
     * @throws IOException
     */
    private void loadEnglishWordList() throws IOException {
        File file = new File("C:\\projects\\Java\\SearchEngine\\src\\edu\\oswego\\assets\\english_words.txt");

        if (file.exists()) {

            BufferedReader br = new BufferedReader(new FileReader(file));

            String st;
            while ((st = br.readLine()) != null) {
                String[] temp = st.split("\\s");
                try {
                    englishWordFreq.put(temp[0], Long.parseLong(temp[1]));
                } catch (NumberFormatException e) {
                    System.out.println("This is not a number or is too many bytes");
                    System.out.println(e.getMessage());
                }
            }

        } else {
            System.out.println("ERROR: English word file not found, ending program");
            System.exit(0);
        }
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