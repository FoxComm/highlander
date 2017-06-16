import React from 'react';
import { mount } from 'enzyme';

import Currency from './currency';

describe('Currency', function () {

  it('should render non-empty tag by default', function () {

    const currency = mount(
      <Currency />
    );

    expect(currency.text()).to.equal('$0.00');
  });

  it('should render empty tag with incorrect value', function () {

    const currency = mount(
      <Currency value={'abc'} />
    );

    expect(currency.text()).to.equal('');
  });

  it('should render correct value', function () {

    const currency = mount(
      <Currency value={'abc'} />
    );

    expect(currency.text()).to.equal('');
  });

  it('should render correct currency', function () {

    const currency = mount(
      <Currency value={'123'} />
    );

    expect(currency.text()).to.equal('$1.23');
  });

  it('should render correct negative value', function () {

    const currency = mount(
      <Currency value={'-123'} />
    );

    expect(currency.text()).to.equal('-$1.23');
  });

  it('should render value with delimiter', function () {

    const currency = mount(
      <Currency value={'1234567890'} />
    );

    expect(currency.text()).to.equal('$12,345,678.90');
  });

  it('should support another fractionBase', function () {

    const currency = mount(
      <Currency value={'123'} fractionBase={0} />
    );

    expect(currency.text()).to.equal('$123.00');
  });

  it('should support another currency', function () {

    const currency = mount(
      <Currency value={'123'} currency="EUR" />
    );

    expect(currency.text()).to.equal('€1.23');
  });

  it('should support big integers', function () {

    const currency = mount(
      <Currency value={'15151542519515184515'} bigNumber />
    );

    expect(currency.text()).to.equal('$151,515,425,195,151,845.15');
  });

  it('should render positive style in Transaction mode', function () {

    const currency = mount(
      <Currency value={'100'} isTransaction />
    );

    expect(currency.find('Change').hasClass('positive')).to.be.true;
  });

  it('should render negative style in Transaction mode', function () {

    const currency = mount(
      <Currency value={'-100'} isTransaction />
    );

    expect(currency.find('Change').hasClass('negative')).to.be.true;
  });
});
