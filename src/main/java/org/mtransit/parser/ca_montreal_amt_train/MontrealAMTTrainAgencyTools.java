package org.mtransit.parser.ca_montreal_amt_train;

import org.jetbrains.annotations.NotNull;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Locale;
import java.util.regex.Pattern;

import static org.mtransit.commons.StringUtils.EMPTY;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/trains/google_transit.zip
public class MontrealAMTTrainAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new MontrealAMTTrainAgencyTools().start(args);
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "exo";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String result) {
		result = CleanUtils.SAINT.matcher(result).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		return CleanUtils.cleanLabel(result);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	private static final Pattern MONT_SAINT_HILAIRE_ = CleanUtils.cleanWords("mont-saint-hilaire", "mont-st-hilaire");
	private static final String MONT_SAINT_HILAIRE_REPLACEMENT = CleanUtils.cleanWordsReplacement("St-Hilaire");

	private static final Pattern STARTS_WITH_SLASH = Pattern.compile("(^[^/]+/( )?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeading) {
		tripHeading = MONT_SAINT_HILAIRE_.matcher(tripHeading).replaceAll(MONT_SAINT_HILAIRE_REPLACEMENT);
		tripHeading = STARTS_WITH_SLASH.matcher(tripHeading).replaceAll(EMPTY);
		tripHeading = GARE.matcher(tripHeading).replaceAll(EMPTY);
		return CleanUtils.cleanLabelFR(tripHeading);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern GARE = Pattern.compile("(gare )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = GARE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_EN_DASHES.matcher(gStopName).replaceAll(CleanUtils.CLEAN_EN_DASHES_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(Locale.FRENCH, gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		//noinspection deprecation
		return gStop.getStopId(); // using stop ID as stop code (useful to match with GTFS real-time)
	}

	private static final String ZERO = "0";

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private static final String D = "D";

	@Override
	public int getStopId(@NotNull GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode.equals(ZERO)) {
			throw new MTLog.Fatal("Unexpected stop ID %s!", gStop);
		}
		int stopId = Integer.parseInt(stopCode); // using stop code as stop ID
		//noinspection deprecation
		final String stopId1 = gStop.getStopId();
		if (stopId1.endsWith(A)) {
			return 1_000_000 + stopId;
		} else if (stopId1.endsWith(B)) {
			return 2_000_000 + stopId;
		} else if (stopId1.endsWith(C)) {
			return 3_000_000 + stopId;
		} else if (stopId1.endsWith(D)) {
			return 4_000_000 + stopId;
		}
		throw new MTLog.Fatal("Unexpected stop ID %s!", gStop);
	}
}
