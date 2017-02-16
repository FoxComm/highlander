/* @flow */

//libs
import React, { PropTypes, Element } from 'react';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';
import Currency from 'components/common/currency';

type LabelProps = {
  value: string|number,
  className: string
};

export const Label = (props: LabelProps): Element<*>=> {
  const { value, className } = props;
  const prefixed = prefix(className);

  return (
    <div className={prefixed('value')}>
      <Currency value={value} />
    </div>
  );
};

Label.propTypes = propTypes;
