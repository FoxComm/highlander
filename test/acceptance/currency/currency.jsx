import React from 'react';
import TestUtils from 'react-addons-test-utils';
import ShallowTestUtils from 'react-shallow-testutils';

describe('Currency', function() {
  const Currency = requireComponent('common/currency.jsx');

  it('should render empty tag by default', function() {
    expect(Currency({}), 'to equal', <span className="fc-currency" />);
  });

  it('should render empty tag with incorrect value', function() {
    expect(
      Currency({value: 'abc'}),
      'to equal',
      <span className="fc-currency" />
    );
  });

  it('should render correct value', function() {
    expect(
      Currency({value: 123, base: 100, currency: '$'}),
      'to equal',
      <span className="fc-currency">$1.23</span>
    );
  });

  it('should render correct negative value', function() {
    expect(
      Currency({value: -123, base: 100, currency: '$'}),
      'to equal',
      <span className="fc-currency">-$1.23</span>
    );
  });

  it('should render value with delimiter', function() {
    expect(
      Currency({value: 1234567890, base: 100, currency: '$'}),
      'to equal',
      <span className="fc-currency">$12,345,678.90</span>
    );
  });

  it('should support another base', function() {
    expect(
      Currency({value: 123, base: 1, currency: '$'}),
      'to equal',
      <span className="fc-currency">$123.00</span>
    );
  });

  it('should support another currency', function() {
    expect(
      Currency({value: 123, base: 100, currency: '€'}),
      'to equal',
      <span className="fc-currency">€1.23</span>
    );
  });
});
