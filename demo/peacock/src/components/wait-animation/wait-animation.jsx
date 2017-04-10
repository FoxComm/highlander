// @flow

import classNames from 'classnames';
import React from 'react';
import s from './wait-animation.css';

type Props = {
  size: string, // PropTypes.oneOf(['s', 'm', 'l']),
  className?: string,
};

const WaitAnimation = (props: Props) => {
  const cls = classNames(props.className, s.root, s[`_size_${props.size}`]);

  return (
    <div className={cls}>
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
