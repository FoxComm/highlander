//libs
import React, { PropTypes } from 'react';

//components
import Dropdown from '../../../dropdown/dropdown';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func.isRequired,
};

const Input = ({criterion, value, changeValue}) => {
  return (
    <Dropdown value={value}
              items={criterion.input.config.choices}
              onChange={changeValue} />
  );
};
Input.propTypes = propTypes;

const Label = ({value, prefixed}) => {
  return (
    <div className={prefixed('')}>
      {value}
    </div>
  );
};
Label.propTypes = propTypes;

export default {
  Input,
  Label,
};
