package spotifymusicgenerator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.hc.core5.http.ParseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.special.SnapshotResult;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;


public class SpotUser {
    private SpotifyApi api;
    private String token;


    // constructor to handle the spotify user and
    // generting the music new music playlist
    public SpotUser(String id, String secretID) throws UnsupportedEncodingException{
        this.api = new SpotifyApi.Builder()
            .setClientId(id)
            .setClientSecret(secretID)
            .build();
        this.token = "Bearer " + getToken(id, secretID);
    }

    // Returns the token for a user to interact with spotify api
    private static String getToken(String clientId, String clientSecret) {
        try {
            String authString = clientId + ":" + clientSecret;
            String base64Auth = Base64.getEncoder().encodeToString(authString.getBytes());

            URL tokenUrl = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection connection = (HttpURLConnection) tokenUrl.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + base64Auth);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            String postData = "grant_type=client_credentials";
            connection.getOutputStream().write(postData.getBytes());

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            // Parse the JSON response to get the access token
            String accessToken = response.toString().split("\"access_token\":\"")[1].split("\"")[0];
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // This will generate a new playlist where there are num
    // songs from each artist
    public void makePlayList(List<String> artist, int num, String playListName) throws IOException, UnirestException, ParseException, SpotifyWebApiException{
        try {
            List<String> songID = new ArrayList<>();
            for(int i = 0; i < artist.size(); i++){
                String artistName = artist.get(i);
                String artistID = getArtistId(artistName);
                String url = "https://api.spotify.com/v1/artists/" + artistID + "/top-tracks?country=US";
                URL searchUrl = new URL(url);

                HttpURLConnection connection = (HttpURLConnection) searchUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", token);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                // gets the first num tracks
                songID.addAll(getSongIDs(response, num));
            }
            String[] uris = createURI(songID);
            String playListId = getPlayListId(playListName);
            // Add tracks to the playlist
            AddItemsToPlaylistRequest addItemsToPlaylistRequest = api
            .addItemsToPlaylist(playListId, uris)
            .build();
            SnapshotResult temp = addItemsToPlaylistRequest.execute();
            temp.builder().build();
        } catch (Exception E){
            E.printStackTrace();
        }
    }
    // this will get the playlist id
    private String getPlayListId(String name) throws UnirestException{
         try {
            // Encode the search query to handle spaces
            String encodedPlayList = URLEncoder.encode(name, "UTF-8");

            String apiUrl = "https://api.spotify.com/v1/search?q=" + encodedPlayList + "&type=playlist";
            URL searchUrl = new URL(apiUrl);

            HttpURLConnection connection = (HttpURLConnection) searchUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", token);


            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            System.out.println(response.toString());
            return getStringID(response);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getStringID(StringBuilder jsonResponse) throws JsonMappingException, JsonProcessingException{
        // Use Jackson ObjectMapper to read the JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse.toString());

            List<JsonNode> value = jsonNode.findValues("href");
            String temp = value.get(0).asText();
            // Find the position of "query="
            int queryIndex = temp.indexOf("query=");
            String queryValue = "";
            if (queryIndex != -1) {
            // Extract the substring after "query="
            String queryString = temp.substring(queryIndex + 6);

            // Find the position of the next "&" to truncate the substring
            int ampersandIndex = queryString.indexOf('&');
            if (ampersandIndex != -1) {
                // Extract the value of the "query" parameter
                queryValue = queryString.substring(0, ampersandIndex);
                System.out.println(queryValue);
            }
        }
        return queryValue;
    }

    // This will convert our song uris into an String array format
    private String[] createURI(List<String> uri){
        String[] result = new String[uri.size()];
        for(int i = 0; i < result.length; i++){
            result[i] = uri.get(i);
        }
        return result;
    }

    // This will add the songs from a artist to the playlist
    private List<String> getSongIDs(StringBuilder jsonResponse, int num){
        // Use Jackson ObjectMapper to read the JSON string
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse.toString());

            // find a way to iterate through that field and add track numbers
            List<String> jsonData = jsonNode.findValuesAsText("uri");
            //String temp = jsonNode.toPrettyString(); save incase i need to see json data
            List<String> songs = new ArrayList<>();
            for(int i = 0; i < jsonData.size(); i++){
                String temp = jsonData.get(i);
                if(temp.contains("track") && num > 0){
                    songs.add(temp);
                }
            }
            return songs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // returns the spotify id of a artist 
    private String getArtistId(String artist) throws IOException{
        try {
            // Encode the search query to handle spaces
            String encodedArtistName = URLEncoder.encode(artist, "UTF-8");

            String apiUrl = "https://api.spotify.com/v1/search?q=" + encodedArtistName + "&type=artist&limit=1";
            URL searchUrl = new URL(apiUrl);

            HttpURLConnection connection = (HttpURLConnection) searchUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", token);


            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            // get ID substring
            String id = idIndex(response.toString());
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // returns just and ID from a spotify search term
    private String idIndex(String reponse){
        int index = reponse.indexOf("id");
        int imageIndex = reponse.indexOf("images");
        String temp = reponse.substring(index, imageIndex);
        String result = temp.substring(temp.indexOf(":") + 3, temp.indexOf(",") - 1);
        return result;
    }

}   
