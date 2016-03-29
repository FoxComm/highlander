//libs
import React, { PropTypes } from 'react';

//helpers
import formatCurrency from '../../../../lib/format-currency';

//components
import propTypes from '../widgets/propTypes';


export const Label = ({value, prefixed}) => {
  return (
    <div className={prefixed('value')}>
      $({formatCurrency(value, {bigNumber: true, fractionBase: 2})})
    </div>
  );
};
Label.propTypes = propTypes;
