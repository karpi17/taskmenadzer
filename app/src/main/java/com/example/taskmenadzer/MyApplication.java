package com.example.taskmenadzer;


import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
// import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory; // <-- TEGO NIE MOŻE BYĆ NA RAZIE

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this); // To inicjuje Firebase w Twojej klasie Application

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        // To jest linia, która powinna generować token debugowania:
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );
         System.out.println("DEBUG: MyApplication onCreate called and App Check initialized!"); // Możesz to dodać dla testu
    }
}