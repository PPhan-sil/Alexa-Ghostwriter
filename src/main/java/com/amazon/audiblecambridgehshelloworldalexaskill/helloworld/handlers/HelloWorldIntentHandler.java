package com.amazon.audiblecambridgehshelloworldalexaskill.helloworld.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Request;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static com.amazon.ask.request.Predicates.intentName;

/**
 * Handles HelloWorldIntent
 */
public class HelloWorldIntentHandler implements RequestHandler {

    private final String speechTextWithWord = "This is Alexa GhostWriter, %1$s is your word. \n %2$s";
    private final String speechTextNoWord = "No word given";

    /**
     * Determine if this handler can handle the intent (but doesn't actually handle it)
     *
     * This is called by the ASK framework.
     *
     * @param input
     * @return
     */
    @Override
    public boolean canHandle(HandlerInput input) {
        return input.matches(intentName("GhostWriterIntent"));
    }

    /**
     * Actually handle the event here.
     *
     * This is called by the ASK framework.
     *
     * @param input
     * @return
     */
    @Override
    public Optional<Response> handle(HandlerInput input) {
        log(input, "Starting request");
        logSlots(input);

        Map<String, Slot> slots = getSlots(input);

        String speechText = null;

        // if we're given a word
        if(slots.containsKey("WordSlot") && null != slots.get("WordSlot").getValue()) {
            try {
                speechText = String.format(speechTextWithWord, slots.get("WordSlot").getValue(), createSong(slots.get("WordSlot").getValue()));
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            speechText = speechTextNoWord;
        }

        log(input, "Speech text response is " + speechText);

        // response object with a card (shown on devices with a screen) and speech (what alexa says)
        return input.getResponseBuilder()
                .withSpeech(speechText) // alexa says this
                .withSimpleCard("HelloWorld", speechText) // alexa will show this on a screen
                .build();
    }

    public static String createSong(String keyWord) throws Exception {
        ArrayList<JsonObject> syns = new ArrayList<>();
        syns = getSyn(keyWord);

        ArrayList<JsonObject> rhymes = new ArrayList<>();
        rhymes = getRhyme(syns, keyWord);
        Set<JsonObject> rhymeSet = new HashSet<>(rhymes);
        rhymes.clear();
        rhymes.addAll(rhymeSet);

        StringBuilder song = new StringBuilder();
        for (JsonObject word : rhymes) {
            song.append(word.get("word").getAsString());
            song.append("\n");
        }

        ArrayList<JsonObject> syns2 = new ArrayList<>();
        syns2 = getSyn(syns.get(0).get("word").getAsString());

        ArrayList<JsonObject> rhymes2 = new ArrayList<>();
        rhymes2 = getRhyme(syns2, syns.get(0).get("word").getAsString());
        Set<JsonObject> rhymeSet2 = new HashSet<>(rhymes2);
        rhymes2.clear();
        rhymes2.addAll(rhymeSet2);

       /* StringBuilder syn1 = new StringBuilder();
        for(JsonObject word : syns){
            syn1.append(word.get("word").getAsString());
            syn1.append(" ");
            syn1.append(word.get("numSyllables").getAsString());
            syn1.append(" ");
            syn1.append(word.get("tags").getAsJsonArray().toString());
            syn1.append("\n");
        }

        StringBuilder song2 = new StringBuilder();
        for (JsonObject word : rhymes2) {
            song2.append(word.get("word").getAsString());
            song2.append(" ");
            song2.append(word.get("numSyllables").getAsString());
            song2.append(" ");
            song2.append(word.get("tags").getAsJsonArray().toString());
            song2.append("\n");
        }*/

        String finalSong = structure(rhymes, rhymes2, keyWord);


        return finalSong;
    }

    private static String structure(ArrayList<JsonObject> rhymes, ArrayList<JsonObject> rhyme2, String keyWord) {
        String song = "";
        String lineOne = "Once upon a time there was a " + keyWord + ".";
        String lineTwo = "That had a " + rhymes.get((int)(Math.random()*rhymes.size())).get("word").getAsString() + ".";
        String lineThree = "Which then became a " + rhyme2.get((int)(Math.random()*rhyme2.size())).get("word").getAsString() + ".";
        String lineFour = "Instead of a " + rhyme2.get((int)(Math.random()*rhyme2.size())).get("word").getAsString() + ".";
        String lineFive = "In the end it lived happily ever after as a " + rhymes.get((int)(Math.random()*rhymes.size())).get("word").getAsString() + ".";
        song = lineOne + "\n" + lineTwo + "\n" + lineThree + "\n" + lineFour + "\n" + lineFive;
        return song;
    }


    public static ArrayList<JsonObject> getSyn(String keyWord) throws Exception {
        if(keyWord.contains(" ")){
            keyWord = keyWord.replaceAll(" ", "+");
        }

        String apiEndpoint = "https://api.datamuse.com/words?ml=" + keyWord + "&md=s&md=p";
        String query = "";

        // lets get some data from it
        HttpURLConnection urlc = (HttpURLConnection) new URL(apiEndpoint).openConnection();

        urlc.setRequestMethod("GET");
        urlc.setRequestProperty("Accept", "application/json");

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (urlc.getInputStream())));

