package fucklegym.top.entropy;

import android.util.Log;

import com.alibaba.fastjson.*;

import javax.net.ssl.HttpsURLConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.util.*;

import central.stu.fucklegym.Encrypter;

public class NetworkSupport {
    public enum UploadStatus {
        SUCCESS, FAIL, WARNING, NOTLOGIN;
    }

    private static final String URL_LOGIN = "https://cpes.legym.cn/authorization/user/manage/login";
    private static final String URL_UPLOAD_RUNNINGDETAIL = "https://cpes.legym.cn/running/app/uploadRunningDetails";
    private static final String URL_GETSEMESTERID = "https://cpes.legym.cn/education/semester/getCurrent";
    private static final String URL_GETRUNNINGLIMIT = "https://cpes.legym.cn/running/app/getRunningLimit";
    private static final String URL_TODAYACTIVITIES = "https://cpes.legym.cn/education/app/activity/getActivityList";
    private static final String URL_SIGNUP = "https://cpes.legym.cn/education/app/activity/signUp";
    private static final String URL_CANCELSIGNUP = "https://cpes.legym.cn/education/app/activity/cancelSignUp";
    private static final String URL_SIGN = "https://cpes.legym.cn/education/activity/app/attainability/sign";
    private static final String URL_GETCOURSELIST = "https://cpes.legym.cn/education/course/today";
    private static final String URL_SIGNCOURSE = "https://cpes.legym.cn/education/course/app/forStudent/sign";
    private static final String URL_GETCOURSETODAY = "https://cpes.legym.cn/education/course/today";

    private static final double CALORIE_PER_MILEAGE = 58.3;

    public static JSONObject postForReturn(String url, Map<String, String> header, String content) throws IOException {
        URL serverUrl = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) serverUrl.openConnection();
        conn.setRequestMethod("POST");
//        conn.setDoOutput(true);
//        conn.setDoInput(true);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        OutputStream out = conn.getOutputStream();
        out.write(content.getBytes(StandardCharsets.UTF_8));
        conn.connect();
        StringBuilder stringBuffer = new StringBuilder();
        ;
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        BufferedReader buffer = new BufferedReader(reader);
        String tmp;
        while ((tmp = buffer.readLine()) != null) {
            stringBuffer.append(tmp);
        }
        return JSON.parseObject(stringBuffer.toString());
    }

    public static JSONObject getForReturn(String url, Map<String, String> header) throws IOException {
        URL serverUrl = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) serverUrl.openConnection();
        conn.setRequestMethod("GET");
