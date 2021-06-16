package org.ligi.passandroid.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.ligi.axt.AXT;
import org.ligi.passandroid.R;
import org.ligi.passandroid.maps.PassbookMapsFacade;
import org.ligi.passandroid.model.PassBitmapDefinitions;
import org.ligi.passandroid.model.pass.Pass;
import org.ligi.passandroid.model.pass.PassField;
import org.ligi.passandroid.ui.pass_view_holder.PassViewHolder;
import org.ligi.passandroid.ui.pass_view_holder.VerbosePassViewHolder;

public class PassViewActivity extends PassViewActivityBase {

    @OnClick(R.id.barcode_img)
    void onBarcodeClick() {
        AXT.at(this).startCommonIntent().activityFromClass(FullscreenBarcodeActivity.class);
    }

    @OnClick(R.id.moreTextView)
    void onMoreClick() {
        if (back_tv.getVisibility() == View.VISIBLE) {
            back_tv.setVisibility(View.GONE);
            moreTextView.setText(R.string.more);
        } else {
            back_tv.setVisibility(View.VISIBLE);
            moreTextView.setText(R.string.less);
        }
    }

    @BindView(R.id.moreTextView)
    TextView moreTextView;

    @BindView(R.id.logo_img)
    ImageView logo_img;

    @BindView(R.id.footer_img)
    ImageView footer_img;

    @BindView(R.id.thumbnail_img)
    ImageView thumbnail_img;

    @BindView(R.id.strip_img)
    ImageView strip_img;

    @BindView(R.id.back_fields)
    TextView back_tv;

    @BindView(R.id.front_field_container)
    ViewGroup frontFieldsContainer;

    @BindView(R.id.toolbar)
    Toolbar toolbar;


    @Override
    protected void refresh() {
        super.refresh();

        final Pass pass = currentPass;

        if (pass == null) { // don't deal with invalid passes
            return;
        }

        final PassViewHelper passViewHelper = new PassViewHelper(this);

        new BarcodeUIController(getWindow().getDecorView(), pass.getBarCode(), this, passViewHelper);

        passViewHelper.setBitmapSafe(logo_img, pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_LOGO));
        passViewHelper.setBitmapSafe(footer_img, pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_FOOTER));

        passViewHelper.setBitmapSafe(thumbnail_img, pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_THUMBNAIL));
        passViewHelper.setBitmapSafe(strip_img, pass.getBitmap(passStore, PassBitmapDefinitions.BITMAP_STRIP));

        if (findViewById(R.id.map_container) != null) {
            if (!(pass.getLocations().size() > 0 && PassbookMapsFacade.init(this))) {
                findViewById(R.id.map_container).setVisibility(View.GONE);
            }
        }

        if (pass.getType() != null) {
            final StringBuilder back_str = new StringBuilder();

            frontFieldsContainer.removeAllViews();

            for (PassField field : pass.getFields()) {
                if (field.getHide()) {
                    back_str.append(field.toHtmlSnippet());
                } else {
                    final View v = getLayoutInflater().inflate(R.layout.main_field_item, frontFieldsContainer, false);
                    final TextView key = (TextView) v.findViewById(R.id.key);
                    key.setText(field.getLabel());
                    final TextView value = (TextView) v.findViewById(R.id.value);
                    value.setText(field.getValue());

                    frontFieldsContainer.addView(v);
                }
            }


            if (back_str.length() > 0) {
                back_tv.setText(Html.fromHtml(back_str.toString()));
                moreTextView.setVisibility(View.VISIBLE);
            } else {
                moreTextView.setVisibility(View.GONE);
            }

        }

        Linkify.addLinks(back_tv, Linkify.ALL);

        final PassViewHolder passViewHolder = new VerbosePassViewHolder(findViewById(R.id.pass_card));
        passViewHolder.apply(pass, passStore, this);

        super.onPostResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AXT.at(this).disableRotation();

        setContentView(R.layout.activity_pass_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentPass == null) {
            return;
        }

        AXT.at(this).disableRotation();

        final View contentView = getLayoutInflater().inflate(R.layout.activity_pass_view, null);
        setContentView(contentView);

        final ViewGroup extraViewContainer = (ViewGroup) contentView.findViewById(R.id.passExtrasContainer);
        final View passExtrasView = getLayoutInflater().inflate(R.layout.pass_view_extra_data, extraViewContainer, false);
        extraViewContainer.addView(passExtrasView);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        configureActionBar();
        refresh();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final boolean res = super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.menu_map).setVisible((currentPass != null && !currentPass.getLocations().isEmpty()));
        menu.findItem(R.id.menu_update).setVisible(mightPassBeAbleToUpdate(currentPass));
        return res;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_item, menu);
        getMenuInflater().inflate(R.menu.update, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            final Intent upIntent = NavUtils.getParentActivityIntent(this);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
                finish();
            } else {
                NavUtils.navigateUpTo(this, upIntent);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttachedToWindow() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

}
