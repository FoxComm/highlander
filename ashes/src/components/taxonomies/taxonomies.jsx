/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from 'components/list-page';

// actions
import { actions } from 'modules/taxonomies/list';

type Props = {
  children: any,
};

// TaxonomiesPage is a presentation component that is responsible for the page
// structure under /admin/taxonomies.
const TaxonomiesPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.taxonomies.list, actions);
  const addAction = () => transitionTo('taxonomy-details', { taxonomyId: 'new', context: 'default' });
  const navLinks = [{ title: 'Lists', to: 'taxonomies' }];

  return (
    <ListPageContainer
      title="Taxonomies"
      subtitle={<TotalCounter />}
      addTitle="Taxonomy"
      handleAddAction={addAction}
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

export default TaxonomiesPage;
