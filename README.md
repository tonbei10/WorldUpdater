# WorldUpdater

----------minecraft server batファイル例----------
@echo off
java -Xms1024M -Xmx4096M -jar minecraft_server.1.13.2.jar

重要 : 「nogui」と「pause」を削除してください。


----------WorldUpdaterの使い方----------
1.ダウンロードしたjarファイルをsever本体のjarファイルとbatファイルがあるフォルダと同じ場所に移動する。
2.jarファイルを起動するためのbatファイルの作成

@echo off
java -jar WorldUpdater-0.1.jar
pause

3.作成したbatファイルから起動


----------共有フォルダの設定----------
マップを保存するDriveの共有フォルダは「リンクを知っている全員が編集可」に設定してください。
