/* @flow */

import get from 'lodash/fp/get';
import type { FormField } from '../../core/types/fields';

const normalize = (value, prev) => (/^([0-9]+(\.[0-9]{0,2})?)?$/.test(value) ? value : prev);

export const fields: Array<FormField> = [
  {
    name: 'usp7',
    type: 'checkbox',
    placeholder: 'UPS Standard (7 days)',
  },
  {
    name: 'usp7Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('usp7'),
    validation: 'required',
    normalize,
  },
  {
    name: 'usp2',
    type: 'checkbox',
    placeholder: 'UPS 2-Day (2 days)',
  },
  {
    name: 'usp2Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('usp2'),
    validation: 'required',
    normalize,
  },
  {
    name: 'usp1',
    type: 'checkbox',
    placeholder: 'UPS Overnight (1 day)',
  },
  {
    name: 'usp1Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('usp1'),
    validation: 'required',
    normalize,
  },
  {
    name: 'usps7',
    type: 'checkbox',
    placeholder: 'USPS Standard (7 days)',
  },
  {
    name: 'usps7Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('usps7'),
    validation: 'required',
    normalize,
  },
  {
    name: 'usps2',
    type: 'checkbox',
    placeholder: 'USPS 2-Day (2 days)',
  },
  {
    name: 'usps2Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('usps2'),
    validation: 'required',
    normalize,
  },
  {
    name: 'usps1',
    type: 'checkbox',
    placeholder: 'USPS Overnight (1 day)',
  },
  {
    name: 'usps1Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('usps1'),
    validation: 'required',
    normalize,
  },
  {
    name: 'fedex7',
    type: 'checkbox',
    placeholder: 'FedEx Standard (7 days)',
  },
  {
    name: 'fedex7Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('fedex7'),
    validation: 'required',
    normalize,
  },
  {
    name: 'fedex2',
    type: 'checkbox',
    placeholder: 'FedEx 2-Day (2 days)',
  },
  {
    name: 'fedex2Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('fedex2'),
    validation: 'required',
    normalize,
  },
  {
    name: 'fedex1',
    type: 'checkbox',
    placeholder: 'FedEx Overnight (1 day)',
  },
  {
    name: 'fedex1Price',
    type: 'currency',
    placeholder: 'Price',
    showPredicate: get('fedex1'),
    validation: 'required',
    normalize,
  },
];
