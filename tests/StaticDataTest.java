import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;


/**
 * Created by junyuanlau on 14/5/16.
 */
public class StaticDataTest extends TestCase{
    DataCube dataCube;
    protected void setUp(){

    }

    @Test
    public void testCompanyStaticData(){
        dataCube = new DataCube();
        dataCube.generateCompanyStaticData("testStockQty");
        long datetime = 1451656800000L;
        String country = "France";
        String key = dataCube.issueMapKey(datetime, country);
        assertEquals(key, "1451656800000|France");
        Object o = dataCube.getIssueQuantity(datetime, country);
        assertTrue(o instanceof HashMap);
        HashMap<String, Integer> map =(HashMap<String, Integer>) o;
        assertTrue(map.containsKey("ACCOR"));
        assertEquals((int) map.get("ACCOR"), 100);

        datetime  = 1451664000000L;
        key = dataCube.issueMapKey(datetime, country);
        assertNull(dataCube.getIssueQuantity(datetime, country).get("ACCOR"));

        dataCube.generateMarketStaticData("testStockPrice");
        HashMap<String, Double> prices = dataCube.getPriceData(datetime,country);
        assertEquals(prices.get("ACCOR"), 1.22);
        prices = dataCube.getPriceData(datetime,"HK");

        assertEquals(prices.get("AIRBUS GROUP"), 30.44);

    }



}
