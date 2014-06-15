package com.homage.app.story;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.androidquery.AQuery;
import com.homage.app.R;
import com.homage.app.main.MainActivity;
import com.homage.model.User;

public class StoryDetailsFragment extends Fragment {
    String TAG = "TAG_"+getClass().getName();

    private static final String ARG_SECTION_NUMBER = "section_number";

    View rootView;
    LayoutInflater inflater;
    AQuery aq;

    public static StoryDetailsFragment newInstance(int sectionNumber) {
        StoryDetailsFragment fragment;
        fragment = new StoryDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.inflater = inflater;
        rootView = inflater.inflate(R.layout.fragment_story_details, container, false);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }



}
