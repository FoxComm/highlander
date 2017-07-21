/* @flow */

import React from 'react';

import { Link } from 'react-router';

import styles from './perfume.css';

type TileProps = {
};

const tiles = [
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_THE_FILM.png',
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_THE_STORY.png',
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_THE_NOTES_6.png',
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_THE_BOTTLE.png',
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_EXPERTS.png',
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_INTERVIEW.png',
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_BEHIND_THE_SCENES.png',
  'http://www.charlottetilbury.com/us/media/wysiwyg/CTB16-09-SOAD_New-LandingPage-Banners_360_VIDEO.png',
  'http://www.charlottetilbury.com/us//media/wysiwyg/soad_the-look.jpg',
];

const tile = (props: TileProps) => {
  return (
    <div styleName="tile">
      <img src={props.src} />
    </div>
  );
};

const CategoryPerfume = () => {
  return (
    <div styleName="perfume-block">
      <div styleName="perfume">
        <div styleName="title-block">
          <div styleName="title">
            <img src="http://www.charlottetilbury.com/us/media/wysiwyg/scent-of-a-dream/title.png" />
          </div>
          <div styleName="title-controlls">
            <div styleName="left">
              <img src="http://www.charlottetilbury.com/us/media/wysiwyg/scent-of-a-dream/scent-of-a-dream-spinning-optimised.gif" alt="Scent of a dream" />
            </div>
            <div styleName="center">
              <h1 styleName="heading">It's The Key to Attraction</h1>
              <Link styleName="link" to="/c/perfume">
                Shop Now
              </Link>
            </div>
            <div styleName="right">
              <img src="http://www.charlottetilbury.com/us/media/wysiwyg/scent-of-a-dream/scent-of-a-dream-spinning-optimised.gif" alt="Scent of a dream" />
            </div>
          </div>
        </div>
        <div styleName="tiles">
          {tiles.map(t => tile({src: t}))}
        </div>
      </div>
    </div>
  );
};

export default CategoryPerfume;
