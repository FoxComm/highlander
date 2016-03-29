//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

//components
import Dropdown from '../../../dropdown/dropdown';
import propTypes from '../widgets/propTypes';


export const Label = ({criterion, value, prefixed}) => {
  const item = _.find(criterion.widget.config.choices, item => item[0] === value);

  return (
    <div className={prefixed('value')}>
      {item ? item[1]: null}
    </div>
  );
};
Label.propTypes = propTypes;
