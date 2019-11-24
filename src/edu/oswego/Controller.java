package edu.oswego;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {

    private static final ConcurrentHashMap<String, Document> bookList = new ConcurrentHashMap<String, Document>();
    private static final ConcurrentHashMap<String, Integer> wordList = new ConcurrentHashMap<String, Integer>();
    private final ConcurrentHashMap<String, Long> englishWordFreq = new ConcurrentHashMap<String, Long>();
    @FXML
    public TextField Query;
    @FXML
    public TextArea Result;
    @FXML
    public Label ResultNum;
    private int index;//init on query completion to 1, + and - on result change

    public Controller() throws IOException {
        //initialize gui (aka add loading thing) ////or do i make the initial page the loading one and then switch off afterwards
        System.out.println("Start Check");

        //load english words into hashmap<word, int of how common word is>
        loadEnglishWordList();

        //grab info and do stuff with them
        //this is the path where my unstructured txt files are located
        File f = new File("C:\\projects\\Java\\SearchEngine\\src\\edu\\oswego\\assets\\books");
        String[] fileName = f.list();
        if (fileName != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            for (int i = 0; i < fileName.length; i++) {
                if (fileName[i].endsWith(".txt")) {//if it is a text file
                    // System.out.println("Loading " + fileName[i]);
                    Loader temp = new Loader(fileName[i], f, bookList);//thread it
                    executor.execute(temp);
                } else {
                    System.out.println("ERROR: \"" + fileName[i] + "\" is not a valid .txt file");
                }
            }
            executor.shutdown();
            while (!executor.isShutdown()) {
                //wait for executor to shutdown
            }

        } else {
            //system can't run without txt files so exit
            System.out.println("Can not find assets");
            System.out.println("Please make sure path file is correct and that the text files are in the correct locations");
            System.exit(0);
        }


        //go to main gui page
    }

    /**
     * does main search analysis
     * if query textbox is empty then do nothing, possibly show warning "please enter a query"
     *
     * @param e - unused
     */
    public void searchPressed(ActionEvent e) {
        //get what is in search bar
        String query = "test word";

        //if null do nothing
        if (query != null) {
            query = query.toLowerCase().trim();

            Pattern pattern = Pattern.compile("\\s");
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {//if query is a phrase search


                phraseSearch(query);
            } else {
                //if not phrase do seaarch normally
                //do preliminary comparison to check if word is too common or not in known list of words, if so give user decision to continue

                //check if word is known and if no attempt spellchecking
                if (queryIsKnown(query)) {

                    //if word size 4 or less check if word is too common in english language by =>630072115 (somewhat arbitrary number)


                    singleSearch(query);


                } else {
                    //query not known so attempt spell checking
                    if (spellCheck(query)) {//true if succeeds
                        //foud new word
                    } else {
                        //spellchecking failed so tell user their query is not recognized

                    }
                }
            }


            //do query seperation


            //post search check for spell checking on some decision
            //if decided to un-disable spellcheck button and query user, dont do spell checking itself unless
        }
    }

    /**
     * When query had been processed, shows first result in result array
     * if query has not been processed or already on top level result, do nothing
     *
     * @param e - unused
     */
    public void topTab(ActionEvent e) {
        if (false) {

            //likely need thread to change GUI

        } //else do nothing
    }

    /**
     * Changes result display to next result, if last result do nothing
     *
     * @param e - unused
     */
    public void nextTab(ActionEvent e) {

    }

    /**
     * Changes result display to previous result, if first result do nothing
     *
     * @param e - unused
     */
    public void prevTab(ActionEvent e) {

    }

    public void spellCheckClick(ActionEvent e) {
        //on user press make hyprling+label invisable and clear them
        //searchFor(spellCheckedWord); <-basically

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
        boolean known = false;
        for (String key : wordList.keySet()) {
            if (key.equals(query)) {
                known = true;

            }
        }
        return known;
    }

    /**
     * does the actual searching
     * either will return result or will process result itself
     *
     * @param query - user query
     */
    private void singleSearch(String query) {


        //idf = log(collection size/docs with term)
        //tf * idf = ranking result
        //can store results as <bookname, ranking>?
        //where 0 is one that doesnt have the term at all
    }

    private void phraseSearch(String query) {

    }


    /**
     * loads file of english words and an int of how common the word is into a hashmap
     *
     * @throws IOException
     */
    private void loadEnglishWordList() throws IOException {
        File file = new File("C:\\projects\\Java\\SearchEngine\\src\\edu\\oswego\\english_words.txt");

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

        //possibly create a book object and place that inside a concurrent hashmap when done,
        // book ojb can have all the tables then after threads done controller can take info in all books and create super tables(lol)

        //when done put info where it needs to be(decide on this later)

    }


}