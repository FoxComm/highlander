/* @flow */

// libs
import React from 'react';
import { Link } from 'react-router';

// styles
import styles from './about-page.css';

// components
import IntroSLider from './intro-slider';
import MentionsSlider from './mentions-slider';
import CookingBlock from './cooking-block';


export default class AboutPage extends React.Component {
  render() {
    return (
      <div>
        <div styleName="top-header-wrap">
          <div styleName="text-wrap">
            <h1 styleName="default-sesction-title">CHEF CRAFTED MEALS DELIVERED TO YOUR DOOR</h1>
          </div>
        </div>
        <div styleName="intro-block">
          <div styleName="intro-title">THE PERFECT GOURMET IS PERFECT FOR</div>
          <IntroSLider />
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

        <CookingBlock />

        <div styleName="fresh-block">
          <div styleName="fresh-header-wrap">
            <div styleName="text-wrap">
              <div styleName="fresh-description">QUALITY IS OUR PRIORITY</div>
              <h1 styleName="default-sesction-title">THE NEW FRESH</h1>
              <div styleName="fresh-text">
                Our chefs only cook with locally sourced, fresh ingredients.
                Our meals are then flash frozen, ensuring optimal taste, nutrition
                and convenience. So you know you never have to sacrifice qualify
                for convenience.
              </div>
            </div>
          </div>
        </div>

        <div styleName="mentions">
          <div styleName="mentions-header-wrap">
            <div styleName="text-wrap">
              <div styleName="mentions-description">OUR CUSTOMERS ARE SHARING THE LOVE</div>
              <h1 styleName="mentions-title">WHAT PEOPLE ARE SAYING</h1>
            </div>
          </div>
          <MentionsSlider />
        </div>

        <div styleName="gifts-block">
          <div styleName="gifts-header-wrap">
            <div styleName="gifts-wrap">
              {/*<div styleName="text-wrap">
                <div styleName="gifts-description">THE PERFECT GOURMET</div>
                <h1 styleName="gifts-title">MAKES THE PERFECT GIFT</h1>
              </div>*/}
            </div>
          </div>
        </div>

      </div>
    );
  }
}
