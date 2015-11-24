
import React, { PropTypes } from 'react';
import modalWrapper from '../../modal/wrapper';
import AddressForm from './address-form';
import ContentBox from '../../content-box/content-box';

const AddressFormWrapper = props => {
  const actionBlock = <i onClick={props.closeAction} className="fc-btn-close icon-close" title="Close"></i>;

  return (
    <ContentBox title="Address Book" actionBlock={ actionBlock }>
      <AddressForm {...props} />
    </ContentBox>
  );
};

AddressFormWrapper.propTypes = {
  closeAction: PropTypes.func.isRequired
};

const ModalAddressForm = modalWrapper(AddressFormWrapper);

export default ModalAddressForm;
