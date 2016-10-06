/* @flow */

import toString from 'lodash/toString';

import type { FormField } from '../../core/types/fields';

const LIST_SCHEDULE = ['Just Once', 'Daily', 'Weekly'];
const LIST_DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export const fields: Array<FormField> = [
  {
    name: 'feed_name',
    type: 'text',
    placeholder: 'Feed Name',
    validation: 'required',
  },
  {
    name: 'feed_url',
    type: 'text',
    placeholder: 'Feed URL',
    validation: 'required format.uri',
  },
  {
    name: 'feed_schedule',
    type: 'radio',
    placeholder: 'How often should we import your feed?',
    values: LIST_SCHEDULE,
    value: LIST_SCHEDULE[0],
  },
  {
    name: 'feed_days',
    type: 'select',
    multi: false,
    placeholder: 'Schedule',
    value: LIST_DAYS[0],
    values: LIST_DAYS,
    parse: toString,
  },
];

export const initialValues = {
  feed_schedule: LIST_SCHEDULE[0],
  feed_days: LIST_DAYS[0],
};
