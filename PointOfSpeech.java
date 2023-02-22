import org.opencv.core.Point;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PointOfSpeech {

    public HashMap<String, Integer> observations;               // Maps a given word with
    public HashMap<String, Map<String, Double>> transitions;    // Maps a given state to all its possible nextStates, with the appropriate score
    public String trainSentences;
    public String trainTags;

    /**
     * class to hold the score and previous type of given state
     */
    public class State{

        public double score;
        public String prev;

        public State(Double score, String prev){
            this.score = score;
            this.prev = prev;
        }


    }


    /**
     *  constructor
     */
    public PointOfSpeech(String trainSentencesFile, String trainTagsFile){
        this.trainSentences = trainSentencesFile;
        this.trainTags = trainTagsFile;

    }

    /**
     * perform Viterbi decoding to find the best sequence of tags for a line (sequence of words)
     */
    public String[] viterbi(String[] sentence){
        //yes
        HashMap<String, Double> currScore = new HashMap<>();
        //yes
        HashSet<String> currStates = new HashSet<>();
        //idk
        HashMap<String, State> temp = new HashMap<>();
        //yes
        ArrayList<Map<String, State>> backPointer = new ArrayList<>();

        currScore.put("#", 0.0);    // Let  "#" be the tag "before" the start of the sentence.

        for(String currentWord : sentence){

            for(String state : currStates){
                // For each possible transition associated with the current state
                for(String transition : transitions.get(state).keySet()){

                }

            }
        }



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
