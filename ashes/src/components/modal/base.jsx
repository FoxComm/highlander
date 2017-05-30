/* @flow */

// libs
import noop from 'lodash/noop';
import React, { Element } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

// components
import Overlay from 'components/overlay/overlay';

type Props = {
  isVisible: boolean,
  onCancel?: () => void,
  children?: Element<*> | Array<Element<*>>, // This is an ugly bug in Flow :(
};

const ModalContainer = ({ children, isVisible, onCancel = noop }: Props) => {
  let content;

  const handleEscKeyPress = (event) => {
    if (event.keyCode === 27 /*esc*/) {
      event.preventDefault();
      onCancel();
    }
  };

  if (isVisible) {
    content = (
      <div className="fc-modal">
        <Overlay shown={isVisible} onClick={onCancel} />
        <div className="fc-modal-container" onKeyDown={handleEscKeyPress}>
          {children}
        </div>
      </div>
    );
  }

  return (
    <Transition component="div"
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
