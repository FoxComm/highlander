//libs
import React, { PropTypes } from 'react';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func.isRequired,
};

const Input = type => ({value, prefixed, changeValue}) => {
  return (
    <input className={prefixed('field')}
           type={type}
           onChange={({target}) => changeValue(target.value)}
           value={value} />
  );
};
Input.propTypes = propTypes;

const Label = type => ({value, prefixed}) => {
  return (
    <div className={prefixed('')}>
      {value}
    </div>
  );
};
Label.propTypes = propTypes;

export default function (type) {
  return {
    Input: Input(type),
    Label: Label(type)
  };
}
