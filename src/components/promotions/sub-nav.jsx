/**
 * @flow
 */

// libs
import React, { Component, PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

type SubNavProps = {
  promotionId: string|number;
}

const SubNav = (props: SubNavProps) => {
  const params = {
    promotionId: props.promotionId,
  };

  return (
    <LocalNav>
      <IndexLink to="promotion-details" params={params}>Details</IndexLink>
      <Link to="promotion-notes" params={params}>Notes</Link>
      <Link to="promotion-activity-trail" params={params}>Activity Trail</Link>
    </LocalNav>
  );
};

export default SubNav;
