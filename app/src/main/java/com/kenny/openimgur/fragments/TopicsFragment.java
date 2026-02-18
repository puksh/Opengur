package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ImgurService;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.api.responses.TopicGalleryResponse;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.util.DBContracts.TopicsContract;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 2/19/15.
 */
public class TopicsFragment extends BaseGridFragment {
    private static final String KEY_TOPIC_ID = "topics_id";

    private static final String KEY_TOPIC_NAME = "topics_name";

    private static final String KEY_SORT = "topics_sort";

    private static final String KEY_TOP_SORT = "topics_topSort";

    private static final String KEY_TOPIC = "topics_topic";

    ImgurTopic mTopic;

    private ImgurFilters.GallerySort mSort = ImgurFilters.GallerySort.TIME;

    private ImgurFilters.TimeSort mTimeSort = ImgurFilters.TimeSort.DAY;

    private final Callback<TopicGalleryResponse> mTopicCallback = new Callback<TopicGalleryResponse>() {
        @Override
        public void onResponse(Call<TopicGalleryResponse> call, Response<TopicGalleryResponse> response) {
            if (!isAdded()) return;

            if (response != null) {
                TopicGalleryResponse topicResponse = response.body();

                if (topicResponse != null) {
                    GalleryResponse galleryResponse = topicResponse.toGalleryResponse();
                    boolean allowNSFW = app.getPreferences().getBoolean(SettingsActivity.NSFW_KEY, false);
                    galleryResponse.purgeNSFW(allowNSFW);
                    onApiResult(galleryResponse);
                } else {
                    onApiFailure(ApiClient.getErrorCode(response.code()));
                }
            } else {
                onApiFailure(R.string.error_network);
            }
        }

        @Override
        public void onFailure(Call<TopicGalleryResponse> call, Throwable t) {
            LogUtil.e(TAG, "Error fetching gallery items", t);
            if (!isAdded()) return;
            onApiFailure(ApiClient.getErrorCode(t));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.topics, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (mTopic != null) {
                    refresh();
                }
                return true;

            case R.id.filter:
                Activity activity = getActivity();
                View anchor;
                anchor = activity.findViewById(R.id.filter);
                if (anchor == null) anchor = activity.findViewById(R.id.refresh);

                PopupMenu m = new PopupMenu(getActivity(), anchor);
                m.inflate(R.menu.filter_gallery_search);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.newest:
                                onFilterChange(ImgurFilters.GallerySort.TIME, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.popularity:
                                onFilterChange(ImgurFilters.GallerySort.VIRAL, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.scoringDay:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.scoringWeek:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.WEEK);
                                return true;

                            case R.id.scoringMonth:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.MONTH);
                                return true;

                            case R.id.scoringYear:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.YEAR);
                                return true;

                            case R.id.scoringAll:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.ALL);
                                return true;
                        }

                        return false;
                    }
                });
                m.show();
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    void onFilterChange(ImgurFilters.GallerySort sort, ImgurFilters.TimeSort timeSort) {
        if (sort == mSort && mTimeSort == timeSort) {
            LogUtil.v(TAG, "Filters have not been updated");
            return;
        }

        mSort = sort;
        mTimeSort = timeSort;
        mCurrentPage = 0;
        mIsLoading = true;
        mHasMore = true;
        if (getAdapter() != null) getAdapter().clear();
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

        if (mListener != null && mTopic != null) {
            mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_STARTED);
        }

        fetchGallery();
        saveFilterSettings();
    }

    private void saveFilterSettings() {
        app.getPreferences().edit()
                .putInt(KEY_TOPIC_ID, mTopic != null ? mTopic.getId() : -1)
                .putString(KEY_TOPIC_NAME, mTopic != null ? mTopic.getName() : null)
                .putString(KEY_TOP_SORT, mTimeSort.getSort())
                .putString(KEY_SORT, mSort.getSort()).apply();
    }

    @Override
    protected void fetchGallery() {
        if (mTopic == null) {
            // We cannot fetch gallery items without a selected topic.
            // Fetch topics first so retry always performs useful work.
            mIsLoading = false;
            fetchTopics();
            return;
        }
        super.fetchGallery();

        ImgurService apiService = ApiClient.getService();
        String topicSlug = getTopicSlug(mTopic);

        if (mSort == ImgurFilters.GallerySort.HIGHEST_SCORING) {
            apiService.getTopicForTopSorted(topicSlug, mTimeSort.getSort(), mCurrentPage).enqueue(mTopicCallback);
        } else {
            apiService.getTopic(topicSlug, mSort.getSort(), mCurrentPage).enqueue(mTopicCallback);
        }
    }

    @Nullable
    @Override
    public void onRetryClick() {
        if (mTopic == null) {
            fetchTopics();
        } else {
            super.onRetryClick();
        }
    }

    @Override
    protected void onEmptyResults() {
        mIsLoading = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
                        /* No results came back from the api, topic must have been removed.
                         This needs to be confirmed that this can happen */
            String message = getString(R.string.topics_empty_result, mTopic.getName());
            ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, message);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);
        SqlHelper sql = SqlHelper.getInstance(getActivity());
        sql.deleteFromTable(TopicsContract.TABLE_NAME);
        sql.insertDefaultTopics();

        if (savedInstanceState == null) {
            SharedPreferences pref = app.getPreferences();
            mSort = ImgurFilters.GallerySort.getSortFromString(pref.getString(KEY_SORT, null));
            mTimeSort = ImgurFilters.TimeSort.getSortFromString(pref.getString(KEY_TOP_SORT, null));
            String topicName = pref.getString(KEY_TOPIC_NAME, null);

            if (topicName != null) {
                List<ImgurTopic> topics = sql.getTopics();

                for (int i = 0; i < topics.size(); i++) {
                    ImgurTopic topic = topics.get(i);

                    if (topicName.equalsIgnoreCase(topic.getName())) {
                        mTopic = topic;
                        break;
                    }
                }
            }

            if (mTopic == null) {
                mTopic = sql.getTopic(pref.getInt(KEY_TOPIC_ID, -1));
            }
        } else {
            mSort = ImgurFilters.GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT, ImgurFilters.GallerySort.TIME.getSort()));
            mTimeSort = ImgurFilters.TimeSort.getSortFromString(savedInstanceState.getString(KEY_TOP_SORT, null));
            mTopic = savedInstanceState.getParcelable(KEY_TOPIC);
        }

        List<ImgurTopic> topics = sql.getTopics();

        if (!topics.isEmpty()) {
            if (mTopic == null) mTopic = topics.get(0);
            if (mListener != null) mListener.onUpdateActionBarSpinner(topics, mTopic);
            fetchGallery();
        } else {
            fetchTopics();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_TOPIC, mTopic);
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putString(KEY_TOP_SORT, mTimeSort.getSort());
    }

    public void onTopicChanged(@NonNull ImgurTopic topic) {
        if (mTopic == null || mTopic.getId() != topic.getId()) {
            mTopic = topic;
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            fetchGallery();
            saveFilterSettings();
        }
    }

    private void fetchTopics() {
        GalleryAdapter adapter = getAdapter();
        if (adapter == null || adapter.isEmpty()) {
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
        }

        SqlHelper sql = SqlHelper.getInstance(getActivity());
        sql.deleteFromTable(TopicsContract.TABLE_NAME);
        sql.insertDefaultTopics();
        List<ImgurTopic> topics = sql.getTopics();

        if (!topics.isEmpty()) {
            boolean found = false;

            if (mTopic != null) {
                for (int i = 0; i < topics.size(); i++) {
                    if (topics.get(i).getId() == mTopic.getId()) {
                        found = true;
                        break;
                    }
                }
            }

            if (mTopic == null || !found) {
                mTopic = topics.get(0);
            }

            if (mListener != null) mListener.onUpdateActionBarSpinner(topics, mTopic);

            if (getAdapter() == null || getAdapter().isEmpty()) {
                fetchGallery();
            } else {
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            }
        } else {
            ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, R.string.error_network);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }
    }

    @NonNull
    private String getTopicSlug(@NonNull ImgurTopic topic) {
        return topic.getName().trim().toLowerCase(Locale.US).replace(" ", "-");
    }
}
