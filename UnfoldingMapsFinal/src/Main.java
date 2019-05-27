import java.util.*;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.*;
import de.fhpotsdam.unfolding.utils.MapUtils;
import module6.CityMarker;
import module6.CommonMarker;
import module6.LandQuakeMarker;
import module6.EarthquakeMarker;
import module6.OceanQuakeMarker;
import parsing.ParseFeed;
import processing.core.PApplet;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * Author: UC San Diego Intermediate Software Development MOOC team
 * @author Volfegan [Daniel L L]
 * Date: 2019.02.14
 * */
public class Main extends PApplet {

    public static void main(String[] args) {
        System.out.println("Unfolding Maps project! Final");
        PApplet.main("Main", args);
    }

	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";


	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";
	
	// The map
	private UnfoldingMap map;
	
	// Markers for each city
	private List<Marker> cityMarkers;

	// Markers for each earthquake
	private List<Marker> quakeMarkers;
	// Sorted earthquakes list
	private List<EarthquakeMarker> sortedQuakeMarkers; //EXTENSION3 ASSIGNMENT
	//earthquake features extracted from RSS feed
	List<PointFeature> earthquakes;

	// A List of country markers
	private List<Marker> countryMarkers;

	// Controls for mouse clicks
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

	//Earthquake list for ticker scroll view //EXTENSION2 ASSIGNMENT
	float textpos1;  // horizontal location of headline 1
	float textpos2;  // horizontal location of headline 2
	StringBuilder earthquakesByForce = new StringBuilder("");
	StringBuilder earthquakesByLocation = new StringBuilder("-   ");

