// libs
import _ from 'lodash';
import React, { Component } from 'react';

// helpers
import { ReasonType } from '../../../lib/reason-utils';
import { numberize } from '../../../lib/text-utils';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import { CancelReason } from '../../fields';

// styles
import s from './modal.css';

type Props = {
  entity: string;
  label?: string;
  count: number;
  onCancel: Function;
  onConfirm: Function;
};

type State = {
  reason: any;
};

export default class CancelModal extends Component {
  props: Props;

  state: State = {
    reason: null
  };

  render() {
    const { entity, count, label: rawLabel, onCancel, onConfirm } = this.props;

    const entityForm = numberize(entity, count);

    const label = rawLabel
      ? rawLabel
      : <span key="label">Are you sure you want to cancel <b>{count} {numberize(entity, count)}</b>?</span>;

    const body = [
      label,
      <CancelReason
        reasonType={ReasonType.CANCELLATION}
        className={s.cancelReason}
        value={this.state.reason}
        onChange={(reason) => this.setState({reason})}
        key="reason"
      />
    ];

    return (
      <ConfirmationModal
        isVisible
        title={`Cancel ${_.capitalize(entityForm)}?`}
        confirmLabel="Yes, Cancel"
        onCancel={onCancel}
        onConfirm={() => onConfirm(this.state.reason)}
        saveDisabled={this.state.reason === null}
      >
        {body}
      </ConfirmationModal>
    );
  }
}
