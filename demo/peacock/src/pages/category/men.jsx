// @flow

import React from 'react';
import Button from 'ui/buttons';
import CategoryCardList from 'components/category-cards/list';
import LocalNav from 'components/local-nav/local-nav';
import styles from './men.css';

type Props = {};

const navItems = [
  { label: 'Apparel', to: '/men/apparel' },
  { label: 'Shoes', to: '/men/shoes' },
  { label: 'Accessories', to: '/men/accessories' },
  { label: 'View All', to: '/men' },
];

const categories = [
  {
    label: 'Shoes',
    imageSrc: '/images/categories/men-shoe.jpg',
    to: '/men/shoes',
  }, {
    label: 'Sweatshirts & Hoodies',
    imageSrc: '/images/categories/men-hoodie.jpg',
    to: '/men/hoodies',
  }, {
    label: 'Jackets',
    imageSrc: '/images/categories/men-jacket.jpg',
    to: '/men/jackets',
  }, {
    label: 'Pants',
    imageSrc: '/images/categories/men-pants.jpg',
    to: '/men/pants',
  },
];


const MenCatPage = (props: Props) => {
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
      <CategoryCardList categories={categories} />
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
