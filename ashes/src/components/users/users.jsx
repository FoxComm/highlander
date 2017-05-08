import React from 'react';
import { actions } from '../../modules/users/list';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import UserRow from './user-row';
import { SelectableSearchList } from '../list-page';

const getState = state => ({ list: state.users.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const Users = props => {
  const renderRow = (row, index, columns, params) => {
    const key = `user-${row.id}`;
    return (
      <UserRow key={key}
               user={row}
               columns={columns}
               params={params} />
    );
  };

  const tableColumns = [
    { field: 'image', text: 'Image' },
    { field: 'name', text: 'Name' },
    { field: 'email', text: 'Email' },
    { field: 'roles', text: 'Roles' },
    { field: 'createdAt', text: 'Created at', type: 'datetime' },
    { field: 'state', text: 'State' },
  ];

  return (
    <SelectableSearchList
      entity="users.list"
      emptyMessage="No users found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions} />
  );
};

export default connect(getState, mapDispatchToProps)(Users);
