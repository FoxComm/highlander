//libs
import moment from 'moment';
import React from 'react';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';
import { labelDateFormat } from '../widgets/date';

export const Label = ({value, className}) => {
  const prefixed = prefix(className);

  return (
    <div className={prefixed('value')}>
      {moment(value).format(labelDateFormat)}
    </div>
  );
};
Label.propTypes = propTypes;
