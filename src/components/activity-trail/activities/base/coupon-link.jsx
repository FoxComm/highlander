
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

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
