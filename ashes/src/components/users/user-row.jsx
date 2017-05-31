/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import { RoundedPill } from 'components/core/rounded-pill';
import MultiSelectRow from '../table/multi-select-row';
import UserInitials from '../user-initials/initials';

type Props = {
  user: Object,
  columns: Columns,
  params: Object,
};

const UserRow = (props: Props) => {
  const { user, columns, params } = props;

  const setCellContents = (user: Object, field: string) => {
    const state = _.get(user, 'state', 'invited');
    const text = state.charAt(0).toUpperCase() + state.slice(1);

    switch (field) {
      case 'state':
        return <RoundedPill text={text} />;
      case 'roles':
        return _.get(user, field, 'Super Admin');
      case 'image':
        return <UserInitials name={_.get(user, 'name')} />;
      default:
        return _.get(user, field);
    }
  };

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="user"
      linkParams={{userId: user.id}}
      row={user}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default UserRow;
