package com.example.conducto2;

import android.util.Log;

import com.example.conducto2.Lesson;
import com.example.conducto2.User;

public class DataManager {
    private static User user;
    private static Lesson curLesson;

    public static User getUserInstance(){
        return user;
    }
    public  static void setUser(User other){
        user = new User(other.getEmail(), other.getFname(), other.getLname());
    }

    public static  Lesson getCurLessonInstance(){return curLesson;}
    public static void setCurLesson(Lesson lesson){
        curLesson = new Lesson(lesson);
        Log.d("Odi", lesson.toString());
    }
}
