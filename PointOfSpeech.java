import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Create a part of speech (POS) tagger labels each word in a sentence
 * with its part of speech (noun, verb, etc.) using a bigram Hidden Markov Model.
 * @author willhodgson, Dartmouth CS10, Winter 2023
 * @author cullumtwiss, Dartmouth CS10, Winter 2023
 * Thanks to recitation leader Bill Zheng for Viterbi backtrack implementation advice
 */
public class PointOfSpeech {
    public HashMap<String, HashMap<String, Double>> observations;   // Maps a given tag with all its associated words and their normalized frequencies
    public HashMap<String, HashMap<String, Double>> transitions;    // Maps a given state to all its possible nextStates, with the appropriate score
    public String trainSentencesFile;
    public String trainTagsFile;
    public String testTagsFile;
    public String testSentencesFile;
    public int unseen = 10;

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
     * @param sentence list of words split by whitespace. Periods and punctuation should be their own 'word'
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
            // Create a map to track the scores of all the potential new states
            HashMap<String, Double> newScore = new HashMap<>();
            // Loop through all current possible states
            for(String currState : currScore.keySet()) {
                // if the transition score map of the current state is null, skip this iteration
                if (transitions.get(currState) == null) {continue;}
                // Loop through all possible transitions
                for (String nextState : transitions.get(currState).keySet()) {
                    // Define the score of the next state as the current score plus the transition score to the next state
                    double nextScore = currScore.get(currState) + transitions.get(currState).get(nextState);
                    // If there is no observation for the current state, skip current iteration
                    if(observations.get(nextState)!=null) {
                        // If the current word is known to have current tag,
                        if (observations.get(nextState).containsKey(currentWord)) {
                            nextScore += observations.get(nextState).get(currentWord);  // Add the observation score
                        } else {
                            nextScore -= unseen;       // Otherwise, assign unseen penalty of -100
                        }
                    }
                    // If the current state does not have an associated score, or if the new calculated score is better than the existing one,
                    if (!newScore.containsKey(nextState) || nextScore > newScore.get(nextState)) {
                        newScore.put(nextState, nextScore);       // Assign this computed score to the associated state
                        previous.put(nextState, currState);       // Add each winning state to a map with its predecessor
                    }
                }
            }
            // Advance
            currScore = newScore;
            backTrack.add(previous);
        }

        // Set up placeholders for best state
        String bestState = null;
        // Find the best state for the last word
        for(String state : currScore.keySet()){
            // If current state has a better score than previous best
            if(bestState == null || currScore.get(state) > currScore.get(bestState)){
                bestState = state;                      // Record this new best state
            }
        }
        // Create an arraylist to temporarily hold the (reversed) backtrack
        ArrayList<String> backwards = new ArrayList<>();
        // Add best state for last word at end of list
        backwards.add(bestState);
        // Build the resulting path
        int end = backTrack.size()-1;
        while(!backTrack.isEmpty()){
            Map temp = backTrack.remove(end);               // Get the map for the current word
            String prev = (String) temp.get(bestState);     // Get the previous state
            backwards.add(prev);                            // Insert the previous state
            bestState = prev;                               // Advance
            end--;
        }
        // Because the previous map associates a state to its predecessor, the backtrack is, well backwards.
        // Flip the backtrack
        while(!backwards.isEmpty()){
            res.add(backwards.remove(backwards.size()-1));
        }
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
            String[] words = line.toLowerCase().split("\\s+");                     // Create array with all words
            String[] tags = sentenceTags.readLine().split("\\s+");   // Create array with all tags

            // If a blank line is encountered, skip this iteration
            if(tags.length ==0 || words.length == 0) continue;

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
                keyMap.put(nextState, Math.log10(keyMap.get(nextState) / keyCount));
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
        // Incorporate simple quit command - exits program if q key entered
        if(!line.equals("q")) {
            // Get user input as a line and turn to lowercase
            String[] words = line.toLowerCase().split("\\s+");
            // Calculate the tags for that sentence
            String[] tags = viterbi(words).toArray(new String[0]);
            System.out.println("Tags: " + String.join(" ", tags));
            testUserInput();
        }
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
            // Create a list of words, converted to lowercase and split by whitespace
            String[] sentence = line.toLowerCase().split("\\s+");
            // Create a list of tags split by whitespace
            String[] tempTags = testTags.readLine().split("\\s+");
            // Convert tags sentence to a string so we can add the # start state
            List<String> tagsList = new LinkedList<String>(Arrays.asList(tempTags));
            tagsList.add(0, "#");
            // Calculate the model-generated tags
            ArrayList<String> calculatedTags = viterbi(sentence);
            // Loop through each calculated tag, checking against the actual tag. Keep track of the number correct!
            for(int i=0; i<calculatedTags.size()-1; i++){
                if(!tagsList.get(i).equals(calculatedTags.get(i))){
                    incorrect++;
                }
                else{
                    correct++;
                }
            }
        }
        System.out.println("Result of file test: ");
        System.out.println("Correct Tags: " + correct);
        System.out.println("Incorrect Tags: " + incorrect);

    }


    public void hardCodeTest(){

    }
    /**
     * Driver method, for ease of toggling between different sets of test and train files
     * @param testName first word in file names
     * @throws IOException
     */
    static void execute(String testName) throws IOException {
        if(testName.equals("Brown")){
            PointOfSpeech testBrown = new PointOfSpeech("Texts/brown-train-sentences.txt",
                    "Texts/brown-train-tags.txt", "Texts/brown-test-sentences.txt",
                    "Texts/brown-test-tags.txt");
            System.out.println("Test files: Brown, Train files: Brown");
            testBrown.trainModel();
            testBrown.testModel();
            testBrown.testUserInput();
        }
        if (testName.equals("Simple")){
            PointOfSpeech testSimple = new PointOfSpeech("Texts/simple-train-sentences.txt",
                    "Texts/simple-train-tags.txt", "Texts/simple-test-sentences.txt",
                    "Texts/simple-test-tags.txt");
            System.out.println("Test files: Simple, Train files: Simple");
            testSimple.trainModel();
            testSimple.testModel();
            testSimple.testUserInput();
        }
        if (testName.equals("hardCodeViterbi")) {
            PointOfSpeech testViterbi = new PointOfSpeech(null, null, null, null);
            System.out.println("Hardcoded test from PD7");

            // Hardcode PD7 transition map. Rather than change compare in viterbi method, scores are all negative
            testViterbi.unseen = 10;
            // This way the lowest number/highest frequency still wins
            HashMap<String, Double> NP = new HashMap<>(Map.of("chase",10.0));
            HashMap<String, Double> CNJ = new HashMap<>(Map.of("and",10.0));
            HashMap<String, Double> V = new HashMap<>(Map.of("get",1.0, "chase", 3.0, "watch", 6.0));
            HashMap<String, Double> N = new HashMap<>(Map.of("cat", 4.0, "dog", 4.0, "watch", 2.0));
            testViterbi.observations.put("NP", NP);
            testViterbi.observations.put("CNJ", CNJ);
            testViterbi.observations.put("V", V);
            testViterbi.observations.put("N", N);

            // Hardcode PD7 Observations
            HashMap<String, Double> startobs = new HashMap<>(Map.of("NP", 3.0, "N", 7.0));
            HashMap<String, Double> NPobs = new HashMap<>(Map.of("V", 8.0, "CNJ", 2.0));
            HashMap<String, Double> Vobs = new HashMap<>(Map.of("NP", 4.0, "CNJ", 2.0, "N", 4.0));
            HashMap<String, Double> Nobs = new HashMap<>(Map.of("CNJ", 2.0, "V", 8.0));
            HashMap<String, Double> CNJobs = new HashMap<>(Map.of("NP", 2.0, "N", 4.0, "V", 4.0));
            testViterbi.transitions.put("#", startobs);
            testViterbi.transitions.put("NP", NPobs);
            testViterbi.transitions.put("CNJ", CNJobs);
            testViterbi.transitions.put("V", Vobs);
            testViterbi.transitions.put("N", Nobs);

            System.out.println(testViterbi.viterbi(new String[]{"chase", "watch", "dog","chase"," watch"}));

        }
    }

    /**
     * Main method to test
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        execute("hardCodeViterbi");
        //execute("Brown");
        //execute("Simple");


    }
}
