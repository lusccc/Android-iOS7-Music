package com.stark.music.fragment.main;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.stark.music.R;
import com.stark.util.UpdateManager;

import cn.waps.AppConnect;


public class MoreFragment extends Fragment implements View.OnClickListener {
	private View view;
	private Context context;
    private RelativeLayout rl0,rl1,rl2,rl3;
    private LinearLayout llAD;
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
        /**
         * 检测新版本
         * 分享APP
         * 意见反馈
         * 赚积分
         */
        rl0 = (RelativeLayout)view.findViewById(R.id.RelativeLayout2);
        rl1 = (RelativeLayout)view.findViewById(R.id.RelativeLayout3);
        rl2 = (RelativeLayout)view.findViewById(R.id.RelativeLayout4);
        rl3 = (RelativeLayout)view.findViewById(R.id.RelativeLayout5);

        llAD = (LinearLayout)view.findViewById(R.id.AdLinearLayout);
        AppConnect.getInstance(getActivity()).showBannerAd(getActivity(), llAD);
        //AppConnect.getInstance(getActivity()).showPopAd(getActivity());

    }

    private void setListener(){
        rl0.setOnClickListener(this);
        rl1.setOnClickListener(this);
        rl2.setOnClickListener(this);
        rl3.setOnClickListener(this);
    }
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.RelativeLayout2:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();

                        new UpdateManager(getActivity());
                        Looper.loop();
                    }
                }).start();
                // 检查软件更新
                //Toast.makeText(getActivity(),"已是最新版本",Toast.LENGTH_SHORT).show();
                break;
            case R.id.RelativeLayout3:
                Intent intent=new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "分享");
                intent.putExtra(Intent.EXTRA_TEXT, "待定");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(intent, getString(R.string.share_app)));
                break;
            case R.id.RelativeLayout4:
                Intent data=new Intent(Intent.ACTION_SENDTO);
                data.setData(Uri.parse("mailto:stark_lu@foxmail.com"));
                data.putExtra(Intent.EXTRA_SUBJECT, "Sun音乐Android 反馈");
                data.putExtra(Intent.EXTRA_TEXT, "========TEST=======");
                startActivity(data);
                break;
            case R.id.RelativeLayout5:
                AppConnect.getInstance(getActivity()).showAppOffers(getActivity());
                break;

        }
    }



}
