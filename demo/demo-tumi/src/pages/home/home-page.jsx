/* @flow */

// libs
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { Link } from 'react-router';

import Slider from 'react-slick';
import Youtube from 'components/youtube';

// styles
import styles from './home-page.css';

type State = {
  playerActive: boolean;
}

const settingsSlides = {
  dots: false,
  arrows: false,
  fade: true,
  infinite: true,
  autoplay: true,
  speed: 500,
  slidesToShow: 1,
};

const slidesOrlebar = [
  'https://cdn-media.amplience.com/tumi/images/SP17_LS_TUMI OB_1_1400x933.jpg',
  'https://cdn-media.amplience.com/tumi/images/SP17_LS_TUMI OB_2_1400x933.jpg',
  'https://cdn-media.amplience.com/tumi/images/SP17_LS_TUMI OB_3_1400x933.jpg',
];

const slidesPrint = [
  'https://cdn-media.amplience.com/tumi/images/OB-LP-Instastory3_1600x625.jpg',
  'https://cdn-media.amplience.com/tumi/images/OB-LP-Instastory5_1600x625.jpg',
];

const categories = [
  {
    title: 'The best in travel.',
    link: 'Shop Luggage',
    to: '/categories/luggage',
    bg: 'https://cdn-media.amplience.com/tumi/images/04.27.17_HP_Secondary_luggage-_600x600.jpg',
  },
  {
    title: 'Mum\'s the Word',
    link: 'Shop Backpacks',
    to: '/categories/backpacks',
    bg: 'https://cdn-media.amplience.com/tumi/images/04.27.17_HP_Secondary_mothersday_600x600.jpg',
  },
  {
    title: 'Ladies\' choice.',
    link: 'Shop Women\'s',
    to: '/categories/accessories',
    bg: 'https://cdn-media.amplience.com/tumi/images/04.27.17_HP_Secondary_women-_600x600.jpg',
  },
];

class HomePage extends Component {
  state: State = {
    playerActive: false,
  };

  @autobind
  renderSlide(slide: string) {
    return <div styleName="slide" style={{ backgroundImage: `url("${slide}")` }} key={slide} />;
  }

  @autobind
  renderCategory(category: Object) {
    return (
      <div styleName="collection" key={category.to}>
        <div styleName="collection-image" style={{ backgroundImage: `url(${category.bg})` }} />
        <div styleName="collection-title">{category.title}</div>
        <Link styleName="collection-link" to={category.to}>{category.link}&nbsp;&gt;</Link>
      </div>
    );
  }

  render() {
    return (
      <div styleName="page">
        <div styleName="hero">
          <Youtube
            playButtonClassName={styles.playButton}
            YTid="KyaEZoVkR0c"
            height="100%"
            width="89%"
            fadedAtStart
          />
        </div>
        <div styleName="orlebar">
          <div styleName="orlebar-logo">
            <img src="https://cdn-media.amplience.com/tumi/images/TUMI_X_ORLEBAR_BROWN_LOCKUP.png" />
          </div>
          <div styleName="caption">
            In celebration of the ultimate summer weekend and all its possibilities, TUMI teamed up with luxury<br />
            swimwear brand Orlebar Brown to create an exclusive collection of poolside-perfect totes.
          </div>

          <Slider styleName="slides" {...settingsSlides}>
            {slidesOrlebar.map(this.renderSlide)}
          </Slider>
          <div styleName="product">
            <div styleName="product-image">
              <img src="https://cdn-media.amplience.com/tumi/images/TUMI-2221394PP2_main_72dpi.jpg" />
            </div>
            <div styleName="product-title">TUMI X Orlebar Brown</div>
            <div styleName="product-links">
              <a styleName="product-link" href="#">Medium Tote&nbsp;&gt;</a>
              <a styleName="product-link" href="#">Small Tote&nbsp;&gt;</a>
            </div>
          </div>

          <a styleName="orlebar-button-shop" href="#">Shop the Collection</a>
        </div>

        <div styleName="print">
          <div styleName="print-text">
            <div styleName="print-title">
              Recognize this print? It's based on an iconic<br />
              photograph of the Hotel du Cap-Eden-Roc in France,<br />
              taken by the legendary photographer Slim Aarons.<br />
            </div>
            <div styleName="print-by">
              Photograph of Eden-Roc Pool by Slim Aarons, from the Slim Aarons Archive,
              owned and housed by Getty Images
            </div>
          </div>

          <div styleName="print-cover" />

          <Slider styleName="print-slides" {...settingsSlides}>
            {slidesPrint.map(this.renderSlide)}
          </Slider>
        </div>

        <div styleName="collections">
          <h3 styleName="collections-title">Shop By Category</h3>

          {categories.map(this.renderCategory)}
        </div>

        <div styleName="tribeca">
          <div styleName="tribeca-title">TUMI + TRIBECA FILM FESTIVAL PRESENT</div>
          <div styleName="tribeca-title-big">THE 19 DEGREE EXPERIENCE</div>
          <a styleName="tribeca-more" href="https://www.tumi.com/s/tribecafilmfestival">Learn more&nbsp;&gt;</a>
          <img src="https://cdn-media.amplience.com/tumi/images/TFF-ATT_lockup_600x295.png" />
        </div>
      </div>
    );
  }
}

export default HomePage;
