package com.stark.music.fragment.main;

import com.stark.music.R;
import com.stark.view.ElasticListView;
import com.stark.view.SideBar;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MoreFragment extends Fragment {
	private View view;
	private Context context;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		context = getActivity();
		view = inflater.inflate(R.layout.fragment_more_layout, null);
		return view;
	}
}
