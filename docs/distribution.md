# Windows配布版

## 利用者

ZIPを展開し、`HanasakiAdvisor/HanasakiAdvisor.exe`をダブルクリックします。
詳細は同梱のREAD-ME.txt（元ファイルはdistribution-user-guide.txt）を参照してください。
EXE単体では動作しません。フォルダー全体を配布します。

## 開発者のビルド

Windows x64、JDK 21以上（jpackage付き）、Node.js/npm、Mavenを用意します。
初回や依存更新時のビルドにはインターネット接続が必要です。
受け取る利用者には、これらの導入は不要です。
同梱するJDKの配布ライセンスを確認し、runtime内のlegal等を削除しないでください。

プロジェクト直下のPowerShellから実行します。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\build-distribution.ps1
```

JAVA_HOMEが未設定、または別のJDKを使う場合は指定します。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\build-distribution.ps1 -JdkHome "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot" -Version 1.0.0
```

出力は`dist/HanasakiAdvisor-1.0.0-windows.zip`です。
同じバージョンのZIPとdist内の作業用フォルダーは再ビルドで置換されます。
フロントの依存インストールとビルドはdist/frontend-buildで行うため、開発用node_modulesは変更しません。
配布EXEが起動中の場合は、Windowsのファイルロックを避けるため終了してからビルドしてください。
署名・インストーラー・自動更新は含みません。署名のないEXEはWindowsや組織のポリシーで警告・制限される場合があります。

## 構成

- distribution Mavenプロファイルがfrontend/distをJARのstaticに同梱します。
- 通常の開発では従来通りバックエンド8080とVite5173を起動します。APIはViteのプロキシ経由です。
- 配布EXEは同梱JavaでSpring Bootを起動し、同じJVM内のSwingウィンドウで起動・終了を管理します。
- ポートはOSが自動選択し、127.0.0.1にのみバインドします。画面のHTTP 200を確認してからブラウザを開きます。
- 配布版はclasspath内の設定と学校マスターを明示指定します。開発用local設定はJARに含めません。
- 同一Windowsユーザーにつき一つのアプリをファイルロックで保護します。異常終了後もOSがロックを解放します。
- ログは%LOCALAPPDATA%/HanasakiAdvisor/application.logです。成績CSVは保存しません。
- 起動ウィンドウを閉じるとSpringを終了します。ブラウザのタブは自動では閉じません。

## 配布前チェック

1. ビルドの自動テストが成功すること。
2. 展開したEXEから起動し、CSV取り込み・個人帳票・一覧帳票・印刷を確認すること。
3. 二重起動を拒否し、終了後に再起動できること。
4. 起動直後の終了や、ブラウザを閉じた後の「画面を開く」も確認すること。
5. 日本語・空白を含むフォルダー、書き込み権限がない配布フォルダーでも確認すること。
6. Java、Node.js、Maven、PostgreSQLがない別のWindows x64 PCで、オフライン起動から終了まで確認すること。
7. フォント・プリンター差による帳票の改ページ・文字欠けを確認すること。

別PCでの最終確認は開発PCでのテストだけでは代替できません。

## 帳票のブラウザ自動検証（開発者向け）

PlaywrightとMicrosoft Edgeを使用できる場合、配布EXEのURLを指定して既存の検証スクリプトを実行できます。
配布版のポートは毎回変わります。開いたブラウザのアドレス欄からURLを確認してください。

```powershell
$env:ADVISOR_TEST_FRONTEND_URL = 'http://127.0.0.1:実際のポート'
$env:ADVISOR_TEST_BACKEND_URL = $env:ADVISOR_TEST_FRONTEND_URL
# Playwrightが通常のnode_modulesにない場合は、そのモジュールの絶対パスを指定します。
# $env:PLAYWRIGHT_MODULE = 'C:\...\node_modules\playwright'
node .\tools\db-removal\browser-check.cjs
```

PDFとスクリーンショットはbackend/target/db-removal-browserに出力されます。

## 実装時の確認結果（2026-10-24）

- 配布ビルドの自動テスト20件が成功。DB廃止前との58ケースの比較も含みます。
- 最終ZIPを日本語・空白を含むフォルダーに展開し、JAVA_HOMEなし・システムのみのPATHで起動成功。
- 127.0.0.1だけで待ち受けることを確認。
- 最終ZIPから起動したアプリで一覧6ページ・個人26ページ・3教科1ページの帳票検証が成功。外部リクエストなし、はみ出しなし。
- 起動ウィンドウを閉じるとバックエンドが停止し、再起動できることを確認。起動直後の終了も確認。
- ZIP内206ファイルが生成フォルダーと一致。Javaのライセンス類102ファイルを含みます。
- 開発環境のない別PC・実プリンター・読み取り専用の配置先での確認は未実施です。PATHを外したテストはクリーンPCでの実機テストの代替ではありません。
