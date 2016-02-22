import _ from 'lodash';

export const groups = {
  assignees: 'assignees',
  watchers: 'watchers',
};

export const emptyTitle = {
  [groups.assignees]: 'Unassigned',
  [groups.watchers]: 'Unwatched',
};
