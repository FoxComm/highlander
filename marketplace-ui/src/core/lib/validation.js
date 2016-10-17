/* @flow */

import { isEmpty, invoke } from 'lodash';
import validators from './validators';

import type { FormField, FormData, FieldValue, ValidationRule, ErrorsList } from '../../core/types/fields';

const validateField = (value: FieldValue, rules: ValidationRule): ?string => {
  if (!rules) {
    return;
  }

  const rulesArray = Array.isArray(rules) ? rules : rules.split(' ');

  return rulesArray.find(rule => !invoke(validators, rule, value));
};

export default (fields: Array<FormField>) => (values: FormData): ErrorsList => {
  const errors = {};


  fields.forEach(field => {
    if (field.showPredicate && !invoke(field, 'showPredicate', values)) {
      return;
    }

    const violatedRule = validateField(values[field.name], field.validation);

    if (violatedRule) {
      errors[field.name] = `validate.${violatedRule}`;
    }
  });

  return errors;
};
