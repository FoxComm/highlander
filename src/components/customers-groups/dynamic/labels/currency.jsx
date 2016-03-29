//libs
import React, { PropTypes } from 'react';

//helpers
import { prefix } from '../../../../lib/text-utils';
import formatCurrency from '../../../../lib/format-currency';

//components
import propTypes from '../widgets/propTypes';


export const Label = ({value, className}) => {
  const prefixed = prefix(className);

  return (
    <div className={prefixed('value')}>
      $({formatCurrency(value, {bigNumber: true, fractionBase: 2})})
    </div>
  );
};
Label.propTypes = propTypes;
