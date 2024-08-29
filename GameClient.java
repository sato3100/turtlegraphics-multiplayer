
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

import javafx.scene.paint.Color;
import tg.Turtle;
import tg.TurtleFrame;

public class GameClient {
  Socket chatS = null; // サーバーとの接続用ソケット
  BufferedReader in = null; // サーバーからの入力ストリーム
  BufferedReader userInput = null; // キーボードからの入力用
  PrintStream out = null; // サーバーへの出力ストリーム

  static String sName; // サーバIPアドレス
  static int portN; // ポート番号
  static String uName; // ユーザ名

  String userName; // クライアントのユーザ名

  static HashMap<String, Turtle> turtles = new HashMap<>();
  static TurtleFrame turtleFrame = new TurtleFrame();
  static Turtle hidden;

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
  }

  // サーバからメッセージを受信して表示するメソッド
  public void startChat() {
    String fromServer;
    // サーバから来た文字列を、コンソールに出力するだけにする
    try {
      while ((fromServer = in.readLine()) != null) {
        // text.setText(text.getText().concat(fromServer + "\n"));
        if (fromServer.equals("exit")) {
          break;
        }
        System.out.println(fromServer);
        ServerCommand(fromServer);
      }
    } catch (IOException e) {
    }
  }

  // キーボードからの入力を受け付ける
  public void Input() {
    String inputComand = null;
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

  public void ServerCommand(String command) {
    String[] parts = command.split(" ");
    String cmd = parts[0];
    String id = parts[1];

    switch (cmd) {
    case "generate":
      double x = Double.parseDouble(parts[3]);
      double y = 400.0 - Double.parseDouble(parts[4]);
      double a = 90.0 - Double.parseDouble(parts[5]);
      double e = Double.parseDouble(parts[6]) / 10000.0;
      Turtle turtle = new Turtle(x, y, a);
      turtleFrame.add(turtle);
      turtle.tScale = e;
      // 自分のタートルかどうかを判断し、色を設定
      if (id.equals(chatS.getLocalSocketAddress().toString().substring(1))) {
        turtle.setTColor(Color.RED);
      }
      turtles.put(id, turtle);
      turtleFrame.addMesh();
      break;

    case "moveto":
      Turtle moveTurtle = turtles.get(id);
      if (moveTurtle != null) {
        moveTurtle.moveTo(Double.parseDouble(parts[2]), 400.0 - Double.parseDouble(parts[3]),
            90.0 - Double.parseDouble(parts[4]));
        moveTurtle.tScale = Double.parseDouble(parts[5]) / 10000.0;
      }
      break;

    case "remove":
      Turtle removeTurtle = turtles.remove(id);
      if (removeTurtle != null) {
        turtleFrame.remove(removeTurtle);
      }
      break;

    case "attack":
      double startX = Double.parseDouble(parts[2]);
      double startY = 400.0 - Double.parseDouble(parts[3]);
      double endX = Double.parseDouble(parts[4]);
      double endY = 400.0 - Double.parseDouble(parts[5]);
      Turtle attackerTurtle = turtles.get(id);
      if (attackerTurtle != null) {
        double angle = attackerTurtle.getAngle();
        AttackHiden(startX, startY, endX, endY, angle);
      }
      break;

    default:
      break;
    }
  }

  static void AttackHiden(double startX, double startY, double endX, double endY, double angle) {
    hidden = new Turtle(startX, startY, angle);
    turtleFrame.add(hidden);
    hidden.setTColor(Color.WHITE);
    hidden.up();
    hidden.moveTo(startX, startY, angle);
    hidden.down();
    hidden.moveTo(endX, endY, angle);
    turtleFrame.remove(hidden);
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
    new GameClient().start();
  }
}