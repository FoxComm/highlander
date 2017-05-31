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
  onClose: () => any,
  children?: Element<any>,
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

export function withModal<P:Object, S:Object>(InnerComponent: Class<Component<any, P, S>>) {
  return ({ isVisible, onClose, ...rest }: { isVisible: boolean, onClose: () => any }) => (
    <ModalContainer isVisible={isVisible} onClose={onClose}>
      <InnerComponent {...rest} />
    </ModalContainer>
  );
}
