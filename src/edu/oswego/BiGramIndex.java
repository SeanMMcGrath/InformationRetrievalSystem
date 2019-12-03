package edu.oswego;

import java.util.ArrayList;
import java.util.HashMap;

import static edu.oswego.Controller.wordFreq;

public class BiGramIndex {

    //<word, bigram>
    HashMap<String, String[]> index;

    /**
     * takes a list of words and turns it into a bigram inedx
     *
     * @param words
     */
    public BiGramIndex(ArrayList<String> words) {
        index = new HashMap<>();

        //process word list into bigram index
        for (String word : words) {
            try {
                index.put(word, createBigram(word));
            } catch (NegativeArraySizeException e) {
                System.out.println(word.length());
                System.exit(0);
            }
        }
    }

    /**
     * processes a query into bigram and does spell checking on it
     *
     * @param query - user input query
     * @return - word that is best matched to user query, used to suggest to user a spell-checked word
     */
    public String process(String query) {
        String[] bigram = createBigram(query);

        //threshold for bigram overlapping(rounds down sine its an int)
        int overlapThreshold = (bigram.length / 2);
        double jaccardThreshold = .50;//test on 75%

        //find the overlaps for each word
        //<word, overlap>
        HashMap<String, Integer> overlaps = new HashMap<>();
        for (String word : index.keySet()) {
            int overlap = 0;
            for (String aBigram : bigram) {
                if (doesMatch(aBigram, index.get(word))) {
                    overlap++;
                }
            }
            if (overlap >= overlapThreshold) {//if it passes the threshold then keep it
                overlaps.put(word, overlap);
            }
        }
        if (overlaps.size() == 0) {
            System.out.println("no overlaps");
            return null;
        }

        //prune list using Jaccard Coefficient
        ArrayList<String> words = new ArrayList<>();
        for (String word : overlaps.keySet()) {
            double jaccard = (double) overlaps.get(word) / ((index.get(word).length + bigram.length) - overlaps.get(word));
            if (jaccard >= jaccardThreshold) {
                words.add(word);
            }
        }
        if (words.size() == 0) {
            System.out.println("no jaccard");
            return null;//no results, save time and return
        } else if (words.size() == 1) {
            return words.get(0);
        }

        //find min edit distance to rank remaining terms (keeping only the best)
        //<word, minDistance from query term>
        HashMap<String, Integer> distance = new HashMap<>();
        for (String word : words) {
            distance.put(word, minDistance(word, query));
        }

        //if more than 1 term remain(out of best edit distance), use how common the word is in documents to decide which to return
        String currentBest = null;
        ArrayList<String> list = new ArrayList<>();
        for (String word : distance.keySet()) {//find best distance found
            if (currentBest == null) {
                currentBest = word;
                list.add(currentBest);
            } else {
                if (distance.get(currentBest) > distance.get(word)) {
                    currentBest = word;
                    list = new ArrayList<>();
                    list.add(currentBest);
                } else if (distance.get(currentBest).equals(distance.get(word))) {
                    list.add(word);
                }
            }
        }
        if (list.size() == 1) {
            return list.get(0);
        } else {
            String result = list.get(0);
            int common = wordFreq.get(list.get(0));
            for (String word : list) {
                if (wordFreq.get(word) > common) {
                    result = word;
                    common = wordFreq.get(word);
                }
            }
            return result;
        }
    }

    private String[] createBigram(String word) {
        String[] bigram = new String[word.length() - 1];

        for (int i = 0; i < word.length() - 1; i++) {
            bigram[i] = word.substring(i, i + 2);
        }

        return bigram;
    }

    /**
     * matches two bigrams
     *
     * @param gram - part of bigram of the user's input
     * @param word - bigram of the word to be matched to
     * @return - integer of how many bigrams match between the two
     */
    private boolean doesMatch(String gram, String[] word) {
        for (int i = 0; i < word.length; i++) {
            if (word[i].equals(gram)) {
                return true;
            }
        }

        return false;
    }

    private int minDistance(String word1, String word2) {
        int len1 = word1.length();
        int len2 = word2.length();

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 0; i < len1; i++) {
            char c1 = word1.charAt(i);
            for (int j = 0; j < len2; j++) {
                char c2 = word2.charAt(j);

                if (c1 == c2) {
                    dp[i + 1][j + 1] = dp[i][j];
                } else {
                    //chars not equal so try all edit types and choose best one
                    int replace = dp[i][j] + 1;
                    int insert = dp[i][j + 1] + 1;
                    int delete = dp[i + 1][j] + 1;

                    int min = replace > insert ? insert : replace;
                    min = delete > min ? min : delete;
                    dp[i + 1][j + 1] = min;
                }
            }
        }

        return dp[len1][len2];
    }
}