//        conn.setDoOutput();
        for (Map.Entry<String, String> entry : header.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        StringBuilder stringBuffer = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        BufferedReader buffer = new BufferedReader(reader);
        String tmp;
        while ((tmp = buffer.readLine()) != null) {
            stringBuffer.append(tmp);
        }
        return JSON.parseObject(stringBuffer.toString());
    }

    public static JSONObject putForReturn(String url, Map<String, String> header, String content) throws IOException {
        URL serverUrl = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) serverUrl.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }
        OutputStream out = conn.getOutputStream();
        out.write(content.getBytes(StandardCharsets.UTF_8));
        Log.d("network", "putForReturn: " + conn.getResponseCode());
        StringBuilder stringBuffer = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        BufferedReader buffer = new BufferedReader(reader);
        String tmp;
        while ((tmp = buffer.readLine()) != null) {
            stringBuffer.append(tmp);
        }
        return JSON.parseObject(stringBuffer.toString());
    }

    public static Pair<String, String> getAccessTokenId(String name, String pwd) throws IOException {
        JSONObject content = new JSONObject();
        content.put("userName", name);
        content.put("password", pwd);
        content.put("entrance", "1");
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Content-type", "application/json");
        JSONObject data = postForReturn(URL_LOGIN, header, content.toString()).getJSONObject("data");
        Pair<String, String> ret = new Pair<String, String>(data.getString("accessToken"), data.getString("id"));
        if (ret.getKey() == null) return null;
        else return ret;
    }

    public static JSONObject getSemesterId(String accessToken) throws IOException {
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        return getForReturn(URL_GETSEMESTERID, header).getJSONObject("data");
    }

    public static RunningLimitInfo getRunningLimiteInfo(String accessToken, String semesterId) throws IOException {
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        JSONObject content = new JSONObject();
        content.put("semesterId", semesterId);
        JSONObject info = postForReturn(URL_GETRUNNINGLIMIT, header, content.toString());
        double tot = 0.0, dai = 0.0;
        try {
            tot = info.getJSONObject("data").getDouble("totalDayMileage");
            dai = info.getJSONObject("data").getDouble("dailyMileage");
            return new RunningLimitInfo(info.getJSONObject("data").getString("limitationsGoalsSexInfoId").toString(), tot, dai);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return new RunningLimitInfo("null", 0, 0);
        }
    }

    public static UploadStatus uploadRunningDetail(String accessToken, String limitationsGoalsSexInfoId, String semesterId, double totMileage, double validMileage, Date startTime, Date endTime, String map, String type) throws IOException {
        //这函数啥也不管，只管上传，检查数据是否安全不是它的事
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        Random random = new Random(System.currentTimeMillis());
        //随机偏移一定的跑步路程避免整数
        double offset = random.nextDouble() / 100;
        if (totMileage >= 3.5) {
            totMileage -= offset;
            validMileage -= offset;
        } else {
            totMileage += offset;
            validMileage += offset;
        }
        JSONObject content = new JSONObject();
        double pace = 0.5 + random.nextInt(6) / 10.0;
        content.put("paceRange", pace);
        content.put("totalMileage", totMileage);
        content.put("limitationsGoalsSexInfoId", limitationsGoalsSexInfoId);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        content.put("endTime", formatter.format(endTime));
        content.put("startTime", formatter.format(startTime));
        content.put("effectiveMileage", validMileage);
        content.put("semesterId", semesterId);
        content.put("scoringType", 1);
        content.put("signPoint", new JSONArray());
        content.put("totalPart", 1);
        int calorie = (int) (totMileage * CALORIE_PER_MILEAGE);
        content.put("calorie", calorie);
        ArrayList<HashMap<String, String>> runPoints = new ArrayList<>();
        try {
            ArrayList<Pair<Double, Double>> genPoints = PathGenerator.genRegularRoutine(map, totMileage);
            for (Pair<Double, Double> point : genPoints) {
                HashMap<String, String> tmp = new HashMap<>();
                tmp.put("latitude", point.getKey().toString());
                tmp.put("longitude", point.getValue().toString());
                runPoints.add(tmp);
            }
        }catch (Exception e){
            e.printStackTrace();
            return UploadStatus.FAIL;
        }
        int keeptime = (int) (endTime.getTime() - startTime.getTime()) / 1000;
        content.put("keepTime", keeptime);
        content.put("routineLine", runPoints);
        content.put("type", type);
        int paceNumber = (int) (totMileage * 1000 / pace / 2);
        content.put("paceNumber", paceNumber);
        content.put("effectivePart", 1);
        content.put("gpsMileage", totMileage);
        content.put("uneffectiveReason", "");
        int avePace = ((int) ((endTime.getTime() - startTime.getTime()) / 1000 / totMileage)) * 1000;
        content.put("avePace", avePace);
        //生成关于本次跑步数据的SHA1签名
        content.put("signDigital", Encrypter.getSha1(validMileage
                + "1"
                + formatter.format(startTime)
                + calorie
                + avePace
                + keeptime
                + paceNumber
                + totMileage
                + "1" + Encrypter.run_salt));


        //System.out.println(content.toString());return UploadStatus.NOTLOGIN;
        JSONObject res = postForReturn(URL_UPLOAD_RUNNINGDETAIL, header, content.toString());
        Log.d("runRes", "uploadRunningDetail: " + res.toString());
        //System.out.println(res.toString());
        if (res.getBoolean("data")) return UploadStatus.SUCCESS;
        else return UploadStatus.FAIL;
    }

    public static Map<String, String> getTodayActivities(String accessToken) throws IOException {
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        JSONObject content = new JSONObject();
        content.put("name", "");
        content.put("campus", "");
        content.put("page", 1);
        content.put("size", 30);
        content.put("state", "");
        content.put("topicId", "");
        content.put("week", "");
        JSONObject ret = postForReturn(URL_TODAYACTIVITIES, header, content.toString());
        JSONArray items = ret.getJSONObject("data").getJSONArray("items");
        HashMap<String, String> acts = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            acts.put(items.getJSONObject(i).getString("name"), items.getJSONObject(i).getString("id"));
        }
        return acts;
    }

    public static NetworkSupport.UploadStatus signup(String accessToken, String activityId) throws IOException {
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        JSONObject content = new JSONObject();
        content.put("activityId", activityId);
        JSONObject ret = postForReturn(URL_SIGNUP, header, content.toString());
        Log.d("signupIt", "signup: " + ret.toJSONString());
        if (ret.getJSONObject("data").getBoolean("success")) return UploadStatus.SUCCESS;
        else return UploadStatus.FAIL;
    }

    public static NetworkSupport.UploadStatus cancelSignup(String accessToken, String activityId) throws IOException {
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        JSONObject content = new JSONObject();
        content.put("activityId", activityId);
        JSONObject ret = postForReturn(URL_CANCELSIGNUP, header, content.toString());
        if (ret.getBoolean("data")) return UploadStatus.SUCCESS;
        else return UploadStatus.FAIL;
    }

    public static NetworkSupport.UploadStatus sign(String accessToken, String userId, String activityId) throws IOException {
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        JSONObject content = new JSONObject();
        content.put("pageType", "activity");
        content.put("times", "1");
        content.put("activityType", 0);
        content.put("attainabilityType", 2);
        content.put("activityId", activityId);
        content.put("userId", userId);
        JSONObject ret = putForReturn(URL_SIGN, header, content.toString());
        if (ret.getString("message").equals("成功")) return UploadStatus.SUCCESS;
        else return UploadStatus.FAIL;
    }

    public static JSONObject getCourseList(String accessToken) throws IOException {
        HashMap<String, String> header = new HashMap<String, String>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        JSONObject nullJson = new JSONObject();
        try {
            JSONArray array = getForReturn(URL_GETCOURSETODAY, header).getJSONArray("data");
            for (int i = 0; i < array.size(); i++) {
                if (array.getJSONObject(i).getIntValue("courseActivityType") == 1) {
                    return array.getJSONObject(i);
                }
            }
            nullJson.put("projectName", "今天没有体育课程哦~");
            nullJson.put("timeStart", 0L);
            nullJson.put("timeEnd", 0L);
        }catch (Exception e){
            e.printStackTrace();
            nullJson.put("projectName", "今天没有体育课程哦~");
            nullJson.put("timeStart", 0L);
            nullJson.put("timeEnd", 0L);
            return nullJson;
        }
        return nullJson;
    }

    public static NetworkSupport.UploadStatus signCourse(String accessToken, String userId, String courseId, int weekNumber) throws IOException {
        HashMap<String, String> header = new HashMap<>();
        header.put("Content-type", "application/json");
        header.put("Authorization", "Bearer " + accessToken);
        JSONObject content = new JSONObject();
        content.put("attainabilityType", 0);
        content.put("courseId", courseId);
        content.put("userId", userId);
        content.put("weekNumber", weekNumber);
        content.put("pageType", "course");
        content.put("startSignNumber", 1);
        Log.d("signcourse", "signCourse: " + content.toJSONString());
        JSONObject ret = putForReturn(URL_SIGNCOURSE, header, content.toString());
        Log.d("signcourse", "signCourse: " + ret.toString());
        if (ret.getString("message").equals("成功")) {
            return UploadStatus.SUCCESS;
        } else return UploadStatus.FAIL;
    }
}
