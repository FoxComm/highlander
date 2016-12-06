/* @flow */

// libs
import React from 'react';
import { Link } from 'react-router';
import scrollTo from 'lib/scroll-to';
import { autobind } from 'core-decorators';

// styles
import styles from './about-page.css';

// components
import IntroSLider from './intro-slider';
import MentionsSlider from './mentions-slider';
import CookingBlock from './cooking-block';

const headerOffset = 86;

export default class AboutPage extends React.Component {

  @autobind
  scrollToIntroBlock() {
    scrollTo(this.refs.introBlock, 1000, headerOffset);
  }

  render() {
    return (
      <div>
        <div styleName="top-header-wrap">
          <div styleName="logo-white" />
          <div styleName="text-wrap">
            <h1 styleName="title">CHEF CRAFTED MEALS DELIVERED TO YOUR DOOR</h1>
          </div>
          <div styleName="scroll-arrow" onClick={this.scrollToIntroBlock} />
        </div>

        <div styleName="intro-block" ref="introBlock">
          <div styleName="content-wrapper">
            <div styleName="intro-title">THE PERFECT GOURMET IS PERFECT FOR</div>
            <IntroSLider />
            <div styleName="intro-text">
              <p>We know that life can be hectic, and time is not always abundant.
                We want dinner to be an enjoyable end to your busy day, not another reason to stress.
                The Perfect Gourmet has been delivering delectable frozen entrées and appetizers nationwide since 2006,
                and ships over 200,000 orders a year.
                Our goal is to deliver delicious, restaurant quality food to your door,
                so that you can spend less time preparing and more time enjoying.
                Whether you’re looking for cocktail party appetizers or a quick and easy dinner,
                we have a variety of delicious foods to help simplify your busy life,
                without sacrificing taste and quality. Every dish is prepared with quality ingredients,
                then flash frozen to lock in flavors and freshness.</p>
              <p>Let us show you why frozen is the new fresh.</p>
            </div>

            <Link to="/best-sellers" styleName="action-link">
              Shop now
            </Link>
          </div>
        </div>

        <CookingBlock />

        <div styleName="fresh-header-wrap">
          <div styleName="content-wrapper">
            <div styleName="text-wrap">
              <div styleName="fresh-description">QUALITY IS OUR PRIORITY</div>
              <h1 styleName="title">THE NEW FRESH</h1>
              <div styleName="fresh-text">
                Our chefs only cook with locally sourced, fresh ingredients.
                Our meals are then flash frozen, ensuring optimal taste, nutrition
                and convenience. So you know you never have to sacrifice quality
                for convenience.
              </div>
            </div>
          </div>
        </div>

        <div styleName="mentions">
          <div styleName="mentions-header-wrap">
            <div styleName="mentions-text-wrap">
              <div styleName="mentions-description">OUR CUSTOMERS ARE SHARING THE LOVE</div>
              <h1 styleName="mentions-title">WHAT PEOPLE ARE SAYING</h1>
            </div>
          </div>
          <MentionsSlider />
        </div>

        <div styleName="gifts-block">
          <div styleName="gifts-header-wrap">
            <div styleName="gifts-wrap">
              <div styleName="gifts-description">THE PERFECT GOURMET</div>
              <h1 styleName="gifts-title">MAKES THE PERFECT GIFT</h1>
              <div styleName="gifts-text">
                Give the gift of delicious food for any occasion
                with The Perfect Gourmet digital gift cards.
              </div>
              <Link to="/gift-cards" styleName="shop-gift-cards-link">
                Shop gift cards
              </Link>
            </div>
          </div>
        </div>

      </div>
    );
  }
}
