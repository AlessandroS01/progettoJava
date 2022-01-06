package it.univpm.ProgettoOOP.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.univpm.ProgettoOOP.Exception.HourGivenNotFound;
import it.univpm.ProgettoOOP.Exception.WrongFileException;
import it.univpm.ProgettoOOP.Filter.MyFilter;
import it.univpm.ProgettoOOP.Model.Place;
import it.univpm.ProgettoOOP.Model.Weather;
import it.univpm.ProgettoOOP.Services.StartConnectionURL5Days;
import it.univpm.ProgettoOOP.Services.StartConnectionURLCurrent;
import it.univpm.ProgettoOOP.Statistics.FillingModel;
import it.univpm.ProgettoOOP.Statistics.Stats;
import it.univpm.ProgettoOOP.Timer.WriteLocalFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Vector;

import org.json.simple.*;

@RestController
public class Roots {
	
	/**
	 * This is a root that has as @return the informations of
	 * the current time taken by the API.
	 * @param city is the city we want to study.
	 */
	@RequestMapping ( {"/get/current/{city}" , "/get/current/"})
	public JSONObject getCurrentWeather (@PathVariable ( value = "city" , required = false ) String city){
		StartConnectionURLCurrent start = new StartConnectionURLCurrent(city);
		return start.startCurrentConnection();
	}

	/**
	 * This is a root that has as @return the informations of
	 * the forecast for the next 5 days taken by the API.
	 * @param city is the city we want to study.
	 */
	@RequestMapping ( {"/get/forecast/{city}" , "/get/forecast/"})
	public JSONObject getWeather5Days (@PathVariable ( value = "city" , required = false ) String city) {
		StartConnectionURL5Days startForecast = new StartConnectionURL5Days(city);
		return startForecast.startConnection5Days();
	}
	
	/**
	 * This is a root used to call the Weather API and write automatically every hour
	 * the informations inside @ApiCallsByTime(local file situated in the resources).
	 * The @return is "Carimento Completato" if everything works properly.
	 * If the root has a problem while searching for the file it gives "The file given doesn't exist" 
	 * as @return
	 */
	@RequestMapping ( {"/writer/3/hours/{city}" , "/writer/3/hours/" })
	public String writer( @PathVariable ( value = "city" , required = false ) String city ) {
		WriteLocalFile write = new WriteLocalFile( city);
		try {
			write.WriteOnLocalFile3Hours();
		} catch (WrongFileException e) {
			return e.sendMessage();
		}
		return "Caricamento completato";
	}
	
	/**
	 * This is a root used to call the Forecast API and write automatically 
	 * the informations inside @ApiForecast(local file situated in the resources).
	 * The @return is "Carimento Completato" if everything works properly.
	 * If the root has a problem while searching for the file it gives "The file given doesn't exist" 
	 * as @return
	 */
	@RequestMapping ({ "/writer/forecast/{city}" , "/writer/forecast/" })
	public String writerForecast( @PathVariable ( value = "city" , required = false ) String city ) {
		WriteLocalFile write = new WriteLocalFile( city);
		try {
			write.WriteOnLocalFileForecast();
		} catch (WrongFileException e) {
			return e.sendMessage();
		}
		return "Caricamento completato";
	}
	
	/**
	 * This is a root in which the user can choose the weather during the time 
	 * he provides in the @method filterPerHour written inside @class MyFilter.
	 * @return @class Place created using a Vector of Weather with @param hours equal
	 * 		to every object of the Vector.
	 * @param hour should be written in this manner : "hour:minutes,AM/PM" (AM or PM)
	 * Example : 10:00,PM
	 */
	@RequestMapping({"/filter/per/hour/{time}" , "/filter/per/hour/"})
	public String filteredHour(@PathVariable ( value = "time", required = false )String time) throws HourGivenNotFound, SQLException {
		MyFilter filter = new MyFilter();
		
		 if ( filter.filterPerHour(time).getWeatherXTime() != null )
				return filter.filterPerHour(time).toString();
		 else throw new HourGivenNotFound( "The time given has no matches");
	}
	
	/**
	 * This is a root in which the user can choose the weather during the day 
	 * he provides in the @method filterPerDay written inside @class MyFilter.
	 * @return @class Place created using a Vector of Weather with @param day equal
	 * 		to every object of the Vector.
	 * @param date should be written in this manner : "day_of_the_week,month day_of_the_month,year" 
	 * Example : Saturday,January 1,2022
	 */
	@RequestMapping({"/filter/per/day/{date}" , "/filter/per/day/"})
	public Place filteredDay(@PathVariable ( value = "date", required = false )String date) {
		MyFilter filter = new MyFilter();
		
		if( filter.filterPerDay(date) != null )
			return filter.filterPerDay(date);
		else return null;
	}
	
	/**
	 * This root provides the user to see the differences between the speed of the wind datas assimilated 
	 * through the days.
	 * @return a String containing those differences indicating the exact time and date of the the result.
	 */
	@RequestMapping( "/difference/speed/" )
	public String differenceSpeed () {
		
		Stats statistics = new Stats ();
		return statistics.differenceSpeedCurrentForecast();
	}
	
	
	/**
	 * This root provides the user to see statistics calculated with @file ApiCallsByTime , so using the datas 
	 * coming from current Weather API.
	 * @return a String containing those statistics.
	 */
	@RequestMapping({ "/see/statistics/{date}" , "/see/statistics/" })
	public String seeStatistics( @PathVariable ( value = "date", required = false )String date ) {
		
		if (date == null ) date = "Wednesday,December 29,2021";
		Stats statistics = new Stats();
		return "During the " + date + " the average wind degree of the wind was :" 
				+ statistics.getAverageDegree(date) + "°.\n"
				+ "Furthermore the max speed registered was : " +statistics.getMaxSpeedXDay(date) +"\n"
				+ "Instead the min speed registered was : " + statistics.getMinSpeedXDay(date);
		
	}
	
	
	@GetMapping("/see/ApiCallsByTime")
	public Place seeApiCalls() {
		FillingModel c= new FillingModel();
		
		return c.fillPlaceCurrent();
	}
	
	@GetMapping("/see/ApiForecast")
	public Place seeApiForecast() {
		FillingModel c= new FillingModel();
		
		return c.fillPlaceForecast();
	}
	

}
