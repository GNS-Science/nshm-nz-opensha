package nz.cri.gns.NZSHM22.opensha.inversion;

import org.dom4j.DocumentException;
import com.google.common.base.Preconditions;

import scratch.UCERF3.FaultSystemRupSet;
import scratch.UCERF3.FaultSystemSolution;
import scratch.UCERF3.enumTreeBranches.InversionModels;
import scratch.UCERF3.inversion.UCERF3InversionConfiguration.SlipRateConstraintWeightingType;
import scratch.UCERF3.logicTree.LogicTreeBranch;
import scratch.UCERF3.utils.FaultSystemIO;
import scratch.UCERF3.utils.aveSlip.AveSlipConstraint;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Runs the standard NSHM inversion on a crustal rupture set.
 */
public class NZSHM22_SubductionInversionRunner extends NZSHM22_AbstractInversionRunner {

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

	private NZSHM22_SubductionInversionConfiguration inversionConfiguration;

	/**
	 * Creates a new NZSHM22_InversionRunner with defaults.
	 */
	public NZSHM22_SubductionInversionRunner() {
		super();
	}

	public NZSHM22_SubductionInversionRunner setInversionMinutes(long inversionMinutes) {
		return (NZSHM22_SubductionInversionRunner) super.setInversionMinutes(inversionMinutes);
	}

	public NZSHM22_SubductionInversionRunner setInversionSeconds(long inversionSeconds) {
		return (NZSHM22_SubductionInversionRunner) super.setInversionSeconds(inversionSeconds);
	}

	public NZSHM22_SubductionInversionRunner setEnergyChangeCompletionCriteria(double energyDelta,
			double energyPercentDelta, double lookBackMins) {
		return (NZSHM22_SubductionInversionRunner) super.setEnergyChangeCompletionCriteria(energyDelta,
				energyPercentDelta, lookBackMins);
	}

	public NZSHM22_SubductionInversionRunner setSyncInterval(long syncInterval) {
		return (NZSHM22_SubductionInversionRunner) super.setSyncInterval(syncInterval);
	}

	public NZSHM22_SubductionInversionRunner setNumThreads(int numThreads) {
		return (NZSHM22_SubductionInversionRunner) super.setNumThreads(numThreads);
	}

	public NZSHM22_SubductionInversionRunner setRuptureSetFile(String ruptureSetFileName)
			throws IOException, DocumentException {
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
	public NZSHM22_SubductionInversionRunner setRuptureSetFile(File ruptureSetFile)
			throws IOException, DocumentException {
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
	public NZSHM22_SubductionInversionRunner setGutenbergRichterMFDWeights(double mfdEqualityConstraintWt,
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
	 * @throws IllegalArgumentException if the weighting types is not supported by
	 *                                  this constraint
	 * @return
	 */
	public NZSHM22_SubductionInversionRunner setSlipRateConstraint(SlipRateConstraintWeightingType weightingType,
			double normalizedWt, double unnormalizedWt) {
		Preconditions.checkArgument(weightingType != SlipRateConstraintWeightingType.UNCERTAINTY_ADJUSTED,
				"setSlipRateConstraint() using  %s is not supported. Use setSlipRateUncertaintyConstraint() instead.",
				weightingType);
		this.slipRateWeightingType = weightingType;
		this.slipRateConstraintWt_normalized = normalizedWt;
		this.slipRateConstraintWt_unnormalized = unnormalizedWt;
		return this;
	}

	public NZSHM22_SubductionInversionRunner setInversionConfiguration(
			NZSHM22_SubductionInversionConfiguration config) {
		System.out.println("Building Inversion Configuration");
		inversionConfiguration = config;
		return this;
	}

	public NZSHM22_SubductionInversionRunner configure() {
		LogicTreeBranch logicTreeBranch = this.rupSet.getLogicTreeBranch();
		InversionModels inversionModel = logicTreeBranch.getValue(InversionModels.class);

		inversionConfiguration = NZSHM22_SubductionInversionConfiguration.forModel(inversionModel, rupSet,
				mfdEqualityConstraintWt, mfdInequalityConstraintWt);

		// set up slip rate config
		inversionConfiguration.setSlipRateWeightingType(this.slipRateWeightingType);
		inversionConfiguration.setSlipRateConstraintWt_normalized(this.slipRateConstraintWt_normalized);
		inversionConfiguration.setSlipRateConstraintWt_unnormalized(this.slipRateConstraintWt_unnormalized);

		/*
		 * Build inversion inputs
		 */
//		List<AveSlipConstraint> aveSlipConstraints = null;	
		NZSHM22_SubductionInversionInputGenerator inversionInputGenerator = new NZSHM22_SubductionInversionInputGenerator(
				rupSet, inversionConfiguration);
		super.setInversionInputGenerator(inversionInputGenerator);
		return this;
	}

	@SuppressWarnings("unchecked")
	protected FaultSystemRupSet loadRupSet(File file) throws IOException, DocumentException {
		FaultSystemRupSet fsRupSet = FaultSystemIO.loadRupSet(file);
		return fsRupSet;

	}

	public static void main(String[] args) throws IOException, DocumentException {

		File inputDir = new File("/home/chrisbc/Downloads");
		File outputRoot = new File("/tmp/NZSHM");
		File ruptureSet = new File(inputDir,
				"RupSet_Sub_FM(SBD_0_1_HKR_KRM_30)_mnSbS(2)_mnSSPP(2)_mxSSL(0.5)_ddAsRa(2.0,5.0,5)_ddMnFl(0.2)_ddPsCo(0.0)_ddSzCo(0.005)_thFc(0.0).zip");
		File outputDir = new File(outputRoot, "inversions");
		Preconditions.checkState(outputDir.exists() || outputDir.mkdir());

		NZSHM22_SubductionInversionRunner runner = new NZSHM22_SubductionInversionRunner();
		runner.setInversionMinutes(1).setRuptureSetFile(ruptureSet).configure();

		NZSHM22_InversionFaultSystemSolution solution = runner.runInversion();
//		solution.setGridSourceProvider(null);
		File solutionFile = new File(outputDir, "SubductionInversionSolution.zip");

		FaultSystemIO.writeSol(solution, solutionFile);

	}

}
