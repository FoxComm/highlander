//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

//components
import Dropdown from '../../../dropdown/dropdown';
import propTypes from '../widgets/propTypes';


export const Input = ({criterion, value, changeValue}) => {
  return (
    <Dropdown value={value}
              items={criterion.widget.config.choices}
              onChange={changeValue} />
  );
};
Input.propTypes = propTypes;
