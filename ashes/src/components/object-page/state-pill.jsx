// @flow
import React from 'react';
import { isActive } from 'paragons/common';
import styles from './state-pill.css';

// components
import RoundedPill from '../rounded-pill/rounded-pill';

type Props = {
  object: {
    activeFrom: ?string,
    activeTo: ?string,
  },
}

const StatePill = (props: Props) => {
  const { object = {} } = props;
  const state = isActive(object.activeFrom, object.activeTo);
  const text = state ? 'Active' : 'Inactive';
  const className = state ? void 0 : styles['_inactive'];

  return <RoundedPill text={text} className={className} />;
};

export default StatePill;
