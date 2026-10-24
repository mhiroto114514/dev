# DB廃止の移行記録

## 移行データ

基準コードは main の `e12b5b0`。ユーザー指定に従い、実DBではなく同リビジョンの `backend/src/main/resources/data.sql` を正とした。
VALUES記載順を旧serial IDとして1〜474を明示した `school-master.csv` へ変換した。
公立247・私立218・国立6・高専3件。旧469件のID対応表も同じマスタから更新する。

元SQLは先頭にTRUNCATEと末尾に余分なカンマを含む。通常のDBへは実行していない。
比較時だけ空の専用PostgreSQLへ投入し、末尾のカンマのみ除去した。
`backend/src/test/resources/legacy-data.sql` は元ファイルそのものを残したテスト専用資料で、実行用JARには入らない。
移行後の更新元は `backend/src/main/resources/school-master.csv`。JSONマスタやDBへの自動フォールバックはない。

## 意図して変わる動作

- 生徒登録・成績保存は行わない。元のCSVをユーザーが保管する。
- 未使用の生徒検索API `/api/students/{studentCode}` を廃止する。
- DBの認証失敗・接続失敗・保存エラーは発生しない。
- 単票で旧NUMERIC(4,1)を超える偏差値は、DBエラーの代わりに400と説明を返す。一覧は従来どおり保存制約を適用しない。
- DBの生徒名VARCHAR制限や「登録済みの氏名を更新しない」という保存上の動作は廃止する。帳票は従来もCSVの氏名を使用していた。
- 学校マスタは起動時に検証する。不正な区分・重複ID・壊れたCSV・読込不能は起動エラーにし、不完全なマスタで判定しない。
- application-local.properties はビルド成果物へ同梱しない。外部マスタは `--advisor.school-master=file:...` で指定する。

実DBとdata.sqlの内容が異なる場合、実DB基準の判定一致は対象外。正と指定されたdata.sqlとの一致を検証している。
既存の実DBとPostgreSQLサービスは削除・変更していない。必要な旧成績が元CSVにない場合は、別途退避が必要。

## 維持する動作

CSVの22列・旧社会列名・空欄・0点・小数処理・志望校最大5件と空き順位、各APIの返却形式を維持する。
単票は志望校1件以上が必要、一覧はゼロ件可。一覧の同一内容の重複排除、氏名不一致・成績衝突、1〜7回の制限、並び順も維持する。
単票CSVには以前から一覧と同じ回数上限チェックやJSON APIのBean Validationはかかっていないため、この移行では追加していない。
取込途中で失敗した場合、途中までの帳票を成功結果として返さない。取込ごとの生徒・成績の状態は保持しない。

学校名はコース込みの旧名をそのまま使用し、内部コードschool-ID/course-ID、普通科、FIVE_SUBJECTの解釈を維持する。
全コースFIVE_SUBJECTであること、偏差値欠損時に通常値・各教科平均などへ切り替える優先順は現行コードの仕様を引き継いだ。
判定差は3以上A、-2以上B、-5以上C、それ未満D。強制D判定は基準コードにないため追加していない。
印刷用コンポーネント・CSSは変更しない。

## 自動比較

旧コードを専用の実PostgreSQL 18で動かし、APIが返すデータと400エラー相当の説明を58ケース取得した。
通常利用中のDBには接続していない。比較DBはlocalhost:18432のみで、採取後に停止した。
全474校について9つの判定差（-5.1/-5/-4.9/-2.1/-2/-1.9/2.9/3/3.1）を単票と一覧の両方で比較する。
ほかに学校一覧・コース一覧、理社なし、0点、偏差値の小数丸め、埼玉値・通常値・平均への切替、BOM、旧CSV、空き志望順位、ゼロ志望、重複ID、不明ID、空ファイル、列欠落、数値エラー、範囲外の回数、複数CSVの順序・重複・衝突・氏名不一致を含む。

