/* @flow */

import isEmpty from 'lodash/isEmpty';

import type { FormField } from '../../core/types/fields';

export const LIST_SCHEDULE = ['Once', 'Daily', 'Weekly'];
export const LIST_DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export const fields: Array<FormField> = [
  {
    name: 'name',
    type: 'text',
    placeholder: 'Feed Name',
    validation: 'required',
  },
  {
    name: 'url',
    type: 'url',
    placeholder: 'Feed URL',
    validation: 'required format.uri',
  },
  {
    name: 'schedule',
    type: 'radio',
    placeholder: 'How often should we import your feed?',
    values: LIST_SCHEDULE,
  },
  {
    name: 'scheduleDays',
    type: 'select',
    multi: false,
    placeholder: 'Schedule',
    value: LIST_DAYS[0],
    values: LIST_DAYS,
    showPredicate: values => !isEmpty(values) && values.schedule === LIST_SCHEDULE[2],
  },
];

export const initialValues = {
  schedule: LIST_SCHEDULE[0],
  scheduleDays: LIST_DAYS[0],
};
