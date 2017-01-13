//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { LookupDropdown } from 'components/lookup';
import propTypes from '../widgets/propTypes';


export const Input = ({data, value, className, changeValue}) => {
  const prefixed = prefix(className);
  const item = _.find(data, {label: value});

  return (
    <LookupDropdown className={prefixed('lookup')}
                    data={data}
                    value={value && item ? item.id : null}
                    minQueryLength={3}
                    onSelect={({label}) => changeValue(label)} />
  );
};
Input.propTypes = {
  ...propTypes,
  data: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.any,
    label: PropTypes.string,
  })),
};

export const getDefault = () => '';

export const isValid = value => Boolean(value);
