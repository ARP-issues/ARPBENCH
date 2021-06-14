package fr.free.nrw.commons.category;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.upload.MwVolleyApi;

/**
 * Displays the category suggestion and selection screen. Category search is initiated here.
 */
public class CategorizationFragment extends Fragment {
    public static interface OnCategoriesSaveHandler {
        public void onCategoriesSave(ArrayList<String> categories);
    }

    ListView categoriesList;
    protected EditText categoriesFilter;
    ProgressBar categoriesSearchInProgress;
    TextView categoriesNotFoundView;
    TextView categoriesSkip;

    CategoriesAdapter categoriesAdapter;
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

    private OnCategoriesSaveHandler onCategoriesSaveHandler;

    protected HashMap<String, ArrayList<String>> categoriesCache;

    // LHS guarantees ordered insertions, allowing for prioritized method A results
    private final Set<String> results = new LinkedHashSet<String>();
    PrefixUpdater prefixUpdaterSub;
    MethodAUpdater methodAUpdaterSub;

    private ContentProviderClient client;

    protected final static int SEARCH_CATS_LIMIT = 25;
    private static final String TAG = CategorizationFragment.class.getName();

    public static class CategoryItem implements Parcelable {
        public String name;
        public boolean selected;

        public static Creator<CategoryItem> CREATOR = new Creator<CategoryItem>() {
            public CategoryItem createFromParcel(Parcel parcel) {
                return new CategoryItem(parcel);
            }

            public CategoryItem[] newArray(int i) {
                return new CategoryItem[0];
            }
        };

        public CategoryItem(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public CategoryItem(Parcel in) {
            name = in.readString();
            selected = in.readInt() == 1;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(name);
            parcel.writeInt(selected ? 1 : 0);
        }
    }

    /**
     * Retrieves recently-used categories and nearby categories, and merges them without duplicates.
     * @return a list containing these categories
     */
    protected ArrayList<String> recentCatQuery() {
        ArrayList<String> items = new ArrayList<String>();
        Set<String> mergedItems = new LinkedHashSet<String>();

        try {
            Cursor cursor = client.query(
                    CategoryContentProvider.BASE_URI,
                    Category.Table.ALL_FIELDS,
                    null,
                    new String[]{},
                    Category.Table.COLUMN_LAST_USED + " DESC");
            // fixme add a limit on the original query instead of falling out of the loop?
            while (cursor.moveToNext() && cursor.getPosition() < SEARCH_CATS_LIMIT) {
                Category cat = Category.fromCursor(cursor);
                items.add(cat.getName());
            }
            cursor.close();

            if (MwVolleyApi.GpsCatExists.getGpsCatExists() == true) {
                //Log.d(TAG, "GPS cats found in CategorizationFragment.java" + MwVolleyApi.getGpsCat().toString());
                List<String> gpsItems = new ArrayList<String>(MwVolleyApi.getGpsCat());
                //Log.d(TAG, "GPS items: " + gpsItems.toString());

                mergedItems.addAll(gpsItems);
            }

            mergedItems.addAll(items);
        }
        catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        //Needs to be an ArrayList and not a List unless we want to modify a big portion of preexisting code
        ArrayList<String> mergedItemsList = new ArrayList<String>(mergedItems);
        return mergedItemsList;
    }

    /**
     * Displays categories found to the user as they type in the search box
     * @param categories a list of all categories found for the search string
     * @param filter the search string
     */
    protected void setCatsAfterAsync(ArrayList<String> categories, String filter) {

        if (getActivity() != null) {
            ArrayList<CategoryItem> items = new ArrayList<CategoryItem>();
            HashSet<String> existingKeys = new HashSet<String>();
            for (CategoryItem item : categoriesAdapter.getItems()) {
                if (item.selected) {
                    items.add(item);
                    existingKeys.add(item.name);
                }
            }
            for (String category : categories) {
                if (!existingKeys.contains(category)) {
                    items.add(new CategoryItem(category, false));
                }
            }

            categoriesAdapter.setItems(items);
            categoriesAdapter.notifyDataSetInvalidated();
            categoriesSearchInProgress.setVisibility(View.GONE);

            if (categories.isEmpty()) {
                if (TextUtils.isEmpty(filter)) {
                    // If we found no recent cats, show the skip message!
                    categoriesSkip.setVisibility(View.VISIBLE);
                } else {
                    categoriesNotFoundView.setText(getString(R.string.categories_not_found, filter));
                    categoriesNotFoundView.setVisibility(View.VISIBLE);
                }
            } else {
                categoriesList.smoothScrollToPosition(existingKeys.size());
            }
        }
        else {
            Log.e(TAG, "Error: Fragment is null");
        }
    }

