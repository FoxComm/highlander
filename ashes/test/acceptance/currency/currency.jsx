import React from 'react';
import ReactTestUtils from 'react-addons-test-utils';
import * as ShallowTestUtils from 'react-shallow-testutils';

describe('Currency', function() {
  const Currency = requireComponent('common/currency.jsx');

  it('should render not empty tag by default', function() {
    expect(Currency({}), 'to equal', <span className="fc-currency">0.00</span>);
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
      Currency({value: 123, currency: 'USD'}),
      'to equal',
      <span className="fc-currency">$1.23</span>
    );
  });

  it('should render correct negative value', function() {
    expect(
      Currency({value: -123, currency: 'USD'}),
      'to equal',
      <span className="fc-currency _negative">-$1.23</span>
    );
  });

  it('should render value with delimiter', function() {
    expect(
      Currency({value: 1234567890, currency: 'USD'}),
      'to equal',
      <span className="fc-currency">$12,345,678.90</span>
    );
  });

  it('should support another fractionBase', function() {
    expect(
      Currency({value: 123, fractionBase: 0, currency: 'USD'}),
      'to equal',
      <span className="fc-currency">$123.00</span>
    );
  });

  it('should support another currency', function() {
    expect(
      Currency({value: 123, currency: 'EUR'}),
      'to equal',
      <span className="fc-currency">€1.23</span>
    );
  });

  it('should support big integers', function() {
    expect(
      Currency({value: '15151542519515184515', currency: 'USD', bigNumber: true}),
      'to equal',
      <span className="fc-currency">$151,515,425,195,151,845.15</span>
    );
  });
});
