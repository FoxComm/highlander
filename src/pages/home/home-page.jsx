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
    imageUrl: '/images/home-page/Home_Page_Holidays_2x.jpg',
    description: 'We’ve got you covered',
    title: 'Entertaining for the holidays?',
    action: { title: 'Shop holiday picks', link: '/holiday' },
    align: 'left',
  },
  {
    imageUrl: '/images/home-page/Home_Page_Favorites_2x.jpg',
    description: 'We’re got you covered with dinners ready in less than 30 minutes',
    title: 'Pressed for time?',
    action: { title: 'Shop our favorites', link: '/favorites' },
    align: 'right',
  },
  {
    imageUrl: '/images/home-page/Home_Page_Best_Sellers_2x.jpg',
    description: 'Our tried and true favorites you keep coming back for',
    title: 'Best sellers',
    action: { title: 'Shop best sellers', link: '/best-sellers' },
    align: 'left',
  },
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
        <div styleName="instagram-image-hover">
          <Icon name="fc-instagram" styleName="instagram-icon"/>
        </div>
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
          Share the love using #mygourmet for a chance to be featured here.
          Every month’s top 3 Instagram posts win a gift card!
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
