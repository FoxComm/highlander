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

const ArchiveConfirmation = (props: Props) => {
  const confirmation = (
    <div>
      <Alert type={Alert.WARNING}>
        Warning! This action cannot be undone
      </Alert>
      <p>
        Are you sure you want to archive <strong>{props.title}</strong> ?
      </p>
    </div>
  );

  return (
    <ConfirmationModal
      isVisible={props.isVisible}
      title={`Archive ${props.type} ?`}
      body={confirmation}
      cancel="Cancel"
      confirm={`Archive ${props.type}`}
      onConfirm={props.archive}
      onCancel={props.closeConfirmation}
      asyncState={props.archiveState}
    />
  );
};

export default ArchiveConfirmation;
