package testingdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;

public class MySQLAccess {
	private Connection connect = null;
	
	public MySQLAccess(String conn) throws Exception {
		// This will load the MySQL driver, each DB has its own driver
		Class.forName("com.mysql.jdbc.Driver");
		//Setup the connection with the DB
		connect = DriverManager.getConnection(conn);
	}
	
	public void insertChampions(Map<Integer, String> champList) throws SQLException {
		if(champList == null) return;
		String query = "insert into zc_champions(id, title) values(?, ?)";
		
		Object[] keys = champList.keySet().toArray();
		PreparedStatement statement;
		Integer temp;
		
		for(int i = 0; i < keys.length; ++i) {
			statement = connect.prepareStatement(query);
			temp = (Integer) keys[i];
			statement.setInt(1, temp.intValue());
			statement.setString(2, champList.get(temp));
			statement.executeUpdate();
		}
	}
	
	public void insertSumSpells(Map<Integer, String> spellList) throws SQLException {
		if(spellList == null) return;
		String query = "insert into zc_spells(id, title) values(?, ?)";
		
		Object[] keys = spellList.keySet().toArray();
		PreparedStatement statement;
		Integer temp;
		
		for(int i = 0; i < keys.length; ++i) {
			statement = connect.prepareStatement(query);
			temp = (Integer) keys[i];
			statement.setInt(1, temp.intValue());
			statement.setString(2, spellList.get(temp));
			statement.executeUpdate();
		}
	}
	
	//gets an id for an ign
	public ArrayList<Long> getFriends() throws SQLException {
		String query = "select sum_id from d_players where friend = 1";
		ArrayList<Long> sum_ids = new ArrayList<Long>();
		PreparedStatement statement = connect.prepareStatement(query);
		ResultSet res = statement.executeQuery();
		while (res.next()) {
			long x = res.getLong(1);
			sum_ids.add(x);
		}
		res.close();
		statement.close();
		return sum_ids;
	}
	
	//checks whether the input ign is already in the database
	public boolean userExists(String st) throws SQLException {
		String query = "select id from d_players where ign = ?";
		PreparedStatement statement = connect.prepareStatement(query);
		statement.setString(1,st);
		ResultSet res = statement.executeQuery();
		return res.next();
	}
	
	public void updateMap(int map_id, String match_id) throws SQLException {
		String query = "update d_games set map = ? where match_id = ?";
		PreparedStatement statement = connect.prepareStatement(query);
		
		statement.setInt(1, map_id);
		statement.setString(2, match_id);
		
		statement.executeUpdate();
		statement.close();
	}
	
	//finds all match IDs that need to be updated
	//this can probably serve as a template for any time I need to add a new data column
	public ArrayList<String> getMatchIDs() throws SQLException {
		String query = "select distinct match_id from d_games where map is null";
		ArrayList<String> match = new ArrayList<String>();
		PreparedStatement statement = connect.prepareStatement(query);
		ResultSet res = statement.executeQuery();
		
		while(res.next()) {
			match.add(res.getString(1));
		}
		
		res.close();
		statement.close();
		
		return match;
	}
	
	//takes a game id and a pipe-delimited string of bans and inserts them into the database
	//expects 6 tokens with the first three being blue team and the last three being red team
	public void insertBans(ArrayList<Integer> banList, int team_id, long match_id) throws SQLException {
		String insert = "insert into r_bans(game_id, champion_id, team_id, pick_order) values(?, ?, ?, ?)";
		PreparedStatement statement;
		
		String match_str = String.valueOf(match_id);
		
		for(int i = 0; i < banList.size(); ++i) {
			statement = connect.prepareStatement(insert);
			statement.setString(1, match_str);
			statement.setInt(2, banList.get(i));
			statement.setInt(3, team_id);
			statement.setInt(4, i);
			
			statement.executeUpdate();
			statement.close();
		}
	}
	
	//checks whether a player with the input in-game name exists in the database
	//if not, adds the player to the database
	public void checkPlayer(String ign, String sum_id) throws SQLException {
		String sel = "select * from d_players where sum_id = ?";
		PreparedStatement statement = connect.prepareStatement(sel);
		statement.setString(1, sum_id);
		ResultSet res = statement.executeQuery();
		
		boolean check = res.next();
		if(!check) {
			String insPlay = "insert into d_players(ign, sum_id) values(?, ?)";
			statement.close();
			statement = connect.prepareStatement(insPlay);
			statement.setString(1, ign);
			statement.setString(2, sum_id);
			statement.executeUpdate();
		}
		
		statement.close();
		res.close();
	}
	
	//we check whether the match exists at a different point
	public void insertMatch(long match_id, long match_date, long duration, int winner, String game_type, int map_id) throws SQLException {
		String match_str = String.valueOf(match_id);
		String insPlay = "insert into d_games(match_id, game_date, winner, duration, game_type, map) values(?, ?, ?, ?, ?, ?)";
		PreparedStatement statement = connect.prepareStatement(insPlay);
		statement.setString(1, match_str);
		statement.setTimestamp(2, new Timestamp(match_date));
		statement.setInt(3, winner);
		statement.setLong(4, duration);
		statement.setString(5, game_type);
		statement.setInt(6, map_id);
		statement.executeUpdate();
		
		statement.close();
	}
	
	//checks whether the match is already in the database
	public boolean checkMatch(long match_id) throws SQLException {
		String sel = "select * from d_games where match_id = ?";
		String match_str = String.valueOf(match_id);
		PreparedStatement statement = connect.prepareStatement(sel);
		statement.setString(1, String.valueOf(match_str));
		ResultSet res = statement.executeQuery();
		boolean found = res.next();
		
		statement.close();
		res.close();
		return found;
	}
	
	public boolean insertMatchData(long match_id, String sum_id, int[] values) throws SQLException {
		String match_str = String.valueOf(match_id);
		
		String insGame = "insert into r_plays(game_id, player_id, champion_id, kills, deaths, assists, gold, creeps, wards_placed, wards_killed, "
				+ "spell1, spell2, team_id, participant_id, rank_c, division, sum_level, champ_level) "
				+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement statement = connect.prepareStatement(insGame);
		
		statement.setString(1, match_str);
		statement.setString(2, sum_id);
		
		for(int i = 3; i < values.length; ++i) {
			statement.setInt(i, values[i]);
		}
		
		statement.executeUpdate();
		statement.close();
		
		return true;
	}

	//close the connection when you're done
	public void close() throws SQLException {
		if (connect != null) connect.close();
	}
} 
