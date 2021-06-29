package nz.cri.gns.NZSHM22.opensha.inversion;

import org.dom4j.DocumentException;
import org.opensha.sha.earthquake.faultSysSolution.inversion.constraints.InversionConstraint;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.PlausibilityConfiguration;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.util.RuptureConnectionSearch;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.util.SectionDistanceAzimuthCalculator;
import org.opensha.sha.faultSurface.FaultSection;

import com.google.common.base.Preconditions;

import nz.cri.gns.NZSHM22.opensha.enumTreeBranches.NZSHM22_FaultModels;
import nz.cri.gns.NZSHM22.opensha.griddedSeismicity.NZSHM22_GridSourceGenerator;
import nz.cri.gns.NZSHM22.opensha.ruptures.NZSHM22_AzimuthalRuptureSetBuilder;
import scratch.UCERF3.FaultSystemRupSet;
import scratch.UCERF3.FaultSystemSolution;
import scratch.UCERF3.SlipEnabledSolution;
import scratch.UCERF3.analysis.FaultSystemRupSetCalc;
import scratch.UCERF3.enumTreeBranches.InversionModels;
import scratch.UCERF3.inversion.UCERF3InversionConfiguration;
import scratch.UCERF3.inversion.UCERF3InversionConfiguration.SlipRateConstraintWeightingType;
import scratch.UCERF3.inversion.UCERF3SectionConnectionStrategy;
import scratch.UCERF3.inversion.laughTest.OldPlausibilityConfiguration;
import scratch.UCERF3.logicTree.LogicTreeBranch;
import scratch.UCERF3.inversion.CommandLineInversionRunner;
import scratch.UCERF3.inversion.InversionFaultSystemRupSet;
import scratch.UCERF3.inversion.InversionFaultSystemSolution;
import scratch.UCERF3.inversion.SectionClusterList;
import scratch.UCERF3.inversion.SectionConnectionStrategy;
//import scratch.UCERF3.inversion.CommandLineInversionRunner.getSectionMoments;
import scratch.UCERF3.simulatedAnnealing.ConstraintRange;
import scratch.UCERF3.simulatedAnnealing.ThreadedSimulatedAnnealing;
import scratch.UCERF3.simulatedAnnealing.completion.*;
import scratch.UCERF3.utils.FaultSystemIO;
import scratch.UCERF3.utils.aveSlip.AveSlipConstraint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the standard NSHM inversion on a crustal rupture set.
 */
public class NZSHM22_CrustalInversionRunner extends NZSHM22_AbstractInversionRunner {

	/*
	 * Sliprate constraint default settings
	 */
	// If normalized, slip rate misfit is % difference for each section (recommended
	// since it helps fit slow-moving faults).
	// If unnormalized, misfit is absolute difference.
	// BOTH includes both normalized and unnormalized constraints.
	protected SlipRateConstraintWeightingType slipRateWeightingType = SlipRateConstraintWeightingType.BOTH; // (recommended:
																																									// BOTH)
	// For SlipRateConstraintWeightingType.NORMALIZED (also used for
	// SlipRateConstraintWeightingType.BOTH) -- NOT USED if UNNORMALIZED!
	protected double slipRateConstraintWt_normalized = 1;
	// For SlipRateConstraintWeightingType.UNNORMALIZED (also used for
	// SlipRateConstraintWeightingType.BOTH) -- NOT USED if NORMALIZED!
	protected double slipRateConstraintWt_unnormalized = 100;

	/*
	 * MFD constraint default settings
	 */
	protected double totalRateM5 = 5d;
	protected double bValue = 1d;
	protected double mfdTransitionMag = 7.85; // TODO: how to validate this number for NZ? (ref Morgan Page in
												// USGS/UCERF3) [KKS, CBC]
	protected int mfdNum = 40;
	protected double mfdMin = 5.05d;
	protected double mfdMax = 8.95;

