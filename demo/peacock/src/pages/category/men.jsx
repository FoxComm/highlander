// @flow

import React from 'react';
import Button from 'ui/buttons';
import CategoryCardList from 'components/category-cards/list';
import CategoryTextList from 'components/category-list/list';
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

const categoryLists = [
  {
    category: { label: 'Sport Shoes', to: '/men/shoes' },
    subCategories: [
      { label: 'Running', to: '/men/shoes/running' },
      { label: 'Basketball', to: '/men/shoes/basketball' },
      { label: 'Soccer', to: '/men/shoes/soccer' },
    ],
  }, {
    category: { label: 'Lifestyle Shoes', to: '/men/shoes/lifestyle' },
    subCategories: [
      { label: 'Originals', to: '/men/shoes/originals' },
      { label: 'Stan Smith', to: '/men/shoes/stan-smith' },
      { label: 'Superstar', to: '/men/shoes/superstar' },
    ],
  }, {
    category: { label: 'Tops', to: '/men/tops' },
    subCategories: [
      { label: 'Jackets', to: '/men/jackets' },
      { label: 'Tees', to: '/men/tees' },
      { label: 'Hoodies', to: '/men/hoodies' },
    ],
  }, {
    category: { label: 'Bottoms', to: '/men/bottoms' },
    subCategories: [
      { label: 'Pants', to: '/men/pants' },
      { label: 'Tights', to: '/men/tights' },
      { label: 'Shorts', to: '/men/shorts' },
    ],
  },
];

const MenCatPage = (props: Props) => {
  const categoryTextLists = categoryLists.map(list => {
    return (
      <div styleName="cat-text-list">
        <CategoryTextList {...list} />
      </div>
    )
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
            <Button>
              Shop Now
            </Button>
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
