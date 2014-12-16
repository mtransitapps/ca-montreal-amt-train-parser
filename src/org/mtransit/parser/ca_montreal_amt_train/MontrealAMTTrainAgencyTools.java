package org.mtransit.parser.ca_montreal_amt_train;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.amt.qc.ca/developers/
// http://www.amt.qc.ca/xdata/trains/google_transit.zip
public class MontrealAMTTrainAgencyTools extends DefaultAgencyTools {

	public static final String ROUTE_TYPE_FILTER = "2"; // train only

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-montreal-amt-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MontrealAMTTrainAgencyTools().start(args);
	}

	@Override
	public void start(String[] args) {
		System.out.printf("Generating AMT train data...\n");
		long start = System.currentTimeMillis();
		super.start(args);
		System.out.printf("Generating AMT train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (ROUTE_TYPE_FILTER != null && !gRoute.route_type.equals(ROUTE_TYPE_FILTER)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return cleanRouteLongName(gRoute.route_long_name);
	}

	private String cleanRouteLongName(String result) {
		result = MSpec.SAINT.matcher(result).replaceAll(MSpec.SAINT_REPLACEMENT);
		return MSpec.cleanLabel(result);
	}

	@Override
	public void setTripHeadsign(MRoute route, MTrip mTrip, GTrip gTrip) {
		String stationName = cleanTripHeadsign(gTrip.trip_headsign);
		int directionId = Integer.valueOf(gTrip.direction_id);
		mTrip.setHeadsignString(stationName, directionId);
	}

	@Override
	public String cleanTripHeadsign(String tripHeading) {
		return MSpec.cleanLabelFR(tripHeading.substring("Direction ".length()));
	}

	private static List<String> VH = Arrays.asList(new String[] { "Beaconsfield", "Hudson", "Vaudreuil" });
	private static String VH_HV = "Vaudreuil / Hudson / Beaconsfield";
	private static List<String> DM = Arrays.asList(new String[] { "Roxboro-Pierrefonds", "Deux-Montagnes" });
	private static String DM_HV = "Deux-Montagnes / Roxboro-Pierrefonds";

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (VH.contains(mTrip.getHeadsignValue()) && VH.contains(mTripToMerge.getHeadsignValue())) {
			mTrip.setHeadsignString(VH_HV, mTrip.getHeadsignId());
			return true;
		}
		if (DM.contains(mTrip.getHeadsignValue()) && DM.contains(mTripToMerge.getHeadsignValue())) {
			mTrip.setHeadsignString(DM_HV, mTrip.getHeadsignId());
			return true;
		}
		return super.mergeHeadsign(mTrip, mTripToMerge);
	}

	public static final String PLACE_CHAR_STATION = "gare";
	public static final int PLACE_CHAR_STATION_LENGTH = PLACE_CHAR_STATION.length();



	@Override
	public String cleanStopName(String gStopName) {
		String result = gStopName.toLowerCase(Locale.ENGLISH);
		result = MSpec.CLEAN_EN_DASHES.matcher(result).replaceAll(MSpec.CLEAN_EN_DASHES_REPLACEMENT);
		return super.cleanStopNameFR(result);
	}

	@Override
	public String getStopCode(GStop gStop) {
		if (gStop.stop_code.equals("0")) {
			System.out.println("stop ID 0: " + gStop.stop_code + ", " + gStop.stop_id + ", " + gStop.stop_name);
			System.exit(-1);
		}
		return super.getStopCode(gStop);
	}

	@Override
	public int getStopId(GStop gStop) {
		return Integer.valueOf(getStopCode(gStop)); // using stop code as stop ID
	}

}
