import React from 'react';
import sinon from 'sinon';
import { shallow, mount } from 'enzyme';

import { Checkbox } from './checkbox';
import { PartialCheckbox } from './checkbox';
import { BigCheckbox } from './checkbox';
import { SliderCheckbox } from './checkbox';

describe('Checkboxes', function () {

  // Checkbox

  it('should render Checkbox with label', function () {
    const checkbox = mount(
      <Checkbox id="id" label="Test Label" />
    );

    expect(checkbox.text()).to.equal('Test Label');
  });

  it('should render className in Checkbox', function () {
    const checkbox = shallow(
      <Checkbox id="id" className="test" />
    );

    expect(checkbox.hasClass('test')).to.be.true;
  });

  it('should handle input change', function () {
    const onChange = sinon.spy();
    const checkbox = mount(
      <Checkbox id="id" onChange={onChange} />
    );

    checkbox.find('input').simulate('change');
    expect(onChange.called).to.be.true;
  });

  it('should have disabled input when props.disabled=true', function () {
    const checkbox = mount(
      <Checkbox id="id" disabled />
    );

    expect(checkbox.find('input').prop('disabled')).to.be.true;
  });

  it('should render inCell', function () {
    const checkbox = shallow(
      <Checkbox inCell />
    );

    expect(checkbox.hasClass('inCell')).to.be.true;
  });

  // PartialCheckbox

  it('should render className in PartialCheckbox', function () {
    const checkbox = shallow(
      <PartialCheckbox className="test" />
    );

    expect(checkbox.hasClass('test')).to.be.true;
  });

  it('should render halfChecked in PartialCheckbox', function () {
    const checkbox = shallow(
      <PartialCheckbox checked halfChecked />
    );

    expect(checkbox.hasClass('halfChecked')).to.be.true;
  });

  it('should not render halfChecked if it is just checked', function () {
    const checkbox = shallow(
      <PartialCheckbox checked />
    );

    expect(checkbox.hasClass('halfChecked')).to.be.false;
  });

  // BigCheckbox

  it('should render className in BigCheckbox', function () {
    const checkbox = shallow(
      <BigCheckbox className="test" />
    );

    expect(checkbox.hasClass('test')).to.be.true;
    expect(checkbox.hasClass('bigCheckbox')).to.be.true;
  });

  // SliderCheckbox

  it('should render className in SliderCheckbox', function () {
    const checkbox = shallow(
      <SliderCheckbox className="test" />
    );

    expect(checkbox.hasClass('test')).to.be.true;
    expect(checkbox.hasClass('slideCheckbox')).to.be.true;
  });

});
