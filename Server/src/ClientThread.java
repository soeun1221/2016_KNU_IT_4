import com.googlecode.javacpp.annotation.Const;
import com.sun.tools.javac.jvm.Gen;
import data.OBJECT;
import fr.lium.spkDiarization.programs.*;
import fr.lium.spkDiarization.system.Diarization;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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
    private String name;
    private Gender gender;
    private int id;
    private ArrayList<String> conversations;

    private boolean isRunning = true;

    enum Gender {
        FEMALE, MALE, UNDEFINED
    }

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
            id = list.size();
            conversations = new ArrayList<>();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void run() {
        while (true) {
            receiveData();
            if (!isRunning) break;
//            sendData();
        }
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

    private synchronized void writeToWaveFile(byte[] pcm_data, String filename) {

        AudioFormat frmt = new AudioFormat(16000, 16, 1, true, false);
        AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(pcm_data), frmt,
                pcm_data.length / frmt.getFrameSize()
        );

        try {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new
                    File(filename)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void working() {
        try {
            FileOutputStream stream = null;
            switch (objectToWork.getMessage()) {
                //안드로이드로 부터 전달받은 OBJECT:objectToWork의 메세지를 분석해서
                //케이스에 따라 필요한 일을 수행한다.
                //인자는 안드로이드에서 필요한 형태에 따라 넘겨준다.
                case Constant.SEND_BASE_WAVE:
                    String base = "base_" + id;

                    stream = new FileOutputStream(Constant.projectDir + "/working/" + base + ".wav");
                    try {
                        stream.write((byte[]) objectToWork.getObject(0));
                    } finally {
                        stream.close();
                    }

                    diarization(base);
                    print(Constant.projectDir + "/working/" + base + ".seg");
                    rename(base);
                    print(Constant.projectDir + "/working/" + base + ".ident.seg");
                    trainInit(base);
                    fileCopy(Constant.projectDir + "/" + base + ".out.gmms", Constant.projectDir + "/working" + "/" + base + ".init.gmm");

                    trainMap(base);
                    fileCopy(Constant.projectDir + "/" + base + ".out.gmms", Constant.projectDir + "/voicedb" + "/" + base + ".gmm");

                    gender = getGenderFromSegmentFile(Constant.projectDir + "/working/" + base + ".seg");
                    writer.println("build complete");
                    writer.flush();
                    break;
                case Constant.SEND_NAME:
                    name = (String) objectToWork.getObject(0);
                    sendAll("#" + name + "님이 들어오셨습니다");// 사용자가 만든 센드올 메소드에 파라미터로 이름을 주고
                    break;
                case Constant.SEND_MESSAGE:
                    // get gender
                    String msgfile = "msg_" + id;
                    writeToWaveFile((byte[]) objectToWork.getObject(0), Constant.projectDir + "/working/" + msgfile + ".wav");
                    System.out.println("write complete");
//                    String msg = (String) objectToWork.getObject(1);
//                    sendAll(name + ">" + msg);// 접합 연산자를 통해서 사용자 이름과 내용이 다른 클라이언트들에게
                    //diarization(msgfile);
                    //segment(msgfile);
                    gender_detection(msgfile);
                    print(Constant.projectDir + "/working/" + msgfile + ".seg");
                    Gender genderFromMsg = getGenderFromSegmentFile(Constant.projectDir + "/working/" + msgfile + ".seg");
                    System.out.println("gender:" + gender + ", genderFromMsg:" + genderFromMsg);
                    if (gender == genderFromMsg) {
                        String msg = (String) objectToWork.getObject(1);
                        //diarization(msgfile);
                        //score(msgfile);
//                        writer.println(name + ">" + msg);
//                        writer.flush();
                        sendAll(name + ">" + msg);// 접합 연산자를 통해서 사용자 이름과 내용이 다른 클라이언트들에게
                    }
                    break;
                case Constant.SEND_BYE:
                    System.out.println(name + "[out]");
                    sendAll(name + "[out]");
                    isRunning = false;
                    try {
                        socket.close();
                    } catch (Exception e) {

                    }
                    break;
                case Constant.SAVE_REQUEST:
                    writer.println("save");
                    writer.flush();
                    for (String str : conversations) {
                        writer.println(str);
                    }
                    writer.println("finish");
                    writer.flush();
                    break;
                default:
                    System.err.println("[Worker]알수없는 메세지");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void diarization(String filebase) {
        String dir = Constant.projectDir + "/working";
        ProcessBuilder pb = new ProcessBuilder().inheritIO().command("java", "-Xmx2048m", "-jar",
                Constant.projectDir + "/lib/lium_spkdiarization-8.4.1.jar", "--fInputMask=" + dir + "/%s.wav",
                "--sOutputMask=" + dir + "/%s.seg", "--doCEClustering", filebase);
        Process p = null;
        try {
            p = pb.start();
            p.waitFor();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private synchronized void score(String filebase) {
        try {
            String dir = Constant.projectDir + "/working";
            String[] scoreParams = {"--sInputMask=" + dir + "/%s.seg", "--fInputMask=" + dir + "/%s.wav",
                    "--sOutputMask=" + dir + "/%s.ident.score.seg",
                    "--sOutputFormat=seg,UTF8", "--fInputDesc=audio2sphinx,1:3:2:0:0:0,13,1:0:300:4",
                    "--tInputMask=" + Constant.projectDir + "/voicedb/" + filebase + ".gmm", " --sTop=8," + Constant.projectDir + "/ubm.gmm",
                    "--sSetLabel=add", "--sByCluster", filebase};
            MScore.main(scoreParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void gender_detection(String filebase) {
        try {
            String dir = Constant.projectDir + "/working";
            String[] segInitParams = {"--fInputMask=" + dir + "/%s.wav", "--fInputDesc=audio2sphinx,1:1:0:0:0:0,13,0:0:0",
                    "--sInputMask=", "--sOutputMask=" + dir + "/%s.s.seg", filebase};
            MSegInit.main(segInitParams);

            String[] decodeParams = {"--fInputMask=" + dir + "/%s.wav", "--fInputDesc=audio2sphinx,1:3:2:0:0:0,13,0:0:0",
                    "--sInputMask=" + dir + "/%s.s.seg", "--sOutputMask=" + dir + "/%s.g.seg", "--dPenality=10,10,50",
                    "--tInputMask=" + Constant.projectDir + "/sms.gmms", filebase};
            MDecode.main(decodeParams);

            String[] scoreParams = {"--help", "--sGender", "--sByCluster", "--fInputDesc=audio2sphinx,1:3:2:0:0:0,13,1:1:0:0",
                    "--fInputMask=" + dir + "/%s.wav", "--sInputMask=" + dir + "/%s.g.seg", "--sOutputMask=" + dir + "/%s.seg",
                    "--tInputMask=gender.gmms", filebase};
            MScore.main(scoreParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void rename(String filebase) {
        try {
            Scanner input = new Scanner(new File(Constant.projectDir + "/working/" + filebase + ".seg"));
            String cluster = null;
            while (input.hasNext()) {
                String token = input.next();
                if (token.equals("cluster")) {
                    cluster = input.next();
                    break;
                }
            }
            input.close();

            Path path = Paths.get(Constant.projectDir + "/working/" + filebase + ".seg");
            Charset charset = StandardCharsets.UTF_8;

            String content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll(cluster, filebase);
            path = Paths.get(Constant.projectDir + "/working/" + filebase + ".ident.seg");
            Files.write(path, content.getBytes(charset));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized Gender getGenderFromSegmentFile(String filename) {

        Gender gender = Gender.UNDEFINED;
        Scanner input = null;
        try {
            input = new Scanner(new File(filename));
            while (input.hasNext()) {
                input.nextLine();
                String line = input.nextLine();
                System.out.println(line);
                switch (line.split(" ")[4]) {
                    case "F":
                        gender = Gender.FEMALE;
                        break;
                    case "M":
                        gender = Gender.MALE;
                        break;
                }
                break;
            }
            input.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return gender;
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

    private synchronized void trainInit(String filebase) {
        System.out.println("[TrainInit]");
        String dir = Constant.projectDir + "/working";
        String[] trainInitParams = {"--sInputMask=" + dir + "/%s.ident.seg", "--fInputMask=" + dir + "/%s.wav",
                "--fInputDesc=audio2sphinx,1:3:2:0:0:0,13,1:1:300:4", "--emInitMethod=copy",
                "--tInputMask=" + Constant.projectDir + "/ubm.gmm", filebase};

        try {
            MTrainInit.main(trainInitParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void trainMap(String filebase) {
        System.out.println("[TrainMap]");

        String dir = Constant.projectDir + "/working";
        String[] trainMapParams = {"--sInputMask=" + dir + "/%s.ident.seg", "--fInputMask=" + dir + "/%s.wav",
                "--fInputDesc=audio2sphinx,1:3:2:0:0:0,13,1:1:300:4", "--tInputMask=" + dir + "/%s.init.gmm",
                "--tInputMask=" + dir + "/%s.init.gmm", "--emCtrl=1,5,0.01", "--varCtrl=0.01,10.0", filebase};

        try {
            MTrainMAP.main(trainMapParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void sendData() {
//        //안드로이드로 데이터를 전송한다.
//        //처리한 결과를 OBJECT형으로 전송한다.
//        String METHOD = ".sendData()";
//        try {
//            ObjectOutputStream sendObject = new ObjectOutputStream(socket.getOutputStream());
//            System.out.println(TAG + METHOD + " : Wait Output");
//            sendObject.writeObject(objectToSend);
//            System.out.println(TAG + METHOD + " : Complete Output [" + objectToSend.getMessage() + "]");
//        } catch (Exception e) {
//            System.err.println(TAG + METHOD + " : " + e.getClass().getName());
//        }
//    }

    private void sendAll(String str)// 문자열 타입에 str선언
    {
        conversations.add(str);
        for (PrintWriter writer : list)// 리스에 있는 프린트 라이터를 리스에 저장된 갯수만큼 포문 돌아가게함
        // 그리고 클라이언트에 보내기 위해서 씀
        {
            writer.println(str);// str에 대입된 값을 클라이언트 들에게 보냄
            writer.flush();// 만약에 메모리버퍼에 뭔가 남아있으면 한꺼번에 비운다
        }
    }

    private synchronized void fileCopy(String orgFilePath, String newFilePath) {
        File orgFile = new File(orgFilePath);
        File newFile = new File(newFilePath);

        if (orgFile.exists()) {
            orgFile.renameTo(newFile);
            System.out.println("file copy");
        } else {
            System.out.println(orgFilePath + ":file copy error");
        }
    }

}
