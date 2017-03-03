import classNames from 'classnames';
import React, { PropTypes } from 'react';
import s from './wait-animation.css';

const WaitAnimation = props => {
  const cls = classNames(s.root, `_size_${props.size}`, props.className);

  return (
    <div className={cls}>
      <div className={s.circle1} />
      <div className={s.circle2} />
      <div className={s.circle3} />
      <div className={s.circle4} />
    </div>
  );
};

WaitAnimation.propTypes = {
  size: PropTypes.oneOf(['s', 'm', 'l']),
  className: PropTypes.string,
};

WaitAnimation.defaultProps = {
  size: 'l'
};

export default WaitAnimation;
