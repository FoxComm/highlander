// libs
import _ from 'lodash';
import React, { Component } from 'react';

// helpers
import { ReasonType } from '../../../lib/reason-utils';
import { numberize } from '../../../lib/text-utils';

// components
import ModalBase from './modal-base';
import modal from 'components/modal/wrapper';
import { CancelReason } from '../../fields';

type Props = {
  entity: string;
  label?: string;
  count: number;
  onCancel: Function;
  onConfirm: Function;
};

class CancelModal extends Component {
  props: Props;

  state = {
    reason: null
  };

  render() {
    const { entity, count, label: rawLabel, onCancel, onConfirm } = this.props;

    const entityForm = numberize(entity, count);

    const label = rawLabel
      ? rawLabel
      : <span>Are you sure you want to cancel <b>{count} {numberize(entity, count)}</b>?</span>;

    const body = [
      label,
      <CancelReason
        reasonType={ReasonType.CANCELLATION}
        className="fc-modal-cancel-reason"
        value={this.state.reason}
        onChange={(reason) => this.setState({reason})}
      />
    ];

    return (
      <ModalBase
        title={`Cancel ${_.capitalize(entityForm)}?`}
        label={body}
        onCancel={onCancel}
        onConfirm={() => onConfirm(this.state.reason)}
        saveText="Yes, Cancel"
        className="fc-bulk-action-modal"
        saveDisabled={this.state.reason === null}
      />
    );
  }
}

export default modal(CancelModal);
