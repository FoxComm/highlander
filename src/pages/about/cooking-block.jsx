/* @flow */

import React from 'react';
import { assetsUrl } from 'lib/env';

import styles from './cooking-block.css';

const cookingData = [
  {
    imgUrl: '/images/about-page/Icon_Chef_Prepared.svg',
    title: 'Delicious',
    description:
      `It begins with inspiration.
      Looking to both new and classic favorites, we create unique, original recipes with you in mind`,
  },
  {
    imgUrl: '/images/about-page/Icon_Deliciously_Convienient.svg',
    title: 'Quality',
    description:
      `We use fresh ingredients to prepare dishes that are flash frozen---ensuring optimal flavor
      and nutrition—and designed to be prepared from frozen in a matter of minutes.`,
  },
  {
    imgUrl: '/images/about-page/Icon_Stress_Free.svg',
    title: 'Time Saving',
    description:
      `We do all the prep work for you, so that your food can go from freezer to oven to table
      in a few short minutes. That way, you can spend less time in the kitchen, and more time
      tending to the rest of your busy life.`,
  },
];

const cookingSections = cookingData.map(({ imgUrl, title, description }, i) => (
  <div styleName="section" key={i}>
    <img styleName="section-img" src={assetsUrl(imgUrl)} />
    <div styleName="section-title">{title}</div>
    <div styleName="section-description">{description}</div>
  </div>
));

const CookingBlock = () => (
  <div styleName="block">
    <div styleName="header-wrap">
      <div styleName="header-text-wrap">
        <div styleName="description">STRESS FREE GOURMET</div>
        <h1 styleName="title">DELICIOUSLY CONVENIENT</h1>
      </div>
    </div>
    <div styleName="sections">
      {cookingSections}
    </div>
  </div>
);

export default CookingBlock;
