/* @flow */

// libs
import upperFirst from 'lodash/upperFirst';
import React from 'react';
import { numberize } from 'lib/text-utils';

// components
import Modal from 'components/core/modal';
import ObjectScheduler from '../object-scheduler/object-scheduler';
import SaveCancel from 'components/core/save-cancel';

// styles
import styles from './scheduler-modal.css';

type Props = {
  entity: string;
  count: number;
  onCancel: Function;
  onConfirm: Function;
};

export default (props: Props) => {
  const { entity, count, onCancel, onConfirm } = props;

  const entityForm = numberize(entity, count);
  const entityCap = upperFirst(entityForm);

  const originalAttrs = {
    activeFrom: {
      t: 'datetime',
      v: new Date().toISOString()
    },
    activeTo: {
      t: 'datetime',
      v: null
    },
  };

  let newAttrs = originalAttrs;

  const updateSchedule = (attrs) => {
    newAttrs = attrs;
  };

  const confirmChanges = () => {
    onConfirm(newAttrs);
  };

  const footer = (
    <SaveCancel
      saveLabel="Confirm changes"
      onSave={confirmChanges}
      onCancel={onCancel}
    />
  );

  return (
    <Modal
      className={styles.modal}
      title={`Schedule ${entityCap}`}
      footer={footer}
      isVisible
      onClose={onCancel}
    >
      <ObjectScheduler
        parent="Discounts"
        attributes={originalAttrs}
        title={entityCap}
        onChange={updateSchedule}
      />
    </Modal>
  );
};
