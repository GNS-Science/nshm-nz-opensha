package nz.cri.gns.NZSHM22.util;

import org.dom4j.DocumentException;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.util.RupSetDiagnosticsPageGen;

import com.google.common.base.Preconditions;

import nz.cri.gns.NZSHM22.opensha.inversion.NZSHM22_InversionFaultSystemSolution;
import nz.cri.gns.NZSHM22.opensha.inversion.NZSHM22_SubductionInversionRunner;
import scratch.UCERF3.FaultSystemRupSet;
import scratch.UCERF3.FaultSystemSolution;
import scratch.UCERF3.utils.FaultSystemIO;

import java.io.File;
import java.io.IOException;

public class NZSHM22_InversionDiagnosticsReportBuilder {
    String name;
    String ruptureSetName;
    String outputDir;
    String faultFilter;

    public NZSHM22_InversionDiagnosticsReportBuilder() {
    }

    public NZSHM22_InversionDiagnosticsReportBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public NZSHM22_InversionDiagnosticsReportBuilder setRuptureSetName(String ruptureSetName) {
        this.ruptureSetName = ruptureSetName;
        return this;
    }

    public NZSHM22_InversionDiagnosticsReportBuilder setOutputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }
    
    public NZSHM22_InversionDiagnosticsReportBuilder setFaultFilter(String faultFilter) {
    	this.faultFilter = faultFilter;
    	return this;
    }

    public void generateRateDiagnosticsPlot() throws IOException, DocumentException {
        String[] args = new String[]
                {"--name", name,
                  "--rupture-set", ruptureSetName,
                  "--output-dir", outputDir};        
        NZSHM22_InversionRateDiagnosticsPlot.create(args).generatePage();
    }

    public void generateInversionDiagnosticsReport() throws IOException, DocumentException {

		FaultSystemSolution inputSol = FaultSystemIO.loadSol(new File(ruptureSetName));
		FaultSystemRupSet inputRupSet = inputSol.getRupSet();
		
        RupSetDiagnosticsPageGen builder = new RupSetDiagnosticsPageGen(
        		inputRupSet, inputSol, name, new File(outputDir));
		builder.setSkipPlausibility(true);
		builder.setSkipBiasiWesnousky(true);
		builder.setSkipConnectivity(true);
		builder.setSkipSegmentation(true);
		builder.generatePage();        
    }
    
    public void generateRuptureSetDiagnosticsReport() throws IOException, DocumentException {

		FaultSystemRupSet inputRupSet = FaultSystemIO.loadRupSet(new File(ruptureSetName));
		FaultSystemSolution inputSol = null;
			
        RupSetDiagnosticsPageGen builder = new RupSetDiagnosticsPageGen(
        		inputRupSet, inputSol, name, new File(outputDir));
		builder.setSkipPlausibility(true);
		builder.setSkipBiasiWesnousky(true);
		builder.setSkipConnectivity(true);
		builder.setSkipSegmentation(true);
		builder.generatePage();        
    }
    
    
    public void generateFilteredInversionDiagnosticsReport() throws IOException, DocumentException {
        String[] args = new String[]
                {"--name", name,
                  "--rupture-set", ruptureSetName,
                  "--output-dir", outputDir,
                  "--fault-name", faultFilter};
        NZSHM22_FilteredInversionDiagnosticsReport.parseArgs(args);
        RupSetDiagnosticsPageGen builder = NZSHM22_FilteredInversionDiagnosticsReport.getReportBuilder();
		builder.setSkipPlausibility(true);
		builder.setSkipBiasiWesnousky(true);
		builder.setSkipConnectivity(true);
		builder.setSkipSegmentation(true);     
        builder.generatePage();
        //        NZSHM22_FilteredInversionDiagnosticsReport.createReportBuilder(args).generatePage();
    }
    
	public static void main(String[] args) throws IOException, DocumentException {
		
		File inputDir = new File("/tmp/NZSHM/inversions");
		File outputRoot = new File(inputDir, "report");
		Preconditions.checkState(outputRoot.exists() || outputRoot.mkdir());
			
		NZSHM22_InversionDiagnosticsReportBuilder builder = new NZSHM22_InversionDiagnosticsReportBuilder();
		builder.setRuptureSetName("/tmp/NZSHM/inversions/SubductionInversionSolution.zip")
			.setName("Hikurangi")
			.setOutputDir(outputRoot.getAbsolutePath());
//		builder.generateInversionDiagnosticsReport(); 
		builder.generateRateDiagnosticsPlot();
		
	}  
    
}
