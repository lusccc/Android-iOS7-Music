package com.stark.music.fragment.main;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stark.music.R;


public class MoreFragment extends Fragment implements View.OnClickListener {
	private View view;
	private Context context;
    private TextView tvMore;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		context = getActivity();
		view = inflater.inflate(R.layout.fragment_more_layout, null);
		return view;
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        initView();
        setListener();
        super.onActivityCreated(savedInstanceState);
    }

    private void initView(){
        tvMore = (TextView)view.findViewById(R.id.textView_fragment_title);

    }

    private void setListener(){
        tvMore.setOnClickListener(this);
    }
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.textView_fragment_title:
                Intent intent=new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "分享");
                intent.putExtra(Intent.EXTRA_TEXT, "2222");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(intent, "111"));
                break;
        }
    }
}
