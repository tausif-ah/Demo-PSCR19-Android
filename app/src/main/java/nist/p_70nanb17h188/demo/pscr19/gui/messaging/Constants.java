package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import nist.p_70nanb17h188.demo.pscr19.R;

class Constants {
    static int getPlayBackgroundResource(boolean playing, boolean played) {
        if (playing) return R.color.colorAudioPlayingBackground;
        else if (played) return R.color.colorAudioPlayedBackground;
        else return R.color.colorAudioNotPlayedBackground;
    }

    static int getPlayForegroundResource(boolean playing, boolean played) {
        if (playing) return R.color.colorAudioPlayingForeground;
        else if (played) return R.color.colorAudioPlayedForeground;
        else return R.color.colorAudioNotPlayedForeground;
    }
}
