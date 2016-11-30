/* @flow */

import React from 'react';

import styles from './cooking-block.css';

const cookingData = [
  {
    imgUrl: '/images/about-page/Icon_Chef_Prepared.svg',
    title: 'CHEF PREPARED',
    description:
      `Novice in the kitchen? No worries! Our chefs have
      crafted award-winning, amazing food. Meals arrive at
      your door prepared and ready to cook.`,
  },
  {
    imgUrl: '/images/about-page/Icon_Deliciously_Convienient.svg',
    title: 'DELICIOULSY CONVENIENT',
    description:
      `Instead of chopping vegetables from a meal kit,
      spend your time doing what you love. Our meals
      take an average of just 30 minutes to make!`,
  },
  {
    imgUrl: '/images/about-page/Icon_Stress_Free.svg',
    title: 'STRESS FREE',
    description:
      `Deciding what to do for dinner just got
      a lot easier. You’ll have access to delicious
      meals anytime of the day, anytime of the week.`,
  },
];

const cookingSections = cookingData.map(({ imgUrl, title, description }, i) => (
  <div styleName="section" key={i}>
    <img styleName="section-img" src={imgUrl} />
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
