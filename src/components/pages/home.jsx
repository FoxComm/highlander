/* @flow */

import React from 'react';
import type { HTMLElement } from '../../types';

type HomeParams = {
  params: Object;
}

const Home = (props: HomeParams) : HTMLElement => {
  return (
    <div>
      <h2>Storefront Demo</h2>
      <p>Current category: {props.params.categoryName ? props.params.categoryName : 'all'}</p>
    </div>
  );
};

export default Home;
