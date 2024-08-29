import java.io.BufferedReader; //クライアントからの入力を読み取るためのクラス
import java.io.FileWriter;
import java.io.IOException; //入出力処理中のエラーを扱うためのクラス
import java.io.InputStreamReader; //バイトストリームを文字ストリームに変換するためのクラス
import java.io.PrintWriter; //クライアントへの出力を行うためのクラス
import java.net.ServerSocket; //サーバソケットを作成するためのクラス
import java.net.Socket; //クライアントとの接続を扱うためのクラス
import java.time.LocalDateTime;
import java.util.ArrayList; //クライアントのリストを保持するためのクラス
import java.util.HashMap;
import java.util.Random;

public class ChatServer {
  private static PrintWriter logFile;
  static HashMap<String, TurtleInfo> turtleMap = new HashMap<>();
  public static Object turtleFrame;

  public static void main(String[] args) throws IOException {
    // コマンドライン引数でポート番号が指定されているか確認
    if (args.length != 1) {
      System.out.println("起動方法: java ChatServer ポート番号");
      System.out.println("例: java ChatServer 50002");
      System.exit(1); // 引数が指定されていなかった場合、プログラムを終了
    }

    // ポート番号を引数から取得
    int port = Integer.valueOf(args[0]).intValue();
    ServerSocket serverS = null; // サーバーソケットの宣言
    boolean end = true; // サーバーを実行するかどうかのフラグ

    // 指定されたポート番号でサーバーソケットを作成
    // サーバーソケットはクライアントからの接続を待ち受けるためのもので、2つのパソコンで通信を可能にする
    try {
      // 授業内で追加、ログファイル作成
      String time = LocalDateTime.now().toString();
      logFile = new PrintWriter(new FileWriter("ChatServerLog-" + time + ".txt"));
      serverS = new ServerSocket(port);
    } catch (IOException e) {
      // ポートが使用できない場合のエラーメッセージ
      System.out.println("ポートにアクセスできません");
      System.exit(-1); // エラーが発生した場合プログラムを終了
    }

    // クライアントの接続を待ち続けるループ
    // サーバーが動作し続ける限り、クライアントからの新しい接続を受け付ける
    while (end) {
      // 新しいクライアント接続ごとに新しいスレッドを開始
      // 各クライアント接続を個別のスレッドで処理することで、複数のクライアントからの同時接続を可能にする
      new ChatMThread(serverS.accept()).start();
    }
    serverS.close(); // サーバーソケットを閉じる
    logFile.close(); // 追加したので、閉じる
  }

  // コンソールに情報を表示し、ログファイルに書き込み
  static synchronized void writeLog(String s) {
    System.out.println(s);
    logFile.println(s);
    logFile.flush();
  }

  // HashMapに新しいTurtleInfoを追加
  static synchronized TurtleInfo generateTurtle(String id, String name) {
    Random rand = new Random();
    double x = rand.nextDouble() * 300.0 + 60.0;
    double y = rand.nextDouble() * 300.0 + 60.0;
    double a = 90.0;
    double e = 10000.0;
    TurtleInfo turtle = new TurtleInfo(id, name, x, y, a, e);
    turtleMap.put(id, turtle);
    return turtle;
  }

  // IDからタートルを取得するメソッド
  static synchronized TurtleInfo getTurtle(String id) {
    return turtleMap.get(id);
  }
}

class ChatMThread extends Thread {
  Socket socket; // クライアントとの接続用ソケット
  PrintWriter out; // クライアントへの出力ストリーム
  BufferedReader in; // クライアントからの入力ストリーム
  static ArrayList<ChatMThread> member; // 接続されている全クライアントのリスト

  // コンストラクタ（クライアント接続ごとに新しいスレッドを作成）
  // クライアントごとに個別のスレッドを作成し、並行処理を行う
  ChatMThread(Socket s) {
    super("ChatMThread");
    socket = s;

    // クライアントリストが初期化されていない場合は初期化
    // 初回接続時にクライアントリストを作成し、以降の接続をリストに追加
    if (member == null) {
      member = new ArrayList<ChatMThread>();
    }
    member.add(this); // 新しいクライアントをリストに追加
  }

