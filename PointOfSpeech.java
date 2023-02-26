import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.*;

public class PointOfSpeech {
    //TODO account for periods, punctuation within sentences and lines with only those
    public HashMap<String, HashMap<String, Double>> observations;   // Maps a given tag with all its associated words and their normalized frequencies
    public HashMap<String, HashMap<String, Double>> transitions;    // Maps a given state to all its possible nextStates, with the appropriate score
    public String trainSentencesFile;
    public String trainTagsFile;
    public String testTagsFile;
    public String testSentencesFile;

    /**
     * Constructor
     * @param trainSentencesFile
     * @param trainTagsFile
     */
    public PointOfSpeech(String trainSentencesFile, String trainTagsFile, String testSentencesFile, String testTagsFile){
        this.trainSentencesFile = trainSentencesFile;
        this.trainTagsFile = trainTagsFile;
        this.testTagsFile = testTagsFile;
        this.testSentencesFile = testSentencesFile;
        this.observations = new HashMap<>();
        this.transitions = new HashMap<>();
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
            System.out.println("Current word: "+currentWord);
            // Create a map to track the previous state associated with a given state
            HashMap<String, String> previous = new HashMap<>();
            // Create a map to track the scores of all the potential new states
            HashMap<String, Double> newScore = new HashMap<>();
            // Loop through all current possible states
            for(String currState : currScore.keySet()) {
                System.out.println("Current state: "+ currState);
                // if the transition score map of the current state is null, skip this iteration
                if (transitions.get(currState) == null) {continue;}
                // Loop through all possible transitions
                for (String nextState : transitions.get(currState).keySet()) {
                    // If the transition from the current state to the next hasn't been encountered, add it to the list of possible states
                    //if (!transitions.get(currState).keySet().contains(nextState)) {transitions.get(currState).keySet().add(nextState);}
                    System.out.println("Next state " + nextState + " has score " + transitions.get(currState).get(nextState));
                    // Define the score of the next state as the current score plus the transition score to the next state
                    double nextScore = currScore.get(currState) + transitions.get(currState).get(nextState);
                    // If the current word is known to have current tag,
                    if (observations.get(nextState).containsKey(currentWord)) {
                        nextScore += observations.get(nextState).get(currentWord);  // Add the observation score
                    } else {
                        nextScore -= 100;       // Otherwise, assign unseen penalty of -100
                    }
                    // If the current state does not have an associated score, or if the new calculated score is better than the existing one,
                    if (!newScore.containsKey(nextState) || nextScore > newScore.get(nextState)) {
                        newScore.put(nextState, nextScore);       // Assign this computed score to the associated state
                    }
                }
                String prevState = currState;       // For ease of understanding
                // Add each winning state to a map with its predecessor
                for (String state : newScore.keySet()) {
                    previous.put(state, prevState);
                }
            }
            // Advance
            System.out.println(currentWord + ": " +currScore+ newScore);
            currScore = newScore;
            backTrack.add(previous);
            System.out.println("backMap: " + previous);
        }
        System.out.println(backTrack);

