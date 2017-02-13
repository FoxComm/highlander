/* @flow */

import capitalize from 'lodash/capitalize';
import React from 'react';

import { numberize } from 'lib/text-utils';

import Modal from './modal';

type Props = {
  entity: string;
  stateTitle: string;
  label?: string;
  count: number;
  onCancel: Function;
  onConfirm: Function;
};

const ChangeStateModal = (props: Props) => {
  const { entity, stateTitle, count, label: rawLabel, onCancel, onConfirm } = props;
  const entityForm = numberize(entity, count);

  const label = rawLabel
    ? rawLabel
    : <span>Are you sure you want to change the state to <b>{stateTitle}</b> for <b>{count} {entityForm}</b>?</span>;

  return (
    <Modal title={`Change ${capitalize(entityForm)} state to ${stateTitle}?`}
           label={label}
           onCancel={onCancel}
           onConfirm={onConfirm}
           className="fc-bulk-action-modal"
    />
  );
};

export default ChangeStateModal;
