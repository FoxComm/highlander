// @flow
import React from 'react';
import styles from './customer-group-row.css';

type Group = {
  name: String,
};

type Props = {
  model: Group,
};

const CustomerGroupRow = (props: Props) => {
  const { name } = props.model;

  return (
    <div styleName="item">
      <div styleName="item-name">
        {name}
      </div>
    </div>
  );
};

export default CustomerGroupRow;
