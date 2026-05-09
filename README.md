## 構成

- `backend`: Spring Boot API
- `frontend`: React + Vite フロントエンド

## できること

- 3教科偏差値 / 5教科偏差値 / 志望校名を入力
- 志望校判定を表示
- 現在の学力帯に合うおすすめ高校を表示

## 前提

- 公立高校は 5 教科偏差値で判定
- 私立高校は学校ごとの `scoreType` に従って 3 教科または 5 教科偏差値で判定
- 高校データはサンプルのインメモリ実装

## 起動方法

### backend

1. JDK 17 以上を用意
2. `backend` ディレクトリで起動

```bash
./mvnw spring-boot:run
```

Windows なら:

```powershell
.\mvnw.cmd spring-boot:run
```

`mvnw` を置いていないため、Maven がある環境では以下でも起動できます。

```bash
mvn spring-boot:run
```

### frontend

1. Node.js 20 以上を用意
2. `frontend` ディレクトリで依存関係を入れて起動

```bash
npm install
npm run dev
```

## API

`POST /api/judgements`

リクエスト例:

```json
{
  "threeSubjectDeviation": 61,
  "fiveSubjectDeviation": 64,
  "desiredSchoolName": "浦和西高校"
}
```

レスポンス例:

```json
{
  "desiredSchool": {
    "name": "浦和西高校",
    "department": "普通科",
    "schoolCategory": "PUBLIC",
    "scoreType": "FIVE_SUBJECT",
    "deviationValue": 67,
    "area": "さいたま市"
  },
  "judgement": "C",
  "usedScoreType": "FIVE_SUBJECT",
  "studentDeviationValue": 64,
  "targetDeviationValue": 67,
  "difference": -3,
  "recommendedSchools": [
    {
      "name": "川口北高校",
      "department": "普通科",
      "schoolCategory": "PUBLIC",
      "scoreType": "FIVE_SUBJECT",
      "deviationValue": 63,
      "area": "川口市",
      "recommendationType": "SAFETY"
    }
  ]
}
```

## 今後の拡張

- DB 化して高校マスタを管理
- 地域や公立/私立で絞り込み
- 判定ロジックに内申点や学校選択問題実施校を反映
- 管理画面から学校偏差値や推薦条件を更新
