/* @flow */

// libs
import React from 'react';
import _ from 'lodash';
import { assetsUrl } from 'lib/env';

// components
import ActionBlock from './action-block';
import Icon from 'ui/icon';

// styles
import styles from './home-page.css';

const mainBlocks = [
  {
    imageUrl: '/images/home-page/About_Us_Hero_1_03.jpg',
    description: '',
    title: 'WHAT MAKES US DIFFERENT?',
    action: { title: 'Learn more', link: '/about' },
  },
  {
    imageUrl: '/images/home-page/Home_Section_2_1_03.jpg',
    description: '',
    title: 'SIMPLE STARTERS',
    action: { title: 'Shop Now', link: '/APPETIZERS' },
  },
  {
    imageUrl: '/images/home-page/Home_Section_3_1_03.jpg',
    description: 'No planning, shopping, prepping, or cleaning required',
    title: 'DINNER IS SERVED',
    action: { title: 'Shop Now', link: '/ENTRÉES' },
  },
];

const instagramLinks = [
  'https://www.instagram.com/p/BPvzneyhjGD',
  'https://www.instagram.com/p/BPTXM6qhcQ6',
  'https://www.instagram.com/p/BJI5BhJhGpO',
  'https://www.instagram.com/p/BOAOMe_BiKB',
  'https://www.instagram.com/p/BPd3_L9Bjk1',
];

const HomePage = () => {
  const actionBlocks = mainBlocks.map(
    (blockProps, i) => <ActionBlock {...blockProps} key={i}/>
  );

  const instagramImages = _.range(1, 6).map(i => {
    const imageUrl = assetsUrl(`/images/home-page/Instagram_${i}_2x.jpg`);
    return (
      <div
        styleName="instagram-image"
        style={{ backgroundImage: `url(${imageUrl})`}}
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
