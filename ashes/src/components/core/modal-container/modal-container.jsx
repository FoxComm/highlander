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
  isVisible: boolean,
  onClose?: () => void,
  children?: Element<any>,
};

const transitionProps = {
  component: 'div',
  transitionName: 'modal',
  transitionEnterTimeout: 120,
  transitionLeaveTimeout: 100,
};

export class ModalContainer extends Component {
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
    if (nextProps.isVisible) {
      window.addEventListener('keydown', this.handleKeyPress);
    } else {
      window.removeEventListener('keydown', this.handleKeyPress);
    }
  }

  @autobind
  handleKeyPress(e) {
    if (e.keyCode === 27 /*esc*/) {
      e.preventDefault();

      this.props.onClose();
    }
  }

  get content() {
    if (!this.props.isVisible) {
      return null;
    }

    const { children, isVisible, onClose, className } = this.props;

    return (
      <div className={s.container}>
        <Overlay shown={isVisible} onClick={onClose} />
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

export const withModal = (InnerComponent: Component) => ({ isVisible, onClose, ...rest }) => (
  <ModalContainer isVisible={isVisible} onClose={onClose}>
    <InnerComponent {...rest} />
  </ModalContainer>
);
