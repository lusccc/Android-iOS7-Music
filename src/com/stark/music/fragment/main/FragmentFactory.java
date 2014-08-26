package com.stark.music.fragment.main;

import android.app.Fragment;



/**
 * Created by admin on 13-11-23.
 */
public class FragmentFactory {
    public static Fragment getInstanceByIndex(int index) {
        Fragment fragment = null;
        switch (index) {
            case 1:
                fragment = new PlayListFragment();
                break;
            case 2:
                fragment = new PerformerFragment();
                break;
            case 3:
                fragment = new SongsFragment();
                break;
            case 4:
                fragment = new AlbumFragment();
                break;
            case 5:
                fragment = new MoreFragment();
                break;
        }
        return fragment;
    }
}
