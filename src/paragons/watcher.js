import _ from 'lodash';

export const groups = {
  assignees: 'assignees',
  watchers: 'watchers',
};

export const entityForms = {
  [groups.assignees]: ['assignee', 'assignees'],
  [groups.watchers]: ['watcher', 'watchers'],
};

export const emptyTitle = {
  [groups.assignees]: 'Unassigned',
  [groups.watchers]: 'Unwatched',
};
