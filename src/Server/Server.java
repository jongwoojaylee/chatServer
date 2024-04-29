package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Server {
    private static Map<user, Socket> clients = new HashMap<>();
    private static List<room> rooms = new ArrayList<>();

    public static void main(String[] args) {
        try {
            BufferedReader in;
            PrintWriter out;

            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("서버가 정상적으로 시작되었습니다.");

            //대기실 생성
            room waitngRoom = new room(1,"waiting room");
            rooms.add(waitngRoom);

            //다중 클라이언트 받기
            while (true){
                //클라이언트 연결 대기
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트가 연결되었습니다.");

                new chatThread(clientSocket, clients, rooms).start();

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
class user {
    private String id;
    private int currentRoomNumber = 1;

    public user(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCurrentRoom() {
        return currentRoomNumber;
    }

    public void setCurrentRoom(int currentRoom) {
        this.currentRoomNumber = currentRoom;
    }
}
class room {
    private int roomNumber;
    private String roomName;

    private int currentNumberOfUser =0;

    public room(int roomNumber, String roomName) {
        this.roomNumber = roomNumber;
        this.roomName = roomName;
    }

    public int getCurrentNumberOfUser() {
        return currentNumberOfUser;
    }

    public void setCurrentNumberOfUser(int currentNumberOfUser) {
        this.currentNumberOfUser = currentNumberOfUser;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return "Room Name(number): " + roomName+"("+roomNumber+")"+"\n";
    }
}
class chatThread extends Thread{
    private Socket clientSocket;
    private String id;
    private Map<user, Socket> clients;
    private List<room> rooms;
    private BufferedReader in;
    private PrintWriter out;

    public chatThread(Socket clientSocket, Map<user, Socket> clients, List<room> rooms) {
        this.clientSocket = clientSocket;
        this.clients = clients;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        try {
            //In, out 통로 열기
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            id = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
            //클라이언트로부터 ID 수신

        user user = new user(id);
        clients.put(user, clientSocket);
        System.out.println("사용자 '" + id + "'가 채팅에 참여하였습니다.");
        System.out.println(id + "님의 IP 주소는" + clientSocket.getInetAddress() + "입니다.");

        out.println("대기실에 입장하셨습니다.");

        out.println("방 목록 보기 : /list\n" +
                "대기실 입장 : /stay\n" +
                "방 생성 : /create\n" +
                "방 입장 : /join [방번호]\n" +
                "방 나가기 : /exit\n" +
                "접속종료 : /bye");

        //커맨드 입력받기
        while (true) {

            String command = null;
            try {
                command = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //명령어 받기
            if ("/list".equals(command)) {
                out.println("방 목록 보기");
                out.println(rooms);
            } else if ("/create".equals(command)) {
                out.println("방 이름을 입력해주세요.");
                String nameOfRoom = null;
                try {
                    nameOfRoom = in.readLine();
                    room room = new room(rooms.size() + 1, nameOfRoom);
                    System.out.println((rooms.size() + 1) + "번째 방 생성!");
                    System.out.println(room);
                    rooms.add(room);
                    out.println(nameOfRoom + "방 생성!");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if ("/join".equals(command)) {
                out.println("참가하려는 방의 번호를 입력하세요.");
                while (true) {
                    try {
                        int roomNumber = Integer.parseInt(in.readLine());
                        user.setCurrentRoom(roomNumber);
                        out.println(rooms.get(roomNumber - 1).getRoomName() + "(" + roomNumber + ") 방에 입장하셨습니다!");
                        break;
                        } catch (Exception e) {
                        out.println("잘못된 입렵입니다.");
                    }
                }
                break;
            } else if ("/stay".equals(command)) {
                break;
            } else {
                out.println("다시 입력해주세요");
            }
        }
        //입장 알리기
        String EnterMsg = user.getId() + "님이 " + rooms.get(user.getCurrentRoom() - 1).getRoomName() + "(" + user.getCurrentRoom() + ") 방에 입장하셨습니다!";
        System.out.println(EnterMsg);
        try {
            broadcast(EnterMsg, user.getCurrentRoom());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //채팅시작
        String msg = null;
        try {
            while ((msg = in.readLine()) != null) {
                if ("/bye".equalsIgnoreCase(msg))
                    break;
                else if ("/exit".equalsIgnoreCase(msg)) {
                    out.println("현재 채팅방에서 나가셨습니다.\n" +
                            "대기실로 돌아갑니다.");
                    broadcast(id + "님이 나가셨습니다.", user.getCurrentRoom());
                    user.setCurrentRoom(1);
                }
                broadcast(id + " : " + msg, user.getCurrentRoom());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println(id + "님이 접속을 종료하셨습니다.");
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ;
        }
    }
    public void broadcast(String msg, int roomNumber) throws IOException {
        for (Map.Entry<user, Socket> entry : clients.entrySet()) {
            user user = entry.getKey();
            Socket socket = entry.getValue();
            if (user.getCurrentRoom() == roomNumber) {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
