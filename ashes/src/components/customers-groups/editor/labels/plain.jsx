//libs
import React from 'react';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';

export const Label = type => ({value, className}) => {
  const prefixed = prefix(className);

  return (
    <div className={prefixed('value')}>
      {value}
    </div>
  );
};

Label.propTypes = propTypes;
