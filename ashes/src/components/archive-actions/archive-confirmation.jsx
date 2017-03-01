/* @flow */

// libs
import React, { Component } from 'react';

// components
import ConfirmationDialog from '../modal/confirmation-dialog';
import Alert from '../alerts/alert';

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
      <Alert type="warning">
        Warning! This action cannot be undone
      </Alert>
      <p>
        Are you sure you want to archive <strong>{props.title}</strong> ?
      </p>
    </div>
  );

  return (
    <ConfirmationDialog
      isVisible={props.isVisible}
      header={`Archive ${props.type} ?`}
      body={confirmation}
      cancel="Cancel"
      confirm={`Archive ${props.type}`}
      onCancel={props.closeConfirmation}
      confirmAction={props.archive}
      asyncState={props.archiveState}
    />
  );
};

export default ArchiveConfirmation;
