// @flow

import React from 'react';
import { Link } from 'react-router';
import Button from 'ui/buttons';
import CategoryCardList from 'components/category-cards/list';
import CategoryTextList from 'components/category-list/list';
import LocalNav from 'components/local-nav/local-nav';
import styles from './men.css';

const navItems = [
  { label: 'Apparel', to: '/s/men/apparel' },
  { label: 'Shoes', to: '/s/men/shoes' },
  { label: 'Accessories', to: '/s/men/accessories' },
  { label: 'View All', to: '/s/men' },
];

const categories = [
  {
    label: 'Shoes',
    imageSrc: '/images/categories/men-shoe.jpg',
    to: '/s/men/shoes',
  }, {
    label: 'Sweatshirts & Hoodies',
    imageSrc: '/images/categories/men-hoodie.jpg',
    to: '/s/men/hoodies',
  }, {
    label: 'Jackets',
    imageSrc: '/images/categories/men-jacket.jpg',
    to: '/s/men/jackets',
  }, {
    label: 'Pants',
    imageSrc: '/images/categories/men-pants.jpg',
    to: '/s/men/pants',
  },
];

const categoryLists = [
  {
    category: { label: 'Sport Shoes', to: '/s/men/shoes' },
    subCategories: [
      { label: 'Running', to: '/s/men/shoes/running' },
      { label: 'Basketball', to: '/s/men/shoes/basketball' },
      { label: 'Soccer', to: '/s/men/shoes/soccer' },
    ],
  }, {
    category: { label: 'Lifestyle Shoes', to: '/s/men/shoes/lifestyle' },
    subCategories: [
      { label: 'Originals', to: '/s/men/shoes/originals' },
      { label: 'Stan Smith', to: '/s/men/shoes/stan-smith' },
      { label: 'Superstar', to: '/s/men/shoes/superstar' },
    ],
  }, {
    category: { label: 'Tops', to: '/s/men/tops' },
    subCategories: [
      { label: 'Jackets', to: '/s/men/jackets' },
      { label: 'Tees', to: '/s/men/tees' },
      { label: 'Hoodies', to: '/s/men/hoodies' },
    ],
  }, {
    category: { label: 'Bottoms', to: '/s/men/bottoms' },
    subCategories: [
      { label: 'Pants', to: '/s/men/pants' },
      { label: 'Tights', to: '/s/men/tights' },
      { label: 'Shorts', to: '/s/men/shorts' },
    ],
  },
];

const MenCatPage = () => {
  const categoryTextLists = categoryLists.map((list, index) => {
    return (
      <div key={`cat-${index}`} styleName="cat-text-list">
        <CategoryTextList {...list} />
      </div>
    );
  });

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
            <Link to="/s/men">
              <Button>
                Shop Now
              </Button>
            </Link>
          </div>
        </div>
      </div>
      <CategoryCardList categories={categories} />
      <div styleName="cat-text-lists">
        {categoryTextLists}
      </div>
    </div>
  );
};

export default MenCatPage;
