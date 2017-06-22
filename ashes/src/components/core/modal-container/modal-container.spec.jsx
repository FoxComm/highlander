import noop from 'lodash/noop';
import React from 'react';
import sinon from 'sinon';
import simulant from 'simulant';
import { mount } from 'enzyme';

import ModalContainer from './modal-container';

describe('ModalContainer', function() {
  it('should render empty ModalContainer', function() {
    const modal = mount(<ModalContainer isVisible={false} onClose={noop}>Modal Content</ModalContainer>);

    expect(modal).to.be.empty;
  });

  it('should render modal with overlay and content', function() {
    const modal = mount(<ModalContainer isVisible={false} onClose={noop}>Modal Content</ModalContainer>);

    expect(modal).to.be.empty;

    modal.setProps({ isVisible: true });

    expect(modal).not.to.be.empty;
    expect(modal.find('.overlay')).to.exist;
    expect(modal.find('.container')).to.exist;
    expect(modal.find('.modal')).to.exist;
    expect(modal.text()).to.be.equal('Modal Content');
  });

  it('should handle close on overlay', function() {
    const onClose = sinon.spy();
    const modal = mount(<ModalContainer isVisible onClose={onClose}>Modal Content</ModalContainer>);

    modal.find('.overlay').simulate('click');

    expect(onClose.calledOnce).to.be.true;
  });

  it('should not handle escape click when hidden', function() {
    const onClose = sinon.spy();
    mount(<ModalContainer isVisible={false} onClose={onClose}>Modal Content</ModalContainer>, {
      attachTo: createContainer(),
    });

    const event = simulant('keydown', { keyCode: 27 });

    simulant.fire(document.body, event);

    expect(onClose.calledOnce).to.be.false;
  });

  it('should handle escape click when shown', function() {
    const onClose = sinon.spy();
    mount(<ModalContainer isVisible onClose={onClose}>Modal Content</ModalContainer>, { attachTo: createContainer() });

    const event = simulant('keydown', { keyCode: 27 });

    simulant.fire(document.body, event);

    expect(onClose.calledOnce).to.be.true;
  });
});
