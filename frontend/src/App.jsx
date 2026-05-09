import { useEffect, useMemo, useState } from 'react';

const initialForm = {
  threeSubjectDeviation: 70,
  fiveSubjectDeviation: 70,
  publicDesiredCourseCodes: ['', '', ''],
  privateDesiredCourseCodes: ['', '', '']
};

const recommendationLabels = {
  CHALLENGE: 'チャレンジ校',
  MATCH: '実力相応校',
  SAFETY: '安全校'
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
  title: '志望校判定システム',
  lead: '公立は最大3コース、私立は最大3コースまで選択して、志望コース判定とおすすめ高校をまとめて確認できます。',
  input: '入力',
  result: '結果',
  three: '3教科偏差値',
  five: '5教科偏差値',
  publicTitle: '公立志望コース',
  privateTitle: '私立志望コース',
  max3: '最大3コースまで選択',
  check: '判定する',
  checking: '判定中...',
  notes: '補足',
  note1: '学校名で検索すると、該当する学科・コース候補が表示されます。',
  note2: '公立高校は主に 5 教科偏差値、私立高校はコース設定に応じて 3 教科または 5 教科を使います。',
  note3: '同じコースを複数枠で選ばないようにしてください。',
  noResult: '判定結果はここに表示されます。偏差値を入力し、志望コースを検索して選択してください。',
  failedCourses: 'コース一覧の取得に失敗しました。',
  failedJudgement: '判定に失敗しました。',
  usedScore: '判定使用偏差値',
  targetDev: '目安偏差値',
  judge: '判定',
  studentScore: '生徒偏差値',
  courseTarget: '志望コース偏差値',
  recTitle: 'おすすめの高校',
  noRec: 'この条件で出せる候補がまだありません。',
  publicRecTitle: '公立おすすめ',
  privateRecTitle: '私立おすすめ',
  slotSuffix: '件目',
  selectPlaceholder: '学校名・コース名で検索',
  deviationLabel: '偏差値',
  duplicateBlocked: 'すでに別の枠で選択済みのため、このコースは選べません。',
  noCourseMatch: '該当するコースがありません。',
  clearCourse: '志望コースをクリア'
};

