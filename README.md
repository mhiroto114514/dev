# README

## 1. このシステムで何を動かすか

このプロジェクトは 3 つを同時に使います。

1. PostgreSQL（データベース）
2. Spring Boot（backend API）
3. React + Vite（frontend 画面）

起動の順番は次です。

1. PostgreSQL を起動
2. backend を起動（`http://localhost:8080`）
3. frontend を起動（`http://localhost:5173`）

## 2. プロジェクトを開く

エクスプローラーで、ダウンロードして展開した `saitama-school-advisor` フォルダーを開きます。
フォルダー内の何もない場所を右クリックし、「ターミナルで開く」を選んでください。
ターミナルが開いたら、PowerShell の画面で次を実行します。

```powershell
Get-Location
```

表示された場所の末尾が `saitama-school-advisor` なら準備完了です。以降の手順で `cd backend` や `cd frontend`
を実行するときも、このプロジェクトフォルダーから始めてください。

## 3. PostgreSQL の準備

### 3-1. PostgreSQL を起動

PostgreSQL がサービス起動でない場合は、先に起動してください。  
（例: Windows の「サービス」画面で PostgreSQL を開始）

```powershell
pg_ctl -D "C:\Program Files\PostgreSQL\18\data" start
```

### 3-3. backend の接続先を確認

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/school_advisor
spring.datasource.username=postgres
spring.datasource.password=自分で設定したパスワード
```

ポイント:

1. ポートが 5433 の人は URL の `5432` を `5433` に変更
2. ユーザー名・パスワードは PostgreSQL 側に合わせる

### 3-4. CSVファイルの列

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

別ターミナルで実行します。

```Git Bash
cd backend
mvn spring-boot:run
```

起動成功の目安:

1. `Tomcat initialized with port 8080`
2. `Started AdvisorApplication` が表示される

## 5. frontend 起動

さらに別ターミナルで実行します。

```Git Bash
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
2. `http://localhost:8080/api/courses` をブラウザで開いて JSON が返るか

### 6-2. `Port 8080 was already in use`

原因: 8080 を他プロセスが使用中。  
対処: 8080 使用中のプロセスを停止するか、backend 側ポートを変更。

### 6-3. DB 接続エラー（認証失敗 / 接続拒否）

見直し箇所:

1. PostgreSQL が起動しているか
2. `application-local.properties` の URL・ユーザー名・パスワード（環境変数 `DB_URL`・`DB_USERNAME`・`DB_PASSWORD`
   がある場合はそちらが優先）
3. ポート番号（5432 / 5433）

### 6-4. 文字化けする

以下を確認してください。

1. ファイルエンコーディングを UTF-8 にする
2. IDE（IntelliJ 等）の Project Encoding を UTF-8 にする
3. `.editorconfig` が有効になっているか確認する

## 7. 停止方法

backend / frontend の各ターミナルで `Ctrl + C` を押します。
