// libs
import React, { PropTypes } from 'react';

// helpers
import { prefix } from '../../lib/text-utils';


const prefixed = prefix('fc-lookup__item');

/**
 * Lookup item default component
 */
const LookupItem = ({model}) => {
  return (
    <div className={prefixed('__label')}>
      {model.label}
    </div>
  );
};

/**
 * LookupItem component expected props types
 */
LookupItem.propTypes = {
  model: PropTypes.shape({
    id: PropTypes.any,
    label: PropTypes.string,
  }),
};

export default LookupItem;
