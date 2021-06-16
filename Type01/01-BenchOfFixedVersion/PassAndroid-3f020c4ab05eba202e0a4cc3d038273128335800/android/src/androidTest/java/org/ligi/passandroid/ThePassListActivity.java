package org.ligi.passandroid;

import android.annotation.TargetApi;
import android.test.suitebuilder.annotation.MediumTest;

import com.squareup.spoon.Spoon;

import org.ligi.passandroid.ui.PassListActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.not;
import static org.ligi.passandroid.steps.HelpSteps.checkThatHelpIsThere;

@TargetApi(14)
public class ThePassListActivity extends BaseIntegration<PassListActivity> {


    public ThePassListActivity() {
        super(PassListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        App.setComponent(DaggerTestComponent.builder().build());
        getActivity();
    }

    @MediumTest
    public void testListIsThere() {

        onView(withId(R.id.pass_recyclerview)).check(matches(isDisplayed()));
        Spoon.screenshot(getActivity(), "list");
    }

    @MediumTest
    public void testHelpMenuBringsUsToHelp() {
        onView(withId(R.id.menu_help)).perform(click());

        checkThatHelpIsThere();
    }

    @MediumTest
    public void testNavigationDrawerIsUsuallyNotShown() {
        onView(withId(R.id.navigationView)).check(matches(not(isDisplayed())));
    }


    @MediumTest
    public void testThatNavigationDrawerOpens() {

        onView(withId(R.id.drawer_layout)).perform(open());
        onView(withId(R.id.navigationView)).check(matches(isDisplayed()));

        Spoon.screenshot(getActivity(), "open_drawer");
    }


}
