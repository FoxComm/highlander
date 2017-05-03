/* @flow */

// libs
import noop from 'lodash/noop';
import classNames from 'classnames';
import React, { Element } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

// components
import Overlay from 'components/overlay/overlay';

// styles
import s from './base.css';

type Props = {
  isVisible: boolean,
  size?: 'big',
  onCancel?: () => void,
  children?: Element<*> | Array<Element<*>>, // This is an ugly bug in Flow :(
};

const ModalContainer = ({ children, isVisible, onCancel = noop, size }: Props) => {
  let content;

  const handleEscKeyPress = (event) => {
    if (event.keyCode === 27 /*esc*/) {
      event.preventDefault();
      onCancel();
    }
  };

  if (isVisible) {
    content = (
      <div className={classNames('fc-modal', s.block, { [s._big]: size === 'big' })}>
        <Overlay shown={isVisible} onClick={onCancel} />
        <div className={classNames('fc-modal-container', s.viewport)} onKeyDown={handleEscKeyPress}>
          {children}
        </div>
      </div>
    );
  }

  return (
    <Transition
      component="div"
      transitionName="modal"
      transitionAppear={true}
      transitionAppearTimeout={120}
      transitionEnterTimeout={120}
      transitionLeaveTimeout={100}
    >
      {content}
    </Transition>
  );
};

export {
  ModalContainer
};
