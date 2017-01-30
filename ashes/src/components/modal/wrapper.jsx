import React, { Component, PropTypes } from 'react';
import { ModalContainer } from './base';

/**
 * Wrapper for modal for add extra functionality.
 *
 * Currently it supports only toggling 'fc-is-modal-opened' class on body. It may be helpful if you are using
 * huge modal which can have a greater height than viewport.
 * @param Modal
 * @returns {ModalWrapper}
 */
export default function wrapModal(Modal) {
  class ModalWrapper extends Component {

    static propTypes = {
      children: PropTypes.node,
      isVisible: PropTypes.bool
    };

    componentDidUpdate() {
      document.body.classList[this.props.isVisible ? 'add' : 'remove']('fc-is-modal-opened');
    }
    
    componentWillUnmount() {
      document.body.classList.remove('fc-is-modal-opened');
    }

    render() {
      if (!this.props.isVisible) return null;

      return (
        <ModalContainer {...this.props}>
          <Modal {...this.props}>
            {this.props.children}
          </Modal>
        </ModalContainer>
      );
    }
  }

  return ModalWrapper;
}
