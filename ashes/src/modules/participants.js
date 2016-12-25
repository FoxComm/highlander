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
    const { dispatch } = this;
    return Api.post(url(entity, group), {
      assignees: ids,
    }).then(result => {
      dispatch(fetchParticipants(entity, group));
      return result;
    });
  }
);

export const addParticipants = _addParticipants.perform;

export const _removeParticipant = createAsyncActions(
  'removeParticipant',
  function(entity: EntityType, group: GroupType, id: number) {
    const { dispatch } = this;
    return Api.delete(url(entity, group, id)).then(() => {
      dispatch(fetchParticipants(entity, group));
    });
  }
);

export const removeParticipant = _removeParticipant.perform;

const initialState = {
  participants: [],
};

const reducer = createReducer({
  [_fetchParticipants.succeeded]: (state, result) => {
    return {
      ...state,
      participants: _.map(result, item => item.assignee),
    };
  },
}, initialState);

export default reducer;
