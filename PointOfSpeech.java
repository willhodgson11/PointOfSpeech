import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PointOfSpeech {

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
    public PointOfSpeech(){

    }

    /**
     * perform Viterbi decoding to find the best sequence of tags for a line (sequence of words)
     */
    public String[] viterbi(String[] sentence){
        HashMap<String, Double> currScore = new HashMap<>();
        ArrayList<String> nextStates = new ArrayList<>();
        ArrayList<Map<String, State>> backPointer = new ArrayList<>();

        currScore.put("#", 0.0);


    }

    /**
     * trains a model (observation and transition probabilities) on corresponding lines
     * (sentence and tags) from a pair of training files.
     */
    public void trainModel{

    }

    /**
     * console-based test method to give the tags from an input line.
     */
    public void testUserInput(){}

    /**
     * file-based test method to evaluate the performance on a pair of test files
     */
    public void testFile(){}

}
