package com.stark.music.fragment.addmusic;

import android.app.Fragment;



/**
 * Created by admin on 13-11-23.
 */
public class FragmentFactoryInAddActivity{
    public static Fragment getInstanceByIndex(int index) {
        Fragment fragment = null;
        switch (index) {
            case 1:
                fragment = new PerformerFragmentInAddActivity();
                break;
            case 2:
                fragment = new SongsFragmentInAddActivity();
                break;
            case 3:
                fragment = new AlbumFragmentInAddActivity();
                break;
            case 4:
                fragment = new MoreFragmentInAddActivity();
                break;
        }
        return fragment;
    }
}
