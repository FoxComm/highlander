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
    title: 'DELICIOULSY CONVIENIENT',
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
  <div styleName="cooking-section" key={i}>
    <img styleName="img" src={imgUrl} />
    <div styleName="title">{title}</div>
    <div styleName="description">{description}</div>
  </div>
));

const CookingBlock = () => (
  <div styleName="cooking-block">
    <div styleName="cooking-header-wrap">
      <div styleName="text-wrap">
        <div styleName="cooking-description">STRESS FREE GOURMET</div>
        <h1 styleName="cooking-title">DELICIOUSLY CONVIENT</h1>
      </div>
    </div>
    {cookingSections}
  </div>
);

export default CookingBlock;
