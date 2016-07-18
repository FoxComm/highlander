import React, { PropTypes } from 'react';
import Transition from 'react-addons-css-transition-group';

const ModalContainer = props => {
  let content;

  if (props.isVisible) {
    content = (
      <div className="fc-modal">
        <div className="fc-modal-container">
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
  children: PropTypes.node
};

export {
  ModalContainer
};
