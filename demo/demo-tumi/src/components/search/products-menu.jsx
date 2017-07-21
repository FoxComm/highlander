// @flow

import _ from 'lodash';
import React from 'react';
import s from './products-menu.css';

import ProductCell from './product-cell';

import type { Product } from 'modules/products';

type Props = {
  items: Array<Product>,
  term: string,
}

const ProductsMenu = (props: Props) => {
  const panes = _.map(props.items, (p: Product) => {
    return <ProductCell model={p} key={p.id} />;
  });

  return (
    <div className={s.block}>
      <div className={s.main}>
        <div className={s.header}>
          Popular products matching &quot;{props.term}&quot;
        </div>
        <div className={s.products}>
          {panes}
        </div>
        <div className={s.footer}>
          <button type="submit" className={s.footerButton}>View All Results</button>
        </div>
      </div>
      <div className={s.aside}>
        <img src="//i1.adis.ws/i/tumi/02.23.17_search_314x520?w=314&h=520" />
        <div className={s.asideCover} />
      </div>
    </div>
  );
};

export default ProductsMenu;
