import { useEffect, useMemo, useState } from 'react';

const initialForm = {
  studentCode: 2024001,
  times: 1,
  japaneseScore: 50,
  mathScore: 50,
  englishScore: 50,
  scienceScore: 50,
  socialscienceScore: 50,
  japaneseDeviation: 50,
  mathDeviation: 50,
  englishDeviation: 50,
  scienceDeviation: 50,
  socialscienceDeviation: 50,
  threeSubjectDeviation: 70,
  fiveSubjectDeviation: 70,
  desiredCourseCodes: ['', '', '']
};

const schoolCategoryLabels = {
  PUBLIC: '公立',
  PRIVATE: '私立'
};

const scoreTypeLabels = {
  THREE_SUBJECT: '3教科',
  FIVE_SUBJECT: '5教科'
};

const text = {
  title: '高校受験 判定システム',
  lead: '志望校を選択すると、偏差値差分をもとに判定を表示します。',
  input: '入力',
  result: '結果',
  three: '3教科偏差値',
  five: '5教科偏差値',
  max3: '公立・私立を問わず最大3校まで選択できます',
  check: '判定する',
  checking: '判定中...',
  notes: '注意事項',
  note1: '志望校は公私合算で最大3校です。',
  note2: '同じコースは重複選択できません。',
  note3: '結果は参考情報です。',
  noResult: '判定結果はここに表示されます。',
  failedCourses: 'コース情報の取得に失敗しました。',
  failedJudgement: '判定に失敗しました。',
  usedScore: '判定使用偏差値',
  targetDev: '目標偏差値',
  judge: '判定',
  studentScore: 'あなたの偏差値',
  courseTarget: 'コース基準偏差値',
  slotSuffix: '志望',
  selectPlaceholder: '学校名・コース名・IDで検索',
  deviationLabel: '偏差値',
  noCourseMatch: '該当するコースがありません。',
  clearCourse: 'コースをクリア'
};

const commentOptions = [
  '順調です。このまま継続しましょう。',
  '基礎を固めるとさらに安定します。',
  '苦手単元の復習を優先しましょう。',
  '過去問演習を増やして実戦力を高めましょう。',
  '志望校との距離を意識して学習計画を見直しましょう。'
];

