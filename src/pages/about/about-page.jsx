/* @flow */

// libs
import React from 'react';
import Slider from 'react-slick';
import { Link } from 'react-router';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// styles
import styles from './about-page.css';

import mentions from './mentions';

// components
// import Button from 'ui/buttons';

const introSliderSettings = {
  dots: false,
  infinite: true,
  slidesToShow: 1,
  slidesToScroll: 1,
  autoplay: true,
  autoplaySpeed: 2000,
  centerMode: false,
  arrows: false,
};

const introSliderImages = [
  'Busy_Families',
  'Couples',
  'Entertaining',
  'Everyone',
  'Holidays',
  'Singles',
].map(img =>
  <div key={img}>
    <div styleName="intro-img-wrap">
      <img styleName="intro-img" src={`/images/about-page/${img}.svg`} />
    </div>
  </div>
);

const mentionsSliderSettings = {
  dots: true,
  autoplay: false,
  arrows: false,
  slidesToShow: 1,
  slidesToScroll: 1,
  dotsClass: classNames('slick-dots', styles['mentions-slider-dots']),
};

const mentionsSlides = mentions.map(({ mention, author, location }, i) => {
  return (
    <div key={i}>
      <div styleName="mention-text">{mention}</div>
      <div styleName="mention-description">
        <div>{author}</div>
        <div>{location}</div>
      </div>
    </div>
  );
});

export default class AboutPage extends React.Component {

  @autobind
  nextMention() {
    this.refs.mentionsSlider.slickNext();
  }

  @autobind
  prevMention() {
    this.refs.mentionsSlider.slickPrev();
  }

  render() {
    return (
      <div>
        <div styleName="top-header-wrap">
          <div styleName="text-wrap">
            <h1 styleName="top-title">
              CHEF CRAFTED MEALS DELIVERED TO YOUR DOOR
            </h1>
          </div>
        </div>
        <div styleName="intro-block">
          <div styleName="intro-title">THE PERFECT GOURMET IS PERFECT FOR</div>

          <div styleName="intro-slider">
            <Slider {...introSliderSettings}>
              {introSliderImages}
            </Slider>
          </div>

          <div styleName="intro-text">
            At The Perfect Gourmet, we believe food should be as
            delicious, healthy, and convenient as possible without
            requiring a membership. Whether you are planning a dinner
            party, entertaining last minute guests, or just want to
            treat yourself to a fabulous meal, we make gourmet dining
            easy and delicious.
          </div>

          <Link to="/best-sellers" styleName="action-link">
            Shop best sellers
          </Link>
        </div>

        <div styleName="mentions">
          <div styleName="mentions-header-wrap">
            <div styleName="text-wrap">
              <div styleName="mentions-description">OUR CUSTOMERS ARE SHARING THE LOVE</div>
              <h1 styleName="mentions-title">
                WHAT PEOPLE ARE SAYING
              </h1>
            </div>
          </div>
          <div styleName="mentions-slider-wrapper">
            <div styleName="mentions-slider-controls">
              <div styleName="arrow-control" onClick={this.prevMention}>
                <img src="/images/about-page/Arrow_Left.svg" />
              </div>
              <div styleName="arrow-control" onClick={this.nextMention}>
                <img src="/images/about-page/Arrow_Right.svg" />
              </div>
            </div>

            <Slider {...mentionsSliderSettings} ref="mentionsSlider" styleName="mentions-slider">
              {mentionsSlides}
            </Slider>
          </div>
        </div>
      </div>
    );
  }
}
