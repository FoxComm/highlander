
/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import ConfirmationDialog from '../../modal/confirmation-dialog';

type Props = {
  isVisible: bool,
  probability: number,
  confirmAction: Function,
  cancelAction: Function,
};

const CodeCreationModal = (props: Props) => {
  const { isVisible, probability, confirmAction, cancelAction } = props;

  const body = (
    <div>
      <p>
        There is a&nbsp;
        <strong>{probability}%</strong>
        &nbsp;chance that a coupon code could be guessed based on the quantity and character length chosen.
      </p>
      <p>
        Do you want to generate codes?
      </p>
    </div>
  );

  return (
    <ConfirmationDialog
      isVisible={isVisible}
      header='Generate Codes?'
      body={body}
      cancel='Cancel'
      confirm='Generate Codes'
      confirmAction={confirmAction}
      onCancel={cancelAction}
    />
  );
};

export default CodeCreationModal;
