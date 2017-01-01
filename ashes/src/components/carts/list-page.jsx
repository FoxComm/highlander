// @flow
import React, { Element } from 'react';

// components
import { ListPageContainer, makeTotalCounter } from '../list-page';

// redux
import { actions } from 'modules/carts/list';

type Props = {
  children: Element,
}

const CartListPage = (props: Props) => {
  const TotalCounter = makeTotalCounter(state => state.carts.list, actions);

  const navLinks = [
    { title: 'Lists', to: 'carts' },
    { title: 'Activity Trail', to: 'carts-activity-trail' },
  ];

  return (
    <ListPageContainer
      title="Carts"
      subtitle={<TotalCounter/>}
      navLinks={navLinks}
    >
      {props.children}
    </ListPageContainer>
  );
};

export default CartListPage;
