package testingdb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;


//use League API to populate some data about the game for me

public class GameData {
	private JSONObject champList;
	private JSONObject sumSpellList;
	private Map<String, JSONArray> sum_league;
	private Map<String, JSONObject> sum_data;
	private String baseURL = "https://na.api.pvp.net";
	private String apiKey;
	private String region;
	private ArrayList<Integer> blueBans;
	private ArrayList<Integer> purpleBans;
	
	public GameData(String api, String reg) {
		apiKey = api;
		region = reg;
		champList = null;
		sumSpellList = null;
		sum_league = new Hashtable<String, JSONArray>();
		sum_data = new Hashtable<String, JSONObject>();
		blueBans = new ArrayList<Integer>();
		purpleBans = new ArrayList<Integer>();
	}

	//get the champion name for the input champion id
	//returns null if nothing is found
	private String getChampion(int id) throws Exception {
		if(champList == null) {
			String data = readUrl(baseURL + "/api/lol/static-data/" + region + "/v1.2/champion" + apiKey);
			if(data == null) return null;
			champList = new JSONObject(data);
		}
		JSONObject j2;
		
		JSONObject champs = champList.getJSONObject("data");
		JSONArray keys = champList.getJSONObject("data").names();
		
		//loop through every champion and search for a match
		for(int i = 0; i < keys.length(); ++i) {
			j2 = champs.getJSONObject(keys.getString(i));
			if(j2.getInt("id") == id) return j2.getString("name");
		}
		return null;
	}
	
	public Map<Integer, String> getChamps() throws Exception {
		if(champList == null) {
			String data = readUrl(baseURL + "/api/lol/static-data/" + region + "/v1.2/champion" + apiKey);
			if(data == null) return null;
			champList = new JSONObject(data);
		}
		
		JSONObject j2;
		JSONObject champs = champList.getJSONObject("data");
		JSONArray keys = champList.getJSONObject("data").names();
		Map<Integer, String> arr = new Hashtable<Integer, String>();
		
		//loop through every champion
		for(int i = 0; i < keys.length(); ++i) {
			j2 = champs.getJSONObject(keys.getString(i));
			arr.put(j2.getInt("id"), j2.getString("name"));
		}
		
		return arr;
	}
	
	public Map<Integer, String> getSpells() throws Exception {
		if(sumSpellList == null) {
			String data = readUrl(baseURL + "/api/lol/static-data/" + region + "/v1.2/summoner-spell" + apiKey);
			if(data == null) return null;
			sumSpellList = new JSONObject(data);
		}
		
		JSONObject j2;
		JSONObject sumSpells = sumSpellList.getJSONObject("data");
		JSONArray keys = sumSpellList.getJSONObject("data").names();
		Map<Integer, String> arr = new Hashtable<Integer, String>();
		
		//loop through every champion
		for(int i = 0; i < keys.length(); ++i) {
			j2 = sumSpells.getJSONObject(keys.getString(i));
			arr.put(j2.getInt("id"), j2.getString("name"));
		}
		
		return arr;
	}
	
	//get the summoner spell name name for the input spell id
	//returns null if nothing is found
	private String getSumSpell(int id) throws Exception {
		if(sumSpellList == null) {
			String data = readUrl(baseURL + "/api/lol/static-data/" + region + "/v1.2/summoner-spell" + apiKey);
			if(data == null) return null;
			sumSpellList = new JSONObject(data);
		}
		JSONObject j2;
		
		JSONObject sumSpells = sumSpellList.getJSONObject("data");
		JSONArray keys = sumSpellList.getJSONObject("data").names();
		
		//loop through every champion and search for a match
		for(int i = 0; i < keys.length(); ++i) {
			j2 = sumSpells.getJSONObject(keys.getString(i));
			if(j2.getInt("id") == id) return j2.getString("name");
		}
		return null;
	}
	
