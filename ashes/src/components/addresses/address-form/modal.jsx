
import React, { PropTypes } from 'react';
import modalWrapper from '../../modal/wrapper';
import AddressForm from './address-form';
import ContentBox from '../../content-box/content-box';

const AddressFormWrapper = props => {
  const actionBlock = <i onClick={props.onCancel} className="fc-btn-close icon-close" title="Close"/>;

  return (
    <ContentBox title="Address Book" className="fc-address-form-modal" actionBlock={ actionBlock }>
      <AddressForm {...props} />
    </ContentBox>
  );
};

AddressFormWrapper.propTypes = {
  onCancel: PropTypes.func.isRequired
};

const ModalAddressForm = modalWrapper(AddressFormWrapper);

export default ModalAddressForm;
