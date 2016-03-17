//libs
import React, { PropTypes } from 'react';

// helpers
import { prefix } from '../../lib/text-utils';


const prefixed = prefix('fc-lookup__');

/**
 * Lookup item default component
 */
const LookupInput = props => {
  return (
    <input {...props}
      onChange={({target}) => props.onChange(target.value)}
      value={props.value}
      className={prefixed('input')}
      autofocus={true} />
  );
};

/**
 * LookupItem component expected props types
 */
LookupInput.propTypes = {
  value: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};

export default LookupInput;