	//takes a list of summoner ids and caches league data for all summoners not already cached
	public void processLeagueInfo(ArrayList<String> sum_ids) throws Exception {
		String api_ids = "";
		String temp;
		int count = 0;
		
		//construct a string to send to the api to query summoner objects
		for(int i = 0; i < sum_ids.size(); ++i) {
			temp = sum_ids.get(i);
			if(!sum_league.containsKey(temp)) {
				api_ids += (temp + ",");
				count++;
			}
		}
		
		if(api_ids.equals("")) return; //if we don't find any non-cached, don't do anything
		if(count > 10) { //limit to 10 to avoid api error
			System.out.println("WE HAVE A PROBLEM");
			return;
		}
		
		//cache anything left
		String data = readUrl(baseURL + "/api/lol/" + region + "/v2.5/league/by-summoner/" + api_ids + "/entry" + apiKey);
		if(data == null) return;
		JSONObject json = new JSONObject(data);
		JSONArray keys = json.names();
		String sum_id;
		for(int i = 0; i < keys.length(); ++i) {
			sum_id = keys.getString(i);
			sum_league.put(sum_id, json.getJSONArray(sum_id)); //cache summoner data 
		}
		
	}
	
	//return the number in the database corresponding to the rank (bronze/silver/gold) from the input leagues info array
	private int getRank(JSONArray arr) throws Exception {
		if(arr == null) return 1;
		
		JSONObject temp;
		String rank = "";
		
		//search for ranked solo because that's all that matters
		for(int i = 0; i < arr.length(); ++i) {
			temp = arr.getJSONObject(i);
			if(temp.getString("queue").equals("RANKED_SOLO_5x5")) {
				rank = temp.getString("tier");
				break;
			}
		}
		
		if(rank.equals("BRONZE")) return 2;
		else if(rank.equals("SILVER")) return 3;
		else if(rank.equals("GOLD")) return 4;
		else if(rank.equals("PLATINUM")) return 5;
		else if(rank.equals("DIAMOND")) return 6;
		else if(rank.equals("MASTER")) return 7;
		else if(rank.equals("CHALLENGER")) return 8;
		else return 1; //unranked
	}
	
	//return the division number from the input leagues info array
	private int getDivision(JSONArray arr) throws Exception {
		if(arr == null) return 0;
		
		JSONObject temp;
		String div = "";
		
		//search for ranked solo because that's all that matters
		for(int i = 0; i < arr.length(); ++i) {
			temp = arr.getJSONObject(i);
			if(temp.getString("queue").equals("RANKED_SOLO_5x5")) {
				div = temp.getJSONArray("entries").getJSONObject(0).getString("division");
				break;
			}
		}
		
		if(div.equals("I")) return 1;
		else if(div.equals("II")) return 2;
		else if(div.equals("III")) return 3;
		else if(div.equals("IV")) return 4;
		else if(div.equals("V")) return 5;
		else return 0;
	}
	
	//get summoner id from a summoner name
	public int getSummonerId(String ign) throws Exception {
		JSONObject json = new JSONObject(readUrl(baseURL + "/api/lol/" + region + "/v1.4/summoner/by-name/" + ign + apiKey));
		return json.getJSONObject((String)json.keys().next()).getInt("id");
	}
	
	//takes a list of summoner ids and caches data for all summoners not already cached
	public void processSummoners(ArrayList<String> sum_ids) throws Exception {
		String api_ids = "";
		String temp;
		
		//construct a string to send to the api to query summoner objects
		//might want to consider limiting to 40 in the future as it is the max per request
		for(int i = 0; i < sum_ids.size(); ++i) {
			temp = sum_ids.get(i);
			if(!sum_data.containsKey(temp)) api_ids += (temp + ",");
		}
		
		if(api_ids.equals("")) return; //if we don't find any non-cached, don't do anything
		
		//cache anything left
		JSONObject json = new JSONObject(readUrl(baseURL + "/api/lol/" + region + "/v1.4/summoner/" + api_ids + apiKey));
		JSONArray keys = json.names();
		String sum_id;
		for(int i = 0; i < keys.length(); ++i) {
			sum_id = keys.getString(i);
			sum_data.put(sum_id, json.getJSONObject(sum_id)); //cache summoner data 
		}
		
	}
	
	//checks whether the key exists and returns zero if it doesn't
	private int myGetInt(JSONObject obj, String key) throws Exception {
		if(obj.has(key)) return obj.getInt(key);
		else return 0;
	}
	
	//update this to get only match-specific data (as opposed to player-specific)
	public void printMatchData(JSONObject match, int team, int champ) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z");
		
