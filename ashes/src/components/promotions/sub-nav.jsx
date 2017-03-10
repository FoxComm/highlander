/**
 * @flow
 */

// libs
import React, { Component, PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

type SubNavProps = {
  promotionId: string|number,
  applyType: string
};

const SubNav = (props: SubNavProps) => {
  const params = {
    promotionId: props.promotionId
  };

  const isNew = props.promotionId === 'new';
  const isAutoApply = props.applyType === 'auto';
  
  return (
    <LocalNav>
      <IndexLink to="promotion-details" params={params}>Details</IndexLink>
      {!isNew && <Link to="promotion-notes" params={params}>Notes</Link>}
      {!isNew && !isAutoApply && <Link to="promotion-coupons" params={params}>Coupons</Link>}
      {!isNew && <Link to="promotion-activity-trail" params={params}>Activity Trail</Link>}
    </LocalNav>
  );
};

export default SubNav;
