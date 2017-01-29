
/* @flow */

import _ from 'lodash';
import moment from 'moment';

export function isActive(activeFrom: ?string, activeTo: ?string): boolean {
  const now = moment();

  activeFrom = activeFrom ? moment.utc(activeFrom) : null;
  activeTo = activeTo ? moment.utc(activeTo) : null;

  if (!activeFrom) {
    return false;
  } else if (now.diff(activeFrom) < 0) {
    return false;
  } else if (activeTo && now.diff(activeTo) > 0) {
    return false;
  }

  return true;
}

export function activeStatus(object: Object): string {
  const activeFrom = _.get(object, 'activeFrom');
  const activeTo = _.get(object, 'activeTo');
  return isActive(activeFrom, activeTo) ? 'Active' : 'Inactive';
}

export function isArchived(object: Object): boolean {
  const now = moment();
  let archivedAt = _.get(object, 'archivedAt');
  archivedAt = archivedAt ? moment.utc(archivedAt) : null;
  return now.diff(archivedAt) > 0;
}

export const SAVE_COMBO = {
  NEW: 'save_and_new',
  DUPLICATE: 'save_and_duplicate',
  CLOSE: 'save_and_close',
};

export type SaveComboItems = Array<Array<string>>;

export const SAVE_COMBO_ITEMS: SaveComboItems = [
  [SAVE_COMBO.NEW, 'Save and Create New'],
  [SAVE_COMBO.DUPLICATE, 'Save and Duplicate'],
  [SAVE_COMBO.CLOSE, 'Save and Close'],
];

