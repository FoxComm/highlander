//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';


const Input = Widget => ({criterion, value, prefixed, changeValue}) => {
  const values = value || [null, null];

  const change = index=> (value) => {
    changeValue([
      ...values.slice(0, index),
      value,
      ...values.slice(index + 1),
    ]);
  };

  return (
    <div className={prefixed('range')}>
      {Widget({criterion, value: values[0], prefixed, changeValue: change(0)})}
      <span className={classNames(prefixed('range__separator'), 'icon-minus', 'fc-align-center')}/>
      {Widget({criterion, value: values[1], prefixed, changeValue: change(1)})}
    </div>
  );
};

Input.propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.arrayOf(PropTypes.any),
  changeValue: PropTypes.func.isRequired,
};

export default Input;
