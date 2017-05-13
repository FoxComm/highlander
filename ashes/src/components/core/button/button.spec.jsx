import React from 'react';
import { shallow } from 'enzyme';

import * as Buttons from './button';

describe('Buttons', function () {

  it('should render Button', function () {
    const button = shallow(
      <Buttons.Button>Done</Buttons.Button>
    );

    expect(button.text()).to.equal('Done');
  });

  it('should render Button with icon and text', function () {
    const button = shallow(
      <Buttons.Button icon="fake">Done</Buttons.Button>
    );

    expect(button.text()).to.equal('Done');
    expect(button.find('i').hasClass('icon-fake')).to.be.true;
  });

  it('should set onClick handler to Button', function () {
    let counter = 0;

    const onClick = () => counter += 1;

    const button = shallow(
      <Buttons.Button onClick={onClick}>Done</Buttons.Button>
    );

    button.simulate('click');
    expect(counter).to.equal(1);
  });

  it('should render className in PrimaryButton', function () {
    const className = 'super-button';
    const button = shallow(
      <Buttons.PrimaryButton className={className} />
    );

    expect(button.hasClass('primary')).to.be.true;
    expect(button.hasClass('super-button')).to.be.true;
  });

  it('should render icon in LeftButton', function () {
    const button = shallow(
      <Buttons.LeftButton />
    );

    expect(button.dive().find('i').hasClass('icon-chevron-left')).to.be.true;
  });

  it('should render icon in RightButton', function () {
    const button = shallow(
      <Buttons.RightButton />
    );

    expect(button.dive().find('i').hasClass('icon-chevron-right')).to.be.true;
  });

  it('should render icon in AddButton', function () {
    const button = shallow(
      <Buttons.AddButton />
    );

    expect(button.dive().find('i').hasClass('icon-add')).to.be.true;
  });

  it('should render icon in EditButton', function () {
    const button = shallow(
      <Buttons.EditButton />
    );

    expect(button.dive().find('i').hasClass('icon-edit')).to.be.true;
  });

  it('should render icon in DeleteButton', function () {
    const button = shallow(
      <Buttons.DeleteButton />
    );

    expect(button.hasClass('delete')).to.be.true;
    expect(button.dive().find('i').hasClass('icon-trash')).to.be.true;
  });

  it('should render icon in CloseButton', function () {
    const button = shallow(
      <Buttons.CloseButton />
    );

    expect(button.hasClass('close')).to.be.true;
    expect(button.dive().find('i').hasClass('icon-close')).to.be.true;
  });

  it('should render icon in IncrementButton', function () {
    const button = shallow(
      <Buttons.IncrementButton />
    );

    expect(button.dive().find('i').hasClass('icon-chevron-up')).to.be.true;
  });

  it('should render icon in DecrementButton', function () {
    const button = shallow(
      <Buttons.DecrementButton />
    );

    expect(button.dive().find('i').hasClass('icon-chevron-down')).to.be.true;
  });

  it('should render icon in SocialButton with google icon', function () {
    const button = shallow(
      <Buttons.SocialButton type="google" />
    );

    expect(button.hasClass('socialButton')).to.be.true;
    expect(button.hasClass('google')).to.be.true;
    expect(button.dive().find('i').hasClass('icon-google')).to.be.true;
  });
});
