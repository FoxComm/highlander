
/* @flow */

import _ from 'lodash';
import moment from 'moment';

export function isActive(activeFrom: ?string, activeTo: ?string) {
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

export function stateStatus(object: Object): string {
  const activeFrom = _.get(object, 'activeFrom');
  const activeTo = _.get(object, 'activeTo');
  return isActive(activeFrom, activeTo) ? 'Active' : 'Inactive';
}

