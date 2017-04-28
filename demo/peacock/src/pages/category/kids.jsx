// @flow

import React from 'react';
import { Link } from 'react-router';
import Button from 'ui/buttons';
import CategoryCardList from 'components/category-cards/list';
import CategoryTextList from 'components/category-list/list';
import LocalNav from 'components/local-nav/local-nav';
import styles from './kids.css';

const navItems = [
  { label: 'Apparel', to: '/s/kids/apparel' },
  { label: 'Shoes', to: '/s/kids/shoes' },
  { label: 'Accessories', to: '/s/kids/accessories' },
  { label: 'View All', to: '/s/kids' },
];

const categories = [
  {
    label: 'Shoes',
    imageSrc: '/images/categories/men-shoe.jpg',
    to: '/s/kids/shoes',
  }, {
    label: 'Sweatshirts & Hoodies',
    imageSrc: '/images/categories/men-hoodie.jpg',
    to: '/s/kids/hoodies',
  }, {
    label: 'Jackets',
    imageSrc: '/images/categories/men-jacket.jpg',
    to: '/s/kids/jackets',
  }, {
    label: 'Pants',
    imageSrc: '/images/categories/men-pants.jpg',
    to: '/s/kids/pants',
  },
];

const categoryLists = [
  {
    category: { label: 'Sport Shoes', to: '/s/kids/shoes' },
    subCategories: [
      { label: 'Running', to: '/s/kids/shoes/running' },
      { label: 'Basketball', to: '/s/kids/shoes/basketball' },
      { label: 'Soccer', to: '/s/kids/shoes/soccer' },
    ],
  }, {
    category: { label: 'Lifestyle Shoes', to: '/s/kids/shoes/lifestyle' },
    subCategories: [
      { label: 'Originals', to: '/s/kids/shoes/originals' },
      { label: 'Stan Smith', to: '/s/kids/shoes/stan-smith' },
      { label: 'Superstar', to: '/s/kids/shoes/superstar' },
    ],
  }, {
    category: { label: 'Tops', to: '/s/kids/tops' },
    subCategories: [
      { label: 'Jackets', to: '/s/kids/jackets' },
      { label: 'Tees', to: '/s/kids/tees' },
      { label: 'Hoodies', to: '/s/kids/hoodies' },
    ],
  }, {
    category: { label: 'Bottoms', to: '/s/kids/bottoms' },
    subCategories: [
      { label: 'Pants', to: '/s/kids/pants' },
      { label: 'Tights', to: '/s/kids/tights' },
      { label: 'Shorts', to: '/s/kids/shorts' },
    ],
  },
];

const KidsCatPage = () => {
  const categoryTextLists = categoryLists.map((list, index) => {
    return (
      <div key={`cat-${index}`} styleName="cat-text-list">
        <CategoryTextList {...list} />
      </div>
    );
  });

  return (
    <div>
      <LocalNav categoryName="Kids" links={navItems} />
      <div styleName="header-wrap">
        <div styleName="header-content">
          <div styleName="header-title">
            Kids
          </div>
          <div styleName="header-body">
            <p>
              flowers of life
            </p>
            <Link to="/s/kids">
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

export default KidsCatPage;
