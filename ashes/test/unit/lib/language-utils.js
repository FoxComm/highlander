// @todo
// const { default: formatCurrency, stringToCurrency } = requireSource('lib/language-utils', [
//   'codeToName'
// ]);

describe('language utils', function() {
  it.skip('codeToName should create proper string', function() {
    const code = 'codeToName';

    expect(codeToName(code)).to.be.equal('Code To Name');
  });
});