固定した応答のSHA-256は `backend/src/test/resources/db-removal-baseline.json`。
JSONオブジェクトのキー順だけを正規化し、配列順・数値・null・文字列・エラー内容は比較する。
`DatabaseRemovalParityTest` が新実装の58ケースと照合する。実際の新応答はbackend/target/db-removal-responsesへ出力する。
`SchoolRepositoryTest` は旧SQL全474行とCSVのID・学校名・区分・目標偏差値を独立に照合する。
`DatabaseFreeApplicationTest` は接続不能なDB設定を与えてSpring Bootを起動し、DataSource・JDBC・PostgreSQL/H2ドライバーがないこと、HTTPエンドポイントの応答を確認する。

```powershell
cd backend
mvn clean test
```

基準値を新版の結果から作り直してはならない。判定仕様やマスタ更新による差は理由を確認し、別の変更として記録する。

## 旧版での基準値再取得

1. `git archive e12b5b0` などで旧コードを別の検証フォルダーへ展開する。
2. 現ブランチの `DatabaseRemovalCases.java` と `tools/db-removal/LegacyDatabaseBaselineTest.java` を、その旧コードのbackend/src/test/java/com/example/advisor/judgementへコピーする。
3. 空の検証用PostgreSQLクラスタをユーザーadvisor_test、ローカルポート18432で起動する。普段使用しているDBは使わない。
4. 旧コード側のbackendで次を実行する。

```powershell
mvn "-Dtest=LegacyDatabaseBaselineTest" "-Dbaseline.jdbc.url=jdbc:postgresql://127.0.0.1:18432/postgres" test
```

生成されたdb-removal-baseline.jsonのcasesと現在の基準値を比較する。テストは専用クラスタのschool/student/resultを初期化する。
採取後は専用PostgreSQLを停止する。DBが必要なのはこの歴史的基準の再取得だけで、通常ビルド・テスト・運用には不要。

旧SQLからのCSV再変換は次のコマンドで行える。

```powershell
node tools/db-removal/export-school-master.mjs backend/src/test/resources/legacy-data.sql backend/target/school-master-regenerated.csv
```

## 移行後の運用

元CSV・マスタCSV・アプリ版をセットで保管する。マスタのIDを再利用しない。
起動中はマスタが固定されるため、更新は再起動で反映する。
DB版へ戻す場合は旧リビジョンと元のDBを使用する。この版で読み込んだCSVはDBへ保存されないため、必要なら元CSVを再取込する。
画面同梱・実行環境同梱・EXE化は別工程であり、現在はbackendとfrontendの起動が必要。

## 実施した検証

- Mavenの自動テスト17件を実行。旧PostgreSQL版の58ケースと応答ハッシュがすべて一致。
- 全474件のID・名前・区分・偏差値を元SQLと照合。
- 接続不能なDB設定でも実行用JARのHTTPサーバーが起動し、実CSVを処理できることを確認。
- JAR内に学校マスタCSVを含み、PostgreSQL・H2・HikariCP・JDBCライブラリ、旧SQL、application-local.propertiesを含まないことを確認。
- frontendの本番ビルド成功。
- 実APIへ送信するブラウザーテストで、26人の一覧6ページ（5/5/5/5/5/1人）、単票26ページ、3教科の単票1ページをPDF出力。末尾生徒の出力、A4の向き、ページ枠のはみ出しなしを確認。
- 学校ID参照Excelの全体474件・公立247件・私立218件・国立6件・高専3件を確認。変更箇所を画像化して確認。

ブラウザーテストは `tools/db-removal/browser-check.cjs`。PlaywrightとEdgeを使う検証専用ツールで、通常運用には不要。
backendを18080、frontendをlocalhost:5173で起動し、Playwrightが利用できるNode環境で実行する。
必要なら `PLAYWRIGHT_MODULE`、`ADVISOR_TEST_BACKEND_URL`、`ADVISOR_TEST_FRONTEND_URL` で検証環境を指定できる。
PDF・画像と新応答の作業ファイルはbackend/target配下に出力し、Gitには追加しない。
