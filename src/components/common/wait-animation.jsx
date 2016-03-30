import classNames from 'classnames';
import React, { PropTypes } from 'react';

const WaitAnimation = props => {
  const size = props.size || 'l';
  const cls = classNames('fc-wait-animation', `fc-wait-animation_size_${size}`, props.className);

  return (
    <div className={cls}>
      <div className="circle1"></div>
      <div className="circle2"></div>
      <div className="circle3"></div>
      <div className="circle4"></div>
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
