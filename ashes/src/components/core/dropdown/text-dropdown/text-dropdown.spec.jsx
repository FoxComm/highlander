import React from 'react';
import sinon from 'sinon';
import { mount } from 'enzyme';

import TextDropdown from './text-dropdown';

describe.only('TextDropdown', function() {
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
});
