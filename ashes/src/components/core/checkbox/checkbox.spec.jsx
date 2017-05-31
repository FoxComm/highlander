import React from 'react';
import sinon from 'sinon';
import { shallow, mount } from 'enzyme';

import { DefaultCheckbox } from './checkbox';
import { Checkbox } from './checkbox';
import { PartialCheckbox } from './checkbox';
import { BigCheckbox } from './checkbox';
import { SliderCheckbox } from './checkbox';

describe('Checkboxes', function () {

  // DefaultCheckbox

  it('should render DefaultCheckbox with label', function () {
    const checkbox = shallow(
      <DefaultCheckbox>Test Label</DefaultCheckbox>
    );

    expect(checkbox.text()).to.equal('Test Label');
  });

  it('should render className in DefaultCheckbox', function () {
    const checkbox = shallow(
      <DefaultCheckbox className="test" />
    );

    expect(checkbox.hasClass('test')).to.be.true;
  });

  it('should handle input change', function () {
    const onChange = sinon.spy();
    const checkbox = mount(
      <DefaultCheckbox id="id" onChange={onChange} />
    );

    checkbox.find('input').simulate('change');
    expect(onChange.called).to.be.true;
  });

  it('should have disabled input when props.disabled=true', function () {
    const checkbox = mount(
      <DefaultCheckbox id="id" disabled />
    );

    expect(checkbox.find('input').prop('disabled')).to.be.true;
  });


  // Checkbox

  it('should render className in Checkbox', function () {
    const checkbox = shallow(
      <Checkbox className="test" />
    );

    expect(checkbox.hasClass('test')).to.be.true;
    expect(checkbox.hasClass('checkbox')).to.be.true;
  });

  it('should render inline, docked in Checkbox', function () {
    const checkbox = shallow(
      <Checkbox inline docked="right" />
    );

    expect(checkbox.hasClass('inline')).to.be.true;
    expect(checkbox.hasClass('dockedRight')).to.be.true;
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
