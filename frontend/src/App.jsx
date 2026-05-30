import { useState } from 'react';

const schoolCategoryLabels = {
  PUBLIC: '公立',
  PRIVATE: '私立',
  NATIONAL: '国立',
  KOSEN: '高専'
};

const scoreTypeLabels = {
  THREE_SUBJECT: '3教科',
  FIVE_SUBJECT: '5教科'
};

const text = {
  title: '高校受験 判定システム',
  lead: 'CSVファイルを取り込むと、生徒全員分の判定と帳簿を一括作成します。',
  input: 'CSV入力',
  result: '結果',
  notes: '注意事項',
  noResult: 'CSVを取り込むと、判定結果がここに表示されます。',
  failedImport: 'CSVの取り込みに失敗しました。',
  upload: 'CSVを取り込んで判定する',
  uploading: '取り込み中...',
  usedScore: '判定使用偏差値',
  targetDev: '目標偏差値',
  judge: '判定',
  csvLabel: 'CSVファイル',
  schoolName: '花咲スクール'
};


function toNullableNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function formatDeviation(value) {
  const number = toNullableNumber(value);
  return number === null ? '-' : number.toFixed(1);
}
function App() {
  const [csvFile, setCsvFile] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (event) => {
    event.preventDefault();
    if (!csvFile) {
      setError('CSVファイルを選択してください。');
      return;
    }

    setLoading(true);
    setError('');

    const formData = new FormData();
    formData.append('file', csvFile);

    try {
      const response = await fetch('http://localhost:8080/api/judgements/csv-import', {
        method: 'POST',
        body: formData
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || text.failedImport);
      setImportResult(data);
    } catch (submitError) {
      setImportResult(null);
      setError(submitError.message || text.failedImport);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-shell">
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Saitama Entrance Advisor</p>
          <h1>{text.title}</h1>
          <p className="lead">{text.lead}</p>
        </div>
      </section>

      <main className="content-grid">
        <section className="panel form-panel">
          <h2>{text.input}</h2>
          <form onSubmit={onSubmit} className="score-form">
            <label>
              <span>{text.csvLabel}</span>
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={(event) => setCsvFile(event.target.files?.[0] ?? null)}
                required
              />
            </label>
            {csvFile && <p className="csv-file-name">{csvFile.name}</p>}
            <button type="submit" disabled={loading}>
              {loading ? text.uploading : text.upload}
            </button>
          </form>

          <div className="hint-box">
            <p>{text.notes}</p>
            <ul>
              <li>CSVの必須列: `name`, `student_id`, `times`, 各教科得点/偏差値, 志望校列。</li>
              <li>`student_id` が未登録なら student テーブルへ自動登録します。</li>
              <li>取り込み完了後、この画面から全員分を一括印刷できます。</li>
            </ul>
          </div>
        </section>

        <section className="panel result-panel">
          <h2 className="result-title">{text.result}</h2>

          {error && <div className="error-box">{error}</div>}
          {!importResult && !error && <div className="empty-state">{text.noResult}</div>}

          {importResult && (
            <div className="result-stack">
              <button onClick={() => window.print()} className="print-button" style={{ marginBottom: '20px' }}>
                この結果を印刷する
              </button>

              {importResult.ledgers.map((ledger, index) => (
                <article className="student-ledger" key={`${ledger.studentCode}-${ledger.times}-${index}`}>
                  <div className="result-header">
                    <h2>第{ledger.times}回 学力診断テスト</h2>
                    <p className="result-header-school">{text.schoolName}</p>
                  </div>

                  <div className="ledger-student-head">
                    <span>生徒コード: {ledger.studentCode}</span>
                    <span>氏名: {ledger.studentName || '未登録'}</span>
                  </div>

                  <PrintScoreSummary ledger={ledger} />

                  <div className="result-stack">
                    {ledger.results.map((entry, resultIndex) => {
                      const shouldShowCourseLine =
                        entry.courseName &&
                        entry.courseName !== entry.schoolName &&
                        !entry.courseName.startsWith(entry.schoolName);
                      return (
                        <div className="school-result-block" key={`${entry.schoolName}-${entry.courseName}-${resultIndex}`}>
                          <div className="judgement-card">
                            <div>
                              <p className="meta">{schoolCategoryLabels[entry.schoolCategory] ?? entry.schoolCategory}</p>
                              <h3>{entry.schoolName}</h3>
                              {shouldShowCourseLine && <p className="sub-meta">{entry.courseName}</p>}
                              <p className="sub-meta sub-meta-emphasis">
                                {text.usedScore}: {scoreTypeLabels[entry.usedScoreType]} / {text.targetDev} {formatDeviation(entry.targetDeviationValue)}
                              </p>
                            </div>
                            <div className={`badge badge-${entry.judgement}`}>
                              <span>{entry.judgement}</span>
                              <small>{text.judge}</small>
                            </div>
                          </div>

                          <DeviationAxis
                            studentValue={entry.studentDeviationValue}
                            targetValue={entry.targetDeviationValue}
                            min={30}
                            max={80}
                          />
                        </div>
                      );
                    })}

                    <section className="print-notes">
                      <p>・グラフの見方: 横軸は偏差値です。丸はあなたの偏差値、ひし形は高校の基準偏差値を示します。</p>
                      <p>・この結果は、過去の模擬試験データおよび実際の入試得点、通知表、面接は加味していません。</p>
                    </section>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

function PrintScoreSummary({ ledger }) {
  const displayValue = (value) => (value === null || value === undefined ? '-' : value);

  const japaneseScore = toNullableNumber(ledger.japaneseScore) ?? 0;
  const mathScore = toNullableNumber(ledger.mathScore) ?? 0;
  const englishScore = toNullableNumber(ledger.englishScore) ?? 0;
  const scienceScore = toNullableNumber(ledger.scienceScore);
  const socialstudiesScore = toNullableNumber(ledger.socialstudiesScore);

  const threeSubjectScore = japaneseScore + mathScore + englishScore;
  const fiveSubjectScore = threeSubjectScore + (scienceScore ?? 0) + (socialstudiesScore ?? 0);

  const japaneseDeviation = toNullableNumber(ledger.japaneseDeviation);
  const mathDeviation = toNullableNumber(ledger.mathDeviation);
  const englishDeviation = toNullableNumber(ledger.englishDeviation);
  const scienceDeviation = toNullableNumber(ledger.scienceDeviation);
  const socialstudiesDeviation = toNullableNumber(ledger.socialstudiesDeviation);
  const threeSubjectDeviation = toNullableNumber(ledger.saitamaDeviationThree ?? ledger.threeSubjectDeviation);
  const fiveSubjectDeviation = toNullableNumber(ledger.saitamaDeviationFive ?? ledger.fiveSubjectDeviation);

  const hasFiveSubjects = [
    scienceScore,
    socialstudiesScore,
    scienceDeviation,
    socialstudiesDeviation,
    fiveSubjectDeviation
  ].some((value) => value !== null);

  return (
    <table className="print-score-summary" aria-label="得点と偏差値">
      <thead>
        <tr>
          <th />
          <th>国語</th>
          <th>数学</th>
          <th>英語</th>
          {hasFiveSubjects && (
            <>
              <th>理科</th>
              <th>社会</th>
            </>
          )}
          <th>3教科計</th>
          {hasFiveSubjects && <th>5教科計</th>}
        </tr>
      </thead>
      <tbody>
        <tr>
          <th>得点</th>
          <td>{displayValue(japaneseScore)}</td>
          <td>{displayValue(mathScore)}</td>
          <td>{displayValue(englishScore)}</td>
          {hasFiveSubjects && (
            <>
              <td>{displayValue(scienceScore)}</td>
              <td>{displayValue(socialstudiesScore)}</td>
            </>
          )}
          <td>{displayValue(threeSubjectScore)}</td>
          {hasFiveSubjects && <td>{displayValue(fiveSubjectScore)}</td>}
        </tr>
        <tr>
          <th>偏差値</th>
          <td>{formatDeviation(japaneseDeviation)}</td>
          <td>{formatDeviation(mathDeviation)}</td>
          <td>{formatDeviation(englishDeviation)}</td>
          {hasFiveSubjects && (
            <>
              <td>{formatDeviation(scienceDeviation)}</td>
              <td>{formatDeviation(socialstudiesDeviation)}</td>
            </>
          )}
          <td>{formatDeviation(threeSubjectDeviation)}</td>
          {hasFiveSubjects && <td>{formatDeviation(fiveSubjectDeviation)}</td>}
        </tr>
      </tbody>
    </table>
  );
}

function DeviationAxis({ studentValue, targetValue, min, max }) {
  const clamp = (value) => Math.min(max, Math.max(min, value));
  const toPercent = (value) => ((clamp(value) - min) / (max - min)) * 100;

  return (
    <div className="deviation-axis" aria-label="偏差値比較グラフ">
      <div className="axis-track" />
      <div className="axis-marker axis-marker-student" style={{ left: `${toPercent(studentValue)}%` }}>
        <span className="axis-dot" />
      </div>
      <div className="axis-marker axis-marker-target" style={{ left: `${toPercent(targetValue)}%` }}>
        <span className="axis-diamond" />
      </div>
      <div className="axis-scale">
        <span>{min}</span>
        <span>{max}</span>
      </div>
    </div>
  );
}

export default App;
