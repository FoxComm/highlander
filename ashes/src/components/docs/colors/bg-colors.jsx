// @flow

import classNames from 'classnames';
import React from 'react';

import s from './bg-colors.css';

const colors = [];

let i = 1;
while (s[`color-${i}`]) {
  colors.push({
    value: i++
  });
}

const BgColor = (props) => {
  return (
    <div className={classNames(s.color, s[`color-${props.value}`])}>
      <div className={s.sample} />
      <div className={s.value} />
      <div className={s.desc} />
    </div>
  );
};

export const BgColors = () => {
  return (
    <div className={s.block}>
      {colors.map(cl => <BgColor {...cl} />)}
    </div>
  );
};
