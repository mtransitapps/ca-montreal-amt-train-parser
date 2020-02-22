package org.mtransit.parser.ca_montreal_amt_train;

import org.apache.commons.lang3.StringUtils;
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

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-montreal-amt-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MontrealAMTTrainAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating exo train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating exo train data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	private static final String AGENCY_COLOR = "1F1F1F"; // DARK GRAY (from GTFS)

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute.getRouteLongName());
	}

	private String cleanRouteLongName(String result) {
		result = CleanUtils.SAINT.matcher(result).replaceAll(CleanUtils.SAINT_REPLACEMENT);
		return CleanUtils.cleanLabel(result);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final Pattern DIRECTION = Pattern.compile("(direction )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_SLASH = Pattern.compile("(^[^/]+/( )?)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeading) {
		tripHeading = STARTS_WITH_SLASH.matcher(tripHeading).replaceAll(StringUtils.EMPTY);
		tripHeading = GARE.matcher(tripHeading).replaceAll(StringUtils.EMPTY);
		tripHeading = DIRECTION.matcher(tripHeading).replaceAll(StringUtils.EMPTY);
		return CleanUtils.cleanLabelFR(tripHeading);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
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

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = GARE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_EN_DASHES.matcher(gStopName).replaceAll(CleanUtils.CLEAN_EN_DASHES_REPLACEMENT);
		return CleanUtils.cleanLabelFR(gStopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.getStopId(); // using stop ID as stop code (useful to match with GTFS real-time)
	}

	private static final String ZERO = "0";

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";
	private static final String D = "D";

	@Override
	public int getStopId(GStop gStop) {
		String stopCode = gStop.getStopCode();
		if (stopCode == null || stopCode.equals(ZERO)) {
			MTLog.logFatal("Unexpected stop ID %s!", gStop);
			return -1;
		}
		int stopId = Integer.valueOf(stopCode); // using stop code as stop ID
		if (gStop.getStopId().endsWith(A)) {
			return 1000000 + stopId;
		} else if (gStop.getStopId().endsWith(B)) {
			return 2000000 + stopId;
		} else if (gStop.getStopId().endsWith(C)) {
			return 3000000 + stopId;
		} else if (gStop.getStopId().endsWith(D)) {
			return 4000000 + stopId;
		}
		MTLog.logFatal("Unexpected stop ID %s!", gStop);
		return -1;
	}
}
