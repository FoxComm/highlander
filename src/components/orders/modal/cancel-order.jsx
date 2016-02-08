// libs
import React, { PropTypes } from 'react';

// components
import modal from '../../modal/wrapper';
import ContentBox from '../../content-box/content-box';
import { PrimaryButton, CloseButton } from '../../common/buttons';


const CancelOrder = ({onCancel, onConfirm}) => {
  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
  const handleKeyUp = ({key}) => key === 'Escape' && onCancel();

  return (
    <ContentBox title="Cancel orders"
                className="fc-address-form-modal"
                actionBlock={actionBlock}>
      <div className='fc-modal-body'>
        Are you sure you wanna cancel these orders?
      </div>
      <div className='fc-modal-footer'>
        <a tabIndex="2" className="fc-modal-close" onClick={onCancel}>
          Cancel
        </a>
        <PrimaryButton tabIndex="1" autoFocus={true}
                       onClick={onConfirm}
                       onKeyUp={handleKeyUp}>
          YES
        </PrimaryButton>
      </div>
    </ContentBox>
  );
};

CancelOrder.propTypes = {
  onCancel: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
};

export default modal(CancelOrder);
