/* @flow */

// libs
import React from 'react';
import _ from 'lodash';

// components
import ActionBlock from './action-block';
import Icon from 'ui/icon';

// styles
import styles from './home-page.css';

const mainBlocks = [
  {
    imageUrl: '/images/home-page/Home_Page_Holidays_NEW_2x.jpg',
    description: '',
    title: 'Make Every Day A Reason to Celebrate',
    action: { title: 'Shop Entrées', link: '/ENTRÉES' },
  },
  {
    imageUrl: '/images/home-page/Home_Page_Favorites_2x.jpg',
    description: 'We’re got you covered with dinners ready in less than 30 minutes',
    title: 'Pressed for time?',
    action: { title: 'Shop our favorites', link: '/favorites' },
  },
  {
    imageUrl: '/images/home-page/Home_Page_Best_Sellers_NEW_2x.jpg',
    description: 'Our tried and true favorites you keep coming back for',
    title: 'Best sellers',
    action: { title: 'Shop best sellers', link: '/best-sellers' },
  },
];

const instagramLinks = [
  'https://www.instagram.com/p/BKn_-8wh1mw',
  'https://www.instagram.com/p/BHvPmVLBDaP',
  'https://www.instagram.com/p/BJI5BhJhGpO',
  'https://www.instagram.com/p/BHm4-IJhgtx',
  'https://www.instagram.com/p/BGzWL8gEaqM',
];

const HomePage = () => {
  const actionBlocks = mainBlocks.map(
    (blockProps, i) => <ActionBlock {...blockProps} key={i}/>
  );

  const instagramImages = _.range(1, 6).map(i => {
    return (
      <div
        styleName="instagram-image"
        style={{ backgroundImage: `url(/images/home-page/Instagram_Feed_${i}_2x.jpg)`}}
        key={i}
      >
        <a href={instagramLinks[i - 1]} target="_blank">
          <div styleName="instagram-image-hover">
            <Icon name="fc-instagram" styleName="instagram-icon"/>
          </div>
        </a>
      </div>
    );
  });

  return (
    <div>
      {actionBlocks}
      <div styleName="instagram-info">
        <h1 styleName="instagram-title">BAKE. SNAP. WIN!</h1>
        <div styleName="hashtag-image" />
        <div styleName="instagram-description">
          Love The Perfect Gourmet? Let us know!
          Share the love using #mygourmet for a chance to be featured here!
        </div>
      </div>
      <div styleName="instagram-gallery">
        <div styleName="gallery-wrap">
          {instagramImages}
        </div>
      </div>
    </div>
  );
};

export default HomePage;
