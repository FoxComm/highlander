// @flow

import React from 'react';
import Button from 'ui/buttons';
import LocalNav from 'components/local-nav/local-nav';
import styles from './men.css';

type Props = {};

const MenCatPage = (props: Props) => {
  const navItems = [
    { label: 'Apparel', to: '/men/apparel' },
    { label: 'Shoes', to: '/men/shoes' },
    { label: 'Accessories', to: '/men/accessories' },
    { label: 'View All', to: '/men' },
  ];

  return (
    <div>
      <LocalNav categoryName="Men" links={navItems} />
      <div styleName="header-wrap">
          <div styleName="header-content">
          <div styleName="header-title">
            The Game Moves Forward
          </div>
          <div styleName="header-body">
            <p>
              The Reigning Champ Primeknit Tracksuit is the product of unwavering
              innovation and obsessive craftsmanship.
            </p>
            <Button>
              Shop Now
            </Button>
          </div>
        </div>
      </div>
      <div>
        <ul>
          <li>Shoes</li>
          <li>Hoodies & Sweatshirts</li>
          <li>Jackets</li>
          <li>Pants</li>
        </ul>
      </div>
      <div>
        <div>Sport Shoes</div>
        <ul>
          <li>Running</li>
          <li>Basketball</li>
          <li>Soccer</li>
          <li>Shop All</li>
        </ul>
      </div>
      <div>
        <div>Lifestyle Shoes</div>
        <ul>
          <li>Originals</li>
          <li>Stan Smith</li>
          <li>Superstar</li>
          <li>Shop All</li>
        </ul>
      </div>
      <div>
        <div>Tops</div>
        <ul>
          <li>Jackets</li>
          <li>Tees</li>
          <li>Hoodies</li>
          <li>Shop All</li>
        </ul>
      </div>
      <div>
        <div>Bottoms</div>
        <ul>
          <li>Pants</li>
          <li>Tights</li>
          <li>Shorts</li>
          <li>Shop All</li>
        </ul>
      </div>
      <div>
        <ul>
          <li>Men's Training</li>
          <li>Stan Smith Boost</li>
        </ul>
      </div>
    </div>
  );
};

export default MenCatPage;
