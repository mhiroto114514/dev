import {readFileSync, writeFileSync} from 'node:fs';

const parse = line => {
  const fields = []; let value = ''; let quoted = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    if (c === '"') {
      if (quoted && line[i + 1] === '"') { value += '"'; i++; }
      else quoted = !quoted;
    } else if (c === ',' && !quoted) { fields.push(value); value = ''; }
    else value += c;
  }
  if (quoted) throw new Error('Unclosed CSV quote');
  fields.push(value); return fields;
};
const cell = value => /[,"\r\n]/.test(String(value)) ? `"${String(value).replaceAll('"', '""')}"` : String(value);
const labels = {PUBLIC:'公立', PRIVATE:'私立', NATIONAL:'国立', KOSEN:'高専'};
const [, ...lines] = readFileSync('backend/src/main/resources/school-master.csv', 'utf8').trimEnd().split(/\r?\n/);
const rows = lines.map(parse).map(([id, name, deviation, category]) => {
  if (!labels[category]) throw new Error('Invalid school category');
  return [id, name, deviation, category, labels[category]];
});
const write = (path, data) => writeFileSync(path, '\uFEFF' + ['id,school_name,deviation,category,category_ja', ...data.map(r => r.map(cell).join(','))].join('\r\n') + '\r\n', 'utf8');
write('school_id_mapping.csv', rows);
for (const category of Object.keys(labels)) {
  const selected = rows.filter(r => r[3] === category);
  write(`school_id_mapping_by_category/school_id_mapping_${category.toLowerCase()}.csv`, selected);
  console.log(`${category}: ${selected.length}`);
}
