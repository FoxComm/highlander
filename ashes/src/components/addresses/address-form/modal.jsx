
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import modalWrapper from '../../modal/wrapper';
import AddressForm from './address-form';
import ContentBox from '../../content-box/content-box';

type Props = {
  onCancel?: Function;
};

const AddressFormWrapper = (props: Props) => {
  const actionBlock = <i onClick={props.onCancel} className="fc-btn-close icon-close" title="Close" />;

  return (
    <ContentBox title="Address Book" className="fc-address-form-modal" actionBlock={ actionBlock }>
      <AddressForm {...props} />
    </ContentBox>
  );
};

AddressFormWrapper.propTypes = {
  onCancel: PropTypes.func.isRequired
};

const ModalAddressForm: Class<Component<void, Props, any>> = modalWrapper(AddressFormWrapper);

export default ModalAddressForm;
