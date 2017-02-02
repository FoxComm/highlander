// @flow

import React from 'react';
import styles from './groups.css';

type Props = {
  model: TCustomerGroupShort,
};

export default (props: Props) => {
  const { name, id } = props.model;

  return (
    <div styleName="group-row">
      <span styleName="name">
        {name}
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
