
import React, { PropTypes } from 'react';

const ModalContainer = props => {
  if (!props.isVisible) return <div></div>;

  return (
    <div className="fc-modal">
      <div className="fc-modal-container">
        {props.children}
      </div>
    </div>
  );
};

ModalContainer.propTypes = {
  isVisible: PropTypes.bool.isRequired,
  children: PropTypes.node
};

export {
  ModalContainer
};
