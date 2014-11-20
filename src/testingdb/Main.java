package testingdb;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class Main {
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("Program run at: " + dateFormat.format(date));
		System.out.println();
		
		
		Properties prop = new Properties();
		InputStream input = new FileInputStream("res/config.properties");
		prop.load(input);
		
		String db = prop.getProperty("database");
		String apikey = prop.getProperty("riotapi");
		
		MySQLAccess mysql = new MySQLAccess(db);
		
		//GameData gd = new GameData(apikey, "na");
		RiotAPI api = new RiotAPI(apikey, "na", mysql);
		//gd.listRecentGames(35444184L); //me
		//gd.listRecentGames(35336721L); //west
		//gd.listRecentGames(38307266L); //darkside
		
		//api.updateMap();
		ArrayList<Long> summoners = mysql.getFriends();
		
		for(int i = 0; i < summoners.size(); ++i) {
			api.processRecentGames(summoners.get(i));
		}
		//mysql.insertChampions(api.getChamps());
		//mysql.insertSumSpells(api.getSpells());
		
		mysql.close();
		
		long endTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("Run Time: " + (endTime - startTime));
		System.out.println();
	}
}