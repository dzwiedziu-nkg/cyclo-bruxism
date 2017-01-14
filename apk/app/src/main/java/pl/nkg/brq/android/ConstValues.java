package pl.nkg.brq.android;

/**
 * Created by aaa on 2017-01-14.
 */

public class ConstValues {
    public static final String BASE_URL = "http://192.168.1.103:8000";
}


/*
Scanner input = null;
try {
    input = new Scanner(file);
} catch (FileNotFoundException e) {
    e.printStackTrace();
}

while (input.hasNextLine()){
    Log.d("MYAPP", input.nextLine());
}*/


 /*
        try {
            FileOutputStream fOut = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            int count = mQueue.size();
            while (mQueue.size() > 0) {
                SensorsRecord record = mQueue.poll();
                String rec = record.timestamp + "\t" +
                        record.longitude + "\t" +
                        record.latitude + "\t" +
                        record.altitude + "\t" +
                        record.accuracy + "\t" +
                        record.speed + "\t" +
                        record.soundNoise + "\t" +
                        record.shake + "\t" +
                        record.distance;
                osw.write(rec + "\n");
            }
            osw.flush();
            osw.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }*/