// @flow

import classNames from 'classnames';
import React from 'react';

import s from './text-colors.css';

const colors = [];

let i = 1;
while (s[`color-${i}`]) {
  colors.push({
    value: i++,
  });
}

const TextColor = props => {
  return (
    <div className={classNames(s.color, s[`color-${props.value}`])}>
      <div className={s.sample}>Aa</div>
      <div className={s.value} />
      <div className={s.desc} />
    </div>
  );
};

export const TextColors = () => {
  return (
    <div className={s.block}>
      {colors.map(cl => <TextColor key={cl.value} {...cl} />)}
    </div>
  );
};
