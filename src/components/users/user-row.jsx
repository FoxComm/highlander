/* @flow */

import React from 'react';
import _ from 'lodash';

import { activeStatus } from '../../paragons/common';

import RoundedPill from '../rounded-pill/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';
import UserInitials from '../user-initials/initials';

const setCellContents = (user: Object, field: string) => {
  switch (field) {
    case 'state':
      return <RoundedPill text={activeStatus(user)} />;
    case 'roles':
      return _.get(user, field, 'Super Admin');
    case 'image':
      return <UserInitials name={_.get(user, 'name')} />;
    default:
      return _.get(user, field);
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
      linkTo="user"
      linkParams={{userId: user.id}}
      row={user}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default UserRow;
