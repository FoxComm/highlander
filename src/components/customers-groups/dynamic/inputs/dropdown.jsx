//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

//components
import Dropdown from '../../../dropdown/dropdown';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func,
};

const Input = ({criterion, value, changeValue}) => {
  return (
    <Dropdown value={value}
              items={criterion.input.config.choices}
              onChange={changeValue} />
  );
};
Input.propTypes = propTypes;

const Label = ({criterion, value, prefixed}) => {
  const item = _.find(criterion.input.config.choices, item => item[0] === value);

  return (
    <div className={prefixed('value')}>
      {item ? item[1]: null}
    </div>
  );
};
Label.propTypes = propTypes;

export default {
  Input,
  Label,
};
