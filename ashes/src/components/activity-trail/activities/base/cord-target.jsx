// @flow

import React from 'react';
import PropTypes from 'prop-types';
import CordLink from './cord-link';
import type { CordProps } from './cord-link';

const CordTarget = ({cord}: CordProps) => {
  const title = cord.isCart ? 'Cart': 'Order';
  return (
    <span>
      {title}
      &nbsp;
      <CordLink cord={cord} />
    </span>
  );
};

CordTarget.propTypes = {
  cord: PropTypes.shape({
    isCart: PropTypes.bool,
    referenceNumber: PropTypes.string.isRequired,
  }),
};

export default CordTarget;
