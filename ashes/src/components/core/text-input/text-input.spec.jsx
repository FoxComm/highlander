import React from 'react';
import sinon from 'sinon';
import { shallow, mount } from 'enzyme';

import TextInput from './text-input';

describe('TextInput', function () {

  it('should render TextInput', function () {
    const input = shallow(
      <TextInput value="test value" />
    );

    expect(input.hasClass('input')).to.be.true;
  });

  it('should render TextInput with value', function () {
    const input = shallow(
      <TextInput value="test value" />
    );

    expect(input.find('input').prop('value')).to.equal('test value');
  });

  it('should render className in TextInput', function () {
    const input = shallow(
      <TextInput className="super-input" />
    );

    expect(input.hasClass('super-input')).to.be.true;
  });

  it('should handle onChange', function () {
    const onChange = sinon.spy();
    const input = mount(
      <TextInput onChange={onChange} />
    );

    input.simulate('change');
    expect(onChange.called).to.be.true;
  });
});
