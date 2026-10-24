const {chromium} = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const output = path.resolve(__dirname, '../../backend/target/ui-import-flow');
fs.mkdirSync(output, {recursive:true});
const ledger = {studentCode:9900001, studentName:'UI確認', times:1,
  japaneseScore:50, mathScore:50, englishScore:50,
  japaneseDeviation:50, mathDeviation:50, englishDeviation:50,
  threeSubjectDeviation:50, results:[]};
const file = name => ({name, mimeType:'text/csv', buffer:Buffer.from('name,student_id\nUI,9900001\n')});

(async () => {
  const browser = await chromium.launch({channel:'msedge', headless:true});
  try {
    const page = await browser.newPage({viewport:{width:1440,height:1000}});
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    let release;
    let onRequest;
    let fail = false;
    let requests = 0;
    let payload = '';
    await page.route('**/api/judgements/csv-*', async route => {
      requests++;
      payload = route.request().postDataBuffer().toString();
      await new Promise(resolve => { release = resolve; onRequest(); });
      await route.fulfill({status:fail ? 400 : 200, contentType:'application/json',
        headers:{'access-control-allow-origin':'*'},
        body:JSON.stringify(fail ? {message:'CSVの内容を確認してください。'} : {ledgers:[ledger]})});
    });
    await page.goto(process.env.ADVISOR_TEST_FRONTEND_URL || 'http://localhost:5173');
    const geometry = [];
    for (const mode of ['single','history']) {
      await page.locator('.report-tabs button').nth(mode === 'single' ? 0 : 1).click();
      const controls = page.locator(mode === 'single' ? '.form-panel' : '.history-controls');
      const result = page.locator(mode === 'single' ? '.single-report-panel' : '.history-results');
      const input = controls.locator('input');
      const confirm = controls.getByRole('button', {name:'確認', exact:true});
      const print = controls.getByRole('button', {name:'印刷', exact:true});
      const choose = controls.getByRole('button', {name:'ファイルを選択', exact:true});
      assert(await choose.isEnabled());
      const chooser = page.waitForEvent('filechooser');
      await choose.click();
      assert.equal((await chooser).isMultiple(),mode === 'history');
      assert(await confirm.isDisabled());
      assert(await print.isDisabled());
      assert.equal(await result.count(),0);
      geometry.push(await controls.evaluate(el => {
        const form=el.getBoundingClientRect(), file=el.querySelector('.csv-file-picker').getBoundingClientRect(), button=el.querySelector('.csv-confirm').getBoundingClientRect();
        return {x:form.x,width:form.width,fileX:file.x,buttonX:button.x,buttonWidth:button.width};
      }));
      await page.screenshot({path:path.join(output,mode+'-initial.png')});
      await input.setInputFiles(mode === 'single' ? file('round1.csv') : [file('round1.csv'),file('round2.csv')]);
      assert.equal(await controls.locator('.csv-file-summary').innerText(),mode === 'single' ? 'round1.csv' : 'round1.csv、round2.csv');
      const pickerBounds = await choose.boundingBox();
      const summaryBounds = await controls.locator('.csv-file-summary').boundingBox();
      assert(summaryBounds.x > pickerBounds.x + pickerBounds.width);
      assert.equal(await controls.locator('.csv-selected-files').count(),0);
      assert(await confirm.isEnabled());
      assert(await print.isDisabled());
      const before = requests;
      const arrived = new Promise(resolve => { onRequest = resolve; });
      await confirm.click();
      await arrived;
      await page.waitForFunction(() => !!document.querySelector('[role="status"]'));
      assert(await input.isDisabled());
      assert(await choose.isDisabled());
      assert(await print.isDisabled());
      assert(await controls.getByRole('button',{name:'確認中…',exact:true}).isDisabled());
      assert.equal(requests,before+1);
      const field = mode === 'single' ? 'file' : 'files';
      assert.equal(payload.split(`name="${field}";`).length-1,mode === 'single' ? 1 : 2);
      release();
      await result.waitFor({state:'visible'});
      assert(await confirm.isEnabled());
      assert(await print.isEnabled());
      const confirmBounds = await confirm.boundingBox();
      const printBounds = await print.boundingBox();
      assert.equal(printBounds.y,confirmBounds.y);
      assert(printBounds.x > confirmBounds.x + confirmBounds.width);
      assert.equal(await page.getByRole('button',{name:'印刷',exact:true}).count(),1);
      await page.evaluate(() => { window.printCalls = 0; window.print = () => window.printCalls++; });
      const requestCount = requests;
      await print.click();
      assert.equal(await page.evaluate(() => window.printCalls),1);
      assert.equal(requests,requestCount);
      assert((await result.boundingBox()).y >= (await controls.boundingBox()).y + (await controls.boundingBox()).height);
      await page.screenshot({path:path.join(output,mode+'-result.png')});
      await input.setInputFiles(file('replacement.csv'));
      assert(await print.isDisabled());
      assert.equal(await result.count(),0);
      fail = true;
      const failedRequest = new Promise(resolve => { onRequest = resolve; });
      await confirm.click();
      await failedRequest;
      await page.waitForFunction(() => !!document.querySelector('[role="status"]'));
      release();
      await controls.getByRole('alert').waitFor();
      assert(await print.isDisabled());
      assert.equal(await result.count(),0);
      await input.setInputFiles([]);
      assert(await confirm.isDisabled());
      assert.equal(await controls.getByRole('alert').count(),0);
      fail = false;
      await page.setViewportSize({width:390,height:844});
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth),390);
      await page.screenshot({path:path.join(output,mode+'-mobile.png')});
      await page.emulateMedia({media:'print'});
      assert.equal(await controls.evaluate(el => getComputedStyle(el).display),'none');
      await page.emulateMedia({media:'screen'});
      await page.setViewportSize({width:1440,height:1000});
    }
    assert.deepEqual(geometry[0],geometry[1]);
    assert.deepEqual(errors,[]);
    console.log('PASS: unified confirm/print buttons, print activation and action, disabled/loading/error/reset states, results below input, mobile, print controls hidden');
  } finally { await browser.close(); }
})().catch(error => {console.error(error);process.exitCode=1;});
