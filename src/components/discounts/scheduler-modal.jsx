/* @flow */

import _ from 'lodash';
import React from 'react';
import { numberize } from '../../lib/text-utils';

import styles from './scheduler-modal.css';

import FullObjectScheduler from '../object-scheduler/full-object-scheduler';
import modalWrapper from '../modal/wrapper';
import ContentBox from '../content-box/content-box';
import SaveCancel from '../common/save-cancel';

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
  const entityCap = _.capitalize(entityForm);

  const form = {
    activeFrom: new Date().toISOString(),
    activeTo: null,
  };

  const shadow = {
    activeFrom: {
      ref: 'activeForm',
      type: 'datetime'
    },
    activeTo: {
      ref: 'activeTo',
      type: 'datetime'
    },
  };

  let [newForm, newShadow] = [form, shadow];

  const updateSchedule = (form, shadow) => {
    newForm = form;
    newShadow = shadow;
  };

  const confirmChanges = () => {
    onConfirm(newForm, newShadow);
  };

  return (
    <ContentBox
      title={`Schedule ${entityCap}`}
      className="fc-bulk-action-modal"
      styleName="modal"
      actionBlock={actionBlock}>

      <FullObjectScheduler
        form={form}
        shadow={shadow}
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