	protected double mfdEqualityConstraintWt = 10;
	protected double mfdInequalityConstraintWt = 1000;

	private NZSHM22_CrustalInversionConfiguration inversionConfiguration;
	private int slipRateUncertaintyWeight;
	private int slipRateUncertaintyScalingFactor;


	/**
	 * Creates a new NZSHM22_InversionRunner with defaults.
	 */
	public NZSHM22_CrustalInversionRunner() {
		super();
	}

	public NZSHM22_CrustalInversionRunner setInversionMinutes(long inversionMinutes) {
		return (NZSHM22_CrustalInversionRunner) super.setInversionMinutes(inversionMinutes);
	}


	public NZSHM22_CrustalInversionRunner setInversionSeconds(long inversionSeconds) {
		return (NZSHM22_CrustalInversionRunner) super.setInversionSeconds(inversionSeconds);
	}
	
	public NZSHM22_CrustalInversionRunner setEnergyChangeCompletionCriteria(double energyDelta, double energyPercentDelta,
			double lookBackMins) {
		return (NZSHM22_CrustalInversionRunner) super.setEnergyChangeCompletionCriteria(
				energyDelta, energyPercentDelta, lookBackMins);
	}

	public NZSHM22_CrustalInversionRunner setSyncInterval(long syncInterval) {
		return (NZSHM22_CrustalInversionRunner) super.setSyncInterval(syncInterval);
	}

	public NZSHM22_CrustalInversionRunner setNumThreads(int numThreads) {
		return (NZSHM22_CrustalInversionRunner) super.setNumThreads(numThreads);
	}

	public NZSHM22_CrustalInversionRunner setRuptureSetFile(String ruptureSetFileName) throws IOException, DocumentException {
		File rupSetFile = new File(ruptureSetFileName);
		this.setRuptureSetFile(rupSetFile);
		return this;
	}

	/**
	 * Sets the FaultModel file
	 *
	 * @param ruptureSetFile the rupture file
	 * @return this builder
	 * @throws DocumentException
	 * @throws IOException
	 */
	public NZSHM22_CrustalInversionRunner setRuptureSetFile(File ruptureSetFile) throws IOException, DocumentException {
		FaultSystemRupSet rupSetA = FaultSystemIO.loadRupSet(ruptureSetFile);
		LogicTreeBranch branch = (LogicTreeBranch) LogicTreeBranch.DEFAULT;

		this.rupSet = new NZSHM22_InversionFaultSystemRuptSet(rupSetA, branch);
		return this;
	}

//	/**
//	 * Sets GutenbergRichterMFD arguments
//	 * 
//	 * @param totalRateM5      the number of M>=5's per year. TODO: ref David
//	 *                         Rhodes/Chris Roland? [KKS, CBC]
//	 * @param bValue
//	 * @param mfdTransitionMag magnitude to switch from MFD equality to MFD
//	 *                         inequality TODO: how to validate this number for NZ?
//	 *                         (ref Morgan Page in USGS/UCERF3) [KKS, CBC]
//	 * @param mfdNum
//	 * @param mfdMin
//	 * @param mfdMax
//	 * @return
//	 */
//	public NZSHM22_InversionRunner setGutenbergRichterMFD(double totalRateM5, double bValue, double mfdTransitionMag,
//			int mfdNum, double mfdMin, double mfdMax) {
//		this.totalRateM5 = totalRateM5;
//		this.bValue = bValue;
//		this.mfdTransitionMag = mfdTransitionMag;
//		this.mfdNum = mfdNum;
//		this.mfdMin = mfdMin;
//		this.mfdMax = mfdMax;
//		return this;
//	}

	/**
	 * @param mfdEqualityConstraintWt
	 * @param mfdInequalityConstraintWt
	 * @return
	 */
	public NZSHM22_CrustalInversionRunner setGutenbergRichterMFDWeights(double mfdEqualityConstraintWt,
			double mfdInequalityConstraintWt) {
		this.mfdEqualityConstraintWt = mfdEqualityConstraintWt;
		this.mfdInequalityConstraintWt = mfdInequalityConstraintWt;
		return this;
	}

