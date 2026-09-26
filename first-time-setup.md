# 必要ソフト（最初にインストール）

以下をインストールしてください。

1. Java（JDK 17 以上。21 推奨）
2. Maven（3.9 以上）
3. Node.js（20 系推奨）
4. PostgreSQL（16 以上。18 でも可）

## 1. ダウンロード先（公式）

1. Java（JDK）: [Eclipse Temurin](https://adoptium.net/temurin/releases/)
2. Maven: [Apache Maven](https://maven.apache.org/download.cgi)
3. Node.js: [Node.js 公式](https://nodejs.org/)
4. PostgreSQL: [PostgreSQL Windows ダウンロード](https://www.postgresql.org/download/windows/)

## 2. インストール手順（Windows）

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

## 3. winget でインストールする手順（おすすめ）

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

## 4. インストール確認

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
