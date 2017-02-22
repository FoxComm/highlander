/* @flow */

import capitalize from 'lodash/capitalize';
import React, { Element } from 'react';

import { numberize } from 'lib/text-utils';

import modal from 'components/modal/wrapper';
import ModalBase from './modal-base';

type Props = {
  entity: string,
  stateTitle: string,
  label?: Element<*>|string,
  count: number,
  onCancel: Function,
  onConfirm: Function,
};

const ChangeStateModal = (props: Props) => {
  const { entity, stateTitle, count, label: rawLabel, onCancel, onConfirm } = props;
  const entityForm = numberize(entity, count);

  const label = rawLabel
    ? rawLabel
    : <span>Are you sure you want to change the state to <b>{stateTitle}</b> for <b>{count} {entityForm}</b>?</span>;

  return (
    <ModalBase
      title={`Change ${capitalize(entityForm)} state to ${stateTitle}?`}
      label={label}
      onCancel={onCancel}
      onConfirm={onConfirm}
      saveText="Yes, Change State"
      className="fc-bulk-action-modal"
    />
  );
};

export default modal(ChangeStateModal);
