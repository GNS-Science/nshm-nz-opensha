package nz.cri.gns.NZSHM22.opensha.hazard;

import nz.cri.gns.NZSHM22.opensha.enumTreeBranches.NZSHM22_SpatialSeisPDF;
import nz.cri.gns.NZSHM22.opensha.griddedSeismicity.NZSHM22_GridSourceGenerator;
import nz.cri.gns.NZSHM22.opensha.inversion.NZSHM22_InversionFaultSystemRuptSet;
import org.dom4j.DocumentException;
import scratch.UCERF3.enumTreeBranches.SpatialSeisPDF;
import scratch.UCERF3.inversion.InversionFaultSystemRupSet;
import scratch.UCERF3.inversion.InversionFaultSystemSolution;
import scratch.UCERF3.inversion.UCERF3InversionConfiguration;
import scratch.UCERF3.utils.FaultSystemIO;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class NZSHM22_InversionFaultSystemSolution extends InversionFaultSystemSolution {

    private NZSHM22_InversionFaultSystemSolution(InversionFaultSystemRupSet rupSet, double[] rates,
                                                 UCERF3InversionConfiguration config, Map<String, Double> energies) {
        super(new NZSHM22_InversionFaultSystemRuptSet(rupSet, rupSet.getLogicTreeBranch()), rates, config, energies);
    }

    public static NZSHM22_InversionFaultSystemSolution fromSolution(InversionFaultSystemSolution solution) {

        NZSHM22_InversionFaultSystemSolution ifss = new NZSHM22_InversionFaultSystemSolution(
                solution.getRupSet(),
                solution.getRateForAllRups(),
                solution.getInversionConfiguration(),
                solution.getEnergies());

        ifss.setGridSourceProvider(new NZSHM22_GridSourceGenerator(ifss));
        return ifss;
    }

    public static NZSHM22_InversionFaultSystemSolution fromFile(File file) throws DocumentException, IOException {
        return fromSolution(FaultSystemIO.loadInvSol(file));
    }
}
