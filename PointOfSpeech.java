import org.opencv.core.Point;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PointOfSpeech {

    public HashMap<String, HashMap<String, Double>> observations;   // Maps a given word with all its associated tags and their normalized frequencies
    public HashMap<String, HashMap<String, Double>> transitions;    // Maps a given state to all its possible nextStates, with the appropriate score
    public String trainSentences;
    public String trainTags;

    /**
     * class to hold the score and previous type of given state
     */
    public class State{

        public double score;
        public String prev;

        public State(String prev, Double score){
            this.score = score;
            this.prev = prev;
        }
    }


    /**
     * Constructor
     * @param trainSentencesFile
     * @param trainTagsFile
     */
    public PointOfSpeech(String trainSentencesFile, String trainTagsFile){
        this.trainSentences = trainSentencesFile;
        this.trainTags = trainTagsFile;

    }

    /**
     * perform Viterbi decoding to find the best sequence of tags for a line (sequence of words)
     */
    public String[] viterbi(String[] sentence){
        // initialize a map to track the current score of the "Winners"
        HashMap<String, Double> currScore = new HashMap<>();
        // initialize a list of maps tracking the previous state with the best current state
        ArrayList<Map<String, String>> backTrack = new ArrayList<>();
        // initialize a resulting path with one tag per word
        String[] res = new String[sentence.length];

        currScore.put("#", 0.0);    // Let "#" be the tag "before" the start of the sentence.

        // For each unique word
        for(String currentWord : sentence){
            // Create a map to track the previous state associated with a given state
            HashMap<String, String> previous = new HashMap<>();
            // Loop through all current possible states
            for(String currState : currScore.keySet()){
                // Create a map to track the scores of all the potential new states
                HashMap<String, Double> newScore = new HashMap<>();
                // Loop through all possible transitions
                for(String nextState : transitions.get(currState).keySet()) {
                    // Define the score of the next state as the current score plus the transition score to the next state
                    double nextScore = currScore.get(currState) + transitions.get(currState).get(nextState);
                    // If the current word is known to have current tag,
                    if (observations.get(currentWord).containsKey(currState)) {
                        nextScore += observations.get(currentWord).get(currState);  // Add the observation score
                    } else {
                        nextScore -= 100;       // Otherwise, assign unseen penalty of -100
                    }
                    // If the current state does not have an associated score, or if the new calculated score is better than the existing one,
                    if(!newScore.containsKey(nextState) || nextScore > newScore.get(nextState)){
                        newScore.put(nextState, nextScore);       // Assign this computed score to the associated state
                    }
                }
                // Advance
                currScore = newScore;
                String prevState = currState;       // For ease of understanding
                // Add each winning state to a map with its predecessor
                for(String state : currScore.keySet()) {
                    previous.put(prevState, state);
                }
            }
            backTrack.add(previous);
        }

        return res;
    }

    /**
     * trains a model (observation and transition probabilities) on corresponding lines
     * (sentence and tags) from a pair of training files.
     */
    public void trainModel() throws IOException {
        BufferedReader sentence = new BufferedReader(new FileReader(trainSentences));
        BufferedReader tags = new BufferedReader(new FileReader(trainTags));
    }

    /**
     * console-based test method to give the tags from an input line.
     */
    public void testUserInput(){

    }

    /**
     * file-based test method to evaluate the performance on a pair of test files
     */
    public void testFile(){

    }

    public static void main(String[] args) {
        PointOfSpeech test0 = new PointOfSpeech("simple-train-sentences.txt", "simple-train-tags.txt")
    }
}
