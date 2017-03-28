/* @flow */

// libs
import get from 'lodash/get';
import React, { Element } from 'react';
import { transitionTo } from 'browserHistory';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions as customersActions } from '../../modules/customer-groups/list';

type Props = {
  children?: Array<Element<*>>|Element<*>;
};

const navLinks = [
  { title: 'List', to: 'groups' },
];

const GroupsListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.customerGroups.list, customersActions);

  return (
    <ListPageContainer
      title="Customer Groups"
      subtitle={<TotalCounter />}
      addTitle="Group"
      handleAddAction={ () => transitionTo('customer-group-wizard') }
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

export default GroupsListPage;
