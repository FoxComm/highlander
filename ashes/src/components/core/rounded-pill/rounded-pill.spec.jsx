import React from 'react';
import sinon from 'sinon';
import { shallow } from 'enzyme';

import { RoundedPill } from './rounded-pill';

describe('RoundedPill', function () {

  it('Render', function () {
    const pill = shallow(
      <RoundedPill text="Text" />
    );

    expect(pill.text()).to.equal('Text');
    expect(pill.hasClass('closable')).to.be.false;
    expect(pill.hasClass('clickable')).to.be.false;
    expect(pill.hasClass('_loading')).to.be.false;
  });

  it('Click', function () {
    const onClick = sinon.spy();

    const pill = shallow(
      <RoundedPill text="Text" onClick={onClick} value={1} />
    );

    pill.find('.label').simulate('click');
    expect(onClick.withArgs(1).calledOnce).to.be.true;

    expect(pill.hasClass('closable')).to.be.false;
    expect(pill.hasClass('clickable')).to.be.true;
    expect(pill.hasClass('_loading')).to.be.false;
  });

  it('Closable', function () {
    const onClose = sinon.spy();

    const pill = shallow(
      <RoundedPill text="Text" onClose={onClose} value={2} />
    );

    pill.find('.button').simulate('click');

    expect(onClose.withArgs(2).calledOnce).to.be.true;
    expect(pill.hasClass('closable')).to.be.true;
  });

  it('should render custom className', function () {
    const className = 'haeilrufhaoei';
    const pill = shallow(
      <RoundedPill text="" className={className} />
    );

    expect(pill.hasClass('main')).to.be.true;
    expect(pill.hasClass(className)).to.be.true;
  });
});
