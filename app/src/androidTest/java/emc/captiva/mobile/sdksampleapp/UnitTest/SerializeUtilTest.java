package emc.captiva.mobile.sdksampleapp.UnitTest;

import android.test.suitebuilder.annotation.SmallTest;
import junit.framework.TestCase;
import java.io.InputStream;
import emc.captiva.mobile.sdksampleapp.Util.SerializeUtil;
import emc.captiva.mobile.sdksampleapp.Util.StringUtil;


/**
 * Created by david on 8/30/16.
 */
public class SerializeUtilTest extends TestCase{
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @SmallTest
    public void testGetCookieFromResponse(){

        //1)Mock Up Objects
        String jsonFileName = "loginObject.json";

        InputStream in = this.getClass().getClassLoader().getResourceAsStream(jsonFileName);
        String responseString = StringUtil.getStringFromInputStream(in);

        //2)Call Method
        String result = new SerializeUtil().getCookieFromResponse(responseString).ticket;

        //3)Assert Result is correct
        assertNotNull(result);
        assertEquals(result , "SK22>>>>*!9381189715979665605/jbRgxRa7Yw7TVPt+oLguhB/TRET9C5EJoPVN0THKcx75JaQaZ+pnqj6V/iGCRdQ1acNZuQyMx0reNZ5BIqU0lx9s4lmwPdOmf95riSEqkz6MDJfrUYV/6XBbw0XvSpvWG5gd2LYqliXjEmf6H/tuxr4Ngi+EGyOdXLNe0adM3GXSuSKfI75bYLg9zB+A7X/AL6QXeMcF7VbKoRCRYtBTJ5pg5F04qx7+aMGlEbgdrRF46VLxQJvvVNs0ixV8ijPeOmXkQasrne113jXhKVvDOcAlmPpLyiGKF3XISKFV3LYnyg==*>>>>");
    }


}
