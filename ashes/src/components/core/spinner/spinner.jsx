// @flow

// lib
import React from 'react';
import classNames from 'classnames';

// styles
import s from './spinner.css';

type Props = {
  size?: 's' | 'm' | 'l';
  className?: string;
};

const Spinner = (props: Props) => {
  const mod = props.size ? s[`size_${props.size}`] : null;
  const cls = classNames(s.root, mod, props.className);

  return (
    <div className={cls}>
      <div className={s.circle1} />
      <div className={s.circle2} />
      <div className={s.circle3} />
      <div className={s.circle4} />
    </div>
  );
};

export default Spinner;