function App() {
  const [form, setForm] = useState(initialForm);
  const [studentName, setStudentName] = useState('');
  const [courseOptions, setCourseOptions] = useState([]);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [courseLoading, setCourseLoading] = useState(true);
  const [selectedComments, setSelectedComments] = useState({});

  useEffect(() => {
    const loadCourses = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/courses');
        const data = await response.json();
        if (!response.ok) throw new Error(text.failedCourses);
        setCourseOptions([...data.publicCourses, ...data.privateCourses]);
      } catch (loadError) {
        setError(loadError.message);
      } finally {
        setCourseLoading(false);
      }
    };
    loadCourses();
  }, []);

  useEffect(() => {
    const loadStudent = async () => {
      if (!form.studentCode) {
        setStudentName('');
        return;
      }
      try {
        const response = await fetch(`http://localhost:8080/api/students/${form.studentCode}`);
        if (!response.ok) {
          setStudentName('');
          return;
        }
        const data = await response.json();
        setStudentName(data.name ?? '');
      } catch {
        setStudentName('');
      }
    };
    loadStudent();
  }, [form.studentCode]);

  const onScoreChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: value === '' ? '' : Number(value)
    }));
  };

  const onCourseChange = (index, value) => {
    setForm((current) => {
      const nextValues = [...current.desiredCourseCodes];
      nextValues[index] = value;
      return { ...current, desiredCourseCodes: nextValues };
    });
  };

  const onChangeComment = (key, value) => {
    setSelectedComments((current) => ({ ...current, [key]: value }));
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');

    const payload = {
      studentCode: form.studentCode,
      times: form.times,
      japaneseScore: form.japaneseScore,
      mathScore: form.mathScore,
      englishScore: form.englishScore,
      scienceScore: form.scienceScore,
      socialscienceScore: form.socialscienceScore,
      japaneseDeviation: form.japaneseDeviation,
      mathDeviation: form.mathDeviation,
      englishDeviation: form.englishDeviation,
      scienceDeviation: form.scienceDeviation,
      socialscienceDeviation: form.socialscienceDeviation,
      threeSubjectDeviation: form.threeSubjectDeviation,
      fiveSubjectDeviation: form.fiveSubjectDeviation,
      desiredCourseCodes: form.desiredCourseCodes.filter(Boolean)
    };

    try {
      const response = await fetch('http://localhost:8080/api/judgements', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || text.failedJudgement);
      setResult(data);
      setSelectedComments({});
    } catch (submitError) {
      setResult(null);
      setError(submitError.message);
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
            <label><span>生徒コード</span><input type="number" min="1" name="studentCode" value={form.studentCode} onChange={onScoreChange} required /></label>
            <label><span>第何回テストか</span><input type="number" min="1" name="times" value={form.times} onChange={onScoreChange} required /></label>

            <fieldset className="school-group">
              <legend>5教科得点</legend>
              <div className="score-grid">
                <label><span>国語</span><input type="number" min="0" max="100" name="japaneseScore" value={form.japaneseScore} onChange={onScoreChange} required /></label>
                <label><span>数学</span><input type="number" min="0" max="100" name="mathScore" value={form.mathScore} onChange={onScoreChange} required /></label>
                <label><span>英語</span><input type="number" min="0" max="100" name="englishScore" value={form.englishScore} onChange={onScoreChange} required /></label>
                <label><span>理科</span><input type="number" min="0" max="100" name="scienceScore" value={form.scienceScore} onChange={onScoreChange} required /></label>
                <label><span>社会</span><input type="number" min="0" max="100" name="socialscienceScore" value={form.socialscienceScore} onChange={onScoreChange} required /></label>
              </div>
            </fieldset>

            <fieldset className="school-group">
              <legend>5教科偏差値</legend>
              <div className="score-grid">
                <label><span>国語</span><input type="number" min="20" max="90" name="japaneseDeviation" value={form.japaneseDeviation} onChange={onScoreChange} required /></label>
                <label><span>数学</span><input type="number" min="20" max="90" name="mathDeviation" value={form.mathDeviation} onChange={onScoreChange} required /></label>
                <label><span>英語</span><input type="number" min="20" max="90" name="englishDeviation" value={form.englishDeviation} onChange={onScoreChange} required /></label>
                <label><span>理科</span><input type="number" min="20" max="90" name="scienceDeviation" value={form.scienceDeviation} onChange={onScoreChange} required /></label>
                <label><span>社会</span><input type="number" min="20" max="90" name="socialscienceDeviation" value={form.socialscienceDeviation} onChange={onScoreChange} required /></label>
              </div>
            </fieldset>

            <label><span>{text.three}</span><input type="number" min="30" max="80" name="threeSubjectDeviation" value={form.threeSubjectDeviation} onChange={onScoreChange} required /></label>
            <label><span>{text.five}</span><input type="number" min="30" max="80" name="fiveSubjectDeviation" value={form.fiveSubjectDeviation} onChange={onScoreChange} required /></label>

            <CourseSelectGroup description={text.max3} options={courseOptions} values={form.desiredCourseCodes} onChange={onCourseChange} disabled={courseLoading} />

            <button type="submit" disabled={loading || courseLoading}>{loading ? text.checking : text.check}</button>
          </form>

          <div className="hint-box">
            <p>{text.notes}</p>
            <ul>
              <li>{text.note1}</li>
              <li>{text.note2}</li>
              <li>{text.note3}</li>
            </ul>
          </div>
        </section>

        <section className="panel result-panel">
          <div className="result-header">
            <h2>第{form.times}回　学力診断テスト</h2>
            <p className="result-header-school">花咲スクール</p>
          </div>
          <div className="ledger-student-head">
            <span>生徒コード: {form.studentCode}</span>
            <span>氏名: {studentName || '未登録'}</span>
          </div>
          <PrintScoreSummary form={form} />

          {error && <div className="error-box">{error}</div>}
          {!result && !error && <div className="empty-state">{text.noResult}</div>}

          {result && (
            <div className="result-stack">
              <button onClick={() => window.print()} className="print-button" style={{ marginBottom: '20px' }}>この結果を印刷する</button>
              {result.results.map((entry) => {
                const key = `${entry.schoolName}-${entry.courseName}`;
                const shouldShowCourseLine =
                  entry.courseName &&
                  entry.courseName !== entry.schoolName &&
                  !entry.courseName.startsWith(entry.schoolName);
                return (
                  <div className="school-result-block" key={key}>
                    <div className="judgement-card">
                      <div>
                        <p className="meta">{schoolCategoryLabels[entry.schoolCategory]}</p>
                        <h3>{entry.schoolName}</h3>
                        {shouldShowCourseLine && <p className="sub-meta">{entry.courseName}</p>}
                        <p className="sub-meta sub-meta-emphasis">{text.usedScore}: {scoreTypeLabels[entry.usedScoreType]} / {text.targetDev} {entry.targetDeviationValue}</p>
                      </div>
                      <div className={`badge badge-${entry.judgement}`}>
                        <span>{entry.judgement}</span>
                        <small>{text.judge}</small>
                      </div>
                    </div>

                    <DeviationAxis studentValue={entry.studentDeviationValue} targetValue={entry.targetDeviationValue} min={30} max={80} />

                    <div className="comment-select-row">
                      <label>
                        <span>コメント</span>
                        <select value={selectedComments[key] ?? ''} onChange={(event) => onChangeComment(key, event.target.value)}>
                          <option value="">選択してください</option>
                          {commentOptions.map((option) => (
                            <option key={option} value={option}>{option}</option>
                          ))}
                        </select>
                      </label>
                      <p className="print-comment-text">
                        コメント: {selectedComments[key] || '（未選択）'}
                      </p>
                    </div>
                  </div>
                );
              })}

              <section className="print-notes">
                <p>・グラフの見方: 横軸は偏差値です。丸はあなたの偏差値、ひし形は高校の基準偏差値を示します。</p>
                <p>・この結果は、過去の模擬試験データおよび実際の入試得点、通知表、面接は加味していません。</p>
              </section>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

function PrintScoreSummary({ form }) {
  const japaneseScore = Number(form.japaneseScore) || 0;
  const mathScore = Number(form.mathScore) || 0;
  const englishScore = Number(form.englishScore) || 0;
  const scienceScore = Number(form.scienceScore) || 0;
  const socialscienceScore = Number(form.socialscienceScore) || 0;
  const threeSubjectScore = japaneseScore + mathScore + englishScore;
  const fiveSubjectScore = threeSubjectScore + scienceScore + socialscienceScore;

  return (
    <table className="print-score-summary" aria-label="得点と偏差値">
      <thead>
        <tr>
          <th />
          <th>国語</th>
          <th>数学</th>
          <th>英語</th>
          <th>理科</th>
          <th>社会</th>
          <th>3教科計</th>
          <th>5教科計</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <th>得点</th>
          <td>{japaneseScore}</td>
          <td>{mathScore}</td>
          <td>{englishScore}</td>
          <td>{scienceScore}</td>
          <td>{socialscienceScore}</td>
          <td>{threeSubjectScore}</td>
          <td>{fiveSubjectScore}</td>
        </tr>
        <tr>
          <th>偏差値</th>
          <td>{form.japaneseDeviation}</td>
          <td>{form.mathDeviation}</td>
          <td>{form.englishDeviation}</td>
          <td>{form.scienceDeviation}</td>
          <td>{form.socialscienceDeviation}</td>
          <td>{form.threeSubjectDeviation}</td>
          <td>{form.fiveSubjectDeviation}</td>
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

function CourseSelectGroup({ description, options, values, onChange, disabled }) {
  return (
    <fieldset className="school-group" disabled={disabled}>
      <legend>志望校（公私合算）</legend>
      <p className="group-description">{description}</p>
      <div className="school-select-stack">
        {values.map((value, index) => (
          <SearchableCourseField
            key={`desired-${index}`}
            index={index}
            value={value}
            options={options}
            selectedValues={values}
            onChange={(nextValue) => onChange(index, nextValue)}
            disabled={disabled}
          />
        ))}
      </div>
    </fieldset>
  );
}

function SearchableCourseField({ index, value, options, selectedValues, onChange, disabled }) {
  const selectedCourse = useMemo(() => options.find((course) => course.code === value), [options, value]);
  const [query, setQuery] = useState(selectedCourse ? formatCourseLabel(selectedCourse) : '');
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setQuery(selectedCourse ? formatCourseLabel(selectedCourse) : '');
  }, [selectedCourse]);

  const blockedCodes = useMemo(
    () => selectedValues.filter((selectedValue, selectedIndex) => selectedIndex !== index && selectedValue),
    [index, selectedValues]
  );

  const filteredOptions = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    const availableOptions = options.filter((course) => course.code === value || !blockedCodes.includes(course.code));

    if (!normalizedQuery) return availableOptions.slice(0, 8);
    return availableOptions.filter((course) => formatCourseSearchText(course).includes(normalizedQuery)).slice(0, 8);
  }, [blockedCodes, options, query, value]);

  const handleSelect = (courseCode) => {
    const nextCourse = options.find((course) => course.code === courseCode);
    setQuery(nextCourse ? formatCourseLabel(nextCourse) : '');
    setOpen(false);
    onChange(courseCode);
  };

  const handleInputChange = (event) => {
    const nextQuery = event.target.value;
    setQuery(nextQuery);
    setOpen(true);
    if (!nextQuery.trim()) onChange('');
  };

  const handleBlur = () => {
    window.setTimeout(() => {
      setOpen(false);
      if (!selectedCourse || query !== formatCourseLabel(selectedCourse)) {
        setQuery(selectedCourse ? formatCourseLabel(selectedCourse) : '');
      }
    }, 120);
  };

  return (
    <label className="search-field">
      <span>{index + 1}{text.slotSuffix}</span>
      <div className="search-input-wrap">
        <input type="text" value={query} placeholder={text.selectPlaceholder} onChange={handleInputChange} onFocus={() => setOpen(true)} onBlur={handleBlur} disabled={disabled} />
        {value && (
          <button type="button" className="clear-button" onMouseDown={(event) => event.preventDefault()} onClick={() => { setQuery(''); setOpen(false); onChange(''); }} aria-label={text.clearCourse}>
            ×
          </button>
        )}
      </div>

      {open && filteredOptions.length > 0 && (
        <div className="search-results">
          {filteredOptions.map((course) => (
            <button type="button" key={course.code} className="search-result-item" onMouseDown={(event) => event.preventDefault()} onClick={() => handleSelect(course.code)}>
              <strong>{formatCourseLabel(course)}</strong>
              <span>{schoolCategoryLabels[course.schoolCategory]} / {course.department} / {scoreTypeLabels[course.scoreType]} / {text.deviationLabel} {course.deviationValue}</span>
            </button>
          ))}
        </div>
      )}

      {open && query.trim() && filteredOptions.length === 0 && <div className="search-empty">{text.noCourseMatch}</div>}
    </label>
  );
}

function formatCourseLabel(course) {
  const schoolName = (course.schoolName ?? '').trim();
  const courseName = (course.courseName ?? '').trim();
  if (!courseName) {
    return schoolName;
  }
  if (!schoolName || courseName === schoolName || courseName.startsWith(schoolName)) {
    return courseName;
  }
  return `${schoolName} / ${courseName}`;
}

function formatCourseSearchText(course) {
  const code = course.code ?? '';
  const numericId = String(code).replace(/^course-/, '');
  return `${course.schoolName} ${course.department} ${course.courseName} ${course.schoolCategory} ${code} ${numericId}`.toLowerCase();
}

export default App;
