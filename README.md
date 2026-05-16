この README は、PC 1台で初期構築から起動確認までできるように書いています。  
対象は **Windows 11** を想定しています。

## 1. このシステムで何を動かすか

このプロジェクトは 3 つを同時に使います。

1. PostgreSQL（データベース）
2. Spring Boot（backend API）
3. React + Vite（frontend 画面）

起動の順番は次です。

1. PostgreSQL を起動
2. backend を起動（`http://localhost:8080`）
3. frontend を起動（`http://localhost:5173`）

## 2. 必要ソフト（最初にインストール）

以下をインストールしてください。

1. Java（JDK 17 以上。21 推奨）
2. Maven（3.9 以上）
3. Node.js（20 系推奨）
4. PostgreSQL（16 以上。18 でも可）

### 2-1. ダウンロード先（公式）

1. Java（JDK）: [Eclipse Temurin](https://adoptium.net/temurin/releases/)
2. Maven: [Apache Maven](https://maven.apache.org/download.cgi)
3. Node.js: [Node.js 公式](https://nodejs.org/)
4. PostgreSQL: [PostgreSQL Windows ダウンロード](https://www.postgresql.org/download/windows/)

### 2-2. インストール手順（Windows）

1. Java（JDK）

- 上のリンクから `JDK 21` の Windows x64 インストーラー（`.msi`）を入れて実行
- 基本はデフォルト設定のままでOK

2. Maven

- 上のリンクから `Binary zip archive` をダウンロード
- 例: `C:\tools\apache-maven-3.9.11` に展開
- Windows の環境変数を設定
- `MAVEN_HOME` = `C:\tools\apache-maven-3.9.11`
- `Path` に `%MAVEN_HOME%\bin` を追加
- 設定後、PowerShell をいったん閉じて開き直す

3. Node.js

- 上のリンクから `LTS` 版の Windows インストーラー（`.msi`）を入れて実行
- 基本はデフォルト設定のままでOK

4. PostgreSQL

- 上のリンクから Windows 用インストーラーを入手して実行
- インストール時に決めた `postgres` ユーザーのパスワードを必ず控える
- ポート番号（`5432` または `5433`）も控える

### 2-3. winget でインストールする手順（おすすめ）

PowerShell を **管理者として実行** し、次を順番に実行してください。

```powershell
winget source reset --force
winget source update
```

続いて、各ソフトをインストールします。

```powershell
winget install -e --id EclipseAdoptium.Temurin.21.JDK
winget install -e --id OpenJS.NodeJS.LTS
winget install -e --id PostgreSQL.PostgreSQL
winget install -e --id Apache.Maven
```

環境によっては ID が変わる場合があるため、失敗したときは次で検索してから入れてください。

```powershell
winget search temurin
winget search nodejs
winget search postgresql
winget search maven
```

### 2-4. インストール確認

インストール後、PowerShell で次を実行して確認します。

```powershell
java -version
mvn -version
node -v
npm -v
psql --version
```

1つでも「コマンドが見つかりません」と出た場合は、そのソフトのインストールが未完了です。

補足:

1. `mvn` が見つからない場合は、Maven の `Path` 設定が不足していることがほとんどです。
2. `psql` が見つからない場合は、PostgreSQL の `bin` フォルダ（例: `C:\Program Files\PostgreSQL\18\bin`）を `Path`
   に追加してください。
3. `winget` が見つからない場合は、Microsoft Store の「アプリ インストーラー」を更新するか、次を実行してください。

```powershell
Add-AppxPackage -RegisterByFamilyName -MainPackage Microsoft.DesktopAppInstaller_8wekyb3d8bbwe
```

## 3. プロジェクトを開く

PowerShell を開き、プロジェクトのフォルダへ移動します。

```powershell
cd C:\DevDrive\saitama-school-advisor
```

## 4. PostgreSQL の準備

### 4-1. PostgreSQL を起動

PostgreSQL がサービス起動でない場合は、先に起動してください。  
（例: Windows の「サービス」画面で PostgreSQL を開始）

```powershell
pg_ctl -D "C:\Program Files\PostgreSQL\18\data" start
```

### 4-2. DB 作成

`psql` でログインし、DB を作成します。

```powershell
psql -U postgres -p 5433
```

`5433` は現在のこのプロジェクト設定に合わせた例です。  
PostgreSQL が 5432 の場合は `-p 5432` にしてください。

`psql` に入ったら次を実行します。

```sql
CREATE
DATABASE school_advisor;
```

作成済みならこの手順はスキップで大丈夫です。

### 4-3. backend の接続先を確認

`backend/src/main/resources/application.properties` を開き、次の 3 項目を自分の環境に合わせます。

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/school_advisor
spring.datasource.username=postgres
spring.datasource.password=match114
```

ポイント:

1. ポートが 5432 の人は URL の `5433` を `5432` に変更
2. ユーザー名・パスワードは PostgreSQL 側に合わせる

## 5. backend 起動

別ターミナルで実行します。

```powershell
cd C:\DevDrive\saitama-school-advisor\backend
mvn spring-boot:run
```

起動成功の目安:

1. `Tomcat initialized with port 8080`
2. `Started AdvisorApplication` が表示される

## 6. frontend 起動

さらに別ターミナルで実行します。

```powershell
cd C:\DevDrive\saitama-school-advisor\frontend
npm install
npm run dev
```

起動後、ブラウザで以下を開きます。

- `http://localhost:5173`

## 7. 動作確認（画面 + DB）

### 7-1. 画面で判定実行

1. 生徒コード、得点、偏差値、志望校を入力
2. 判定ボタンを押す
3. 結果カードが表示されることを確認

### 7-2. DB に保存されたか確認

`psql` で次を実行します。

```sql
SELECT id, student_id, times, first_choice, second_choice, third_choice
FROM result
ORDER BY id DESC LIMIT 10;
```

期待値:

1. 新しい行が追加される
2. `student_id` が「生徒コード（例: 2024001）」になっている

## 8. 重要な仕様（必ず読んでください）

現在の設定では、**backend 起動時に毎回 SQL 初期化はしません**。  
`application.properties` は次の設定です。

```properties
spring.sql.init.mode=${DB_INIT_MODE:never}
```

つまり、通常起動（`DB_INIT_MODE` 未設定）では `schema.sql` / `data.sql` は実行されません。  
過去の判定結果は、再起動しても消えません。

初回だけテーブル作成や初期データ投入をしたい場合は、1回だけ次のように実行します。

```powershell
cd C:\DevDrive\saitama-school-advisor\backend
$env:DB_INIT_MODE="always"
mvn spring-boot:run
```

初期化後は backend を停止し、環境変数を消して通常起動に戻してください。

```powershell
Remove-Item Env:DB_INIT_MODE
```

注意: `data.sql` には次が含まれるため、`DB_INIT_MODE=always` で起動すると既存データは初期化されます。

```sql
TRUNCATE TABLE result, student, school RESTART IDENTITY CASCADE;
```

## 9. よくあるエラーと対処

### 9-1. `Failed to fetch`

原因のほとんどは backend 未起動です。

確認:

1. `backend` ターミナルが落ちていないか
2. `http://localhost:8080/api/courses` をブラウザで開いて JSON が返るか

### 9-2. `Port 8080 was already in use`

原因: 8080 を他プロセスが使用中。  
対処: 8080 使用中のプロセスを停止するか、backend 側ポートを変更。

### 9-3. DB 接続エラー（認証失敗 / 接続拒否）

見直し箇所:

1. PostgreSQL が起動しているか
2. `application.properties` の URL・ユーザー名・パスワード
3. ポート番号（5432 / 5433）

### 9-4. 文字化けする

以下を確認してください。

1. ファイルエンコーディングを UTF-8 にする
2. IDE（IntelliJ 等）の Project Encoding を UTF-8 にする
3. `.editorconfig` が有効になっているか確認する

## 10. API 一覧（簡易）

1. `GET /api/courses`  
   志望校選択用のコース一覧を取得

2. `GET /api/students/{studentCode}`  
   生徒コードから氏名を取得

3. `POST /api/judgements`  
   判定実行 + 判定入力内容を `result` テーブルへ保存

## 11. 停止方法

backend / frontend の各ターミナルで `Ctrl + C` を押します。


