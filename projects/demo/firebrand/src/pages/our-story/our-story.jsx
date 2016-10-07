/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';

import TextBanner from '../../components/banner/text-banner';
import styles from './our-story.css';


class OurStory extends Component {

  get ourStory(): HTMLElement {
    return (
      <div styleName="our-story-banner">
        <div styleName="wrap">
          <TextBanner header="Our Story">
            Some Brand was founded with a rebellious spirit
            and a lofty objective: to offer designer eyewear
            at a revolutionary price.
          </TextBanner>
        </div>
      </div>
    );
  }

  get design(): HTMLElement {
    return (
      <div styleName="design-banner">
        <div styleName="wrap">
          <TextBanner header="Design">
            <p>
              Our in-house design team gathers inspiration from around the
              globe. Each frame is designed in-house from initial sketch to
              prototype testing to final design.
            </p>
            <p>
              Our designers cook up custom pattern variations and features,
              like our never-before-seen triple-gradient lenses.
            </p>
          </TextBanner>
        </div>
      </div>
    );
  }

  get handmade(): HTMLElement {
    return (
      <div styleName="handmade-banner">
        <div styleName="wrap">
          <TextBanner header="100% Hand Made">
            Acetate frames are hand-polished and tumbled
            for at least three days. An imported German
            polishing wax compound helps us achieve the
            highest shine.
          </TextBanner>
        </div>
      </div>
    );
  }

  get materials(): HTMLElement {
    return (
      <div styleName="materials-banner">
        <div styleName="wrap">
          <TextBanner header="Materials">
            <p>
              From premium Japanese titanium to custom single-sheet
              cellulose acetate sourced from a family-run Italian factory, we use
              the best materials for our frames.
            </p>
            <p>
              Acetate frames are hand-polished and tumbled for at least three
              days. An imported German polishing wax compound helps us
              achieve the highest shine.
            </p>
          </TextBanner>
        </div>
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div>
        {this.ourStory}
        {this.design}
        {this.handmade}
        {this.materials}
      </div>
    );
  }
}

export default OurStory;
