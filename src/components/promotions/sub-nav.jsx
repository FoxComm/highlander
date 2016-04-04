/**
 * @flow
 */

// libs
import React, { Component, PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

const SubNav = props => {
  const params = {
    promotionId: props.promotionId,
  };

  return (
    <LocalNav>
      <IndexLink to="promotions-details" params={params}>Details</IndexLink>
      <Link to="promotions-notes" params={params}>Notes</Link>
      <Link to="promotions-activity-trail" params={params}>Activity Trail</Link>
    </LocalNav>
  );
};

export default SubNav;
