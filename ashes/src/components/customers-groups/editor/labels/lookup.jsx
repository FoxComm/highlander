//libs
import React from 'react';
import PropTypes from 'prop-types';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';

export const Label = ({value, className}) => {
  const prefixed = prefix(className);

  return (
    <div className={prefixed('value')}>
      {value}
    </div>
  );
};
Label.propTypes = {
  ...propTypes,
  data: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.any,
    label: PropTypes.string,
  })),
};
