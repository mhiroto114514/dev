import {Fragment, useState} from 'react';
import PrintPages from './PrintPages';

const subjects = ['国', '数', '英', '理', '社'];
const fields = ['japanese', 'math', 'english', 'science', 'socialstudies'];
const deviation = value => value == null ? '—' : Number(value).toFixed(1);

export default function HistoryReport() {
  const [files, setFiles] = useState([]);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    if (!files.length) return;
    setLoading(true);
    setResult(null);
    setError('');
    const body = new FormData();
    files.forEach(file => body.append('files', file));
    try {
      const response = await fetch('http://localhost:8080/api/judgements/csv-preview', {method: 'POST', body});
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'CSVの取り込みに失敗しました。');
      setResult(data);
    } catch (ex) {
      setError(ex.message);
    } finally {
      setLoading(false);
    }
  }

  const students = new Map();
  for (const row of result?.ledgers ?? []) {
    if (!students.has(row.studentCode)) students.set(row.studentCode, {name: row.studentName, rows: new Map()});
    students.get(row.studentCode).rows.set(row.times, row);
  }

  return <main className="history-page">
    <section className="history-controls">
      <h1>全7回の成績一覧</h1>
      <p>各回のCSVをまとめて選択してください。選んだCSVから一覧を作成します。成績は登録されません。</p>
      <form onSubmit={submit}>
        <label>CSVファイル（複数選択可）<input type="file" multiple accept=".csv,text/csv" required
          disabled={loading} onChange={event => setFiles(Array.from(event.target.files ?? []))}/></label>
        <ul>{files.map((file, index) => <li key={index}>{file.name}</li>)}</ul>
        <button disabled={loading || !files.length}>{loading ? '一覧を作成中…' : '成績一覧を作成する'}</button>
      </form>
      <p>第1回〜第7回が対象です。未取込の回は空欄、未受験などの欠損値は「—」で表示します。</p>
      {error && <p role="alert" className="error-box">{error}</p>}
      {result && <button type="button" onClick={() => window.print()}>一覧を印刷する（A4横）</button>}
    </section>
    <div className="report-screen-content">{Array.from(students, ([code, student]) => <HistoryStudent code={code} student={student} key={code}/>)}</div>
    <PrintPages items={Array.from(students)} perPage={5} landscape renderItem={([code, student]) => <HistoryStudent code={code} student={student} key={code}/>}/>
  </main>;
}

function HistoryStudent({code, student}) {
  return <section className="history-student">
      <div className="history-student-heading"><h2>氏名：{student.name}　 ID：{code}</h2></div>
      <div className="history-table-scroll"><table className="history-table" aria-label={`${student.name}さんの全7回の成績`}>
        <colgroup><col style={{width: '4%'}}/>{Array.from({length: 12}, (_, i) => <col key={i} style={{width: '3%'}}/>)}
          {[0, 1, 2, 3, 4].map(i => <Fragment key={i}><col style={{width: '10.5%'}}/><col style={{width: '1.5%'}}/></Fragment>)}
        </colgroup>
        <thead><tr><th rowSpan={2}>回数</th><th colSpan={5}>得点</th><th colSpan={7}>偏差値</th><th colSpan={10}>志望校判定</th></tr>
          <tr>{[...subjects, ...subjects, '3教科', '5教科'].map((s, i) => <th key={i}>{s}</th>)}
            {['第一志望', '第二志望', '第三志望', '第四志望', '第五志望'].map(s => <th key={s} colSpan={2}>{s}</th>)}
          </tr></thead>
        <tbody>{Array.from({length: 7}, (_, i) => {
          const row = student.rows.get(i + 1);
          return <tr key={i}><th scope="row">第{i + 1}回</th>
            {fields.map(f => <td key={f}>{row ? row[`${f}Score`] ?? '—' : ''}</td>)}
            {fields.map(f => <td key={f}>{row ? deviation(row[`${f}Deviation`]) : ''}</td>)}
            <td>{row ? deviation(row.saitamaDeviationThree ?? row.threeSubjectDeviation) : ''}</td>
            <td>{row ? deviation(row.saitamaDeviationFive ?? row.fiveSubjectDeviation) : ''}</td>
            {[0, 1, 2, 3, 4].map(slot => {
              const choice = row?.results[slot];
              const course = choice?.courseName;
              const showCourse = course && course !== choice.schoolName && !course.startsWith(choice.schoolName);
              return <Fragment key={slot}><td className="history-school">{choice ? <>{choice.schoolName}{showCourse && <><br/>{course}</>}</> : row ? '—' : ''}</td>
                <td className="history-grade">{choice?.judgement ?? (row ? '—' : '')}</td></Fragment>;
            })}
          </tr>;
        })}</tbody>
      </table></div>
    </section>;
}
