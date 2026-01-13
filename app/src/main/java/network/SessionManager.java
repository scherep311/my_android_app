package network;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "drive_school_prefs";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences sp;

    public SessionManager(Context context) {
        sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveUserId(long userId) {
        sp.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public long getUserId() {
        return sp.getLong(KEY_USER_ID, -1);
    }

    public void clear() {
        sp.edit().clear().apply();
    }
}
