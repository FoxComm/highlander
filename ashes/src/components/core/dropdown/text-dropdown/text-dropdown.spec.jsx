import React from 'react';
import sinon from 'sinon';
import { mount } from 'enzyme';

import TextDropdown from './text-dropdown';

describe('TextDropdown', function() {
  it('should set phaceholder if no value', function() {
    const phaceholderText = 'ahuilerhg';
    const textDropdown = mount(<TextDropdown value={null} placeholder={phaceholderText} />);

    expect(textDropdown.find('.displayText')).text().to.equal(phaceholderText);
  });

  it('should convert null to empty string value', function() {
    const textDropdown = mount(<TextDropdown value={null} />);

    expect(textDropdown.state().selectedValue).to.equal('');
  });

  it('should convert number to string value', function() {
    const textDropdown = mount(<TextDropdown value={4} />);

    expect(textDropdown.state().selectedValue).to.equal('4');
  });

  it('should trigger onChange and change displayText by click', function() {
    const onChange = sinon.spy();
    const textDropdown = mount(<TextDropdown items={['one', 'two']} onChange={onChange} placeholder="plAceholder" />);

    expect(textDropdown.find('.displayText').text()).to.equal('plAceholder');

    textDropdown.find('.pivot').simulate('click');
    textDropdown.find('.item').at(1).simulate('click');

    expect(textDropdown.find('.displayText').text()).to.equal('two');

    expect(onChange.calledOnce).to.be.true;
  });
});
