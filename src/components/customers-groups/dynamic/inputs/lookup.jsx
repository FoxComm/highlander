//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

//components
import { LookupDropdown } from '../../../lookup';
import propTypes from '../widgets/propTypes';


export const Input = ({data, value, prefixed, changeValue}) => {
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