  // スレッドのメイン処理
  public void run() {
    try {
      // ソケットの入出力ストリームを初期化
      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      String fromClient;

      // クライアント接続のログ記録
      String clientInfo = socket.getRemoteSocketAddress().toString().substring(1);
      String connectMessage = LocalDateTime.now().toString() + " に " + clientInfo + " と接続しました";
      ChatServer.writeLog(connectMessage);

      // クライアントからのメッセージを待ち受けるループ
      // クライアントからのメッセージを受け取り、そのメッセージを他の全クライアントに送信する
      while ((fromClient = in.readLine()) != null) {
        // 授業内で追加、メッセージをコンソールに表示するため
        String logMessage = LocalDateTime.now().toString() + " " + clientInfo + ": " + fromClient;
        ChatServer.writeLog(logMessage);

        if (fromClient.startsWith("connect ")) {
          String userName = fromClient.split(" ")[1];
          TurtleInfo turtle = ChatServer.generateTurtle(clientInfo, userName);
          String turtleData = "generate " + turtle.id + " " + turtle.name + " " + turtle.x + " " + turtle.y + " "
              + turtle.a + " " + turtle.e;

          // 接続時にさっきまでのタートル情報を送信
          for (TurtleInfo turtles : ChatServer.turtleMap.values()) {
            String turtleData2 = "generate " + turtles.id + " " + turtles.name + " " + turtles.x + " " + turtles.y + " "
                + turtles.a + " " + turtles.e;
            out.println(turtleData2);
          }

          // すべての接続されたクライアントにメッセージを送信
          for (ChatMThread client : member) {
            if (client != this) {
              client.out.println(turtleData);
            }
          }
          ChatServer.writeLog(turtleData);
        } else {
          String[] args = fromClient.split(" ");
          Command(args, clientInfo);
        }
      }
    } catch (IOException e) {
      System.out.println("run:" + e);
    }
    end(); // スレッドの終了処理
  }

  private void allSend(String message) {
    for (ChatMThread client : member) {
      client.out.println(message);
    }
  }

