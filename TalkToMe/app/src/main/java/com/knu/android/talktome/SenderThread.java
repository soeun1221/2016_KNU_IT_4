package com.knu.android.talktome;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by star on 2016. 5. 13..
 */
public class SenderThread extends Thread {
    Socket socket;//공유 데이터를 멤버 변수 선언
    String name;// 사용자가 입력한 이름을 받기 위한 멤버변수

    public SenderThread(Socket socket, String name) {
        this.socket = socket;//초기화
        this.name = name;//초기화
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));//키보드로 입력한것을 JVM이 읽어들일때 byte단위로 읽어들이는데 그것을 char타입으로 읽게 하려고 InputStreamReader를 사용하고 그것을 라인단위로 읽게하기 위해서 BufferedReader를 사용
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            //사용자가 입력한 데이터를 JVM이 소켓의 바이트 스트림 단위로 서버에 보내기 위해서 PrintWriter를사용
            writer.println(name);//사용자 이름을 서버로 출력
            writer.flush();//만약에 메모리 버퍼에 남아있는게 있으면 한번에 비워라
            while (true) {
                String str = reader.readLine();//라인단위로 입력받는것을 문자열 타입에str에 대입
                if (str.equals("bye"))//만약에 문자열에 bye라는게 있으면 종료
                    break;
                writer.println(str);//str에 대입된 데이터를 서버로 출력
                writer.flush();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }
}