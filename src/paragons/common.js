
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

export function archivedStatus(object: Object): boolean {
  const now = moment();
  let archivedAt = _.get(object, 'archivedAt');
  archivedAt = archivedAt ? moment.utc(archivedAt) : null;
  return now.diff(archivedAt) > 0;
}

