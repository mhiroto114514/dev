const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../..');
const output = path.join(root, 'backend/target/db-removal-browser');
fs.mkdirSync(output, {recursive:true});

(async () => {
  const browser = await chromium.launch({channel:'msedge', headless:true});
  try {
    const page = await browser.newPage({viewport:{width:1500,height:1100}});
    const errors = [];
    page.on('pageerror', e => errors.push(e.message));
    const frontend = new URL(process.env.ADVISOR_TEST_FRONTEND_URL || 'http://localhost:5173');
    const backend = new URL(process.env.ADVISOR_TEST_BACKEND_URL || 'http://localhost:18080');
    const externalRequests = [];
    page.on('request', request => {
      if (!['data:', 'blob:'].some(prefix => request.url().startsWith(prefix)) &&
          ![frontend.origin, backend.origin].includes(new URL(request.url()).origin)) {
        externalRequests.push(request.url());
      }
    });
    if (frontend.origin !== backend.origin) {
      await page.route(frontend.origin + '/api/**', async route => {
        const url = route.request().url().replace(frontend.origin, backend.origin);
        await route.continue({url});
      });
    }
    await page.goto(frontend.href);
    await page.locator('.report-tabs button').nth(1).click();
    await page.locator('.history-controls input').setInputFiles(['01_round1.csv','02_round2.csv'].map(f => path.join(root,'test-data/csv-history',f)));
    const historyResponse = page.waitForResponse(r => r.url().endsWith('/csv-preview'));
    await page.getByRole('button',{name:'成績一覧を作成する',exact:true}).click();
    const response = await historyResponse;
    assert.equal(response.status(),200,await response.text());
    await page.locator('.history-page .report-print-page').first().waitFor({state:'attached'});
    assert.equal(await page.locator('.history-page .report-screen-content .history-student').count(),26);
    await printCheck('.history-page', 6, 'history');
    await page.emulateMedia({media:'screen'});
    await page.locator('.report-tabs button').nth(0).click();
    await page.locator('.score-form input').setInputFiles(path.join(root,'test-data/csv-history/01_round1.csv'));
    await page.getByRole('button',{name:'CSVを取り込んで判定する',exact:true}).click();
    await page.locator('.single-report-panel .report-print-page').first().waitFor({state:'attached'});
    assert.equal(await page.locator('.single-report-panel .report-screen-content .student-ledger').count(),26);
    await printCheck('.single-report-panel',26,'single');
    await page.emulateMedia({media:'screen'});
    const three = 'name,student_id,times,japanese,math,english,deviation_japanese,deviation_math,deviation_english,deviation_three,first_choice,second_choice,third_choice\nStudent,1,1,50,50,50,50.1,51.2,52.3,55.7,1,,245\n';
    await page.locator('.score-form input').setInputFiles({name:'three.csv',mimeType:'text/csv',buffer:Buffer.from(three)});
    await page.getByRole('button',{name:'CSVを取り込んで判定する',exact:true}).click();
    await page.waitForFunction(() => document.querySelector('.single-report-panel .ledger-student-head')?.textContent.includes('Student'));
    await printCheck('.single-report-panel',1,'three');
    const heading = await page.locator('.report-print-copy .print-score-summary thead').first().innerText();
    assert(!heading.includes('理科') && !heading.includes('社会') && !heading.includes('5教科'));
    assert.deepEqual(errors,[]);
    assert.deepEqual(externalRequests,[]);

    async function printCheck(selector, count, name) {
      await page.emulateMedia({media:'print'});
      await page.evaluate(async () => { await document.fonts.ready; window.dispatchEvent(new Event('beforeprint')); await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))); });
      const frames = page.locator(selector + ' .report-print-page');
      assert.equal(await frames.count(),count);
      const sizes = await frames.evaluateAll(items => items.map(frame => {
        const content = frame.querySelector('.report-print-content').getBoundingClientRect();
        const bounds = frame.getBoundingClientRect();
        return {bottom:content.bottom - bounds.bottom,left:bounds.left - content.left,right:content.right - bounds.right,students:frame.querySelectorAll('.history-student,.student-ledger').length};
      }));
      for (const size of sizes) {
        assert(size.bottom <= 1 && size.left <= 1 && size.right <= 1, JSON.stringify(size));
        assert(size.students <= (name === 'history' ? 5 : 1));
      }
      await page.pdf({path:path.join(output,name+'.pdf'),preferCSSPageSize:true,printBackground:true});
      await frames.first().screenshot({path:path.join(output,name+'.png')});
      console.log(name, 'pages=',count,'no overflow');
    }
  } finally { await browser.close(); }
})().catch(error => {console.error(error);process.exitCode=1;});
