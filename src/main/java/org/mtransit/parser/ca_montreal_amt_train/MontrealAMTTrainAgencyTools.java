package org.mtransit.parser.ca_montreal_amt_train;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

// https://exo.quebec/en/about/open-data
// https://exo.quebec/xdata/trains/google_transit.zip
public class MontrealAMTTrainAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-montreal-amt-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MontrealAMTTrainAgencyTools().start(args);
	}

	private HashSet<Integer> serviceIds;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating exo train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating exo train data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTripInt(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
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
	public String getRouteLongName(@NotNull GRoute gRoute) {
		return cleanRouteLongName(gRoute.getRouteLongName());
	}

	private String cleanRouteLongName(String result) {
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

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_SLASH = Pattern.compile("(^[^/]+/( )?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeading) {
		tripHeading = STARTS_WITH_SLASH.matcher(tripHeading).replaceAll(StringUtils.EMPTY);
		tripHeading = GARE.matcher(tripHeading).replaceAll(StringUtils.EMPTY);
		tripHeading = DIRECTION.matcher(tripHeading).replaceAll(StringUtils.EMPTY);
		return CleanUtils.cleanLabelFR(tripHeading);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) { // exo1 - Vaudreuil-Hudson
			if (Arrays.asList( //
					"Beaconsfield", //
					"Vaudreuil" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Vaudreuil", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Beaconsfield", //
					"Vaudreuil", //
					"Hudson" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Hudson", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) { // exo6 - Deux-Montagnes
			if (Arrays.asList( //
					"Roxboro-Pierrefonds", //
					"Deux-Montagnes" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Deux-Montagnes", mTrip.getHeadsignId());
				return true;
			}
			if (Arrays.asList( //
					"Bois-Franc", //
					"Centrale" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Centrale", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 4L) { // exo2 - St-Jérôme
			if (Arrays.asList( //
					"Concorde", //
					"Parc", //
					"Lucien-L'Allier" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Lucien-L'Allier", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 6L) { // exo5 - Mascouche
			if (Arrays.asList( //
					"Ahuntsic", //
					"Centrale" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Centrale", mTrip.getHeadsignId());
				return true;
			}
		}
		MTLog.logFatal("%s: Couldn't merge %s and %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
		return false;
	}

	private static final Pattern GARE = Pattern.compile("(gare )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = GARE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_EN_DASHES.matcher(gStopName).replaceAll(CleanUtils.CLEAN_EN_DASHES_REPLACEMENT);
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
		if (ZERO.equals(stopCode)) {
			MTLog.logFatal("Unexpected stop ID %s!", gStop);
			return -1;
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
