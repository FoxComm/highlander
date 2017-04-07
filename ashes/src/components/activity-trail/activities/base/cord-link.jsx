// @flow

import React from 'react';
import { Link } from 'components/link';

export type CordProps = {
  cord: Cord
}

const CordLink = ({cord}: CordProps) => {
  const params = {};
  const target = cord.isCart ? 'cart' : 'order';
  params[target] = cord.referenceNumber;

  return (
    <Link className="fc-activity__link" to={target} params={params}>
      {cord.referenceNumber}
    </Link>
  );
};

export default CordLink;
