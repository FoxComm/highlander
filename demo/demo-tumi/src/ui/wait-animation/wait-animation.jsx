// @flow

import classNames from 'classnames';
import React from 'react';
import s from './wait-animation.css';

type Props = {
  size: string|number, // PropTypes.oneOf(['s', 'm', 'l']),
  className?: string,
};

const guessSizeLetter = (size: number): string => {
  if (size <= 32) {
    return 's';
  } else if (size <= 54) {
    return 'm';
  }
  return 'l';
};

const WaitAnimation = (props: Props) => {
  let sizeLetter = props.size;
  let style = null;
  if (typeof props.size == 'number') {
    sizeLetter = guessSizeLetter(props.size);
    style = {
      width: `${props.size}px`,
      height: `${props.size}px`,
    };
  }
  const cls = classNames(props.className, s.root,
    s[`_size_${sizeLetter}`]
  );

  return (
    <div className={cls} style={style}>
      <div className={s.circle1} />
      <div className={s.circle2} />
      <div className={s.circle3} />
      <div className={s.circle4} />
    </div>
  );
};

WaitAnimation.defaultProps = {
  size: 'l',
};

export default WaitAnimation;
