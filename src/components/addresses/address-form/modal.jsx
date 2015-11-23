
import React, { PropTypes } from 'react';
import modalWrapper from '../../modal/wrapper';
import AddressForm from './address-form';
import ContentBox from '../../content-box/content-box';

let ModalAddressForm = props => {
  const actionBlock = <i onClick={props.closeAction} className="fc-btn-close icon-close" title="Close"></i>;

  return (
    <ContentBox title="Address Book" actionBlock={ actionBlock }>
      <AddressForm {...props} />
    </ContentBox>
  );
};

ModalAddressForm.propTypes = {
  closeAction: PropTypes.func.isRequired
};

ModalAddressForm = modalWrapper(ModalAddressForm);

export default ModalAddressForm;
