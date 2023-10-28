package com.android.internal.app;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import com.android.internal.R;
import com.android.internal.app.AppLocaleStore;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
/* loaded from: classes4.dex */
public class LocalePickerWithRegion extends ListFragment implements SearchView.OnQueryTextListener {
    private static final String PARENT_FRAGMENT_NAME = "localeListEditor";
    private static final String TAG = LocalePickerWithRegion.class.getSimpleName();
    private SuggestedLocaleAdapter mAdapter;
    private String mAppPackageName;
    private LocaleSelectedListener mListener;
    private Set<LocaleStore.LocaleInfo> mLocaleList;
    private MenuItem.OnActionExpandListener mOnActionExpandListener;
    private LocaleStore.LocaleInfo mParentLocale;
    private boolean mTranslatedOnly = false;
    private SearchView mSearchView = null;
    private CharSequence mPreviousSearch = null;
    private boolean mPreviousSearchHadFocus = false;
    private int mFirstVisiblePosition = 0;
    private int mTopDistance = 0;
    private CharSequence mTitle = null;

    /* loaded from: classes4.dex */
    public interface LocaleSelectedListener {
        void onLocaleSelected(LocaleStore.LocaleInfo localeInfo);
    }

    private static LocalePickerWithRegion createCountryPicker(Context context, LocaleSelectedListener listener, LocaleStore.LocaleInfo parent, boolean translatedOnly, String appPackageName, MenuItem.OnActionExpandListener onActionExpandListener) {
        LocalePickerWithRegion localePicker = new LocalePickerWithRegion();
        localePicker.setOnActionExpandListener(onActionExpandListener);
        boolean shouldShowTheList = localePicker.setListener(context, listener, parent, translatedOnly, appPackageName);
        if (shouldShowTheList) {
            return localePicker;
        }
        return null;
    }

    public static LocalePickerWithRegion createLanguagePicker(Context context, LocaleSelectedListener listener, boolean translatedOnly) {
        LocalePickerWithRegion localePicker = new LocalePickerWithRegion();
        localePicker.setListener(context, listener, null, translatedOnly, null);
        return localePicker;
    }

    public static LocalePickerWithRegion createLanguagePicker(Context context, LocaleSelectedListener listener, boolean translatedOnly, String appPackageName, MenuItem.OnActionExpandListener onActionExpandListener) {
        LocalePickerWithRegion localePicker = new LocalePickerWithRegion();
        localePicker.setOnActionExpandListener(onActionExpandListener);
        localePicker.setListener(context, listener, null, translatedOnly, appPackageName);
        return localePicker;
    }

