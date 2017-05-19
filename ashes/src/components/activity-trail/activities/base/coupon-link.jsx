
import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/link';

const CouponLink = props => {
  return (
    <Link className="fc-activity__link" to="coupon" params={
      {couponId: props.id}}>
      {props.name}
    </Link>
  );
};

CouponLink.propTypes = {
  id: PropTypes.number.isRequired,
  name: PropTypes.string.isRequired
};

export default CouponLink;
