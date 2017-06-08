import React from 'react';
import sinon from 'sinon';
import { mount } from 'enzyme';

import Counter from './counter';

describe('Counter', function () {

  it('should render Counter', function () {
    const counter = mount(
      <Counter />
    );

    expect(counter.hasClass('counter')).to.be.true;
    expect(counter.find('DecrementButton').hasClass('controls')).to.be.true;
    expect(counter.find('IncrementButton').hasClass('controls')).to.be.true;
    expect(counter.find('input').hasClass('input')).to.be.true;
  });

  it('should set correct value', function () {
    const counter = mount(
      <Counter
        value={1}
      />
    );

    expect(counter.find('input').prop('value')).to.equal(1);
  });

  it('should render className in Counter', function () {
    const counter = mount(
      <Counter
        value={1}
        className="test"
      />
    );

    expect(counter.hasClass('test')).to.be.true;
  });

  it('should handle click on DecrementButton', function () {
    const onChange = sinon.spy();
    const counter = mount(
      <Counter
        value={1}
        className="test"
        onChange={onChange}
      />
    );

    counter.find('DecrementButton').simulate('click');
    expect(onChange.calledWith(0)).to.be.true;
  });

  it('should handle click on IncrementButton', function () {
    const onChange = sinon.spy();
    const counter = mount(
      <Counter
        value={1}
        className="test"
        onChange={onChange}
      />
    );

    counter.find('IncrementButton').simulate('click');
    expect(onChange.calledWith(2)).to.be.true;
  });

});
