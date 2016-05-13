import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by star on 2016. 5. 13..
 */
public class ClientThread extends Thread {
    static List<PrintWriter> list = Collections.synchronizedList(new ArrayList<PrintWriter>());

    Socket socket;// 공유 데이터인 소켓을 멤버 변수로 선언
    PrintWriter writer;// 소켓을 바이트 스트림으로 보내기 위해 멤버변수 선언

    public ClientThread(Socket socket) {
        this.socket = socket;// 초기화
        try {
            writer = new PrintWriter(socket.getOutputStream());// 클라이언트들이 보내온
            // 데이터를 다시
            // 클라이언트들에게 보내기
            // 위해서 바이트 스트림으로
            // 보내기 위해서 감싼다
            list.add(writer);// 바이트 스트림으로 감싼것을 리스트에 추가 함
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void run() {
        String name = null;// 각 클라이언트가 보낸 사용자 이름을 저장하기 위한 로컬변수
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            // 클라이언트에서 보내온 데이터를 소켓의 바이트 스트림으로 받고 그것을 또
            // char타입으로(InputStreamReader()) 그것을 또 라인단위로(BufferedReader())감싼다
            name = reader.readLine();// 사용자가 입력한 이름 대입
            sendAll("#" + name + "님이 들어오셨습니다");// 사용자가 만든 센드올 메소드에 파라미터로 이름을 주고
            // 호출함
            while (true) {
                String str = reader.readLine();// 문자열 변수에다가 라인단위로 대입
                if (str == null)// 아무것도 없으면 종료
                    break;
                sendAll(name + ">" + str);// 접합 연산자를 통해서 사용자 이름과 내용이 다른 클라이언트들에게
                // 하나로 보내짐
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            list.remove(writer);// 클라이언트가 나가면 리스트에 저정된 클라이언트를 하나 삭제
            sendAll("#" + name + "나가셨습니다");// 그리고 그 클라이언트 이름을 파라미터값으로 줘서 다른
            // 클라이언트 들에게도 보냄
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

    private void sendAll(String str)// 문자열 타입에 str선언
    {
        for (PrintWriter writer : list)// 리스에 있는 프린트 라이터를 리스에 저장된 갯수만큼 포문 돌아가게함
        // 그리고 클라이언트에 보내기 위해서 씀
        {
            writer.println(str);// str에 대입된 값을 클라이언트 들에게 보냄
            writer.flush();// 만약에 메모리버퍼에 뭔가 남아있으면 한꺼번에 비운다
        }
    }
}