    private boolean setListener(Context context, LocaleSelectedListener listener, LocaleStore.LocaleInfo parent, boolean translatedOnly, String appPackageName) {
        boolean isForCountryMode;
        boolean shouldShowList;
        this.mParentLocale = parent;
        this.mListener = listener;
        this.mTranslatedOnly = translatedOnly;
        this.mAppPackageName = appPackageName;
        setRetainInstance(true);
        HashSet<String> langTagsToIgnore = new HashSet<>();
        LocaleStore.LocaleInfo appCurrentLocale = LocaleStore.getAppCurrentLocaleInfo(context, appPackageName);
        if (parent != null) {
            isForCountryMode = true;
        } else {
            isForCountryMode = false;
        }
        if (!TextUtils.isEmpty(appPackageName) && !isForCountryMode) {
            LocaleList systemLangList = LocaleList.getDefault();
            for (int i = 0; i < systemLangList.size(); i++) {
                langTagsToIgnore.add(systemLangList.get(i).toLanguageTag());
            }
            if (appCurrentLocale != null) {
                Log.d(TAG, "appCurrentLocale: " + appCurrentLocale.getLocale().toLanguageTag());
                langTagsToIgnore.add(appCurrentLocale.getLocale().toLanguageTag());
            } else {
                Log.d(TAG, "appCurrentLocale is null");
            }
        } else if (!translatedOnly) {
            LocaleList userLocales = LocalePicker.getLocales();
            String[] langTags = userLocales.toLanguageTags().split(",");
            Collections.addAll(langTagsToIgnore, langTags);
        }
        if (!isForCountryMode) {
            this.mLocaleList = LocaleStore.getLevelLocales(context, langTagsToIgnore, null, translatedOnly);
        } else {
            Set<LocaleStore.LocaleInfo> levelLocales = LocaleStore.getLevelLocales(context, langTagsToIgnore, parent, translatedOnly);
            this.mLocaleList = levelLocales;
            if (levelLocales.size() <= 1) {
                if (listener != null && this.mLocaleList.size() == 1) {
                    listener.onLocaleSelected(this.mLocaleList.iterator().next());
                }
                return false;
            }
        }
        Log.d(TAG, "mLocaleList size:  " + this.mLocaleList.size());
        if (!TextUtils.isEmpty(appPackageName)) {
            if (appCurrentLocale != null && !isForCountryMode) {
                this.mLocaleList.add(appCurrentLocale);
            }
            AppLocaleStore.AppLocaleResult result = AppLocaleStore.getAppSupportedLocales(context, appPackageName);
            if (result.mLocaleStatus == AppLocaleStore.AppLocaleResult.LocaleStatus.GET_SUPPORTED_LANGUAGE_FROM_LOCAL_CONFIG || result.mLocaleStatus == AppLocaleStore.AppLocaleResult.LocaleStatus.GET_SUPPORTED_LANGUAGE_FROM_ASSET) {
                shouldShowList = true;
            } else {
                shouldShowList = false;
            }
            for (LocaleStore.LocaleInfo localeInfo : LocaleStore.getSystemCurrentLocaleInfo()) {
                boolean isNotCurrentLocale = appCurrentLocale == null || !localeInfo.getLocale().equals(appCurrentLocale.getLocale());
                if (!isForCountryMode && isNotCurrentLocale) {
                    this.mLocaleList.add(localeInfo);
                }
            }
            this.mLocaleList = filterTheLanguagesNotSupportedInApp(shouldShowList, result.mAppSupportedLocales);
            Log.d(TAG, "mLocaleList after app-supported filter:  " + this.mLocaleList.size());
            if (!isForCountryMode && shouldShowList) {
                this.mLocaleList.add(LocaleStore.getSystemDefaultLocaleInfo(appCurrentLocale == null));
            }
        }
        return true;
    }

    private Set<LocaleStore.LocaleInfo> filterTheLanguagesNotSupportedInApp(boolean shouldShowList, HashSet<Locale> supportedLocales) {
        Set<LocaleStore.LocaleInfo> filteredList = new HashSet<>();
        if (!shouldShowList) {
            return filteredList;
        }
        for (LocaleStore.LocaleInfo li : this.mLocaleList) {
            if (supportedLocales.contains(li.getLocale())) {
                filteredList.add(li);
            } else {
                Iterator<Locale> it = supportedLocales.iterator();
                while (true) {
                    if (it.hasNext()) {
                        Locale l = it.next();
                        if (LocaleList.matchesLanguageAndScript(li.getLocale(), l)) {
                            filteredList.add(li);
                            break;
                        }
                    }
                }
            }
        }
        return filteredList;
    }

    private void returnToParentFrame() {
        getFragmentManager().popBackStack(PARENT_FRAGMENT_NAME, 1);
    }

