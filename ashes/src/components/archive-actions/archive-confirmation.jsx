/* @flow */

// libs
import React, {Component, Element} from 'react';
import {autobind} from 'core-decorators';

// components
import ConfirmationDialog from '../modal/confirmation-dialog';
import Alert from '../alerts/alert';

type Props = {
  title: string,
  isVisible: boolean,
  type: string,
  archive: Function,
  closeConfirmation: Function,
};

class ArchiveConfirmation extends Component {
  props:Props;

  render():Element {
    const confirmation = (
      <div>
        <Alert type="warning">
          Warning! This action cannot be undone
        </Alert>
        <p>
          Are you sure you want to archive <strong>{this.props.title}</strong> ?
        </p>
      </div>
    );

    return (
      <ConfirmationDialog
        isVisible={this.props.isVisible}
        header={`Archive ${this.props.type} ?`}
        body={confirmation}
        cancel="Cancel"
        confirm={`Archive ${this.props.type}`}
        onCancel={this.props.closeConfirmation}
        confirmAction={this.props.archive}
      />
    );
  }
}

export default ArchiveConfirmation;