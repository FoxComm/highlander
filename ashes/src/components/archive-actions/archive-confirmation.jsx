/* @flow */

// libs
import React from 'react';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import Alert from 'components/core/alert';

type Props = {
  title: string,
  isVisible: boolean,
  type: string,
  archive: Function,
  closeConfirmation: Function,
  archiveState: AsyncState,
};

export default (props: Props) =>
  <ConfirmationModal
    isVisible={props.isVisible}
    title={`Archive ${props.type}?`}
    confirmLabel={`Archive ${props.type}`}
    onCancel={props.closeConfirmation}
    onConfirm={props.archive}
    asyncState={props.archiveState}
  >
    <Alert type={Alert.WARNING}>
      Warning! This action cannot be undone
    </Alert>
    <p>
      Are you sure you want to archive <strong>{props.title}</strong>?
    </p>
  </ConfirmationModal>;
