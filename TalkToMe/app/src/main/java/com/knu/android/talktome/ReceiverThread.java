package com.knu.android.talktome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by star on 2016. 5. 13..
 */
public class ReceiverThread extends Thread {
    Socket socket;// 공유데이터 멤버변수 선언

    public ReceiverThread(Socket socket) {
        this.socket = socket;// 초기화
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            // 서버가 보내온 소켓의 바이트 스트림을 다시 char타입의 InputSTreamReader 그것을라인단위로 읽기 위해서
            // BufferedReader로 감싼다

            while (true) {
                String str = reader.readLine();// str변수에 라인단위로 저장
                if (str == null)// null이면
                    break;// 종료
                
                MyFrame.ta.setText(
                        MyFrame.ta.getText() + "\n" + str);

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