	public void setup() {
		size(960, 720);

		//for more map providers: http://unfoldingmaps.org/javadoc/de/fhpotsdam/unfolding/providers/package-summary.html
		AbstractMapProvider provider = new Microsoft.HybridProvider();
		//map location in canvas is not working with offset
		//int v0 = 200;
		//int v1 = 50;
		//int v2 = 650;
		//int v3 = 600;
		int v0 = 0;
		int v1 = 0;
		int v2 = 960;
		int v3 = 720;
		map = new UnfoldingMap(this, v0, v1, v2, v3, provider);

		MapUtils.createDefaultEventDispatcher(this, map);

		// FOR TESTING: Set earthquakesURL to be one of the testing files by uncommenting one of the lines below.
		//earthquakesURL = "test1.atom";
		//earthquakesURL = "test2.atom";
		// Uncomment this line to take the quiz
		//earthquakesURL = "quiz2.atom";


		// Reading in earthquake data and geometric properties
	    // STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);
		
		// STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}
	    
		// STEP 3: read in earthquake RSS feed and pass its data to markers
	    earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();

	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		  }
	    }

	    //Printing countries with quake qty
		System.out.println();
	    printQuakes();
	    //Printing quakes by power with location
	    int number_of_Top_quakes = 20;
		System.out.println("\nTop " + number_of_Top_quakes + " earthquakes:");
		sortAndPrint(number_of_Top_quakes);

	    // Add markers to map
		map.addMarkers(countryMarkers); //EXTENSION ASSIGNMENT
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);

		// Country markers are shaded according to qty of quakes
		shadeCountries(); //EXTENSION ASSIGNMENT

		//EXTENSION2 ASSIGNMENT
		//Get earthquakes list by force and location for the tickers
		List<EarthquakeMarker> earthquakeListByForce = sortQuakesbyForce();
		int counter = 0;
		for (EarthquakeMarker quake : earthquakeListByForce) {
			counter++;
			earthquakesByForce.append(quake + ";   ");

			//if numToPrint = 0 it will print all
			if (number_of_Top_quakes == counter) break;
		}

		List<String> earthquakeListByLocation = earthquakeListByLocation();
		for (String quake : earthquakeListByLocation) earthquakesByLocation.append(quake + "   -   ");

		// Initialize headline offscreen to the right
		textpos1 = width;
		textpos2 = width;

	}  // End setup
	
	
	public void draw() {
		background(0);
		map.draw();
		addKey();
		tickersText();
	}

	/** Event handler that gets called automatically when the mouse moves.*/
	@Override
	public void mouseMoved() {
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
	}

	// If there is a marker under the cursor, and lastSelected is null
	// set the lastSelected to be the first marker found under the cursor
	// Make sure you do not select two markers.
	private void selectMarkerIfHover(List<Marker> markers) {
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}

		for (Marker m : markers) {
			CommonMarker marker = (CommonMarker) m;
			if (marker.isInside(map,  mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				//remove selected marker from map and add back
				//when hovering mouse cursor over last marker in list the title will be on top
				map.getLastMarkerManager().removeMarker(marker);
				map.addMarkers(marker);
				return;
			}
		}
	}

	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked() {
		if (lastClicked != null) {
			unhideMarkers();
			lastClicked = null;
		} else if (lastClicked == null) {
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
			}
		}
	}

	// Helper method that will check if a city marker was clicked on and respond appropriately
	private void checkCitiesForClick() {
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker)marker;
				// Hide all the others cities not closer to quake
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker)mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation()) > quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}
	}

	// Helper method that will check if an earthquake marker was clicked on and respond appropriately
	private void checkEarthquakesForClick() {
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker)m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}

				//hide cities outside threat circle
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation()) > marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				return;
			}
		}
	}

	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}

		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}

	// helper method to draw key in GUI
	private void addKey() {
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);
		stroke(0);
		int xbase = 25;
		int ybase = 50;

		rect(xbase, ybase, 150, 270);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);

		fill(100, 0, 100);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE,
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE,
				tri_ybase+CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);

		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+110);

		fill(255, 255, 255);
		ellipse(xbase+35,
				ybase+70,
				10,
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);

		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);
		// x on circle
		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);

		//EXTENSION ASSIGNMENT
		fill(0, 0, 0);
		textSize(20);
		text("-", xbase+20, ybase+220);
		text("+", xbase+120, ybase+220);
		textSize(12);
		text("(1)", xbase+20, ybase+240);
        text("Qty", xbase+70, ybase+240);
		text("(100)", xbase+110, ybase+240);
		fill(255, 255, 255);
		rect(xbase+40, ybase+220, 70, 10);
		//line(xbase+40, ybase+220, xbase+40, ybase+230);
		//colour the rect gradient
		for (int pixel = xbase+40; pixel < xbase+110; pixel++) {
			int colorLevel = (int) map(pixel, xbase+40, xbase+110, 10, 255);
			stroke(255-colorLevel, 100, colorLevel);
			line(pixel, ybase+220, pixel, ybase+230);
		}
		//reset text for other objects be draw correctly
		stroke(0);
		strokeWeight(1);
		textSize(12);
	}

	//EXTENSION2 ASSIGNMENT
	// helper method to draw earthquakes tickers in GUI
	private void tickersText() {
		float w;
		textpos1 = textpos1 - 3; // ticker speed 4 pixels/frame
		textpos2 = textpos2 - 2; // ticker speed 4 pixels/frame

		// Display Ticker 1 headlines (quakes by location) bottom
		fill(0, 200);
		rect(0, height-20, width, 20); //shadow box 1 for ticker
		fill(255);
		textAlign(LEFT);
		text(earthquakesByLocation.toString(), textpos1, height-4);

		// If textpos is off the screen reset ticker
		w = textWidth(earthquakesByLocation.toString());
		if (textpos1 < -w) {
			textpos1 = width;
		}
		// Display Ticker 2 headlines (quakes by Force) top
		fill(0, 200);
		rect(0, 0, width, 20); //shadow box for 2nd ticker
		fill(255);
		text(earthquakesByForce.toString(), textpos2, 16);
		// If textpos is off the screen reset ticker
		w = textWidth(earthquakesByForce.toString());
		if (textpos2 < -w) {
			textpos2 = width;
		}
	}

	//EXTENSION ASSIGNMENT
	//Helper method to color each country based on earthquakes
	//Red-orange indicates low
	//Blue indicates high
	private void shadeCountries() {
		int countQuakes;
		for (Marker country : countryMarkers) {
			countQuakes = 0;

			for (PointFeature earthquake : earthquakes) {
				// getting location of feature
				Location checkLoc = earthquake.getLocation();

				// some countries represented it as MultiMarker
				// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
				if(country.getClass() == MultiMarker.class) {

					// looping over markers making up MultiMarker
					for(Marker marker : ((MultiMarker)country).getMarkers()) {

						// checking if inside
						if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
							countQuakes++;
						}
					}
				}

				// check if inside country represented by SimplePolygonMarker
				else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
					countQuakes++;
				}
			}
			// Shade the current country marker
			if (countQuakes > 0) {
				float transparency = 100;
				//I really don't need to know what is the max No. of quakes. If it is more than 100, that is a lot for a country.
				int max = 100;
				if (countQuakes > max) max = countQuakes;
				int colorLevel = (int) map(countQuakes, 0, max, 10, 255);
				country.setColor(color(255-colorLevel, 100, colorLevel, transparency));
				//country.setColor(color(colorLevel, 100, 255-colorLevel, transparency)); //inverse for blue to red (don't forget addKey box)
			} else {
				//apply a white shading with 0 alpha (transparent)
				country.setColor(color(255,255,255,0));
			}
		}
	}


	// Checks whether this quake occurred on land.  If it did, it sets the
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {

		// Loop over all countries to check if location is in any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		// not inside any country
		return false;
	}

	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {

			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {

				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));

					// return if is inside one
					return true;
				}
			}
		}

		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));

			return true;
		}
		return false;
	}

	/*
	 * EXTENSION2 ASSIGNMENT
	 * return an array list of earthquake occurrence, sorted by location.
	 */
	private List<String> earthquakeListByLocation() {
		List<String> earthquakeListByLocation = new ArrayList<String>();
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers) {
				EarthquakeMarker eqMarker = (EarthquakeMarker)marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				earthquakeListByLocation.add(countryName + ": " + numQuakes);
			}
		}
		earthquakeListByLocation.add("Ocean quakes: " + totalWaterQuakes);
		return earthquakeListByLocation;
	}

	/*
	 * prints countries with number of earthquakes
	 */
	private void printQuakes() {
		List<String> earthquakeList = earthquakeListByLocation();
		for (String quake : earthquakeList) {
			System.out.println(quake);
		}
	}

	/*
	 * EXTENSION2 ASSIGNMENT
	 * return a sorted array list of earthquake markers in reverse order of their magnitude (highest to lowest).
	 */
	private List<EarthquakeMarker> sortQuakesbyForce() {
		List<EarthquakeMarker> earthquakeList = new ArrayList<EarthquakeMarker>();

		for (Marker marker : quakeMarkers) {
			EarthquakeMarker quake = (EarthquakeMarker)marker;
			earthquakeList.add(quake);
		}
		Collections.sort(earthquakeList);
		return earthquakeList;
	}

	/*
	 * create a new array from the list of earthquake markers in reverse order of their magnitude (highest to
	 * lowest) and then print out the top numToPrint earthquakes.
	 */
	private void sortAndPrint(int numToPrint) {
		List<EarthquakeMarker> earthquakeList = sortQuakesbyForce();
		int counter = 0;
		for (EarthquakeMarker quake : earthquakeList) {
			counter++;
			System.out.println(quake);
			//if numToPrint = 0 it will print all
			if (numToPrint == counter) return;
		}
		// An example input and output files are provided in the data folder: use test2.atom as the input file,
		// and sortandPrint.test2.out.txt is the expected output for a couple different calls to sortAndPrint.
	}

	/* end */
}
