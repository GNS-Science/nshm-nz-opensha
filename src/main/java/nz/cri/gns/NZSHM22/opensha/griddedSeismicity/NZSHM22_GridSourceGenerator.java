package nz.cri.gns.NZSHM22.opensha.griddedSeismicity;

import nz.cri.gns.NZSHM22.opensha.data.region.NewZealandRegions;
import nz.cri.gns.NZSHM22.opensha.hazard.NZSHM22_InversionFaultSystemSolution;
import nz.cri.gns.NZSHM22.opensha.inversion.NZSHM22_InversionFaultSystemRuptSet;
import nz.cri.gns.NZSHM22.opensha.inversion.NZSHM22_InversionTargetMFDs;
import scratch.UCERF3.enumTreeBranches.SpatialSeisPDF;
import scratch.UCERF3.griddedSeismicity.UCERF3_GridSourceGenerator;
import scratch.UCERF3.inversion.InversionFaultSystemSolution;

public class NZSHM22_GridSourceGenerator extends UCERF3_GridSourceGenerator {

    public NZSHM22_GridSourceGenerator(NZSHM22_InversionFaultSystemSolution ifss){
        region = new NewZealandRegions.NZ_TEST_GRIDDED();
        branch = ifss.getLogicTreeBranch();
        srcSpatialPDF = ((NZSHM22_InversionTargetMFDs)ifss.getRupSet().getInversionTargetMFDs()).getPDF();
//		totalMgt5_Rate = branch.getValue(TotalMag5Rate.class).getRateMag5();
        realOffFaultMFD = ifss.getFinalTrulyOffFaultMFD();

        mfdMin = realOffFaultMFD.getMinX();
        mfdMax = realOffFaultMFD.getMaxX();
        mfdNum = realOffFaultMFD.size();

//		polyMgr = FaultPolyMgr.create(fss.getFaultSectionDataList(), 12d);
        polyMgr = ifss.getRupSet().getInversionTargetMFDs().getGridSeisUtils().getPolyMgr();

        System.out.println("   initSectionMFDs() ...");
        initSectionMFDs(ifss);
        System.out.println("   initNodeMFDs() ...");
        initNodeMFDs(ifss);
        System.out.println("   updateSpatialPDF() ...");
        updateSpatialPDF();
    }
}
