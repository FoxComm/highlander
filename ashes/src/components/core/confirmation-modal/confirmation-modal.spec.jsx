import noop from 'lodash/noop';
import React from 'react';
import sinon from 'sinon';
import { mount } from 'enzyme';

import ConfirmationModal from './confirmation-modal';

describe('ConfirmationModal', function() {
  it('should not render content if not active', function() {
    const modal = mount(<ConfirmationModal onCancel={noop} onConfirm={noop} />);

    expect(modal).to.be.empty;
  });

  it('should render content if active', function() {
    const modal = mount(<ConfirmationModal onCancel={noop} onConfirm={noop} />);

    expect(modal).to.be.empty;

    modal.setProps({ isVisible: true });

    expect(modal).not.to.be.empty;
    expect(modal.find('.footer')).to.exist;
    expect(modal.find('.title').text()).to.be.equal('Confirm');
    expect(modal.find('.body').text()).to.be.equal('Are you sure?');
  });

  it('should handle cancel button click', function() {
    const onCancel = sinon.spy();
    const modal = mount(<ConfirmationModal isVisible label="Sure?" onCancel={onCancel} onConfirm={noop} />);

    modal.find('.cancel').simulate('click');
    expect(onCancel.calledOnce).to.be.true;
  });

  it('should handle confirm button click', function() {
    const onConfirm = sinon.spy();
    const modal = mount(<ConfirmationModal isVisible label="Sure?" onCancel={noop} onConfirm={onConfirm} />);

    modal.find('.save').simulate('click');
    expect(onConfirm.calledOnce).to.be.true;
  });

  it('should render label', function() {
    const modal = mount(<ConfirmationModal isVisible label="Sure?" onCancel={noop} onConfirm={noop} />);

    expect(modal.find('.label').text()).to.be.equal('Sure?');
  });

  it('should render children as label if passed', function() {
    const modal = mount(
      <ConfirmationModal isVisible label="Sure?" onCancel={noop} onConfirm={noop}>
        Really???
      </ConfirmationModal>
    );

    expect(modal.find('.label').text()).to.be.equal('Really???');
  });
});
