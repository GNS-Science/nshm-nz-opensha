package nz.cri.gns.NSHM.opensha.ruptures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.opensha.commons.util.FaultUtils;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.ClusterRupture;
import org.opensha.sha.faultSurface.FaultSection;

import scratch.UCERF3.SlipAlongRuptureModelRupSet;
import scratch.UCERF3.enumTreeBranches.ScalingRelationships;
import scratch.UCERF3.enumTreeBranches.SlipAlongRuptureModels;

import nz.cri.gns.NSHM.opensha.ruptures.DownDipFaultSection;

/**
 * This is an example provided by Kevin for demo purposes.
 * 
 * TODO: examine why/how this is different to standard UCERF3 code.
 *
 */
public class NSHMSlipEnabledRuptureSet extends SlipAlongRuptureModelRupSet {

	private double[] rupAveSlips;
	private static final long serialVersionUID = -4984738430816137976L;

	public NSHMSlipEnabledRuptureSet(List<ClusterRupture> ruptures, List<FaultSection> subSections,
			ScalingRelationships scale, SlipAlongRuptureModels slipAlongModel) {
		super(slipAlongModel);

		// build a rupture set (doing this manually instead of creating an inversion
		// fault system rup set,
		// mostly as a demonstration)
		double[] sectSlipRates = new double[subSections.size()];
		double[] sectAreasReduced = new double[subSections.size()];
		double[] sectAreasOrig = new double[subSections.size()];
		for (int s = 0; s < sectSlipRates.length; s++) {
			FaultSection sect = subSections.get(s);
			sectAreasReduced[s] = sect.getArea(true);
			sectAreasOrig[s] = sect.getArea(false);
			sectSlipRates[s] = sect.getReducedAveSlipRate() * 1e-3; // mm/yr => m/yr
		}

		double[] rupMags = new double[ruptures.size()];
		double[] rupRakes = new double[ruptures.size()];
		double[] rupAreas = new double[ruptures.size()];
		double[] rupLengths = new double[ruptures.size()];
		rupAveSlips = new double[ruptures.size()];
		List<List<Integer>> rupsIDsList = new ArrayList<>();

		/*
		 * build a map of downdip ruptures with length
		 */
		Map<Integer, Double> ruptureLengths = new HashMap<>();
		Integer currentMin; // , currentMax;

		for (int r = 0; r < ruptures.size(); r++) {
			ClusterRupture rup = ruptures.get(r);
			currentMin = Integer.MAX_VALUE; // ruptureMinExtents.get(rup.hashCode());

			List<FaultSection> rupSects = rup.buildOrderedSectionList();
			// get min row of downdip (assumes there's just one of these)
			for (FaultSection sect : rupSects) {
				if (DownDipFaultSection.class.isInstance(sect)) {
					if (((DownDipFaultSection) sect).getRowIndex() < currentMin)
						currentMin = ((DownDipFaultSection) sect).getRowIndex();
				}
			}

//			FaultSection sect0 = rup.clusters[0].startSect;
//			if (DownDipFaultSection.class.isInstance(sect0))
//				currentMin = ((DownDipFaultSection) sect0).getRowIndex();

			Double length = 0d;
			// iterate sections adding lengths from those in the minimum row
			for (FaultSection sect : rupSects) {
				if (DownDipFaultSection.class.isInstance(sect)) {
					if (((DownDipFaultSection) sect).getRowIndex() == currentMin) {
						length += sect.getTraceLength() * 1e3;
					}
				}
			}
			if (length > 0)
				ruptureLengths.put(rup.hashCode(), length);
		}

		for (int r = 0; r < ruptures.size(); r++) {
			ClusterRupture rup = ruptures.get(r);
			List<FaultSection> rupSects = rup.buildOrderedSectionList();
			List<Integer> sectIDs = new ArrayList<>();
			double totLength = 0d;
			double totArea = 0d;
			double totOrigArea = 0d; // not reduced for aseismicity
			List<Double> sectAreas = new ArrayList<>();
			List<Double> sectRakes = new ArrayList<>();
			for (FaultSection sect : rupSects) {
				// lengths for crustal sections
				if (!DownDipFaultSection.class.isInstance(sect))
					totLength += sect.getTraceLength() * 1e3; // km --> m;

				sectIDs.add(sect.getSectionId());
				double area = sectAreasReduced[sect.getSectionId()]; // sq-m
				totArea += area;
				totOrigArea += sectAreasOrig[sect.getSectionId()]; // sq-m
				sectAreas.add(area);
				sectRakes.add(sect.getAveRake());
			}

			// extend length for downdip ruptures
			if (ruptureLengths.get(rup.hashCode()) != null)
				totLength += ruptureLengths.get(rup.hashCode());

			rupAreas[r] = totArea;
			rupLengths[r] = totLength;
			rupRakes[r] = FaultUtils.getInRakeRange(FaultUtils.getScaledAngleAverage(sectAreas, sectRakes));
			double origDDW = totOrigArea / totLength;
			rupMags[r] = scale.getMag(totArea, origDDW);
			rupsIDsList.add(sectIDs);
			rupAveSlips[r] = scale.getAveSlip(totArea, totLength, origDDW);
		}

		String info = "Test down-dip subsectioning rup set";

		init(subSections, sectSlipRates, null, sectAreasReduced, rupsIDsList, rupMags, rupRakes, rupAreas, rupLengths,
				info);
		setClusterRuptures(ruptures);
	}

	@Override
	public double getAveSlipForRup(int rupIndex) {
		return rupAveSlips[rupIndex];
	}

	@Override
	public double[] getAveSlipForAllRups() {
		return rupAveSlips;
	}
}
