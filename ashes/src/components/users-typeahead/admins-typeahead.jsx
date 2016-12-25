// @flow
import _ from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';

// components
import UsersTypeahead from './users-typeahead';

// actions
import reducer, { suggestAdmins } from 'modules/users/suggest';

// types
type Props = {
  suggested: Array<UserType>,
  suggestUsers: (term: string) => AbortablePromise,
  suggestState: AsyncState,
}

function mapStateToProps(state) {
  return {
    suggested: state.admins,
    suggestState: _.get(state.asyncActions, 'suggestAdmins', {}),
  };
}

const AdminsTypeahead = (props: Props) => {
  return <UsersTypeahead {...props}/>;
};

export default _.flowRight(
  makeLocalStore(addAsyncReducer(reducer)),
  connect(mapStateToProps, {suggestUsers: suggestAdmins})
)(AdminsTypeahead);