        // Set up placeholders for best state
        String bestState = null;
        // Find the best state for the last word
        for(String state : currScore.keySet()){
            System.out.println("state" +state);
            // If current state has a better score than previous best
            if(bestState == null || currScore.get(state) > currScore.get(bestState)){
                bestState = state;                      // Record this new best state
            }
        }
        // Add best state for last word at end of list
        res.add(bestState);
        // Build the resulting path
        for(int i = 0; i< backTrack.size()-1; i--){
            Map temp = backTrack.get(i);                    // Get the map for the current word
            String prev = (String) temp.get(bestState);     // Get the previous state
            //res.add(prev);                                 // Insert the previous state
            bestState = prev;                               // Advance
            i++;
        }
        System.out.println(res);
        return res;
    }

    /**
     * trains a model (observation and transition probabilities) on corresponding lines
     * (sentence and tags) from a pair of training files.
     */
    public void trainModel() throws IOException {
        BufferedReader sentence = new BufferedReader(new FileReader(trainSentencesFile));
        BufferedReader sentenceTags = new BufferedReader(new FileReader(trainTagsFile));

        HashMap<String, Double> startMap = new HashMap<>();     // Record all states that follow the start, i.e. first states
        transitions.put("#", startMap);                         // Add the empty starting state map to transitions

        String line;
        // Read each line in both files and get the contents (words and tags)
        while ((line = sentence.readLine()) != null) {
            String[] words = line.toLowerCase().split("\\ ");                     // Create array with all words
            String[] tags = sentenceTags.readLine().split("\\ ");   // Create array with all tags

            // If the first state has not been observed yet
            if(!startMap.containsKey(tags[0])){
                startMap.put(tags[0], 1.0);    // Initialize a count for that first transition
            }
            // Otherwise increment the count for that transition
            else{
                startMap.put(tags[0], startMap.get(tags[0])+1);
            }
            // Loop through each tag in the sentence, recording the corresponding word each time it occurs
            for (int i = 0; i < tags.length - 1; i++) {
                // If the current tag has not yet been seen before
                if (!observations.containsKey(tags[i])) {
                    // Create a new map to track the frequency of the words for that state
                    HashMap<String, Double> possibleWords = new HashMap<>();
                    // Initialize a count for the current word
                    possibleWords.put(words[i], 1.0);
                    // Assign that count to the corresponding state
                    observations.put(tags[i], possibleWords);
                }
                // Otherwise, this state has previously been encountered
                else{
                    // Create a map to track the frequency of words for this state
                    HashMap<String, Double> possibleWords = observations.get(tags[i]);
                    // If the current word has not been observed for this particular state
                    if (!possibleWords.containsKey(words[i])) {
                        // Initialize a count for the current word
                        possibleWords.put(words[i], 1.0);
                    }
                    // Otherwise, that word has already been observed. Increment the count by 1.
                    else {
                        possibleWords.put(words[i], (Double) possibleWords.get(words[i]) + 1.0);
                    }
                }
                // loop through each tag, recording the frequency at which each tag transitions to another
                if (i < tags.length - 1) {
                    // If the current tag has not been observed before
                    if (!transitions.containsKey(tags[i])) {
                        // Create a new map to document the frequency of all transitions from the current state to all possible states
                        HashMap<String, Double> transitionFreq = new HashMap<>();
                        // Set the frequency of the transition from the current state to the next to 1
                        transitionFreq.put(tags[i + 1], 1.0);
                        // Add the transitions frequency map to transitions, associated with the current tag
                        transitions.put(tags[i], transitionFreq);
                    }
                    // If the current tag has been observed
                    else {
                        // If the transition to the next tag has not yet been observed
                        if (!transitions.get(tags[i]).containsKey(tags[i + 1])) {
                            // Record the first occurrence of that transition
                            transitions.get(tags[i]).put(tags[i + 1], 1.0);
                        }
                        // If the transition to the next tag has been observed
                        else {
                            // Increment the frequency of the transition from the current state to the next state by 1
                            transitions.get(tags[i]).put(tags[i + 1], transitions.get(tags[i]).get(tags[i + 1])+1);
                        }
                    }
                }
            }
        }
        sentence.close();
        sentenceTags.close();

        // Normalize the transitions, dividing each frequency by a count of all occurrences of that transition from the current state
        normalize(transitions);
        //Normalize the observations, dividing each tag->word frequency by the total occurrences of the associated tag
        normalize(observations);
    }

    /**
     * Helper function to normalize the frequency of all key values in a map.
     * get the value associated with each key for a given state and divide by the
     * sum the values of all keys (associated words or next states) for a that state.
     * @param map A filled map tracking all tags, their associated keys, and the value
     *            of those keys
     */
    private void normalize(HashMap<String, HashMap<String, Double>> map){
        // For each state
        for (String key : map.keySet()) {
            HashMap<String, Double> keyMap = map.get(key);
            // Count all occurrences of a transition from the current state
            double keyCount = countKeys(keyMap);
            // Normalize each frequency by dividing by the total number of transitions
            for (String nextState : keyMap.keySet()) {
                keyMap.put(nextState, Math.log(keyMap.get(nextState) / keyCount));
            }
        }
    }

    /**
     * Helper function to count the total value of keys in a hashmap
     * @param map Map of strings (words, tags) to doubles (frequency)
     * @return
     */
    public double countKeys(HashMap<String, Double> map){
        int keyCount = 0;

        // Count all occurrences of a given key
        for (String key : map.keySet()) {
            keyCount += map.get(key);
        }
        return keyCount;
    }


    /**
     * console-based test method to give the tags from an input line.
     */
    public void testUserInput(){
        // Create a scanner to receive input
        Scanner input = new Scanner(System.in);
        System.out.println("Viterbi > ");
        String line = input.nextLine();
        String[] words = line.toLowerCase().replace(".", "").split("\\ ");
        String[] tags = viterbi(words).toArray(new String[0]);
        System.out.println("tag" +tags);

    }

    /**
     * file-based test method to evaluate the performance on a pair of test files
     */
    public void testModel() throws IOException {
        BufferedReader testTags = new BufferedReader(new FileReader(testTagsFile));
        BufferedReader testWords = new BufferedReader(new FileReader(testSentencesFile));
        String line;
        int incorrect = 0;
        int correct = 0;
        while((line = testWords.readLine()) != null){
            String[] sentence = line.toLowerCase().replace(".", "").split("\\ ");
            String[] actualTags = testTags.readLine().replace(".", "").split("\\ ");
            ArrayList<String> calculatedTags = viterbi(sentence);
            for(int i=0; i<calculatedTags.size()-1; i++){
                if(!actualTags[i].equals(calculatedTags.get(i))){
                    incorrect++;
                }
                else{
                    correct++;
                }
            }
        }
        System.out.println("Correct Tags: " + correct);
        System.out.println("Incorrect Tags: " + incorrect);

    }

    public static void main(String[] args) throws IOException {
        PointOfSpeech test0 = new PointOfSpeech("Texts/simple-train-sentences.txt",
                "Texts/simple-train-tags.txt", "Texts/simple-test-sentences.txt",
                "Texts/simple-test-tags.txt");
        test0.trainModel();
        //test0.testUserInput();
        test0.testModel();

    }
}
