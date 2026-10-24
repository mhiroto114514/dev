import {useState} from 'react';
import HistoryReport from './HistoryReport';
import './history.css';
import PrintPages from './PrintPages';
import CsvImportForm from './CsvImportForm';

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
  result: '結果',
  failedImport: 'CSVの取り込みに失敗しました。',
  usedScore: '判定使用偏差値',
  targetDev: '目標偏差値',
  judge: '判定',
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
  const [mode, setMode] = useState('single');
  return <>
    <style>{`@media print { @page { size: A4 ${mode === 'history' ? 'landscape' : 'portrait'}; margin: ${mode === 'history' ? '5mm' : '6mm'}; } }`}</style>
    <nav className="report-tabs" aria-label="帳票の選択">
      <button type="button" aria-pressed={mode === 'single'} onClick={() => setMode('single')}>
        個別帳票
      </button>
      <button type="button" aria-pressed={mode === 'history'} onClick={() => setMode('history')}>
        成績一覧
      </button>
    </nav>
    <div hidden={mode !== 'single'}><SingleReport/></div>
    <div hidden={mode !== 'history'}><HistoryReport/></div>
  </>;
}

function SingleReport() {
  const [csvFile, setCsvFile] = useState(null);
  const [importResult, setImportResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const onSubmit = async (event) => {
    event.preventDefault();
    if (loading) return;
    if (!csvFile) {
      setError('CSVファイルを選択してください。');
      return;
    }

    setLoading(true);
    setError('');
    setImportResult(null);

    const formData = new FormData();
    formData.append('file', csvFile);

    try {
      const response = await fetch('/api/judgements/csv-import', {
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
    <div className="page-shell report-workspace">
      <main className="content-grid report-flow">
        <CsvImportForm files={csvFile ? [csvFile] : []} loading={loading} error={error}
          canPrint={Boolean(importResult?.ledgers?.length)}
          onSubmit={onSubmit} onFilesChange={files => {
            setCsvFile(files[0] ?? null);
            setImportResult(null);
            setError('');
          }}/>

        {importResult && <section className="panel result-panel single-report-panel">
          <h2 className="result-title">{text.result}</h2>

            <div className="result-stack report-screen-content">
              {importResult.ledgers.map((ledger, index) => (
                <StudentLedger ledger={ledger} key={`${ledger.studentCode}-${ledger.times}-${index}`}/>
              ))}
            </div>
            <PrintPages items={importResult.ledgers} renderItem={(ledger, index) => <StudentLedger ledger={ledger} key={index}/>}/>
        </section>}
      </main>
    </div>
  );
}

function StudentLedger({ledger}) {
  return (
                <article className="student-ledger" >
                  <div className="result-header">
                    <h2>第{ledger.times}回 学力診断テスト</h2>
                    <p className="result-header-school">{text.schoolName}</p>
                  </div>

                  <div className="ledger-student-head">
                    <span>生徒コード: {ledger.studentCode}</span>
                    <span>氏名: {ledger.studentName || '未登録'}</span>
                  </div>

                  <PrintScoreSummary ledger={ledger}/>

                  <div className="result-stack">
                    {ledger.results.map((entry, resultIndex) => {
                      if (!entry) return null;
                      const shouldShowCourseLine =
                        entry.courseName &&
                        entry.courseName !== entry.schoolName &&
                        !entry.courseName.startsWith(entry.schoolName);
                      return (
                        <div className="school-result-block"
                             key={`${entry.schoolName}-${entry.courseName}-${resultIndex}`}>
                          <div className="judgement-card">
                            <div>
                              <p
                                className="meta">第{resultIndex + 1}志望 ／ {schoolCategoryLabels[entry.schoolCategory] ?? entry.schoolCategory}</p>
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
                      <p>・この結果は、過去の入試得点・模擬試験データを加味しています。</p>
                      <p>・通知表の内容は加味していません。</p>
                    </section>
                  </div>
                </article>
  );
}

function PrintScoreSummary({ledger}) {
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
        <th/>
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

function DeviationAxis({studentValue, targetValue, min, max}) {
  const clamp = (value) => Math.min(max, Math.max(min, value));
  const toPercent = (value) => ((clamp(value) - min) / (max - min)) * 100;

  const ticks = [];
  for (let i = min; i <= max; i += 5) {
    ticks.push(i);
  }

  return (
    <div className="deviation-axis" aria-label="偏差値比較グラフ" style={{paddingBottom: '38px'}}>
      {/* 基準となる100%幅の透明な箱 */}
      <div style={{position: 'relative', width: '100%', height: '4px'}}>

        {/* ① バー本体 */}
        <div className="axis-track" style={{width: '100%', margin: 0}}/>

        {/* ② 生徒と目標の点（縦横の中心を線にピッタリ合わせる） */}
        <div className="axis-marker axis-marker-student" style={{
          left: `${toPercent(studentValue)}%`,
          top: '50%',
          transform: 'translate(-50%, -50%)',
          position: 'absolute'
        }}>
          <span className="axis-dot"/>
        </div>
        <div className="axis-marker axis-marker-target" style={{
          left: `${toPercent(targetValue)}%`,
          top: '50%',
          transform: 'translate(-50%, -50%)',
          position: 'absolute'
        }}>
          <span className="axis-diamond"/>
        </div>

        {/* ③ 目盛りと数字 */}
        {ticks.map((tick) => {
          const isTen = tick % 10 === 0;

          return (
            <div
              key={tick}
              style={{
                position: 'absolute',
                left: `${toPercent(tick)}%`,
                top: '12px', /* バーのすぐ下に配置 */
                transform: 'translateX(-50%)',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                width: '30px' /* 領域を固定して周囲を押し出さないようにする */
              }}
            >
              {/* 目盛りの線 */}
              <div style={{
                width: '1.5px',
                height: isTen ? '6px' : '4px',
                backgroundColor: '#94a3b8',
                marginBottom: '4px'
              }}/>
              {/* 10刻みの数字 */}
              {isTen && (
                <span style={{
                  fontSize: '0.8rem',
                  fontWeight: '700',
                  color: '#64748b',
                  lineHeight: '1'
                }}>
                  {tick}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default App;
