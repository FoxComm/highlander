// @flow

import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { ModalContainer } from './base';

type FunctionComponent<P> = (props: P) => ?React$Element<any>;
type ClassComponent<D, P, S> = Class<Component<D, P, S>>;

/**
 * Wrapper for modal for add extra functionality.
 *
 * Currently it supports only toggling 'fc-is-modal-opened' class on body. It may be helpful if you are using
 * huge modal which can have a greater height than viewport.
 * @param Modal
 * @returns {ModalWrapperInner}
 */
export default function wrapModal<P:Object, S:Object>(
  Modal: ClassComponent<any, P, S> | FunctionComponent<P>,
): Class<Component<void, P, S>> {
  class ModalWrapperInner extends Component {
    props: P;
    state: S;

    static propTypes = {
      children: PropTypes.node,
      isVisible: PropTypes.bool
    };

    componentDidUpdate() {
      if (document.body) {
        if (this.props.isVisible) {
          document.body.classList.add('fc-is-modal-opened');
        } else {
          document.body.classList.remove('fc-is-modal-opened');
        }
      }
    }

    componentWillUnmount() {
      if (document.body) {
        document.body.classList.remove('fc-is-modal-opened');
      }
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

  return ModalWrapperInner;
}
