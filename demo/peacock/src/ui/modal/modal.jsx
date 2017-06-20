/* @flow */

import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

import Overlay from 'ui/overlay/overlay';

import styles from './modal.css';

type Props = {
  show: boolean,
  toggle: Function,
  children?: any,
};

export default class Modal extends Component {
  props: Props;

  componentDidMount() {
    window.addEventListener('keydown', this.handleKeyPress, true);
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKeyPress, true);
  }

  @autobind
  handleKeyPress(e: KeyboardEvent) {
    const { show, toggle } = this.props;
    if (show && e.keyCode === 27) {
      toggle();
    }
  }

  render() {
    const { show, toggle, children } = this.props;
    const modalClass = classNames(styles.modal, {
      [styles.show]: show,
    });

    return (
      <div className={modalClass}>
        <Overlay onClick={toggle} shown={show} />
        <div styleName="modal-wrapper">
          {children}
        </div>
      </div>
    );
  }
}
