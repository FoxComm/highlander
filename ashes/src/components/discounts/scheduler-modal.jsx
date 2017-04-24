/* @flow */

import _ from 'lodash';
import React from 'react';
import { numberize } from 'lib/text-utils';

import styles from './scheduler-modal.css';

import ObjectScheduler from '../object-scheduler/object-scheduler';
import modalWrapper from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import SaveCancel from 'components/core/save-cancel';

type Props = {
  entity: string;
  count: number;
  onCancel: Function;
  onConfirm: Function;
};

const SchedulerModal = (props: Props) => {
  const {entity, count, onCancel, onConfirm} = props;

  const actionBlock = <i onClick={onCancel} className="fc-btn-close icon-close" title="Close" />;
  const entityForm = numberize(entity, count);
  const entityCap = _.upperFirst(entityForm);

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

  return (
    <ContentBox
      title={`Schedule ${entityCap}`}
      className="fc-bulk-action-modal"
      styleName="modal"
      actionBlock={actionBlock}>

      <ObjectScheduler
        parent="Discounts"
        attributes={originalAttrs}
        title={entityCap}
        onChange={updateSchedule}
      />

      <SaveCancel
        className="fc-modal-footer"
        onCancel={onCancel}
        onSave={confirmChanges}
        cancelText="Cancel"
        saveText="Confirm changes"
      />
    </ContentBox>
  );
};

export default modalWrapper(SchedulerModal);
