//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// helpers
import { prefix } from '../../lib/text-utils';

//components
import TextInput from '../../components/forms/text-input';

const prefixed = prefix('fc-lookup');

/**
 * Lookup item default component
 */
const LookupInput = ({className, ...rest}) => {
  return (
    <TextInput className={classNames(prefixed('input'), className)}
               autofocus={true}
               {...rest} />
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
