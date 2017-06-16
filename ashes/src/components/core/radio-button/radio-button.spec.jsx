import React from 'react';
import sinon from 'sinon';
import { shallow, mount } from 'enzyme';

import RadioButton from './radio-button';

describe('RadioButton', function () {
  it('should render RadioButton', function () {
    const button = shallow(
      <RadioButton id="id" />
    );

    expect(button.hasClass('radio')).to.be.true;
  });

  it('should render RadioButton with label content', function () {
    const button = shallow(
      <RadioButton id="id" label="Test Content" />
    );

    expect(button.find('.label').text()).to.equal('Test Content');
  });

  it('should render correct className', function () {
    const button = shallow(
      <RadioButton id="id" className="new-cls" />
    );

    expect(button.hasClass('new-cls')).to.be.true;
  });

  it('should handle change', function () {
    const onChange = sinon.spy();
    const button = mount(
      <RadioButton id="id" onChange={onChange} />
    );

    button.find('input').simulate('change');
    expect(onChange.called).to.be.true;
  });

  it('should have disabled input when props.disabled=true', function () {
    const button = mount(
      <RadioButton id="id" disabled />
    );

    expect(button.find('input').prop('disabled')).to.be.true;
  });
});
