// @flow
import React, { PropTypes } from 'react';
import styles from './user-row.css';

// components
import Initials from '../user-initials/initials';

type Props = {
  model: UserType,
}

const UserRow = (props: Props) => {
  const { name, email } = props.model;

  return (
    <div styleName="item">
      <div styleName="item-icon">
        <Initials {...props.model} />
      </div>
      <div styleName="item-name">
        {name}
      </div>
      <div styleName="item-email">
        {email}
      </div>
    </div>
  );
};

export default UserRow;