    @Override // android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (this.mLocaleList == null) {
            returnToParentFrame();
            return;
        }
        this.mTitle = getActivity().getTitle();
        LocaleStore.LocaleInfo localeInfo = this.mParentLocale;
        boolean countryMode = localeInfo != null;
        Locale sortingLocale = countryMode ? localeInfo.getLocale() : Locale.getDefault();
        this.mAdapter = new SuggestedLocaleAdapter(this.mLocaleList, countryMode, this.mAppPackageName);
        LocaleHelper.LocaleInfoComparator comp = new LocaleHelper.LocaleInfoComparator(sortingLocale, countryMode);
        this.mAdapter.sort(comp);
        setListAdapter(this.mAdapter);
    }

    @Override // android.app.ListFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setNestedScrollingEnabled(true);
        getListView().setDivider(null);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        switch (id) {
            case 16908332:
                getFragmentManager().popBackStack();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mParentLocale != null) {
            getActivity().setTitle(this.mParentLocale.getFullNameNative());
        } else {
            getActivity().setTitle(this.mTitle);
        }
        getListView().requestFocus();
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        SearchView searchView = this.mSearchView;
        if (searchView != null) {
            this.mPreviousSearchHadFocus = searchView.hasFocus();
            this.mPreviousSearch = this.mSearchView.getQuery();
        } else {
            this.mPreviousSearchHadFocus = false;
            this.mPreviousSearch = null;
        }
        ListView list = getListView();
        View firstChild = list.getChildAt(0);
        this.mFirstVisiblePosition = list.getFirstVisiblePosition();
        this.mTopDistance = firstChild != null ? firstChild.getTop() - list.getPaddingTop() : 0;
    }

    @Override // android.app.ListFragment
    public void onListItemClick(ListView parent, View v, int position, long id) {
        LocaleStore.LocaleInfo locale = (LocaleStore.LocaleInfo) parent.getAdapter().getItem(position);
        boolean isSystemLocale = locale.isSystemLocale();
        boolean isRegionLocale = locale.getParent() != null;
        if (isSystemLocale || isRegionLocale) {
            LocaleSelectedListener localeSelectedListener = this.mListener;
            if (localeSelectedListener != null) {
                localeSelectedListener.onLocaleSelected(locale);
            }
            returnToParentFrame();
            return;
        }
        LocalePickerWithRegion selector = createCountryPicker(getContext(), this.mListener, locale, this.mTranslatedOnly, this.mAppPackageName, this.mOnActionExpandListener);
        if (selector != null) {
            getFragmentManager().beginTransaction().setTransition(4097).replace(getId(), selector).addToBackStack(null).commit();
        } else {
            returnToParentFrame();
        }
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem.OnActionExpandListener onActionExpandListener;
        if (this.mParentLocale == null) {
            inflater.inflate(R.menu.language_selection_list, menu);
            MenuItem searchMenuItem = menu.findItem(R.id.locale_search_menu);
            if (!TextUtils.isEmpty(this.mAppPackageName) && (onActionExpandListener = this.mOnActionExpandListener) != null) {
                searchMenuItem.setOnActionExpandListener(onActionExpandListener);
            }
            SearchView searchView = (SearchView) searchMenuItem.getActionView();
            this.mSearchView = searchView;
            searchView.setQueryHint(getText(R.string.search_language_hint));
            this.mSearchView.setOnQueryTextListener(this);
            if (!TextUtils.isEmpty(this.mPreviousSearch)) {
                searchMenuItem.expandActionView();
                this.mSearchView.setIconified(false);
                this.mSearchView.setActivated(true);
                if (this.mPreviousSearchHadFocus) {
                    this.mSearchView.requestFocus();
                }
                this.mSearchView.setQuery(this.mPreviousSearch, true);
            } else {
                this.mSearchView.setQuery(null, false);
            }
            getListView().setSelectionFromTop(this.mFirstVisiblePosition, this.mTopDistance);
        }
    }

    @Override // android.widget.SearchView.OnQueryTextListener
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override // android.widget.SearchView.OnQueryTextListener
    public boolean onQueryTextChange(String newText) {
        SuggestedLocaleAdapter suggestedLocaleAdapter = this.mAdapter;
        if (suggestedLocaleAdapter != null) {
            suggestedLocaleAdapter.getFilter().filter(newText);
            return false;
        }
        return false;
    }

    public void setOnActionExpandListener(MenuItem.OnActionExpandListener onActionExpandListener) {
        this.mOnActionExpandListener = onActionExpandListener;
    }
}
