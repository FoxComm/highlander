/* @flow */

import React, { PropTypes } from 'react';
import Transition from 'react-addons-css-transition-group';

type Props = {
  isVisible: boolean,
  onCancel: () => void,
  children: Element|Array<Element>
};

const ModalContainer = (props: Props) => {
  let content;

  const handleEscKeyPress = (event) => {
    if (event.keyCode === 27 /*esc*/) {
      event.preventDefault();
      props.onCancel();
    }
  };

  if (props.isVisible) {
    content = (
      <div className="fc-modal">
        <div className="fc-modal-container" onKeyDown={handleEscKeyPress}>
          {props.children}
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

ModalContainer.propTypes = {
  isVisible: PropTypes.bool.isRequired,
  children: PropTypes.node,
  onCancel: PropTypes.func
};

export {
  ModalContainer
};
