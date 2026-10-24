import {useId, useRef} from 'react';
import './csv-import.css';

export default function CsvImportForm({multiple = false, files, loading, error, canPrint = false, onFilesChange, onSubmit}) {
  const inputId = useId();
  const helpId = useId();
  const fileInput = useRef(null);

  return <section className={`csv-import ${multiple ? 'history-controls' : 'form-panel'}`}>
    <h2 className="csv-import-heading">{multiple ? '成績一覧' : '個別帳票'} CSV入力</h2>
    <form onSubmit={onSubmit} className={`csv-import-form${multiple ? '' : ' score-form'}`}>
      <div className="csv-import-row">
        <label htmlFor={inputId}>入力ファイル名</label>
        <div className="csv-import-control">
          <div className="csv-file-picker">
          <input ref={fileInput} id={inputId} className="csv-import-file" type="file" accept=".csv,text/csv" hidden
            multiple={multiple} required disabled={loading} aria-describedby={helpId}
            onChange={event => onFilesChange(Array.from(event.target.files ?? []))}/>
          <button type="button" className="csv-file-button" disabled={loading}
            onClick={() => fileInput.current.click()}>ファイルを選択</button>
          <span className="csv-file-summary" aria-live="polite">
            {files.length ? files.map(file => file.name).join('、') : 'ファイルが選択されていません'}
          </span>
          </div>
          <p id={helpId} className="csv-import-help">
            {multiple ? '※各回のCSVファイルをまとめて選択してください（第1回〜第7回）。' : '※CSVファイルを1つ選択してください。'}
          </p>
        </div>
      </div>
      <div className="csv-import-actions">
        <button type="submit" className="csv-action csv-confirm" disabled={loading || !files.length}>
          {loading ? '確認中…' : '確認'}
        </button>
        <button type="button" className="csv-action csv-print" disabled={loading || !canPrint}
          onClick={() => window.print()}>印刷</button>
      </div>
    </form>
    <p className="csv-import-note">成績はアプリ内に保存されません。元のCSVは手元に保管してください。</p>
    {loading && <p role="status" className="csv-import-status">CSVを取り込んでいます。しばらくお待ちください。</p>}
    {error && <div role="alert" className="error-box">{error}</div>}
  </section>;
}
