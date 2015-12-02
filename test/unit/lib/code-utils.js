const { default: formatCurrency, stringToCurrency } = requireSource('lib/code-utils', [
  'codeToName'
]);

describe('code utils', function () {

  if ('codeToName should create proper string', function() {
    const code = 'codeToName';

    expect(codeToName(code)).to.be.equal('Code To Name');
  });

});
