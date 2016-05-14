import junit.framework.TestCase;
import org.junit.Test;

import java.util.HashMap;


/**
 * Created by junyuanlau on 14/5/16.
 */
public class StaticDataTest extends TestCase{
    StaticData staticData;
    protected void setUp(){

    }

    @Test
    public void testCompanyStaticData(){
        staticData = new StaticData();
        staticData.generateCompanyStaticData("testStockQty");
        long datetime = 1451656800000L;
        String country = "France";
        String key = staticData.issueMapKey(datetime, country);
        assertEquals(key, "1451656800000|France");
        Object o = staticData.getIssueQuantity(datetime, country);
        assertTrue(o instanceof HashMap);
        HashMap<String, Integer> map =(HashMap<String, Integer>) o;
        assertTrue(map.containsKey("ACCOR"));
        assertEquals((int) map.get("ACCOR"), 100);

        datetime  = 1451664000000L;
        key = staticData.issueMapKey(datetime, country);
        assertNull(staticData.getIssueQuantity(datetime, country).get("ACCOR"));

        staticData.generateMarketStaticData("testStockPrice");
        HashMap<String, Double> prices = staticData.getPriceData(datetime,country);
        assertEquals(prices.get("ACCOR"), 1.22);
        assertEquals(prices.get("AIRBUS GROUP"), 30.44);

    }



}
