/* @flow */

// libs
import React from 'react';
import Slider from 'react-slick';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// styles
import styles from './mentions-slider.css';

import mentions from './mentions';

const mentionsSliderSettings = {
  dots: true,
  arrows: false,
  slidesToShow: 1,
  slidesToScroll: 1,
  dotsClass: classNames('slick-dots', styles['slider-dots']),
  autoplay: true,
  autoplaySpeed: 3500,
};

const mentionsSlides = mentions.map(({ mention, author, location }, i) => {
  return (
    <div key={i}>
      <div styleName="content-wrapper">
        <div styleName="mention-text">
          {mention}
        </div>
        <div styleName="mention-description">
          <div>{author}</div>
          <div>{location}</div>
        </div>
      </div>
    </div>
  );
});


export default class MentionsSlider extends React.Component {
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
      <div styleName="slider-wrapper">
        <Slider {...mentionsSliderSettings} ref="mentionsSlider" styleName="slider">
          {mentionsSlides}
        </Slider>
      </div>
    );
  }
}
