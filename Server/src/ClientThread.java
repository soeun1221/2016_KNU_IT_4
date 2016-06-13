import com.googlecode.javacpp.annotation.Const;
import data.OBJECT;
import fr.lium.spkDiarization.programs.MTrainInit;
import fr.lium.spkDiarization.programs.MTrainMAP;
import fr.lium.spkDiarization.system.Diarization;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Created by star on 2016. 5. 13..
 */
public class ClientThread extends Thread {

    private static final String TAG = "ClientThread";
    private OBJECT objectToWork;
    private OBJECT objectToSend;

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
        receiveData();
        sendData();
//        String name = null;// 각 클라이언트가 보낸 사용자 이름을 저장하기 위한 로컬변수
//        try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    socket.getInputStream()));
//            String identifier = reader.readLine();
//            if(identifier.equals("base")) {
//
//            } else if(identifier.equals("voice")) {
//
//            } else {
//                name = identifier; // 사용자가 입력한 이름 대입
//            }
//            sendAll("#" + name + "님이 들어오셨습니다");// 사용자가 만든 센드올 메소드에 파라미터로 이름을 주고
//
//            // 호출함
//            while (true) {
//                String str = reader.readLine();// 문자열 변수에다가 라인단위로 대입
//                if (str.equals("bye"))// bye String 받으면 종료
//                    break;
//                sendAll(name + ">" + str);// 접합 연산자를 통해서 사용자 이름과 내용이 다른 클라이언트들에게
//                // 하나로 보내짐
//            }
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        } finally {
//            System.out.println(name + "[out]");
//            sendAll(name + "[out]");
//            try {
//                socket.close();
//            } catch (Exception e) {
//            }
//        }
    }

    private Object receiveData() {
        //안드로이드로부터 데이터를 받아온다.
        //OBJECT형으로 받은 데이터를 worker 클래스로 넘겨
        //분석해서 해야하는 일을 처리한다.
        String METHOD = ".receiveData()";
        try {
            ObjectInputStream getObject = new ObjectInputStream(socket.getInputStream());
            OBJECT readObject = (OBJECT) getObject.readObject();
            System.out.println(TAG + METHOD + " : get Message[" + readObject.getMessage() + "]");
            this.objectToWork = readObject;
            working();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void working(){
        try {
            switch (objectToWork.getMessage()) {
                //안드로이드로 부터 전달받은 OBJECT:objectToWork의 메세지를 분석해서
                //케이스에 따라 필요한 일을 수행한다.
                //인자는 안드로이드에서 필요한 형태에 따라 넘겨준다.
                case Constant.SEND_BASE_WAVE:
                    String id = "base_"+list.size();
                    FileOutputStream stream = new FileOutputStream("working/"+id+".wav");
                    try {
                        stream.write((byte[]) objectToWork.getObject(0));
                    } finally {
                        stream.close();
                    }
                    diarization(id);

                    print(Constant.projectDir+"/working/"+id+".seg");

                    rename(id);

                    print(Constant.projectDir+"/working/"+id+".ident.seg");

                    trainInit(id);

                    trainMap(id);
                    objectToSend = new OBJECT(Constant.SEND_BASE_WAVE);
                    break;

                default:
                    System.err.println("[Worker]알수없는 메세지");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void diarization(String id) {
        String dir = Constant.projectDir +"/working";
        String[] diarizationParams = {"--fInputMask="+dir+"/%s.wav", "--sOutputMask="+dir+"/%s.seg", "--doCEClustering", id};
        Diarization.main(diarizationParams);
    }

    private synchronized void rename(String id) {
        try {
            Scanner input = new Scanner(new File(Constant.projectDir+"/working/"+id+".seg"));
            String cluster = null;
            while(input.hasNext()) {
                String token = input.next();
                if(token.equals("cluster")) {
                    cluster = input.next();
                    break;
                }
            }
            input.close();

            Path path = Paths.get(Constant.projectDir+"/working/"+id+".seg");
            Charset charset = StandardCharsets.UTF_8;

            String content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll(cluster, id);
            path = Paths.get(Constant.projectDir+"/working/"+id+".ident.seg");
            Files.write(path, content.getBytes(charset));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void print(String filename) {
        try {
            Path path = Paths.get(filename);
            Charset charset = StandardCharsets.UTF_8;

            String content = new String(Files.readAllBytes(path), charset);
            System.out.println(content);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void trainInit(String id) {
        String dir = Constant.projectDir +"/working";
        String[] trainInitParams = {"--sInputMask="+dir+"/%s.ident.seg", "--fInputMask="+dir+"/%s.wav",
                "--fInputDesc=audio2sphinx,1:3:2:0:0:0,13,1:1:300:4", "--emInitMethod=copy",
                "--tInputMask="+Constant.projectDir+"/ubm.gmm", id};

        try {
            MTrainInit.main(trainInitParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileCopy(Constant.projectDir+"/"+id+".out.gmms", dir+"/"+id+".init.gmm");
    }

    private void trainMap(String id) {
        String dir = Constant.projectDir +"/working";
        String[] trainMapParams = {"--sInputMask="+dir+"/%s.ident.seg", "--fInputMask="+dir+"/%s.wav",
                "--fInputDesc=audio2sphinx,1:3:2:0:0:0,13,1:1:300:4", "--tInputMask="+dir+"/%s.init.gmm",
                "--tInputMask="+dir+"/%s.init.gmm", "--emCtrl=1,5,0.01", "--varCtrl=0.01,10.0", id};

        try {
            MTrainMAP.main(trainMapParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileCopy(Constant.projectDir+"/"+id+".out.gmms", Constant.projectDir+"/voicedb"+"/"+id+".gmm");
    }

    private void sendData() {
        //안드로이드로 데이터를 전송한다.
        //처리한 결과를 OBJECT형으로 전송한다.
        String METHOD = ".sendData()";
        try {
            ObjectOutputStream sendObject = new ObjectOutputStream(socket.getOutputStream());
            System.out.println(TAG + METHOD + " : Wait Output");
            sendObject.writeObject(objectToSend);
            System.out.println(TAG + METHOD + " : Complete Output [" + objectToSend.getMessage() + "]");
        } catch (Exception e) {
            System.err.println(TAG + METHOD + " : " + e.getClass().getName());
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

    private void fileCopy(String orgFilePath, String newFilePath) {
        File orgFile = new File(orgFilePath);
        File newFile = new File(newFilePath);

        if(orgFile.exists()) {
            orgFile.renameTo(newFile);
        }
    }

}
