/**
 * @flow
 */

// libs
import React from 'react';

// components
import { Link, IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';

type SubNavProps = {
  contentTypeId: string|number,
  applyType: string,
};

const SubNav = (props: SubNavProps) => {
  const params = {
    contentTypeId: props.contentTypeId,
  };

  const isNew = props.contentTypeId === 'new';
  const isAutoApply = props.applyType === 'auto';

  return (
    <PageNav>
      <IndexLink to="content-type-details" params={params}>Details</IndexLink>
      {!isNew && <Link to="content-type-notes" params={params}>Notes</Link>}
      {!isNew && !isAutoApply && <Link to="content-type-coupons" params={params}>Coupons</Link>}
      {!isNew && <Link to="content-type-activity-trail" params={params}>Activity Trail</Link>}
    </PageNav>
  );
};

export default SubNav;
