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
  autoplay: false,
  arrows: false,
  slidesToShow: 1,
  slidesToScroll: 1,
  dotsClass: classNames('slick-dots', styles['slider-dots']),
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
        <div styleName="slider-controls">
          <div styleName="arrow-control" onClick={this.prevMention}>
            <img src="/images/about-page/Arrow_Left.svg" />
          </div>
          <div styleName="arrow-control" onClick={this.nextMention}>
            <img src="/images/about-page/Arrow_Right.svg" />
          </div>
        </div>

        <div styleName="quotes-background" />

        <Slider {...mentionsSliderSettings} ref="mentionsSlider" styleName="slider">
          {mentionsSlides}
        </Slider>
      </div>
    );
  }
}
