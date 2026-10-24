# README

配布用EXEの作成・利用方法は [docs/distribution.md](docs/distribution.md) を参照してください。
以下は開発環境での起動手順です。

## 1. このシステムで何を動かすか

CSVから判定と帳票を作成します。必要ソフトの導入は [first-time-setup.md](first-time-setup.md) を参照してください。
成績は元CSV、学校情報は同梱のマスタCSVで管理します。PostgreSQLの準備は不要です。

1. Spring Boot（backend API）
2. React + Vite（frontend 画面）

起動の順番は次です。

1. backend を起動（`http://localhost:8080`）
2. frontend を起動（`http://localhost:5173`）

## 2. プロジェクトを開く

エクスプローラーで、ダウンロードして展開した `saitama-school-advisor` フォルダーを開きます。
フォルダー内の何もない場所を右クリックし、「ターミナルで開く」を選んでください。
ターミナルが開いたら、PowerShell の画面で次を実行します。

```powershell
Get-Location
```

表示された場所の末尾が `saitama-school-advisor` なら準備完了です。以降の手順で `cd backend` や `cd frontend`
を実行するときも、このプロジェクトフォルダーから始めてください。

## 3. CSVファイルの列

CSVの先頭行には列名を入れます。以下は、現在の全22列を含むヘッダーです。

```csv
name,student_id,times,japanese,math,english,science,socialstudies,deviation_japanese,deviation_math,deviation_english,deviation_science,deviation_socialstudies,deviation_three,deviation_five,saitama_deviation_three,saitama_deviation_five,first_choice,second_choice,third_choice,fourth_choice,fifth_choice
```

【生徒・テスト情報】

- name：生徒氏名（必須）
- student_id：生徒コード（必須）
- times：テスト回数（必須。1〜7で入力。）

【得点】

- japanese：国語の得点（必須）
- math：数学の得点（必須）
- english：英語の得点（必須）
- science：理科の得点（任意。3教科受験なら空欄可）
- socialstudies：社会の得点（任意。3教科受験なら空欄可）

【偏差値】

- deviation_japanese：国語の偏差値（必須）
- deviation_math：数学の偏差値（必須）
- deviation_english：英語の偏差値（必須）
- deviation_science：理科の偏差値（任意。未受験・未算出なら空欄可）
- deviation_socialstudies：社会の偏差値（任意。未受験・未算出なら空欄可）
- deviation_three：3教科偏差値（必須）
- deviation_five：5教科偏差値（任意。未受験・未算出なら空欄可）
- saitama_deviation_three：埼玉県基準の3教科偏差値（任意）
- saitama_deviation_five：埼玉県基準の5教科偏差値（任意）

【志望校】

- first_choice：第1志望のコースID（列は必須。値は空欄可）
- second_choice：第2志望のコースID（列は必須。値は空欄可）
- third_choice：第3志望のコースID（列は必須。値は空欄可）
- fourth_choice：第4志望のコースID（列・値とも任意）
- fifth_choice：第5志望のコースID（列・値とも任意）

## 4. backend 起動

プロジェクトフォルダーで開いたPowerShellから実行します。DBや接続パスワードは不要です。

```powershell
cd backend
mvn spring-boot:run
```

起動成功の目安:

1. `Tomcat initialized with port 8080`
2. `Started AdvisorApplication` が表示される

## 5. frontend 起動

プロジェクトフォルダーで、もう1つPowerShellを開いて実行します。

```powershell
cd frontend
npm install
npm run dev
```

起動後、ブラウザで以下を開きます。

- `http://localhost:5173`

## 6. よくあるエラーと対処

### 6-1. `Failed to fetch`

原因のほとんどは backend 未起動です。

確認:

1. `backend` ターミナルが落ちていないか
2. backendのログに `Started AdvisorApplication` が表示されているか
3. ブラウザの開発者ツールの「ネットワーク」で、CSVの送信先 `http://localhost:8080/api/judgements/csv-import` または `csv-preview` に接続できているか

### 6-2. `Port 8080 was already in use`

原因: 8080 を他プロセスが使用中。  
対処: 8080 使用中のプロセスを停止するか、backend 側ポートを変更。

### 6-3. 学校マスタの読込エラー

`backend/src/main/resources/school-master.csv` の列名・IDの重複・数値・区分を確認してください。
エラーにはファイルと行番号が表示されます。マスタに誤りがある場合は起動を停止します。

### 6-4. 文字化けする

以下を確認してください。

1. ファイルエンコーディングを UTF-8 にする
2. IDE（IntelliJ 等）の Project Encoding を UTF-8 にする
3. `.editorconfig` が有効になっているか確認する

## 7. 停止方法

backend / frontend の各ターミナルで `Ctrl + C` を押します。

## 8. データの保管と学校マスタの更新

成績はアプリ内に保存されません。単票・一覧ともCSVの生徒コード・氏名・成績を使います。元CSVは削除せず保管してください。

学校マスタは `backend/src/main/resources/school-master.csv` です。列は `id,name,deviation,school_category` で、初期474件は旧 `data.sql` の記載順をIDとして移行しています。
区分は `PUBLIC`（公立）、`PRIVATE`（私立）、`NATIONAL`（国立）、`KOSEN`（高専）です。
既存IDの変更・再利用・振り直しは、過去の成績CSVの志望校を変えてしまうため行わないでください。新しい学校は未使用のIDで追加します。

マスタを変更したらbackendを再起動します。JAR配布時は再ビルドするか、次のように外部マスタを明示して起動できます。

```powershell
java -jar backend/target/saitama-school-advisor-0.0.1-SNAPSHOT.jar --advisor.school-master=file:C:/SchoolAdvisor/school-master.csv
```

外部マスタもUTF-8で保存してください。偏差値は小数第1位に丸めます。マスタは起動時に一括読込し、実行中には切り替わりません。
過去と同じ判定を再現するには、元CSVと使用したマスタ・アプリのバージョンもセットで保管してください。
学校IDの確認には `school_id_mapping.csv`、区分別CSV、`school_id_mapping.xlsx` を利用できます。これらは参照用で、実際の判定にはマスタCSVを使います。

DB版との比較範囲・検証方法・意図した変更は [docs/db-removal.md](docs/db-removal.md) に記載しています。
