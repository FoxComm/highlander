/* eslint-disable */

const fs = require('fs');
const stripColorCodes = require('stripcolorcodes');
const Allure = require('allure-js-commons');

fs.readFile(process.argv[2], 'utf8', (err, data) => {
  if (err) {
    console.error(err);
  } else {
    const testCasesBySuite = Object.create(null);
    const outputBlocks = stripColorCodes(data).split('\n\n\n');
    const resultsText = outputBlocks[0].trim();

    for (const testCaseResultText of resultsText.split('\n')) {
      const match = /✔ ([A-Za-z-]+) › (.+) \(\d+\.\d+s\)/.exec(testCaseResultText.trim());
      if (match) {
        const suiteName = match[1];
        const testCaseName = match[2];
        const suite = testCasesBySuite[suiteName] || {};
        suite[testCaseName] = { status: 'success' };
        testCasesBySuite[suiteName] = suite;
      }
    }

    for (const errorBlock of outputBlocks.slice(1)) {
      const errorLines = errorBlock.trim().split('\n');
      const [suiteName, testCaseName] = errorLines[0].split(' › ');
      const suite = testCasesBySuite[suiteName] || {};
      suite[testCaseName] = {
        status: 'failure',
        description: errorLines.slice(1).join('\n'),
      };
      testCasesBySuite[suiteName] = suite;
    }

    const allureReporter = new Allure();

    for (const suiteName in testCasesBySuite) {
      allureReporter.startSuite(suiteName);
      const suite = testCasesBySuite[suiteName];
      for (const testCaseName in suite) {
        const result = suite[testCaseName];
        allureReporter.startCase(testCaseName);
        if (result.status == 'success') {
          allureReporter.endCase('passed');
        } else {
          allureReporter.endCase('failed', {
            message: result.description,
          });
        }
      }
      allureReporter.endSuite();
    }
  }
});
