// @flow

import React from 'react';
import Button from 'ui/buttons';
import LocalNav from 'components/local-nav/local-nav';

type Props = {};

const navItems = [
  { label: 'Apparel', to: '/s/women/apparel' },
  { label: 'Shoes', to: '/s/women/shoes' },
  { label: 'Accessories', to: '/s/women/accessories' },
  { label: 'View All', to: '/s/women' },
];

const WomensCatPage = (props: Props) => {
  return (
    <div>
      <LocalNav categoryName="Women" links={navItems} />
    </div>
  );
};

export default WomensCatPage;