    private class CategoriesAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<CategoryItem> items;

        private CategoriesAdapter(Context context, ArrayList<CategoryItem> items) {
            this.context = context;
            this.items = items;
        }

        public int getCount() {
            return items.size();
        }

        public Object getItem(int i) {
            return items.get(i);
        }

        public ArrayList<CategoryItem> getItems() {
            return items;
        }

        public void setItems(ArrayList<CategoryItem> items) {
            this.items = items;
        }

        public long getItemId(int i) {
            return i;
        }

        public View getView(int i, View view, ViewGroup viewGroup) {
            CheckedTextView checkedView;

            if(view == null) {
                checkedView = (CheckedTextView) getActivity().getLayoutInflater().inflate(R.layout.layout_categories_item, null);

            } else {
                checkedView = (CheckedTextView) view;
            }

            CategoryItem item = (CategoryItem) this.getItem(i);
            checkedView.setChecked(item.selected);
            checkedView.setText(item.name);
            checkedView.setTag(i);

            return checkedView;
        }
    }

    public int getCurrentSelectedCount() {
        int count = 0;
        for(CategoryItem item: categoriesAdapter.getItems()) {
            if(item.selected) {
                count++;
            }
        }
        return count;
    }

    private Category lookupCategory(String name) {
        Cursor cursor = null;
        try {
            cursor = client.query(
                    CategoryContentProvider.BASE_URI,
                    Category.Table.ALL_FIELDS,
                    Category.Table.COLUMN_NAME + "=?",
                    new String[] {name},
                    null);
            if (cursor.moveToFirst()) {
                Category cat = Category.fromCursor(cursor);
                return cat;
            }
        } catch (RemoteException e) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw new RuntimeException(e);
        } finally {
            if ( cursor != null ) {
                cursor.close();
            }
        }

