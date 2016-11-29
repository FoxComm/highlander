// libs
import React from 'react';
import Slider from 'react-slick';

// styles
import styles from './intro-slider.css';

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

const imageNames = [
  'Busy_Families',
  'Couples',
  'Entertaining',
  'Everyone',
  'Holidays',
  'Singles',
];

const introSliderImages = imageNames.map(img =>
  <div key={img}>
    <div styleName="img-wrap">
      <img styleName="img" src={`/images/about-page/${img}.svg`} />
    </div>
  </div>
);

const IntroSlider = () => (
  <div styleName="slider">
    <Slider {...introSliderSettings}>
      {introSliderImages}
    </Slider>
  </div>
);

export default IntroSlider;