function App() {
  const [form, setForm] = useState(initialForm);
  const [courseOptions, setCourseOptions] = useState({ publicCourses: [], privateCourses: [] });
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [courseLoading, setCourseLoading] = useState(true);

  useEffect(() => {
    const loadCourses = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/courses');
        const data = await response.json();

        if (!response.ok) {
          throw new Error(text.failedCourses);
        }

        setCourseOptions(data);
      } catch (loadError) {
        setError(loadError.message);
      } finally {
        setCourseLoading(false);
      }
    };

    loadCourses();
  }, []);

  const onScoreChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: Number(value)
    }));
  };

  const onCourseChange = (categoryKey, index, value) => {
    setForm((current) => {
      const nextValues = [...current[categoryKey]];
      nextValues[index] = value;

      return {
        ...current,
        [categoryKey]: nextValues
      };
    });
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');

    const payload = {
      threeSubjectDeviation: form.threeSubjectDeviation,
      fiveSubjectDeviation: form.fiveSubjectDeviation,
      publicDesiredCourseCodes: form.publicDesiredCourseCodes.filter(Boolean),
      privateDesiredCourseCodes: form.privateDesiredCourseCodes.filter(Boolean)
    };

    try {
      const response = await fetch('http://localhost:8080/api/judgements', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || text.failedJudgement);
      }

      setResult(data);
    } catch (submitError) {
      setResult(null);
      setError(submitError.message);
    } finally {
      setLoading(false);
    }
  };

  const aggregatedRecommendations = useMemo(() => {
    if (!result) {
      return [];
    }

    const recommendationMap = new Map();

    result.results.forEach((entry) => {
      entry.recommendedSchools.forEach((school) => {
        const existing = recommendationMap.get(school.courseCode);

        if (!existing) {
          recommendationMap.set(school.courseCode, school);
          return;
        }

        const priority = { MATCH: 0, SAFETY: 1, CHALLENGE: 2 };
        if (priority[school.recommendationType] < priority[existing.recommendationType]) {
          recommendationMap.set(school.courseCode, school);
        }
      });
    });

    return Array.from(recommendationMap.values()).sort((left, right) => right.deviationValue - left.deviationValue);
  }, [result]);

  const publicRecommendations = aggregatedRecommendations.filter((school) => school.schoolCategory === 'PUBLIC');
  const privateRecommendations = aggregatedRecommendations.filter((school) => school.schoolCategory === 'PRIVATE');

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
              <span>{text.three}</span>
              <input type="number" min="30" max="80" name="threeSubjectDeviation" value={form.threeSubjectDeviation} onChange={onScoreChange} required />
            </label>

            <label>
              <span>{text.five}</span>
              <input type="number" min="30" max="80" name="fiveSubjectDeviation" value={form.fiveSubjectDeviation} onChange={onScoreChange} required />
            </label>

            <CourseSelectGroup
              title={text.publicTitle}
              description={text.max3}
              categoryKey="publicDesiredCourseCodes"
              options={courseOptions.publicCourses}
              values={form.publicDesiredCourseCodes}
              onChange={onCourseChange}
              disabled={courseLoading}
            />

            <CourseSelectGroup
              title={text.privateTitle}
              description={text.max3}
              categoryKey="privateDesiredCourseCodes"
              options={courseOptions.privateCourses}
              values={form.privateDesiredCourseCodes}
              onChange={onCourseChange}
              disabled={courseLoading}
            />

            <button type="submit" disabled={loading || courseLoading}>
              {loading ? text.checking : text.check}
            </button>
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
          <h2>{text.result}</h2>

          {error && <div className="error-box">{error}</div>}
          {!result && !error && <div className="empty-state">{text.noResult}</div>}

          {result && (
            <div className="result-stack">
              <button
                  onClick={() => window.print()}
                  className="print-button"
                  style={{ marginBottom: '20px' }}
              >
                この結果を印刷する（帳票出力）
              </button>
              {result.results.map((entry) => (
                <div className="school-result-block" key={`${entry.schoolName}-${entry.courseName}`}>
                  <div className="judgement-card">
                    <div>
                      <p className="meta">
                        {schoolCategoryLabels[entry.schoolCategory]} / {entry.department}
                      </p>
                      <h3>{entry.schoolName}</h3>
                      <p className="sub-meta">{entry.courseName} / {entry.area}</p>
                      <p className="sub-meta">
                        {text.usedScore}: {scoreTypeLabels[entry.usedScoreType]} / {text.targetDev} {entry.targetDeviationValue}
                      </p>
                    </div>
                    <div className={`badge badge-${entry.judgement}`}>
                      <span>{entry.judgement}</span>
                      <small>{text.judge}</small>
                    </div>
                  </div>

                  <div className="stats-row">
                    <article className="stat-card">
                      <span>{text.studentScore}</span>
                      <strong>{entry.studentDeviationValue}</strong>
                    </article>
                    <article className="stat-card">
                      <span>{text.courseTarget}</span>
                      <strong>{entry.targetDeviationValue}</strong>
                    </article>
                  </div>
                </div>
              ))}

              <div className="recommendations">
                <h3>{text.recTitle}</h3>
                {aggregatedRecommendations.length === 0 && <p className="empty-inline">{text.noRec}</p>}

                {aggregatedRecommendations.length > 0 && (
                  <div className="recommendation-groups">
                    <RecommendationGroup title={text.publicRecTitle} schools={publicRecommendations} />
                    <RecommendationGroup title={text.privateRecTitle} schools={privateRecommendations} />
                  </div>
                )}
              </div>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

function CourseSelectGroup({ title, description, categoryKey, options, values, onChange, disabled }) {
  return (
    <fieldset className="school-group" disabled={disabled}>
      <legend>{title}</legend>
      <p className="group-description">{description}</p>
      <div className="school-select-stack">
        {values.map((value, index) => (
          <SearchableCourseField
            key={`${categoryKey}-${index}`}
            index={index}
            value={value}
            options={options}
            selectedValues={values}
            onChange={(nextValue) => onChange(categoryKey, index, nextValue)}
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
    const normalizedQuery = query.trim();
    const availableOptions = options.filter((course) => course.code === value || !blockedCodes.includes(course.code));

    if (!normalizedQuery) {
      return availableOptions.slice(0, 8);
    }

    return availableOptions
      .filter((course) => formatCourseSearchText(course).includes(normalizedQuery))
      .slice(0, 8);
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

    if (!nextQuery.trim()) {
      onChange('');
    }
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
        <input
          type="text"
          value={query}
          placeholder={text.selectPlaceholder}
          onChange={handleInputChange}
          onFocus={() => setOpen(true)}
          onBlur={handleBlur}
          disabled={disabled}
        />
        {value && (
          <button
            type="button"
            className="clear-button"
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => {
              setQuery('');
              setOpen(false);
              onChange('');
            }}
            aria-label={text.clearCourse}
          >
            ×
          </button>
        )}
      </div>

      {open && filteredOptions.length > 0 && (
        <div className="search-results">
          {filteredOptions.map((course) => (
            <button
              type="button"
              key={course.code}
              className="search-result-item"
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => handleSelect(course.code)}
            >
              <strong>{formatCourseLabel(course)}</strong>
              <span>
                {course.department} / {scoreTypeLabels[course.scoreType]} / {text.deviationLabel} {course.deviationValue} / {course.area}
              </span>
            </button>
          ))}
        </div>
      )}

      {open && query.trim() && filteredOptions.length === 0 && (
        <div className="search-empty">
          {blockedCodes.includes(value) ? text.duplicateBlocked : text.noCourseMatch}
        </div>
      )}
    </label>
  );
}

function RecommendationGroup({ title, schools }) {
  return (
    <section className="recommendation-group">
      <h4>{title}</h4>
      {schools.length === 0 && <p className="empty-inline">{text.noRec}</p>}
      {schools.map((school) => (
        <article className={`recommendation-card recommendation-${school.recommendationType}`} key={school.courseCode}>
          <div>
            <p className="meta">{recommendationLabels[school.recommendationType]}</p>
            <h4>{school.schoolName}</h4>
            <p className="sub-meta">{school.courseName} / {school.area}</p>
            <p className="sub-meta">
              {school.department} / {scoreTypeLabels[school.scoreType]}
            </p>
          </div>
          <div className="recommendation-score">
            {text.deviationLabel} {school.deviationValue}
          </div>
        </article>
      ))}
    </section>
  );
}

function formatCourseLabel(course) {
  return `${course.schoolName} / ${course.courseName}`;
}

function formatCourseSearchText(course) {
  return `${course.schoolName} ${course.department} ${course.courseName} ${course.area}`.toLowerCase();
}

export default App;
