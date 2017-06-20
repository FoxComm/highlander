// libs
import React from 'react';
import PropTypes from 'prop-types';

// helpers
import { prefix } from '../../lib/text-utils';


const prefixed = prefix('fc-lookup__item');

/**
 * Lookup item default component
 */
const LookupItem = ({model}) => {
  return (
    <div className={prefixed('label')}>
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
