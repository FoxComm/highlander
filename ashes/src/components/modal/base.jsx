import React, { PropTypes } from 'react';
import Transition from 'react-addons-css-transition-group';

const ModalContainer = props => {
  let content;

  const handleEscKeyPress = (event) => {
    var cancelFunc = null;
    if (props.onCancel) {
      cancelFunc = props.onCancel;
    } else if (props.cancelAction) {
      cancelFunc = props.cancelAction;
    }

    if (cancelFunc && event.keyCode === 27 /*esc*/) {
      event.preventDefault();
      cancelFunc();
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
  children: PropTypes.node
};

export {
  ModalContainer
};
