import _ from 'lodash';

const { default: formatCurrency, stringToCurrency } = requireSource('lib/format-currency', ['stringToCurrency']);

describe('format currency', function() {
  context('formatCurrency function', function() {
    it('format various set of numbers correctly', function() {
      const correctArgsToValues = [
        [[], '0.00'],
        [[1], '0.01'],
        [[-451], '-4.51'],
        [[49], '0.49'],
        [[1000001, { currency: 'USD' }], '$10,000.01'],
        [[54106027, { currency: 'EUR' }], '€541,060.27'],
        [[299], '2.99'],
        [['41351341455961342345134', { bigNumber: true }], '413,513,414,559,613,423,451.34'],
        // another fraction base
        [[451, { fractionBase: 0 }], '451.00'],
        [[15, { fractionBase: 0 }], '15.00'],
        [[7, { fractionBase: 0 }], '7.00'],
      ];
      _.forEach(correctArgsToValues, ([args, expectedResult]) => {
        const v = formatCurrency(...args);
        expect(v).to.be.equal(expectedResult, `Invalid formatted value for args: ${JSON.stringify(args)}`);

        const opts = _.get(args, 1, {});
        if (!opts.bigNumber && !opts.currency) {
          // also test with bigNumber: true if no currency set
          const newArgs = _.merge(opts, { bigNumber: true });
          const bigV = formatCurrency(args[0], newArgs);
          expect(bigV).to.be.equal(expectedResult, `Invalid formatted big value for args: ${JSON.stringify(newArgs)}`);
        }
      });
    });
  });

  context('stringToCurrency function', function() {
    it('unformat various set of numbers correctly', function() {
      const correctArgToValue = [
        [['0.299'], '30'], // non-significant fraction part should be rounded
        [['54.'], '5400'], //dot without fraction part is allowed
        [[''], '0'],
        [['000045'], '4500'],
        // another fraction base
        [['1345', { fractionBase: 0 }], '1345'],
      ];
      _.forEach(correctArgToValue, ([args, expectedResult]) => {
        const v = stringToCurrency(...args);
        expect(v).to.be.equal(expectedResult, `Invalid result for args: ${JSON.stringify(args)}`);
      });
    });
  });
});
