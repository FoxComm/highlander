//libs
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

// helpers
import { prefix } from '../../lib/text-utils';

//components
import TextInput from 'components/core/text-input';

const prefixed = prefix('fc-lookup');

/**
 * Lookup item default component
 */
const LookupInput = ({ className, ...rest }) => {
  return <TextInput className={classNames(prefixed('input'), className)} {...rest} />;
};

/**
 * LookupItem component expected props types
 */
LookupInput.propTypes = {
  value: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};

export default LookupInput;
