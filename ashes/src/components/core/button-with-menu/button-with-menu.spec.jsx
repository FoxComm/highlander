import React from 'react';
import sinon from 'sinon';
import { mount } from 'enzyme';

import ButtonWithMenu from './button-with-menu';

describe('ButtonWithMenu', function () {
  it('should render ButtonWithMenu', function () {
    const button = mount(
      <ButtonWithMenu title="Save" items={[]} />
    );

    expect(button.state().open).to.be.false;
    expect(button.find('.actionButton').text()).to.equal('Save');
  });

  it('should render ButtonWithMenu with icon', function () {
    const button = mount(
      <ButtonWithMenu title="Save" items={[]} icon="edit" />
    );

    expect(button.find('.icon-edit')).to.have.length(1);
    expect(button.find('.actionButton').text()).to.equal('Save');
  });

  it('should render loading state', function () {
    const button = mount(
      <ButtonWithMenu title="Save" items={[]} isLoading />
    );

    expect(button.find('.actionButton').hasClass('loading')).to.be.true;
  });

  it('should handle action button click', function () {
    const onClick = sinon.spy();
    const button = mount(
      <ButtonWithMenu title="Save" items={[]} onPrimaryClick={onClick} />
    );

    button.find('.actionButton').simulate('click');

    expect(onClick.called).to.be.true;
  });

  it('should not handle action button click when disabled', function () {
    const onClick = sinon.spy();
    const button = mount(
      <ButtonWithMenu title="Save" items={[]} onPrimaryClick={onClick} buttonDisabled />
    );

    button.find('.actionButton').simulate('click');

    expect(onClick.called).to.be.false;
  });

  it('should open menu on menu button click', function () {
    const button = mount(
      <ButtonWithMenu
        title="Save"
        items={[
          ['id1', 'Save and Exit'],
          ['id2', 'Save and Duplicate'],
        ]}
      />
    );

    expect(button.state().open).to.be.false;
    expect(button.find('.menu')).to.have.length(0);
    expect(button.find('.item')).to.have.length(0);

    button.find('.dropdownButton').simulate('click');

    expect(button.state().open).to.be.true;
    expect(button.find('.menu')).to.have.length(1);
    expect(button.find('.item')).to.have.length(2);
  });

  it('should not open menu on disabled menu button click', function () {
    const button = mount(
      <ButtonWithMenu title="Save" items={[]} menuDisabled />
    );

    expect(button.state().open).to.be.false;
    expect(button.find('.menu')).to.have.length(0);

    button.find('.dropdownButton').simulate('click');

    expect(button.state().open).to.be.false;
    expect(button.find('.menu')).to.have.length(0);
  });
});
