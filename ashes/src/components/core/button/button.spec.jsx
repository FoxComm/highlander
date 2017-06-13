import React from 'react';
import sinon from 'sinon';
import { shallow, mount } from 'enzyme';

import * as Buttons from './button';

describe('Buttons', function () {

  it('should render Button', function () {
    const button = shallow(
      <Buttons.Button>Done</Buttons.Button>
    );

    expect(button.text()).to.equal('Done');
  });

  it('should render Button with icon and text', function () {
    const button = mount(
      <Buttons.Button icon="fake">Done</Buttons.Button>
    );

    expect(button.text()).to.equal('Done');
    expect(button.find('i').hasClass('icon-fake')).to.be.true;
  });

  it('should render loading state', function () {
    const button = shallow(
      <Buttons.Button isLoading>Done</Buttons.Button>
    );

    expect(button.hasClass('loading')).to.be.true;
  });

  it('should handle click', function () {
    const onClick = sinon.spy();

    const button = shallow(
      <Buttons.Button onClick={onClick}>Done</Buttons.Button>
    );

    button.simulate('click');
    expect(onClick.called).to.be.true;
  });

  it('should render className in PrimaryButton', function () {
    const className = 'super-button';
    const button = shallow(
      <Buttons.PrimaryButton className={className} />
    );

    expect(button.hasClass('primary')).to.be.true;
    expect(button.hasClass('super-button')).to.be.true;
  });

  it('should not handle click when disabled', function () {
    const onClick = sinon.spy();

    const button = mount(
      <Buttons.Button onClick={onClick} disabled>Done</Buttons.Button>
    );

    button.simulate('click');
    expect(onClick.called).to.be.false;
  });

  it('should not handle click when loading', function () {
    const onClick = sinon.spy();

    const button = mount(
      <Buttons.Button onClick={onClick} isLoading>Done</Buttons.Button>
    );

    button.simulate('click');
    expect(onClick.called).to.be.false;
  });

  it('should render icon in LeftButton', function () {
    const button = mount(
      <Buttons.LeftButton />
    );

    expect(button.find('i').hasClass('icon-chevron-left')).to.be.true;
  });

  it('should render icon in RightButton', function () {
    const button = mount(
      <Buttons.RightButton />
    );

    expect(button.find('i').hasClass('icon-chevron-right')).to.be.true;
  });

  it('should render icon in AddButton', function () {
    const button = mount(
      <Buttons.AddButton />
    );

    expect(button.find('i').hasClass('icon-add')).to.be.true;
  });

  it('should render icon in EditButton', function () {
    const button = mount(
      <Buttons.EditButton />
    );

    expect(button.find('i').hasClass('icon-edit')).to.be.true;
  });

  it('should render icon in DeleteButton', function () {
    const button = mount(
      <Buttons.DeleteButton />
    );

    expect(button.hasClass('delete')).to.be.true;
    expect(button.find('i').hasClass('icon-trash')).to.be.true;
  });

  it('should render icon in CloseButton', function () {
    const button = mount(
      <Buttons.CloseButton />
    );

    expect(button.hasClass('close')).to.be.true;
    expect(button.find('i').hasClass('icon-close')).to.be.true;
  });

  it('should render icon in IncrementButton', function () {
    const button = mount(
      <Buttons.IncrementButton />
    );

    expect(button.find('i').hasClass('icon-chevron-up')).to.be.true;
  });

  it('should render icon in DecrementButton', function () {
    const button = mount(
      <Buttons.DecrementButton />
    );

    expect(button.find('i').hasClass('icon-chevron-down')).to.be.true;
  });

  it('should render icon in SocialButton with google icon', function () {
    const button = mount(
      <Buttons.SocialButton type="google" />
    );

    expect(button.hasClass('socialButton')).to.be.true;
    expect(button.hasClass('google')).to.be.true;
    expect(button.find('i').hasClass('icon-google')).to.be.true;
  });
});
