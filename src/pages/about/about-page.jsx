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
              <p>We believe that every dinner should be an enjoyable end to your busy day, not another reason to stress.
                Our goal is to bring you delicious but affordable food, which you can spend less time preparing and more
                time enjoying.</p>

              <p>We offer a variety of seafood, poultry, beef, and vegetarian options, most around $5 per serving.
                Perfect for young professionals with limited time and kitchenware, big families in need of quick dinner
                solutions, couples looking for relaxing nights in, and everyone in between.</p>

              <p>Every dish starts with fresh, quality ingredients. It&apos;s then flash frozen, so that when you’re ready to
                enjoy, it tastes as fresh and flavorful as if just prepared minutes ago.  Each individual serving is
                conveniently wrapped and ready to cook from frozen-- no thawing required! Cook <i>what</i> you want, <i>when </i>
                you want it. The rest can be stored in your freezer for six months to a year, eliminating waste and facilitating
                meal planning.</p>

              <p>What are you waiting for? Discover for yourself why <i>frozen</i> is the <i>new fresh.</i></p>
            </div>

            <Link to="/best-sellers" styleName="action-link">
              Shop Best Sellers
            </Link>
          </div>
        </div>

        <CookingBlock />

        <div styleName="fresh-header-wrap">
          <div styleName="content-wrapper">
            <div styleName="text-wrap">
              <div styleName="fresh-description">QUALITY IS OUR TOP PRIORITY</div>
              <h1 styleName="title">LET US SHOW YOU WHY FROZEN IS THE NEW FRESH</h1>
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
