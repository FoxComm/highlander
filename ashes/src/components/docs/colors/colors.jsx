// @flow

import classNames from 'classnames';
import React from 'react';

import stc from './text-colors.css';
import sbc from './bg-colors.css';

const colorsText = [];
const colorsBack = [];

let i = 1;
while (stc[`color-${i}`]) {
  colorsText.push({
    value: i++,
  });
}

i = 1;
while (sbc[`color-${i}`]) {
  colorsBack.push({
    value: i++,
  });
}

const TextColor = props => {
  return (
    <div className={classNames(stc.color, stc[`color-${props.value}`])}>
      <div className={stc.sample}>Aa</div>
      <div className={stc.value} />
      <div className={stc.desc} />
    </div>
  );
};

export const Colors = () => {
  return (
    <div className={stc.block}>
      {colorsText.map(cl => <TextColor {...cl} key={cl.value} />)}
    </div>
  );
};

const BgColor = props => {
  return (
    <div className={classNames(sbc.color, sbc[`color-${props.value}`])}>
      <div className={sbc.sample} />
      <div className={sbc.value} />
      <div className={sbc.desc} />
    </div>
  );
};

export const Backgrounds = () => {
  return (
    <div className={sbc.block}>
      {colorsBack.map(cl => <BgColor {...cl} key={cl.value} />)}
    </div>
  );
};
