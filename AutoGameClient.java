
// ファイルI/O用のインポート
import java.io.BufferedReader; // サーバーからのデータを読み取るためのクラス
import java.io.IOException; // 入出力処理中の例外を処理するためのクラス
import java.io.InputStreamReader; // バイト入力ストリームを文字入力ストリームに変換するためのクラス
import java.io.PrintStream; // サーバーへデータを送るためのクラス
// ネットワーク用のインポート
import java.net.InetAddress; // IPアドレスを管理するためのクラス
import java.net.Socket; // サーバーと接続するためのクラス
import java.net.UnknownHostException; // ホストが見つからない場合の例外を処理するためのクラス
import java.util.HashMap;

public class AutoGameClient {
  Socket chatS = null; // サーバーとの接続用ソケット
  BufferedReader in = null; // サーバーからの入力ストリーム
  BufferedReader userInput = null; // キーボードからの入力用
  PrintStream out = null; // サーバーへの出力ストリーム

  static String sName; // サーバIPアドレス
  static int portN; // ポート番号
  static String uName; // ユーザ名

  String userName; // クライアントのユーザ名

  HashMap<String, TurtleInfo> turtles = new HashMap<>();

  public void start() {
    // サーバーとの接続を初期化
    initNet(sName, portN, uName);
    // 別スレッドでサーバと接続し、メッセージを受信して表示
    new Thread(() -> {
      startChat(); // チャットを開始
    }).start();

    new Thread(() -> {
      Input();
    }).start();

    new Thread(() -> { // 1秒ごとにautoActionを実行
      while (true) {
        try {
          Thread.sleep(1000);
          autoAction();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  // サーバからメッセージを受信して表示するメソッド
  public void startChat() {
    String fromServer;
    try {
      // サーバから来た文字列を、コンソールに出力するだけにする
      while ((fromServer = in.readLine()) != null) {
        // text.setText(text.getText().concat(fromServer + "\n"));
        System.out.println(fromServer);
        ServerCommand(fromServer);
      }
    } catch (IOException e) {
      System.out.println("チャット中に問題が起こりました。");
      System.exit(1); // エラーが発生した場合はアプリケーションを終了
    }
  }

//サーバーからのメッセージを処理するメソッド
  private void ServerCommand(String message) {
    String[] parts = message.split(" ");
    String cmd = parts[0];
    String id = parts[1];

    switch (cmd) {
    case "generate":
      // TurtleInfo オブジェクトを生成し、HashMapにput
      double x = Double.parseDouble(parts[3]);
      double y = 400.0 - Double.parseDouble(parts[4]);
      double a = 90.0 - Double.parseDouble(parts[5]);
      double e = Double.parseDouble(parts[6]) / 10000.0;
      turtles.put(id, new TurtleInfo(id, "", x, y, a, e));
      break;
    case "moveto":
      // TurtleInfo オブジェクトの位置とエネルギーを更新
      TurtleInfo moveTurtle = turtles.get(id);
      if (moveTurtle != null) {
        moveTurtle.x = Double.parseDouble(parts[2]);
        moveTurtle.y = 400.0 - Double.parseDouble(parts[3]);
        moveTurtle.a = 90.0 - Double.parseDouble(parts[4]);
        moveTurtle.e = Double.parseDouble(parts[5]) / 10000.0;
      }
      break;
    case "remove":
      turtles.remove(id);
      break;

    default:
      break;
    }
  }

  // キーボードからの入力を受け付ける
  public void Input() {
    String inputComand;
    try {
      userInput = new BufferedReader(new InputStreamReader(System.in));
      while ((inputComand = userInput.readLine()) != null) {
        if (inputComand.equals("exit")) {
          sendMessage("exit");
          end();
          break;
        }
        sendMessage(inputComand);
      }
    } catch (IOException e) {
      System.out.println("チャット中に問題が起こりました。");
    }
  }

  // メッセージをサーバに送信するメソッド
  public void sendMessage(String msg) {
//    System.out.println(msg);  // 入力とかぶるため、二重にターミナルに表示されてしまう
    out.println(msg); // メッセージをサーバに送信
  }

  // サーバーへの接続を初期化するメソッド
  public void initNet(String serverName, int port, String uName) {
    userName = uName; // ユーザ名を設定
    try {
      // サーバへの接続を確立
      chatS = new Socket(InetAddress.getByName(serverName), port);
      in = new BufferedReader(new InputStreamReader(chatS.getInputStream()));
      out = new PrintStream(chatS.getOutputStream());
      sendMessage("connect " + uName);// 接続後に、connect userNameを送信
    } catch (UnknownHostException e) {
      System.out.println("ホストに接続できません"); // ホストに接続できない場合のエラーメッセージ
      System.exit(1); // ホストに接続できない場合は終了
    } catch (IOException e) {
      System.out.println("IOコネクションを得られません"); // 入出力ストリームの取得に失敗した場合のエラーメッセージ
      System.exit(1); // 入出力ストリームの取得に失敗した場合は終了
    }
  }

//自動的に動作
  public void autoAction() {
    TurtleInfo myTurtle = turtles.get(chatS.getLocalSocketAddress().toString().substring(1));
    TurtleInfo target = null;
    double nearestDistance = 10000.0;// 上書きするからなんでもいい
    try {
      // 最も近いタートルの情報を探索
      for (TurtleInfo other : turtles.values()) {
        if (!other.equals(myTurtle)) {

          // 三平方の定理で敵との距離を計算
          double dx = myTurtle.x - other.x;
          double dy = myTurtle.y - other.y;
          double distance = Math.sqrt(dx * dx + dy * dy);

          if (distance < nearestDistance) {
            nearestDistance = distance;
            target = other;
          }
        }
      }

      if (myTurtle != null) {
        if (target != null) {
          double attackRange = 50.0; // 攻撃の長さ
          // 敵タートルの方向を計算
          double dx = target.x - myTurtle.x;
          double dy = target.y - myTurtle.y;
          double distance = Math.sqrt(dx * dx + dy * dy);
          
          // 攻撃の着地地点
          double endX = myTurtle.x + attackRange * Math.cos(Math.toRadians(myTurtle.a));
          double endY = myTurtle.y + attackRange * Math.sin(Math.toRadians(myTurtle.a));

          // 攻撃の着地地点と敵の距離
          double dx3 = endX - target.x;
          double dy3 = endY - target.y;
          double distance3 = Math.sqrt(dx3 * dx3 + dy3 * dy3);

          // 余弦定理を使って内角を計算
          double cosAngle = (distance * distance + attackRange * attackRange - distance3 * distance3)
              / (2 * distance * attackRange);
          double rotateAngle = Math.toDegrees(Math.acos(cosAngle)) - 90;
          System.out.println(rotateAngle);

          if (nearestDistance <= attackRange && Math.abs(rotateAngle) <= 5.0) {
            sendMessage("attack " + distance);
          } else if (Math.abs(rotateAngle) <= 5.0) {
            sendMessage("walk " + 10.0);
          } else if (Math.abs(rotateAngle) > 5.0) {
            sendMessage("rotate " + rotateAngle);
          }
        }
      }
    } catch (NullPointerException e) {
      return;
    }
  }

  // 接続を終了するメソッド
  public void end() {
    try {
      // 入出力ストリームとソケットを閉じる
      out.close();
      in.close();
      chatS.close();
      System.exit(0);
    } catch (IOException e) {
      System.out.println("end:" + e);
    }
  }

  // メインメソッド
  public static void main(String... args) {
    // コマンドライン引数の確認
    if (args.length != 3 && args.length != 4) {
      System.out.println("Usage: java ChatClient サーバのIPアドレス ポート番号 ユーザ名");
      System.out.println("例: java ChatClient 210.0.0.1 50002 ariga");
      System.exit(0); // 引数が正しくない場合は終了
    }
    // 引数からサーバ名、ポート番号、ユーザ名を取得
    sName = args[0];
    portN = Integer.valueOf(args[1]).intValue();
    uName = args[2];
    System.out.println("serverName = " + sName);
    System.out.println("portNumber = " + portN);
    System.out.println("userName = " + uName);
    // launch(args); // JavaFXアプリケーションの開始
    new AutoGameClient().start();
  }
}