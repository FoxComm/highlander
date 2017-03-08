
// libs
import React, { PropTypes } from 'react';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as inventoryActions } from 'modules/skus/list';

const SkusListPage = (props) => {
  const TotalCounter = makeTotalCounter(state => state.skus.list, inventoryActions);

  const navLinks = [
    { title: 'Lists', to: 'skus' },
    { title: 'Activity Trail', to: 'skus-activity-trail' }
  ];

  return (
    <ListPageContainer
      title="SKUs"
      subtitle={<TotalCounter />}
      navLinks={navLinks} >
      {props.children}
    </ListPageContainer>
  );
};

SkusListPage.propTypes = {
  children: PropTypes.node,
};

export default SkusListPage;
