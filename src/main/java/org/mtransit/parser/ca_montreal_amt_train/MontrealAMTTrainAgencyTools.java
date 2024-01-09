package org.mtransit.parser.ca_montreal_amt_train;

import static org.mtransit.commons.RegexUtils.BEGINNING;
import static org.mtransit.commons.RegexUtils.DIGIT_CAR;
import static org.mtransit.commons.RegexUtils.WHITESPACE_CAR;
import static org.mtransit.commons.RegexUtils.atLeastOne;
import static org.mtransit.commons.RegexUtils.group;
import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://exo.quebec/en/about/open-data
public class MontrealAMTTrainAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new MontrealAMTTrainAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_FR;
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

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) { // route ID used to target Twitter news & GTFS RT
		final String rsn = gRoute.getRouteShortName();
		if (!CharUtils.isDigitsOnly(rsn)) {
			switch (rsn) {
			case "VH":
				return 11L;
			case "SJ":
				return 12L;
			case "SH":
				return 13L;
			case "CA":
				return 14L;
			case "MA":
				return 15L;
			default:
				throw new MTLog.Fatal("Unexpected route short name for %s", gRoute.toStringPlus());
			}
		}
		return super.getRouteId(gRoute);
	}

	@Override
	public @NotNull String getRouteShortName(@NotNull GRoute gRoute) {
		return String.valueOf(getRouteId(gRoute));
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return false; // route ID used to target Twitter news & GTFS RT
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_RSN_ = Pattern.compile(group(BEGINNING + atLeastOne(DIGIT_CAR)) + WHITESPACE_CAR + "-" + WHITESPACE_CAR);

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String result) {
		result = CleanUtils.SAINT.matcher(result).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		result = STARTS_WITH_RSN_.matcher(result).replaceAll(EMPTY);
		return CleanUtils.cleanLabelFR(result);
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
		gStopName = GARE.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.CLEAN_EN_DASHES.matcher(gStopName).replaceAll(CleanUtils.CLEAN_EN_DASHES_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(Locale.FRENCH, gStopName);
		gStopName = CleanUtils.cleanStreetTypesFRCA(gStopName);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		if (FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
			return EMPTY; // remove stop code (not visible on agency info) // super.getStopCode(gStop);
		}
		//noinspection deprecation
		return gStop.getStopId(); // using stop ID as stop code (useful to match with GTFS real-time)
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
	}
}
