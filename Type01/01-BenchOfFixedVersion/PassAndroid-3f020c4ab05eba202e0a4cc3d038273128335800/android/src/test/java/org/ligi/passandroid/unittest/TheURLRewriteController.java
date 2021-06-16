package org.ligi.passandroid.unittest;

import android.net.Uri;
import org.junit.Test;
import org.ligi.passandroid.Tracker;
import org.ligi.passandroid.ui.quirk_fix.URLRewriteController;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TheURLRewriteController {

    private URLRewriteController tested = new URLRewriteController(mock(Tracker.class));


    @Test
    public void testAppspotRewrite() {
        final String res = tested.getUrlByUri(Uri.parse("http://pass-cloud.appspot.com/open_or_install?url=http://espass.it/assets/download/pass/movie.espass"));

        assertThat(res).isEqualTo("http://espass.it/assets/download/pass/movie.espass");
    }

    @Test
    public void testPass2URewrite() {
        final String res = tested.getUrlByUri(Uri.parse("pass2u://import/https://api.passdock.com/passes/17969/e5dfb0afff61b1294235918a6a9ac75255daa89f.pkpass"));

        assertThat(res).isEqualTo("https://api.passdock.com/passes/17969/e5dfb0afff61b1294235918a6a9ac75255daa89f.pkpass");
    }


    @Test
    public void testRejection() {
        final String res = tested.getUrlByUri(Uri.parse("http://foo.bar"));

        assertThat(res).isNull();
    }

    @Test
    public void testThatBrusselWorks() {
        final String res = tested.getUrlByUri(Uri.parse("http://prod.wap.ncrwebhost.mobi/mobiqa/wap/14foo/83bar/"));

        assertThat(res).isEqualTo("http://prod.wap.ncrwebhost.mobi/mobiqa/wap/14foo/83bar/passbook");
    }


    @Test
    public void testThatSwissWorks() {
        final String res = tested.getUrlByUri(Uri.parse("http://mbp.swiss.com/mobiqa/wap/14foo/83bar/"));

        assertThat(res).isEqualTo("http://prod.wap.ncrwebhost.mobi/mobiqa/wap/14foo/83bar/passbook");
    }

    @Test
    public void testThatCathayWorks() {
        final String res = tested.getUrlByUri(Uri.parse("https://www.cathaypacific.com/foo?v=bar"));

        assertThat(res).isEqualTo("https://www.cathaypacific.com/icheckin2/PassbookServlet?v=bar");
    }


    @Test
    public void testVirgin1() {
        final String res = tested.getUrlByUri(Uri.parse("https://bazz.virginaustralia.com/boarding/CheckInApiIntegration?key=foo"));

        assertThat(res).isEqualTo("https://mobile.virginaustralia.com/boarding/pass.pkpass?key=foo");
    }


    @Test
    public void testVirgin() {
        final String res = tested.getUrlByUri(Uri.parse("https://bazz.virginaustralia.com/boarding/pass.pkpass?c=foo"));

        assertThat(res).isEqualTo("https://mobile.virginaustralia.com/boarding/pass.pkpass?key=foo");
    }

    @Test
    public void testAirCanada() {
        final String res = tested.getUrlByUri(Uri.parse("http://m.aircanada.ca/ebp/XYZ"));

        assertThat(res).isEqualTo("http://m.aircanada.ca/ebp/XYZ?appDetection=false");
    }
}
