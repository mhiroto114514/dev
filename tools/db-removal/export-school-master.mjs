import {readFileSync, writeFileSync, mkdirSync} from 'node:fs';
import {dirname} from 'node:path';

// One-time migration: SQL VALUES order is the serial ID order in the supplied data.sql.
const [source, destination] = process.argv.slice(2);
if (!source || !destination) throw new Error('Usage: node export-school-master.mjs data.sql school-master.csv');
const sql = readFileSync(source, 'utf8');
const values = sql.slice(sql.indexOf('VALUES') + 6).replace(/^\s*--.*$/gm, '');
const tuple = /\(\s*'((?:[^']|'')*)'\s*,\s*(-?\d+(?:\.\d+)?)\s*,\s*'((?:[^']|'')*)'\s*\)/g;
const rows = [...values.matchAll(tuple)].map((m, i) => [i + 1, m[1].replaceAll("''", "'"), m[2], m[3]]);
if (!rows.length || values.replace(tuple, '').replace(/[,;\s]/g, '')) {
  throw new Error('Unrecognized SQL: refusing a partial school master export.');
}
const cell = value => /[,"\r\n]/.test(String(value)) ? `"${String(value).replaceAll('"', '""')}"` : String(value);
mkdirSync(dirname(destination), {recursive: true});
writeFileSync(destination, ['id,name,deviation,school_category', ...rows.map(r => r.map(cell).join(','))].join('\n') + '\n', 'utf8');
console.log(`Exported ${rows.length} schools with explicit IDs to ${destination}`);
