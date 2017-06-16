/* @flow */

// libs
import noop from 'lodash/noop';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

// components
import Overlay from 'components/overlay/overlay';

// styles
import s from './modal-container.css';

type Props = {
  /** If modal is active or not */
  isVisible: boolean,
  /** Callback to handle close events (overlay/esc click) */
  onClose: () => any,
  /** Modal content */
  children?: Element<any>,
  /** Additional className */
  className?: string,
};

const transitionProps = {
  component: 'div',
  transitionName: 'modal',
  transitionAppear: true,
  transitionAppearTimeout: 120,
  transitionEnterTimeout: 120,
  transitionLeaveTimeout: 100,
};

/**
 * ModalContainer purpose is to provide base means for constructing modal windows.
 * It represents clear modal window with no styles and overlay and used to create custom-styled modal windows.
 *
 * To use project-wide styled modal use `components/core/modal` instead.
 *
 * @class ModalContainer
 */
export default class ModalContainer extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    onClose: noop,
  };

  componentDidMount() {
    if (this.props.isVisible) {
      window.addEventListener('keydown', this.handleKeyPress);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (!this.props.isVisible && nextProps.isVisible) {
      window.addEventListener('keydown', this.handleKeyPress);
    } else if (this.props.isVisible && !nextProps.isVisible) {
      window.removeEventListener('keydown', this.handleKeyPress);
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKeyPress);
  }

  @autobind
  handleKeyPress(e: KeyboardEvent) {
    if (e.keyCode === 27 /*esc*/) {
      e.preventDefault();

      this.props.onClose();
    }
  }

  get content() {
    if (!this.props.isVisible) {
      return null;
    }

    const { children, onClose, className } = this.props;

    return (
      <div className={s.container}>
        <Overlay shown onClick={onClose} />
        <div className={classNames(s.modal, className)} onKeyDown={this.handleKeyPress}>
          {children}
        </div>
      </div>
    );
  }

  render() {
    return (
      <Transition {...transitionProps}>
        {this.content}
      </Transition>
    );
  }
}