        // Newly used category...
        Category cat = new Category();
        cat.setName(name);
        cat.setLastUsed(new Date());
        cat.setTimesUsed(0);
        return cat;
    }

    private class CategoryCountUpdater extends AsyncTask<Void, Void, Void> {

        private String name;

        public CategoryCountUpdater(String name) {
            this.name = name;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Category cat = lookupCategory(name);
            cat.incTimesUsed();

            cat.setContentProviderClient(client);
            cat.save();

            return null; // Make the compiler happy.
        }
    }

    private void updateCategoryCount(String name) {
        Utils.executeAsyncTask(new CategoryCountUpdater(name), executor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_categorization, null);
        categoriesList = (ListView) rootView.findViewById(R.id.categoriesListBox);
        categoriesFilter = (EditText) rootView.findViewById(R.id.categoriesSearchBox);
        categoriesSearchInProgress = (ProgressBar) rootView.findViewById(R.id.categoriesSearchInProgress);
        categoriesNotFoundView = (TextView) rootView.findViewById(R.id.categoriesNotFound);
        categoriesSkip = (TextView) rootView.findViewById(R.id.categoriesExplanation);

        categoriesSkip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getActivity().onBackPressed();
                getActivity().finish();
            }
        });

        ArrayList<CategoryItem> items;
        if(savedInstanceState == null) {
            items = new ArrayList<CategoryItem>();
            categoriesCache = new HashMap<String, ArrayList<String>>();
        } else {
            items = savedInstanceState.getParcelableArrayList("currentCategories");
            categoriesCache = (HashMap<String, ArrayList<String>>) savedInstanceState.getSerializable("categoriesCache");
        }

        categoriesAdapter = new CategoriesAdapter(getActivity(), items);
        categoriesList.setAdapter(categoriesAdapter);

        categoriesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int index, long id) {
                CheckedTextView checkedView = (CheckedTextView) view;
                CategoryItem item = (CategoryItem) adapterView.getAdapter().getItem(index);
                item.selected = !item.selected;
                checkedView.setChecked(item.selected);
                if (item.selected) {
                    updateCategoryCount(item.name);
                }
            }
        });

        categoriesFilter.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                startUpdatingCategoryList();
            }

            public void afterTextChanged(Editable editable) {

            }
        });

        startUpdatingCategoryList();

        return rootView;
    }

    /**
     * Makes asynchronous calls to the Commons MediaWiki API via anonymous subclasses of
     * 'MethodAUpdater' and 'PrefixUpdater'. Some of their methods are overridden in order to
     * aggregate the results. A CountDownLatch is used to ensure that MethodA results are shown
     * above Prefix results.
     */
    private void requestSearchResults() {

        final CountDownLatch latch = new CountDownLatch(1);

        prefixUpdaterSub = new PrefixUpdater(this) {
            @Override
            protected ArrayList<String> doInBackground(Void... voids) {
                ArrayList<String> result = new ArrayList<String>();
                try {
                    result = super.doInBackground();
                    latch.await();
                }
                catch (InterruptedException e) {
                    Log.w(TAG, e);
                    //Thread.currentThread().interrupt();
                }
                return result;
            }

            @Override
            protected void onPostExecute(ArrayList<String> result) {
                super.onPostExecute(result);

                results.addAll(result);
                Log.d(TAG, "Prefix result: " + result);

                String filter = categoriesFilter.getText().toString();
                ArrayList<String> resultsList = new ArrayList<String>(results);
                categoriesCache.put(filter, resultsList);
                Log.d(TAG, "Final results List: " + resultsList);

                categoriesAdapter.notifyDataSetChanged();
                setCatsAfterAsync(resultsList, filter);
            }
        };

        methodAUpdaterSub = new MethodAUpdater(this) {
            @Override
            protected void onPostExecute(ArrayList<String> result) {
                results.clear();
                super.onPostExecute(result);

                results.addAll(result);
                Log.d(TAG, "Method A result: " + result);
                categoriesAdapter.notifyDataSetChanged();

                latch.countDown();
            }
        };
        Utils.executeAsyncTask(prefixUpdaterSub);
        Utils.executeAsyncTask(methodAUpdaterSub);
    }

    private void startUpdatingCategoryList() {

        if (prefixUpdaterSub != null) {
            prefixUpdaterSub.cancel(true);
        }

        if (methodAUpdaterSub != null) {
            methodAUpdaterSub.cancel(true);
        }
        
        requestSearchResults();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, android.view.MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.fragment_categorization, menu);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.categories_activity_title);
        client = getActivity().getContentResolver().acquireContentProviderClient(CategoryContentProvider.AUTHORITY);
    }

    @Override
    public void onResume() {
        super.onResume();

        View rootView = getView();
        if (rootView != null) {
            rootView.setFocusableInTouchMode(true);
            rootView.requestFocus();
            rootView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                        backButtonDialog();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void backButtonDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage("Are you sure you want to go back? The image will not have any categories saved.")
                .setTitle("Warning");
        builder.setPositiveButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //No need to do anything, user remains on categorization screen
            }
        });
        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                getActivity().finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("currentCategories", categoriesAdapter.getItems());
        outState.putSerializable("categoriesCache", categoriesCache);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.menu_save_categories:
                ArrayList<String> selectedCategories = new ArrayList<String>();
                int numberSelected = 0;

                for(CategoryItem item: categoriesAdapter.getItems()) {
                    if(item.selected) {
                        selectedCategories.add(item.name);
                        numberSelected++;
                    }
                }

                //Need to reassign to a final variable to use in inner class
                final ArrayList<String> finalCategories = selectedCategories;

                //If no categories selected, display warning to user
                if (numberSelected == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setMessage("Images without categories are rarely usable. Are you sure you want to submit without selecting categories?")
                            .setTitle("No Categories Selected");
                    builder.setPositiveButton("No, go back", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Exit menuItem so user can select their categories
                            return;
                        }
                    });
                    builder.setNegativeButton("Yes, submit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Proceed to submission
                            onCategoriesSaveHandler.onCategoriesSave(finalCategories);
                            return;
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    //Proceed to submission
                    onCategoriesSaveHandler.onCategoriesSave(finalCategories);
                    return true;
                }
        }
        return super.onOptionsItemSelected(menuItem);
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        onCategoriesSaveHandler = (OnCategoriesSaveHandler) activity;
    }
}
