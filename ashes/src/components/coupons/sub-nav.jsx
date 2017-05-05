
/* @flow */

// libs
import React from 'react';
import PropTypes from 'prop-types';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

type Params = {
  couponId: string,
};

type Props = {
  params: Params,
};

const SubNav = (props: Props) => {
  const params = props.params;

  const notNew = params.couponId != 'new';

  return (
    <LocalNav>
      <IndexLink to="coupon-details" params={params}>Details</IndexLink>
      { notNew && <Link to="coupon-codes" params={params}>Coupon Codes</Link> }
      { notNew && <Link to="coupon-notes" params={params}>Notes</Link> }
      { notNew && <Link to="coupon-activity-trail" params={params}>Activity Trail</Link> }
    </LocalNav>
  );
};

SubNav.propTypes = {
  params: PropTypes.shape({
    couponId: PropTypes.string,
  }),
};

export default SubNav;

