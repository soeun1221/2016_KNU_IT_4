
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by star on 2016. 5. 13..
 */
public class Server {
    private final static String CLASS = "Server : ";

    private static ServerSocket serverSocket; // 서버 소켓 선언하고 초기값 널

    public static void main(String[] args) throws Exception {
        try {
            serverSocket = new ServerSocket(Constant.PORT);//서버 소켓 생성하고 파라미터로 포트번호
            System.out.println("서버 소켓이 만들어졌습니다. 포트 : " + Constant.PORT);

            int cnt = 0;
            while (true) {
                Socket socket = serverSocket.accept();//클라이언트에서 연결 요청이 들어오면 그때 소켓을 하나 생성함
                System.out.println(CLASS + "[" + socket.getInetAddress() + "] 접속 : " + cnt++);
                Thread thread = new ClientThread(socket);// 클라이언트들에게 온 데이터를 처리하기 위한 스레드
                thread.start( );
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
