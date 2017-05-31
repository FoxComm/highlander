/* @flow */
import _ from 'lodash';
import React from 'react';

// helpers
import { numberize } from 'lib/text-utils';

// components
import ConfirmationModal from 'components/core/confirmation-modal';

type Props = {
  entity: string;
  stateTitle: string;
  label?: string;
  count: number;
  onCancel: Function;
  onConfirm: Function;
};

export default (props: Props) => {
  const { entity, stateTitle, count, label: rawLabel, onCancel, onConfirm } = props;
  const entityForm = numberize(entity, count);

  const label = rawLabel
    ? rawLabel
    : <span>Are you sure you want to <b>{stateTitle} {count} {entityForm}</b>?</span>;

  return (
    <ConfirmationModal
      title={`Delete ${_.capitalize(entityForm)}?`}
      confirm="Yes, Delete"
      cancel="No"
      confirmAction={onConfirm}
      onCancel={onCancel}
      isVisible
    >
      {label}
    </ConfirmationModal>
  );
};
