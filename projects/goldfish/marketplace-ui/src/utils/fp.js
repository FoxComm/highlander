import isEmpty from 'lodash/isEmpty';

export const applyIfSet = fn => value => (!isEmpty(value) ? fn(value) : null);

export const popValue = multi => value => (multi ? value : value.pop());
