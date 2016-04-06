
// libs
import React, { Component } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

export default class SubNav extends Component {

  render() {
    const params = this.props.params;
    return (
      <LocalNav>
        <IndexLink to="coupon-details" params={params}>Details</IndexLink>
        <Link to="coupon-details" params={params}>Coupons</Link>
        <Link to="coupon-details" params={params}>Eligible SKUs</Link>
        <Link to="coupon-details" params={params}>Notes</Link>
        <Link to="coupon-details" params={params}>Activity Trail</Link>
      </LocalNav>
    );
  }
}