	/**
	 * If normalized, slip rate misfit is % difference for each section (recommended
	 * since it helps fit slow-moving faults). If unnormalized, misfit is absolute
	 * difference. BOTH includes both normalized and unnormalized constraints.
	 * 
	 * @param weightingType  a value
	 *                       fromUCERF3InversionConfiguration.SlipRateConstraintWeightingType
	 * @param normalizedWt
	 * @param unnormalizedWt
	 * @throws IllegalArgumentException if the weighting types is not supported by this constraint
	 * @return
	 */
	public NZSHM22_CrustalInversionRunner setSlipRateConstraint(
			SlipRateConstraintWeightingType weightingType, double normalizedWt,
			double unnormalizedWt) {
		Preconditions.checkArgument(weightingType != SlipRateConstraintWeightingType.UNCERTAINTY_ADJUSTED,
				"setSlipRateConstraint() using  %s is not supported. Use setSlipRateUncertaintyConstraint() instead.", weightingType);
		this.slipRateWeightingType = weightingType;
		this.slipRateConstraintWt_normalized = normalizedWt;
		this.slipRateConstraintWt_unnormalized = unnormalizedWt;
		return this;
	}

	/**
	 * UCERF3 Slip rate uncertainty constraint
	 * 
	 * @param weightingType  a string value from  fromUCERF3InversionConfiguration.SlipRateConstraintWeightingType
	 * @param normalizedWt
	 * @param unnormalizedWt
	 * @throws IllegalArgumentException if the weighting types is not supported by this constraint
	 * @return
	 */
    public NZSHM22_CrustalInversionRunner setSlipRateConstraint(String weightingType, double normalizedWt, double unnormalizedWt) {
    	setSlipRateConstraint(SlipRateConstraintWeightingType.valueOf(weightingType), normalizedWt, unnormalizedWt);
        return this;
    }
	
	
	/**
	 * New NZSHM22 Slip rate uncertainty constraint
	 * 
	 * @param uncertaintyWeight
	 * @param scalingFactor
	 * @throws IllegalArgumentException if the weighting types is not supported by this constraint
	 * @return
	 */
	public NZSHM22_CrustalInversionRunner setSlipRateUncertaintyConstraint(SlipRateConstraintWeightingType weightingType, 
			int uncertaintyWeight, int scalingFactor) {
		Preconditions.checkArgument(weightingType == SlipRateConstraintWeightingType.UNCERTAINTY_ADJUSTED,
				"setSlipRateUncertaintyConstraint() using %s is not supported. Use setSlipRateConstraint() instead.", weightingType);
		this.slipRateWeightingType = weightingType;
		this.slipRateUncertaintyWeight = uncertaintyWeight;
		this.slipRateUncertaintyScalingFactor = scalingFactor;
		return this;
	}	
	
	/**
	 * New NZSHM22 Slip rate uncertainty constraint
	 * 
	 * @param uncertaintyWeight
	 * @param scalingFactor
	 * @throws IllegalArgumentException if the weighting types is not supported by this constraint
	 * @return
	 */
    public NZSHM22_CrustalInversionRunner setSlipRateUncertaintyConstraint(String weightingType, 
			int uncertaintyWeight, int scalingFactor) {		
    	setSlipRateUncertaintyConstraint(SlipRateConstraintWeightingType.valueOf(weightingType),
    			uncertaintyWeight, scalingFactor);
        return this;
    }

	public NZSHM22_CrustalInversionRunner setInversionConfiguration(NZSHM22_CrustalInversionConfiguration config) {
		System.out.println("Building Inversion Configuration");
		inversionConfiguration = config;
		return this;
	}

