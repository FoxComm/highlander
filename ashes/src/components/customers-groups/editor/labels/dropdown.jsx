//libs
import _ from 'lodash';
import React from 'react';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';

export const Label = ({criterion, value, className}) => {
  const prefixed = prefix(className);
  const item = _.find(criterion.widget.config.choices, item => item[0] === value);

  return (
    <div className={prefixed('value')}>
      {item ? item[1]: null}
    </div>
  );
};
Label.propTypes = propTypes;