  private void Command(String[] args, String clientId) {
    String command = args[0];
    TurtleInfo turtle = ChatServer.getTurtle(clientId);

    if (turtle == null) {
      return;
    }
    switch (command) {
    case "rotate":
      if (args.length < 2)
        return;
      try {
        double d = Double.parseDouble(args[1]);
        d = Math.max(-45.0, Math.min(45.0, d));
        turtle.a = (turtle.a + d) % 360.0;
        if (turtle.a < 0)
          turtle.a += 360.0;
        turtle.e -= Math.abs(d);
        String moveToData = "moveto " + turtle.id + " " + turtle.x + " " + turtle.y + " " + turtle.a + " " + turtle.e;
        allSend(moveToData);
        ChatServer.writeLog(moveToData);

        // エネルギーが10未満になったらタートルを削除
        if (turtle.e < 10) {
          String removeData = "remove " + turtle.id;
          allSend(removeData);
          ChatServer.writeLog(removeData);
          ChatServer.turtleMap.remove(turtle.id);
        }
      } catch (NumberFormatException e) {
        // 数値でない時catchする
      }
      break;

    case "walk":
      if (args.length < 2)
        return;
      try {
        double d = Double.parseDouble(args[1]);
        d = Math.max(-50.0, Math.min(50.0, d));
        double radian = Math.toRadians(turtle.a);
        turtle.x += d * Math.cos(radian);
        turtle.y += d * Math.sin(radian);
        turtle.e -= Math.abs(d);
        String moveToData = "moveto " + turtle.id + " " + turtle.x + " " + turtle.y + " " + turtle.a + " " + turtle.e;
        allSend(moveToData);
        ChatServer.writeLog(moveToData);

        // エネルギーが10未満になったらタートルを削除
        if (turtle.e < 10) {
          String removeData = "remove " + turtle.id;
          allSend(removeData);
          ChatServer.writeLog(removeData);
          ChatServer.turtleMap.remove(turtle.id);
        }
      } catch (NumberFormatException e) {
        // 数値でない時catchする
      }
      break;

    case "attack":
      if (args.length < 2) {
        return;
      }
      try {
        // 攻撃範囲を取得
        double n = Double.parseDouble(args[1]);
        n = Math.max(0.0, Math.min(50.0, n));
        if (n > 0) {
          // 攻撃者の情報を取得
          TurtleInfo attacker = ChatServer.getTurtle(clientId);
          double NearbyTurtle = 10000.0;
          TurtleInfo target = null;

          double startX = attacker.x;
          double startY = attacker.y;
          double angle = attacker.a;
          // 攻撃の着地地点
          double endX = startX + n * Math.cos(Math.toRadians(angle));
          double endY = startY + n * Math.sin(Math.toRadians(angle));

          // 攻撃対象を探す
          for (TurtleInfo other : ChatServer.turtleMap.values()) {
            if (!other.id.equals(attacker.id)) { // 自分自身は含めない
              // 三平方の定理で敵との距離を計算
              double dx = startX - other.x;
              double dy = startY - other.y;
              double distance = Math.sqrt(dx * dx + dy * dy);

              // 攻撃の着地地点との距離の計算
              double dx2 = endX - other.x;
              double dy2 = endY - other.y;
              double distance2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

              if (distance <= n && distance2 <= 10.0 && distance < NearbyTurtle) {
                NearbyTurtle = distance;
                target = other;
              }
            }
          }
          String attackData = "attack" + " " + attacker.id + " " + startX + " " + startY + " " + endX + " " + endY;
          allSend(attackData);
          ChatServer.writeLog(attackData);

          // 攻撃結果の処理とエネルギーの通知
          if (target != null) { // 攻撃成功
            target.e -= 2000;
            String damegeData = "moveto " + target.id + " " + target.x + " " + target.y + " " + target.a + " "
                + target.e;
            allSend(damegeData);
            ChatServer.writeLog(damegeData);

            // エネルギーが10未満になったらタートルを削除
            if (target.e < 10.0) {
              String removeData = "remove " + target.id;
              allSend(removeData);
              ChatServer.writeLog(removeData);
              ChatServer.turtleMap.remove(target.id);
            }
          } else { // 攻撃失敗
            attacker.e -= Math.pow(n / 2, 2);
            String misData = "moveto " + attacker.id + " " + attacker.x + " " + attacker.y + " " + attacker.a + " "
                + attacker.e;
            allSend(misData);
            ChatServer.writeLog(misData);

            // エネルギーが10未満になったらタートルを削除
            if (attacker.e < 10) {
              String removeData = "remove " + attacker.id;
              allSend(removeData);
              ChatServer.writeLog(removeData);
              ChatServer.turtleMap.remove(attacker.id);
            }
          }
        }
      } catch (NumberFormatException e) {
        // 数値でない時catchする
      }
      break;

    case "exit":
      TurtleInfo turtle2 = ChatServer.getTurtle(clientId);
      String removeData = "remove " + turtle2.id;
      allSend(removeData);
      ChatServer.turtleMap.remove(turtle2.id);
      ChatServer.writeLog(removeData);
      break;
      
    case "jump":
      TurtleInfo jumpTurtle = ChatServer.getTurtle(clientId);
      try {
        double radian = Math.toRadians(jumpTurtle.a);
        jumpTurtle.x += 150 * Math.cos(radian);
        jumpTurtle.y += 150 * Math.sin(radian);
        jumpTurtle.e -= Math.abs(150);
        String moveToData = "moveto " + jumpTurtle.id + " " + jumpTurtle.x + " " + jumpTurtle.y + " " + jumpTurtle.a + " " + jumpTurtle.e;
        allSend(moveToData);
        ChatServer.writeLog(moveToData);

        // エネルギーが10未満になったらタートルを削除
        if (jumpTurtle.e < 10) {
          removeData = "remove " + jumpTurtle.id;
          allSend(removeData);
          ChatServer.writeLog(removeData);
          ChatServer.turtleMap.remove(jumpTurtle.id);
        }
      } catch (NumberFormatException e) {
        // 数値でない時catchする
      }
      break;
      
    default:
      break;
    }
  }

  // スレッドの終了処理（リソースのクローズとクライアントリストからの削除）
  public void end() {
    try {
      // 入力ストリーム、出力ストリーム、ソケットを閉じる
      in.close();
      out.close();
      socket.close();
    } catch (IOException e) {
      System.out.println("end:" + e);
    }
    member.remove(this); // クライアントをリストから削除

    // タートルの削除とクライアントへの送信
    TurtleInfo turtle = ChatServer.getTurtle(socket.getRemoteSocketAddress().toString().substring(1));
    if (turtle != null) {
      String removeData = "remove " + turtle.id;
      allSend(removeData);
      ChatServer.writeLog(removeData);
      ChatServer.turtleMap.remove(turtle.id); // HashMapからタートルデータを削除
    }
  }
}
