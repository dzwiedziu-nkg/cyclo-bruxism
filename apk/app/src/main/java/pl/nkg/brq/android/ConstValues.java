package pl.nkg.brq.android;

import android.graphics.Color;

/**
 * Created by aaa on 2017-01-14.
 */

public class ConstValues {
    // Adres serwera z którym łączy się aplikacja
    //public static final String BASE_URL = "https://bike-app-server.herokuapp.com";
    public static final String BASE_URL = "http://192.168.1.10:8000";

    // Timeout połączeń z serwerem
    public static final int CONNECTION_TIMEOUT = 20000;
    // Ilość pomiarów które są wysyłane w jednym pakiecie
    public static final int DATA_CHUNK_SIZE = 1000;

    // Przy false nie wysyłamy danych na serwer
    public static final boolean DATA_SENDING_ACTIVE = true;
    // Przy true zapisujemy nawet podróże w których nic się nie zapisało. Do celów testowych
    public static final boolean SAVE_EMPTY_DATA = false;

    public static final String MODE_USER_ONLY = "userOnly";
    public static final String MODE_ALL_USERS = "allUsers";

    public static final String API_KEY = "AIzaSyBZGDRVjIgsqDaxBhkOCXkNCoLWNYEPw78";
    public static final String DIRECTORY_NAME = "RoadTester";

    // Kolory którymi na mapie są oznaczane poszczególne oceny odcinków podróży
    public static final int colorGradeOne = Color.rgb(58, 240, 22);
    public static final int colorGradeTwo = Color.rgb(142, 240, 22);
    public static final int colorGradeThree = Color.rgb(203, 240, 22);
    public static final int colorGradeFour = Color.rgb(240, 240, 22);
    public static final int colorGradeFive = Color.rgb(240, 210, 22);
    public static final int colorGradeSix = Color.rgb(240, 182, 22);
    public static final int colorGradeSeven = Color.rgb(240, 140, 22);
    public static final int colorGradeEight = Color.rgb(240, 113, 22);
    public static final int colorGradeNine = Color.rgb(240, 70, 22);
    public static final int colorGradeTen = Color.rgb(240, 40, 22);

    // Półprzezroczyste kolory którymi oznaczane na mapie są ocecny z danego rejonu
    public static final int colorTransparentGradeOne = Color.argb(80, 58, 240, 22);
    public static final int colorTransparentGradeTwo = Color.argb(80, 142, 240, 22);
    public static final int colorTransparentGradeThree = Color.argb(80, 203, 240, 22);
    public static final int colorTransparentGradeFour = Color.argb(80, 240, 240, 22);
    public static final int colorTransparentGradeFive = Color.argb(80, 240, 210, 22);
    public static final int colorTransparentGradeSix = Color.argb(80, 240, 182, 22);
    public static final int colorTransparentGradeSeven = Color.argb(80, 240, 140, 22);
    public static final int colorTransparentGradeEight = Color.argb(80, 240, 113, 22);
    public static final int colorTransparentGradeNine = Color.argb(80, 240, 70, 22);
    public static final int colorTransparentGradeTen = Color.argb(80, 240, 40, 22);

}
