/* @flow */

// libs
import React from 'react';

// components
import Modal from 'components/core/modal';
import AddressForm from './address-form';

type Props = {
  isVisible: boolean,
  onCancel?: Function,
};

export default ({ isVisible, ...rest }: Props) => {
  return (
    <Modal title="Address Book" onClose={rest.onCancel} isVisible={isVisible}>
      <AddressForm {...rest} />
    </Modal>
  );
};
