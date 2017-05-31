/* @flow */

// libs
import capitalize from 'lodash/capitalize';
import React, { Element } from 'react';

// helpers
import { numberize } from 'lib/text-utils';

// components
import ConfirmationModal from 'components/core/confirmation-modal';

type Props = {
  entity: string,
  stateTitle: string,
  label?: Element<*> | string,
  count: number,
  onCancel: Function,
  onConfirm: Function,
};

export default (props: Props) => {
  const { entity, stateTitle, count, label: rawLabel, onCancel, onConfirm } = props;
  const entityForm = numberize(entity, count);

  const label = rawLabel
    ? rawLabel
    : <span>Are you sure you want to change the state to <b>{stateTitle}</b> for <b>{count} {entityForm}</b>?</span>;

  return (
    <ConfirmationModal
      title={`Change ${capitalize(entityForm)} state to ${stateTitle}?`}
      confirmLabel="Yes, Change State"
      onConfirm={onConfirm}
      onCancel={onCancel}
      isVisible
    >
      {label}
    </ConfirmationModal>
  );
};