        StringBuilder sb = new StringBuilder();
        String output;
        System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        String result = sb.toString();
        urlc.disconnect();


        ArrayList<JsonObject> words = new ArrayList<>();
        while (result.indexOf("{") > 0) {
            String getParse = result.substring(result.indexOf("{"), result.indexOf("}") + 1);
            com.google.gson.JsonParser parser = new JsonParser();
            JsonElement thisElement = parser.parse(getParse);
            words.add(thisElement.getAsJsonObject());
            result = result.substring(result.indexOf("}") + 1);
        }


        return words;

    }

    private static ArrayList<JsonObject> getRhyme(ArrayList<JsonObject> syns, String keyWord) throws Exception {

        if(keyWord.contains(" ")){
            keyWord = keyWord.replaceAll(" ", "+");
        }

        ArrayList<JsonObject> wordsObj = new ArrayList<>();
        for (int i = 0; i < syns.size(); i++) {

            String rhymeWord = syns.get(i).get("word").getAsString();

            if(rhymeWord.indexOf(" ") > 0){
                rhymeWord = rhymeWord.replaceAll(" ", "+");
            }

            String apiEndpoint = "https://api.datamuse.com/words?ml=" + rhymeWord + "&rel_rhy=" + keyWord + "&md=p";
            String query = "";

            // lets get some data from it
            HttpURLConnection urlc = (HttpURLConnection) new URL(apiEndpoint).openConnection();

            urlc.setRequestMethod("GET");
            urlc.setRequestProperty("Accept", "application/json");

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (urlc.getInputStream())));

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }

            String result = sb.toString();
            urlc.disconnect();


            while (result.indexOf("{") > 0) {
                String getParse = result.substring(result.indexOf("{"), result.indexOf("}") + 1);
                com.google.gson.JsonParser parser = new JsonParser();
                JsonElement thisElement = parser.parse(getParse);
                result = result.substring(result.indexOf("}") + 1);
                wordsObj.add(thisElement.getAsJsonObject());
            }
        }
        return wordsObj;
    }

    /**
     * Get the slots passed into the request
     * @param input The input object
     * @return Map of slots
     */
    Map<String, Slot> getSlots(HandlerInput input) {
        // this chunk of code gets the slots
        Request request = input.getRequestEnvelope().getRequest();
        IntentRequest intentRequest = (IntentRequest) request;
        Intent intent = intentRequest.getIntent();
        return Collections.unmodifiableMap(intent.getSlots());
    }

    /**
     * Log slots for easier debugging
     * @param input Input passed to handle
     */
    void logSlots(HandlerInput input) {
        Map<String, Slot> slots = getSlots(input);
        // log slot values including request id and time for debugging
        for(String key : slots.keySet()) {
            log(input, String.format("Slot value key=%s, value = %s", key, slots.get(key).toString()));
            log(input, String.format("Slot value key=%s, value = %s", key, slots.get(key).toString()));
        }
    }

    /**
     * Logs debug messages in an easier to search way
     * You can also use system.out, but it'll be harder to work with
     */
    void log(HandlerInput input, String message) {
        System.out.printf("[%s] [%s] : %s]\n",
                input.getRequestEnvelope().getRequest().getRequestId().toString(),
                new Date(),
                message);
    }


}