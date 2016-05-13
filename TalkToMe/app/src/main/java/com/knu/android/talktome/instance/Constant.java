package com.knu.android.talktome.instance;

/**
 * Created by star on 2016. 5. 11..
 */
public class Constant {

    public static String IP = "192.168.0.19";
    public static final int PORT = 56789;
    public static final int CONNECT_TIMEOUTRATE = 2500;

    public static final short INSERT_TALK = 200;
    public static final short REGISTER_INSTANCE_ID = 300;

    /* gcm action */
    public static final String REGISTRATION_READY = "registrationReady";
    public static final String REGISTRATION_GENERATING = "registrationGenerating";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
}
