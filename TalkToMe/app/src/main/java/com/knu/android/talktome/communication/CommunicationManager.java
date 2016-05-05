package com.knu.android.talktome.communication;


public abstract class CommunicationManager {
    //각 통신 후 해야하는 일을 구현을 위한 추상 클래스
    public abstract void AfterCommunication(byte[] getInObject);
}
