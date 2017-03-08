import classNames from 'classnames';
import React, { PropTypes } from 'react';
import s from './progressbar.css';

const Progressbar = props => {
  return (
    <div className={classNames(s.root, props.className)}>
      {props.steps.map(step => (
        <div
          key={step.text}
          className={classNames({
            [s.step]: true,
            [s._current]: step.current,
            [s._incompleted]: step.incompleted
          })}
        >
          {step.text}
        </div>
      ))}
    </div>
  );
};

// Progressbar.propTypes = {
//   size: PropTypes.oneOf(['s', 'm', 'l']),
//   className: PropTypes.string,
// };

// Progressbar.defaultProps = {
//   size: 'l'
// };

export default Progressbar;
