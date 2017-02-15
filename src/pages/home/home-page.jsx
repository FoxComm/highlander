/* @flow */

// libs
import React from 'react';
import _ from 'lodash';
import moment from 'moment';

// components
import ActionBlock from './action-block';
import Icon from 'ui/icon';

// styles
import styles from './home-page.css';

const timeTarget = moment('2017-02-15');
const timeNow = moment();

let mainBlocks = [];

if (timeTarget > timeNow) {
  // before timeTarget
  // TODO: Remove this block after 2/15
  mainBlocks = [
    {
      imageUrl: '/images/home-page/Home_Page_Valentines.jpg',
      description: '',
      title: 'Dinners In That Feel Like Nights Out',
      action: { title: 'Shop Now', link: '/VALENTINE' },
    },
    {
      imageUrl: '/images/home-page/Home_Page_Kid_2x.jpg',
      description: 'Under $5 Per Entrée',
      title: 'Weeknight Favorites',
      action: { title: 'Shop Now', link: '/WEEKNIGHT' },
    },
    {
      imageUrl: '/images/home-page/Home_Page_EggRolls.jpg',
      description: 'Dishes you know and love, revisited with unique and delicious twists',
      title: 'A New Spin On The Classics',
      action: { title: 'Shop Now', link: '/SPIN' },
    },
  ];
} else {
  // on and after timeTarget
  mainBlocks = [
    {
      imageUrl: '/images/home-page/Home_Page_Kid_2x.jpg',
      description: 'Under $5 Per Entrée',
      title: 'Weeknight Favorites',
      action: { title: 'Shop Now', link: '/WEEKNIGHT' },
    },
    {
      imageUrl: '/images/home-page/Home_Page_EggRolls.jpg',
      description: 'Dishes you know and love, revisited with unique and delicious twists',
      title: 'A New Spin On The Classics',
      action: { title: 'Shop Now', link: '/SPIN' },
    },
    {
      imageUrl: '/images/home-page/Home_Page_TriedTrue-2x.jpg',
      description: '',
      title: 'Tried and True Favorites',
      action: { title: 'Shop Now', link: '/FAVORITES' },
    },
  ];
}

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
    return (
      <div
        styleName="instagram-image"
        style={{ backgroundImage: `url(/images/home-page/Instagram_${i}_2x.jpg)`}}
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
