/* @flow */

// libs
import React, { Element } from 'react';

// components
import ConfirmationModal from 'components/core/confirmation-modal';

type Props = {
  isVisible: boolean,
  probability: number,
  confirmAction: Function,
  cancelAction: Function,
};

const CodeCreationModal = (props: Props): Element<*> => {
  const { isVisible, probability, confirmAction, cancelAction } = props;

  return (
    <ConfirmationModal
      isVisible={isVisible}
      title="Generate Codes?"
      confirmLabel="Generate Codes"
      onConfirm={confirmAction}
      onCancel={cancelAction}
    >
      <p>
        There is a&nbsp;<strong>{probability}%</strong>&nbsp;
        chance that a coupon code could be guessed based on the quantity and character length chosen.
      </p>
      <p>
        Do you want to generate codes?
      </p>
    </ConfirmationModal>
  );
};

export default CodeCreationModal;