	public NZSHM22_CrustalInversionRunner configure() {
		LogicTreeBranch logicTreeBranch = this.rupSet.getLogicTreeBranch();
		InversionModels inversionModel = logicTreeBranch.getValue(InversionModels.class);

		// this contains all inversion weights
		inversionConfiguration = NZSHM22_CrustalInversionConfiguration.forModel(inversionModel, rupSet, mfdEqualityConstraintWt,
				mfdInequalityConstraintWt);
		
//		inversionConfiguration = NZSHM22_SubductionInversionConfiguration.forModel(inversionModel, rupSet,
//				mfdEqualityConstraintWt, mfdInequalityConstraintWt);
		
		//set up slip rate config
		inversionConfiguration.setSlipRateWeightingType(this.slipRateWeightingType);
		if (this.slipRateWeightingType == SlipRateConstraintWeightingType.UNCERTAINTY_ADJUSTED) {
			System.out.println("config for UNCERTAINTY_ADJUSTED " + this.slipRateUncertaintyWeight + ", " + this.slipRateUncertaintyScalingFactor);
			inversionConfiguration.setSlipRateUncertaintyConstraintWt(this.slipRateUncertaintyWeight);
			inversionConfiguration.setSlipRateUncertaintyConstraintScalingFactor(this.slipRateUncertaintyScalingFactor);
		} else {
			inversionConfiguration.setSlipRateConstraintWt_normalized(this.slipRateConstraintWt_normalized);
			inversionConfiguration.setSlipRateConstraintWt_unnormalized(this.slipRateConstraintWt_unnormalized);
		}
		
		/*
		 * Build inversion inputs
		 */
		List<AveSlipConstraint> aveSlipConstraints = null;	
		NZSHM22_CrustalInversionInputGenerator inversionInputGenerator = new NZSHM22_CrustalInversionInputGenerator(rupSet, 
				inversionConfiguration, null, aveSlipConstraints, null, null);	
		super.setInversionInputGenerator(inversionInputGenerator);
		return this;
	}

	@SuppressWarnings("unchecked")
	protected FaultSystemRupSet loadRupSet(File file) throws IOException, DocumentException {
		FaultSystemRupSet fsRupSet = FaultSystemIO.loadRupSet(file);
		return fsRupSet;

	}
	
	/**
	 * Runs the inversion on the specified rupture set. make sure to call
	 * .configure() first.
	 * 
	 * @return the FaultSystemSolution.
	 * @throws IOException
	 * @throws DocumentException
	 */
	public NZSHM22_InversionFaultSystemSolution runInversion() throws IOException, DocumentException {
		NZSHM22_InversionFaultSystemSolution solution = super.runInversion();
		solution.setGridSourceProvider(new NZSHM22_GridSourceGenerator((NZSHM22_InversionFaultSystemSolution) solution));
		return solution;
	}
	
	public static void main(String[] args) throws IOException, DocumentException {

		File inputDir = new File("/home/chrisbc/Downloads");
		File outputRoot = new File("/tmp/NZSHM");
		File ruptureSet = new File(inputDir,
				"RupSet_Az_FM(CFM_0_3_SANSTVZ)_mxSbScLn(0.5)_mxAzCh(60.0)_mxCmAzCh(560.0)_mxJpDs(5.0)_mxTtAzCh(60.0)_thFc(0.0)(1).zip");
		File outputDir = new File(outputRoot, "inversions");
		Preconditions.checkState(outputDir.exists() || outputDir.mkdir());

		NZSHM22_CrustalInversionRunner runner = new NZSHM22_CrustalInversionRunner();
		runner.setInversionMinutes(1).setRuptureSetFile(ruptureSet)
			.setSlipRateConstraint("BOTH", 1.0, 100.0)
			.setGutenbergRichterMFDWeights(100.0, 1000.0)
			.configure();

		NZSHM22_InversionFaultSystemSolution solution = runner.runInversion();

		File solutionFile = new File(outputDir, "CrustalInversionSolution.zip");

		FaultSystemIO.writeSol(solution, solutionFile);

	}	
}
