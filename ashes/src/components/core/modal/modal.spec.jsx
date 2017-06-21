import noop from 'lodash/noop';
import React from 'react';
import sinon from 'sinon';
import { mount } from 'enzyme';

import Modal from './modal';

describe('Modal', function() {
  it('should render empty Modal if not active', function() {
    const modal = mount(<Modal title="Modal title" onClose={noop}>Modal Content</Modal>);

    expect(modal).to.be.empty;
  });

  it('should render Modal content if active', function() {
    const modal = mount(<Modal title="Title" onClose={noop}>Modal Content</Modal>);

    expect(modal).to.be.empty;

    modal.setProps({ isVisible: true });

    expect(modal).not.to.be.empty;
    expect(modal.find('.footer')).not.to.exist;
    expect(modal.find('.title').text()).to.be.equal('Title');
    expect(modal.find('.body').text()).to.be.equal('Modal Content');
  });

  it('should render Modal footer', function() {
    const modal = mount(<Modal isVisible footer={<span>Footer</span>} onClose={noop}>Modal Content</Modal>);

    expect(modal.find('.footer')).to.exist;
    expect(modal.find('.footer').text()).to.equal('Footer');
  });

  it('should handle close button click', function() {
    const onClose = sinon.spy();
    const modal = mount(<Modal isVisible footer={<span>Footer</span>} onClose={onClose}>Modal Content</Modal>);

    modal.find('.close').simulate('click');
    expect(onClose.calledOnce).to.be.true;
  });
});
