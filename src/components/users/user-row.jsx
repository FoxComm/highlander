/* @flow */

import React from 'react';
import _ from 'lodash';

import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (user, field) => {
  switch (field) {
    case 'id':
    case 'name':
    case 'email':
    case 'department':
      return _.get(user, field);
    case 'createdAt':
      return _.get(user, 'joinedAt', '');
    default:
      return null;
  }
};

type Props = {
  user: Object,
  columns: Array<string>,
  params: Object,
};

const UserRow = (props: Props) => {
  const { user, columns, params } = props;
  const key = `user-${user.id}`;

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      //linkTo="user"
      linkParams={{userId: user.id}}
      row={user}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default UserRow;
