package com.example.conducto2.data.manager;

import androidx.annotation.Nullable;

import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.example.conducto2.data.model.Class;

public class DataManager {

    // this class provide a global reference to data used by activities

    private static User user;
    private static Lesson curLesson;
    private static Class curClass;

    public static User getUserInstance(){
        return user;
    }
    public  static void setUser(User other){
        user = new User(other.getEmail(), other.getFname(), other.getLname(), other.getUserType(), other.getProfilePictureBase64());
    }

    public static  Lesson getCurLesson(){return curLesson;}
    public static void setCurLesson(@Nullable Lesson lesson){
        if (lesson == null) { // imply lesson creation
            curLesson = null;
            return;
        }
        curLesson = new Lesson(lesson);
    }

    public static Class getCurClass(){
        return curClass;
    }
    public static void setCurClass(Class cls){
        curClass = cls;
    }
}
