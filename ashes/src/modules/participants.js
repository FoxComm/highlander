// libs
import _ from 'lodash';
import Api from 'lib/api';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';

// types
import type { EntityType } from 'types/entity';
import type { GroupType } from 'types/participants';

function url(entity: EntityType, group: GroupType, ...args: Array<string>): string {
  return `/${[entity.entityType, entity.entityId, group, ...args].join('/')}`;
}

const _fetchParticipants = createAsyncActions(
  'fetchParticipants',
  function(entity: EntityType, group: GroupType) {
    return Api.get(url(entity, group));
  }
);

export const fetchParticipants = _fetchParticipants.perform;

const _addParticipants = createAsyncActions(
  'addParticipants',
  function(entity: EntityType, group: GroupType, ids: Array<number>) {
    return Api.post(url(entity, group), {
      assignees: ids,
    });
  }
);

export const addParticipants = _addParticipants.perform;

export const _removeParticipant = createAsyncActions(
  'removeParticipant',
  function(entity: EntityType, group: GroupType, id: number) {
    return Api.delete(url(entity, group, id));
  }
);

export const removeParticipant = _removeParticipant.perform;

const initialState = {
  participants: [],
};

function assignParticipants(state, result) {
  return {
    ...state,
    participants: _.map(result, item => item.assignee),
  };
}

const reducer = createReducer({
  [_addParticipants.succeeded]: (state, response) => {
    return assignParticipants(state, response.result);
  },
  [_fetchParticipants.succeeded]: assignParticipants,
  [_removeParticipant.succeeded]: assignParticipants,
}, initialState);

export default reducer;
