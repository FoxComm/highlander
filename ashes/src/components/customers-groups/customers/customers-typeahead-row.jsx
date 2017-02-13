// @flow

import React from 'react';
import styles from './customers.css';

type Props = {
  model: Object,
};

export default (props: Props) => {
  const { name, email, id } = props.model;

  return (
    <div styleName="customer-row">
      <span styleName="name">
        {name}
        &nbsp;
      </span>
      <span styleName="type">
        {email}
        &nbsp;
      </span>
      <span styleName="type" className="fc-icon icon-dot" />
      <span styleName="type">
        &nbsp;
        ID: {id}
      </span>
    </div>
  );
};
