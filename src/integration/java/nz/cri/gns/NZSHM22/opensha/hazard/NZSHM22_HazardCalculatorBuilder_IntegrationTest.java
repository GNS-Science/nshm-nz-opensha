package nz.cri.gns.NZSHM22.opensha.hazard;

import static org.junit.Assert.*;

import org.dom4j.DocumentException;
import org.junit.Test;
import org.opensha.commons.data.function.DiscretizedFunc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class NZSHM22_HazardCalculatorBuilder_IntegrationTest {

    protected File getSolution() throws URISyntaxException {
        URL vernonSolution = Thread.currentThread().getContextClassLoader().getResource("AlpineVernonInversionSolution.zip");
        return new File(vernonSolution.toURI());
    }

    @Test
    public void hazardCalcTest() throws URISyntaxException, DocumentException, IOException {
        NZSHM22_HazardCalculatorBuilder builder = new NZSHM22_HazardCalculatorBuilder();
        builder.setSolutionFile(getSolution());
        NZSHM22_HazardCalculator calculator = builder.build();
        DiscretizedFunc actual = calculator.calc(-41.289, 174.777);

        List<Double> expected = Arrays.asList(0.9999999999999769, 0.9999999999999735, 0.9999999999999678, 0.9999999999999557, 0.9999999999999282, 0.9999999999998456, 0.999999999999608, 0.9999999999987376, 0.9999999999946059, 0.9999999999724754, 0.9999999998218194, 0.999999998756106, 0.9999999912132966, 0.999999932588022, 0.999999539839664, 0.9999970174574307, 0.9999825713302176, 0.9999107233829534, 0.9996017508085968, 0.9984799915809908, 0.9949990727909503, 0.9858929635877043, 0.9656310966036452, 0.9270989593382446, 0.8637280293783651, 0.7733284357618077, 0.6598669064254563, 0.5337584982597231, 0.40826119356477497, 0.2953479548666311, 0.20235762281182934, 0.1316970197405316, 0.08163160607627096, 0.04831622422584625, 0.02735805152211157, 0.01483182242291281, 0.007698918611387162, 0.0038225996771694692, 0.0018118969195560775, 8.175785182840123E-4, 3.49948143422929E-4, 1.4150631936282743E-4, 5.3824846851568964E-5, 1.917335866730152E-5, 6.369844108400358E-6, 1.9664201289515404E-6, 5.623446812652588E-7, 1.4863532515896338E-7, 3.6263732017260963E-8, 8.165036535778825E-9, 1.6982115713659596E-9);
        assertEquals(expected, actual.yValues());
    }
}
