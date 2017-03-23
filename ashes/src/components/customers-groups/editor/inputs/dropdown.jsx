//libs
import _ from 'lodash';
import React from 'react';

//components
import Dropdown from 'components/dropdown/dropdown';
import propTypes from '../widgets/propTypes';

export const Input = ({criterion, value, changeValue}) => {
  return (
    <Dropdown value={value}
              items={criterion.widget.config.choices}
              onChange={changeValue} />
  );
};
Input.propTypes = propTypes;

export const getDefault = criterion => criterion.widget.config.choices[0][0];

export const isValid = (value, criterion) => (
  Boolean(_.find(criterion.widget.config.choices, choice => choice[0] === value))
);