		System.out.println(match.getString("queueType"));
		System.out.println(sdf.format(match.getLong("matchCreation")));
		System.out.println(match.getLong("matchDuration"));
		
		System.out.println("Duration: " + formatDuration(match.getLong("matchDuration")));
		
		processBans(match.getJSONArray("teams"));
		printBans();
		
		getPlayerMatchData(match, team, champ);
		
		//System.out.println();
	}
	
	// grab data/stats related to a player in a match
	public void getPlayerMatchData(JSONObject match, int team, int champ) throws Exception {
		JSONObject part_data = getParticipant(match, team, champ);
		
		System.out.println(getChampion(part_data.getInt("championId")));
		System.out.println(getSumSpell(myGetInt(part_data, "spell1Id")));
		System.out.println(getSumSpell(myGetInt(part_data, "spell2Id")));
		
		System.out.println("Team: " + getTeamName(part_data.getInt("teamId")));
		System.out.println("Participant: " + myGetInt(part_data, "participantId"));
		
		part_data = part_data.getJSONObject("stats");
		
		System.out.println("Kills: " + myGetInt(part_data, "kills"));
		System.out.println("Deaths: " + myGetInt(part_data, "deaths"));
		System.out.println("Assists: " + myGetInt(part_data, "assists"));
		System.out.println("Gold: " + myGetInt(part_data, "goldEarned"));
		System.out.println("Minions: " + (myGetInt(part_data, "minionsKilled") + myGetInt(part_data, "neutralMinionsKilled")));
		
		
		System.out.println("Wards Placed: " + myGetInt(part_data, "wardsPlaced"));
		System.out.println("Wards Killed: " + myGetInt(part_data, "wardsKilled"));
	}
	
	
	
	//mostly a debug function
	private void printBans() throws Exception {
		if(blueBans.size() > 0) {
			System.out.println("blue");
			for(int i = 0; i < blueBans.size(); ++i) {
				System.out.println(getChampion(blueBans.get(i)));
			}
			System.out.println();
		}
		
		if(purpleBans.size() > 0) {
			System.out.println("purple");
			for(int i = 0; i < purpleBans.size(); ++i) {
				System.out.println(getChampion(purpleBans.get(i)));
			}
			System.out.println();
		}
	}
	
	//stores ban lists in private variables given team ban data objects
	private void processBans(JSONArray teams) throws Exception {
		JSONObject team;
		JSONArray bans;
		
		//reset banlists
		blueBans = new ArrayList<Integer>();
		purpleBans = new ArrayList<Integer>();
		
		int champId;
		
		for(int i = 0; i < teams.length(); ++i) {
			team = teams.getJSONObject(i);
			if(team.has("bans")) { //some game modes don't use bans, so make sure to check first
				bans = team.getJSONArray("bans");
				for(int j = 0; j < bans.length(); ++j) {
					champId = bans.getJSONObject(j).getInt("championId");
					//store in list based on what team it is
					if(team.getInt("teamId") == 100) blueBans.add(champId);
					else if(team.getInt("teamId") == 200) purpleBans.add(champId);
				}
			}
		}
	}
	
	public ArrayList<Integer> getBans(int team_id) {
		if(team_id == 100) return blueBans;
		else if(team_id == 200) return purpleBans;
		else return null;
	}
	
	private String getTeamName(int teamId) {
		if(teamId == 100) return "blue";
		else if(teamId == 200) return "purple";
		else return null;
	}
	
	// return a participant data based on a team and champion
	private JSONObject getParticipant(JSONObject match, int team, int champ) throws Exception {
		if(match.getString("queueType").equals("ONEFORALL_5x5")) return null;
		
		JSONArray parts = match.getJSONArray("participants");
		JSONObject temp;
		for(int i = 0; i < parts.length(); ++i) {
			temp = parts.getJSONObject(i);
			if(temp.getInt("championId") == champ && temp.getInt("teamId") == team) {
				return temp;
			}
		}
		
		return null;
	}
	
	// helper function to handle conversion of seconds to game duration
	public String formatDuration(long time) {
		long seconds = time%60;
		time /= 60;
		
		
		return String.format("%02d:%02d", time, seconds);
	}
	
	public void listRecentGames(long sum_id) throws Exception {
		String data = readUrl(baseURL + "/api/lol/" + region + "/v1.3/game/by-summoner/" + sum_id + "/recent" + apiKey);
		
		if(data == null) {
			System.out.println("Summoner not found.");
			return;
		}
		
		String st_sum = String.valueOf(sum_id);
		ArrayList<String> summoner = new ArrayList<String>();
		summoner.add(st_sum);
		processSummoners(summoner);
		processLeagueInfo(summoner);
		
		JSONObject sum_stuff = sum_data.get(st_sum);
		System.out.println(sum_stuff.getString("name"));
		System.out.println("Summoner Level: " + sum_stuff.getInt("summonerLevel"));
		
		//get league data
		JSONArray league_stuff = sum_league.get(st_sum);
		System.out.println(getRank(league_stuff) + " " + getDivision(league_stuff));
		
		
		JSONObject json = new JSONObject(data);
		JSONArray matches = json.getJSONArray("games");
		JSONObject j2, player_dat, match_data;
		JSONArray fellows;
		
		Map<String, JSONObject> player_info;
		
		for(int i = 0; i < matches.length(); ++i) {
			j2 = matches.getJSONObject(i);
			if(j2.getJSONObject("stats").getBoolean("win")) System.out.println("Victory");
			else System.out.println("Defeat");
			
			
			data = readUrl(baseURL + "/api/lol/" + region + "/v2.2/match/" + j2.getLong("gameId") + apiKey);
			match_data = new JSONObject(data);
			
			printMatchData(match_data, j2.getJSONObject("stats").getInt("team"), j2.getInt("championId"));
			
			if(!j2.has("fellowPlayers")) continue; //if a game is only bots, there will be no fellow players
			
			fellows = j2.getJSONArray("fellowPlayers");
			summoner = new ArrayList<String>();
			String temp_sum;
			player_info = new Hashtable<String, JSONObject>();
			for(int j = 0; j < fellows.length(); ++j) {
				player_dat = fellows.getJSONObject(j);
				temp_sum = String.valueOf(player_dat.getLong("summonerId"));
				summoner.add(temp_sum);
				player_info.put(temp_sum, player_dat);
			}
			processSummoners(summoner);
			processLeagueInfo(summoner);
			
			System.out.println();
			System.out.println("Fellow Players");
			System.out.println();
			for(int j = 0; j < summoner.size(); ++j) {
				temp_sum = summoner.get(j);
				player_dat = player_info.get(temp_sum);
				System.out.println(temp_sum + " - " + sum_data.get(temp_sum).getString("name"));
				
				//league info
				league_stuff = sum_league.get(temp_sum);
				System.out.println(getRank(league_stuff) + " " + getDivision(league_stuff));
				
				//team and champion
				//System.out.println(getTeamName(player_dat.getInt("teamId")));
				//System.out.println(getChampion(player_dat.getInt("championId")));
				
				//print statistics for each champion
				getPlayerMatchData(match_data, player_dat.getInt("teamId"), player_dat.getInt("championId"));
				
				System.out.println();
			}
			
			System.out.println();
		}
	}
	
	//makes an HTTP request to the url and returns the content
	public String readUrl(String urlString) throws Exception {
		System.out.println("Processing delay for URL: "+urlString); //for logging and sanity check
		//Thread.sleep(2000); //delay to make sure the rate limit doesn't trigger
		Thread.sleep(1500); //trying a lower delay for lower run time
		HttpURLConnection connection = null;
		BufferedReader rd  = null;
		StringBuilder sb = null;
		String line = null;
		URL serverAddress = null;
	
		try {
			serverAddress = new URL(urlString.replace(" ", "%20"));
			//set up out communications stuff
			connection = null;
		
			//Set up the initial connection
			connection = (HttpURLConnection)serverAddress.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setReadTimeout(10000);
			connection.connect();
			
			if(connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) return null;
		
			//read the result from the server
			rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			sb = new StringBuilder();
		
			while ((line = rd.readLine()) != null) {
				sb.append(line + '\n');
			}
		    
			return sb.toString();
		                
		} finally {
			//close the connection, set all objects to null
			connection.disconnect();
			rd = null;
			sb = null;
			connection = null;
		}
	}
	
}
