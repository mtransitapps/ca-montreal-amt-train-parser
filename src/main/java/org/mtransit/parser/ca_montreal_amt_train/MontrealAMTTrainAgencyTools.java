package org.mtransit.parser.ca_montreal_amt_train;

import static org.mtransit.commons.RegexUtils.ANY;
import static org.mtransit.commons.RegexUtils.BEGINNING;
import static org.mtransit.commons.RegexUtils.DIGIT_CAR;
import static org.mtransit.commons.RegexUtils.WHITESPACE_CAR;
import static org.mtransit.commons.RegexUtils.any;
import static org.mtransit.commons.RegexUtils.atLeastOne;
import static org.mtransit.commons.RegexUtils.group;
import static org.mtransit.commons.RegexUtils.matchGroup;
import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.List;
import java.util.Locale;

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
		return super.getRouteId(gRoute); // used for GTFS-RT
	}

	private static final Cleaner STARTS_WITH_RSN_ = new Cleaner(
			BEGINNING + group(atLeastOne(DIGIT_CAR)) + WHITESPACE_CAR + "-" + WHITESPACE_CAR + group(any(ANY)),
			matchGroup(1)
	);

	@Override
	public @NotNull String getRouteShortName(@NotNull GRoute gRoute) {
		String rsn = gRoute.getRouteShortName();
		if (!CharUtils.isDigitsOnly(rsn)) {
			rsn = STARTS_WITH_RSN_.clean(gRoute.getRouteLongNameOrDefault());
		}
		if (!CharUtils.isDigitsOnly(rsn)) {
			throw new MTLog.Fatal("Unexpected route short name '%s' for %s", rsn, gRoute.toStringPlus());
		}
		return rsn;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return false; // route ID used to target Twitter news & GTFS RT
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.SAINT.matcher(routeLongName).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		routeLongName = STARTS_WITH_RSN_.clean(routeLongName, matchGroup(2));
		return CleanUtils.cleanLabelFR(routeLongName);
	}

	private static final Cleaner MONT_SAINT_HILAIRE_ = new Cleaner(
			Cleaner.matchWords("mont-saint-hilaire", "mont-st-hilaire"),
			"St-Hilaire",
			true
	);

	private static final Cleaner STARTS_WITH_SLASH = new Cleaner("(^[^/]+/( )?)", true);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeading) {
		tripHeading = MONT_SAINT_HILAIRE_.clean(tripHeading);
		tripHeading = STARTS_WITH_SLASH.clean(tripHeading);
		tripHeading = GARE.clean(tripHeading);
		return CleanUtils.cleanLabelFR(tripHeading);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Cleaner GARE = new Cleaner("(gare )", true);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = GARE.clean(gStopName);
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
