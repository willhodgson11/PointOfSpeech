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
     * @param sentence
     */
    public ArrayList<String> viterbi(String[] sentence){
        // initialize a map to track the current score of the "Winners"
        HashMap<String, Double> currScore = new HashMap<>();
        // initialize a list of maps tracking the previous state with the best current state
        ArrayList<Map<String, String>> backTrack = new ArrayList<>();
        // initialize a resulting path with one tag per word
        ArrayList<String> res = new ArrayList<>();

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

        // Set up placeholders for best state and score
        String bestState = null;
        double bestScore;
        // Find the best state for the last word
        for(String state : currScore.keySet()){
            // If current state has a better score than previous best
            if(bestState == null || currScore.get(state) > bestScore){
                bestState = state;                      // Record this new best state
                bestScore =  currScore.get(state) ;     // Record this new best score
            }
        }
        // Add best state for last word at end of list
        res.add(backTrack.size()-1, bestState);
        // Build the resulting path
        for(int i = backTrack.size()-1; i > 0; i--){
            Map temp = backTrack.get(i);                    // Get the map for the current word
            String prev = (String) temp.get(bestState);     // Get the previous state
            res.add(i-1, prev);                       // Insert the previous state
            bestState = prev;                               // Advance
            i--;
        }

        return res;
    }

    /**
     * trains a model (observation and transition probabilities) on corresponding lines
     * (sentence and tags) from a pair of training files.
     */
    public void trainModel() throws IOException {
        BufferedReader sentence = new BufferedReader(new FileReader(trainSentences));
        BufferedReader sentenceTags = new BufferedReader(new FileReader(trainTags));


        String line;
        // Read each line in both files and get the contents (words and tags)
        while ((line = sentence.readLine()) != null) {
            String[] words = line.split("\\ ");                     // Create array with all words
            String[] tags = sentenceTags.readLine().split("\\ ");   // Create array with all tags
            HashMap<String, Integer> wordFreq = new HashMap<>();          // Create a map with the total occurrences of each word

            // Loop through each word in the sentence, recording the corresponding tag each time it occurs
            for (int i = 0; i < words.length - 1; i++) {
                // If the current word has not yet been seen before
                if (!observations.containsKey(words[i])) {
                    // Add that word to the frequency map
                    wordFreq.put(words[i], 1);
                    // Create a new map to track the frequency of the states for that word
                    Map possibleTags = observations.get(words[i]);
                    // If the current state has not been observed for this particular word
                    if (possibleTags.containsKey(tags[i])) {
                        // Initialize a count for the current tag
                        possibleTags.put(words[i], 1.0);
                    }
                    // Otherwise, that state has already been observed. Increment the count by 1.
                    else {
                        possibleTags.put(words[i], (Double) possibleTags.get(tags[i]) + 1.0);
                    }
                }
                // Increment the total frequency of that word by one
                else {
                    wordFreq.put(words[i], wordFreq.get(words[i] + 1));
                }
                // loop through each tag, recording the frequency at which each tag transitions to another
                if(i< tags.length-1){
                    // If the current tag has not been observed before
                    if(!transitions.containsKey(tags[i])){
                        // Create a new map to document the frequency of all transitions from the current state to all possible states
                        HashMap<String, Double> transitionFreq = new HashMap<>();
                        // Set the frequency of the transition from the current state to the next to 1
                        transitionFreq.put(tags[i+1], 1.0);
                        // Add the transitions frequency map to transitions, associated with the current tag
                        transitions.put(tags[i], transitionFreq);
                    }
                    // If the current tag has been observed
                    else {
                        // If the transition to the next tag has not yet been observed
                        if (!transitions.get(tags[i]).containsKey(tags[i+1])){
                            // Record the first occurrence of that transition
                            transitions.get(tags[i]).put(tags[i+1], 1.0);
                        }
                        // If the transition to the next tag has been observed
                        else {
                            // Increment the frequency of the transition from the current state to the next state by 1
                            transitions.get(tags[i]).put(tags[i + 1], transitions.get(tags[i]).get(tags[i + 1]));
                        }
                }
            }
        }
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
