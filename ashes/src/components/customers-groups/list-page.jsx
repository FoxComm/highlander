/* @flow */

// libs
import get from 'lodash/get';
import React, { Element } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as customersActions } from '../../modules/customers/list';

type Props = {
  children?: Element;
}

const GroupsListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => get(state, 'customers.list'), customersActions);

  const navLinks = [
    { title: 'List', to: 'groups' },
  ];

  return (
    <ListPageContainer
      title="Customer Groups"
      subtitle={<TotalCounter />}
      addTitle="Group"
      handleAddAction={ () => transitionTo('new-dynamic-customer-group') }
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

export default GroupsListPage;
